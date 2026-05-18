# Buenas prácticas: memoria, ANR y accesibilidad

Documento de evidencia (MISW4203 · TSDC · Vinilos). Recoge las prácticas
aplicadas en el código para **reducir el consumo de memoria**, **evitar ANRs**
y **mejorar la accesibilidad**, con la cita `archivo:línea` que las respalda.

> Complementa a [`corrutinas-y-microoptimizaciones.md`](./corrutinas-y-microoptimizaciones.md),
> que documenta el detalle de corrutinas y microoptimizaciones con evidencia
> antes/después por commit. Aquí se consolidan las tres dimensiones de calidad
> pedidas y se evitan duplicar los fragmentos ya documentados allí.

---

## 1. Consumo de memoria

### 1.1 Coil con caché de memoria y disco acotadas

**Archivo:** `app/src/main/java/com/misw4203/vinilos/VinilosApplication.kt:14-30`

El `ImageLoader` se configura una sola vez vía `ImageLoaderFactory` con
**topes explícitos** en lugar de los defaults de Coil:

```kotlin
override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
    .crossfade(true)
    .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }   // ≤20% del heap
    .diskCache  { DiskCache.Builder().directory(cacheDir.resolve("image_cache"))
                                     .maxSizePercent(0.02).build() }          // ≤2% del disco
    .memoryCachePolicy(CachePolicy.ENABLED)
    .diskCachePolicy(CachePolicy.ENABLED)
    .build()
```

**Impacto:** la caché de bitmaps en RAM queda limitada al 20 % del heap (Coil
descarta LRU antes de presionar al GC); la portada/imagen se decodifica una vez
y se reutiliza desde memoria/disco en vez de re-descargarse y re-decodificarse.

### 1.2 Imágenes con `ContentScale.Crop` y tamaño fijo

**Archivos:** `AlbumCard.kt:56-61`, `MusicianRow.kt:66-71`

`AsyncImage` carga siempre dentro de un `Modifier.size(...)` fijo
(72.dp / 48.dp) y `ContentScale.Crop`. Coil dimensiona el bitmap al tamaño de
destino en vez de mantener en memoria el bitmap a resolución original.

### 1.3 Listas virtualizadas (`LazyColumn`/`LazyRow`) con keys estables

**Archivos:** `AlbumListScreen.kt` (`items(..., key = { it.id })`),
`AlbumDetailScreen.kt` (`itemsIndexed(..., key = { _, t -> "track-${t.id}" })`).

Las listas no acotadas se componen de forma perezosa: solo los ítems visibles
(o cercanos) ocupan nodos de composición. Las `key` estables permiten reutilizar
nodos al hacer scroll en lugar de recrearlos. El detalle de la migración
`Column(verticalScroll)` → `LazyColumn` y la corrección de keys globalmente
únicas está en
[`corrutinas-y-microoptimizaciones.md` §2.2](./corrutinas-y-microoptimizaciones.md).

### 1.4 Estado de UI mínimo y de solo lectura

**Archivo:** `MusicianDetailViewModel.kt:33-34` (patrón replicado en todos los VM)

```kotlin
private val _uiState = MutableStateFlow<MusicianDetailUiState>(MusicianDetailUiState.Loading)
val uiState: StateFlow<MusicianDetailUiState> = _uiState.asStateFlow()
```

El estado se expone como `StateFlow` inmutable mediante `asStateFlow()` (un solo
contenedor de estado por pantalla, sin colecciones mutables filtradas a la UI).
Los `UiState` son `sealed` con variantes `data object` para los estados sin
datos (`Loading`/`NotFound`), evitando asignaciones innecesarias.

### 1.5 Persistencia: blobs JSON y `Converters` sin asignaciones por llamada

**Archivo:** `data/local/converter/Converters.kt`

Los campos de lista anidados (tracks, comments, prizes…) se guardan como blob
JSON en una sola fila en lugar de tablas separadas; el `Gson` y los `TypeToken`
son estáticos izados (se construyen una vez, no por cada acceso a la caché) y
`decode` devuelve `emptyList()` ante blobs corruptos. Evidencia antes/después en
[`corrutinas-y-microoptimizaciones.md` §2.1](./corrutinas-y-microoptimizaciones.md).

---

## 2. Evitar ANRs (no bloquear el hilo principal)

### 2.1 Trabajo de I/O fuera del main thread (dispatcher inyectable)

**Archivos:** `di/DispatcherModule.kt`, los 5 `data/repository/*RepositoryImpl.kt`

Toda operación de red/DB se ejecuta bajo `withContext(ioDispatcher)`
(`@IoDispatcher` = `Dispatchers.IO`, inyectado por Hilt). Invariante verificado:
`git grep "withContext(Dispatchers.IO)" -- app/src/main` → 0 coincidencias.
Detalle y commits en
[`corrutinas-y-microoptimizaciones.md` §1.1](./corrutinas-y-microoptimizaciones.md).

### 2.2 DAOs `suspend` (Room nunca bloquea el hilo de UI)

**Archivo:** `data/local/dao/*Dao.kt`

Todas las consultas y escrituras de Room son funciones `suspend` (Room las
ejecuta en su executor de I/O). Ninguna consulta síncrona puede bloquear el
main thread.

### 2.3 Timeouts de red en OkHttp

**Archivo:** `di/NetworkModule.kt:30-34`

```kotlin
OkHttpClient.Builder()
    .addInterceptor(logging)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
```

**Impacto:** una red lenta/caída no deja peticiones colgadas indefinidamente;
falla con `IOException` a los 15 s y el VM la clasifica como error de red en
lugar de mantener la pantalla en *Loading* eterno.

### 2.4 Concurrencia estructurada en `viewModelScope`

**Archivo:** `MusicianDetailViewModel.kt:48-58` (patrón en todos los VM)

Las cargas se lanzan en `viewModelScope.launch`: al destruirse el ViewModel se
cancelan automáticamente (no hay corrutinas huérfanas ni callbacks tras la
muerte de la pantalla). Las llamadas de red independientes se paralelizan con
`async`/`await`/`awaitAll` dentro de `coroutineScope`, reduciendo la latencia
percibida (ver
[§1.2 del doc de corrutinas](./corrutinas-y-microoptimizaciones.md)).

### 2.5 Cancelación del job anterior cuando el arg cambia

**Archivo:** `BandDetailViewModel.kt:24-31`

```kotlin
private var loadJob: Job? = null
fun loadBand(id: Int) {
    loadJob?.cancel()                     // cancela la carga previa
    _uiState.value = BandDetailUiState.Loading
    loadJob = viewModelScope.launch { … }
}
```

**Impacto:** al navegar rápido entre detalles no se acumulan cargas
concurrentes ni se entregan resultados de una pantalla a otra (sin *race*).

### 2.6 Clasificación canónica de errores con relanzo de `CancellationException`

**Archivo:** `presentation/common/DomainResult.kt:21-32`

```kotlin
suspend inline fun <T> runCatchingDomain(block: () -> T): DomainResult<T> =
    try { DomainResult.Ok(block()) }
    catch (e: CancellationException) { throw e }              // SIEMPRE primero
    catch (e: HttpException) { if (e.code() == 404) NotFound else Server }
    catch (e: IOException)   { Network }
    catch (e: Exception)     { Server }
```

**Impacto:** el `try/catch` que antes estaba duplicado y con deriva en cada VM
se centraliza; la `CancellationException` se relanza siempre primero, de modo
que una corrutina cancelada (usuario sale de la pantalla) no queda absorbida y
la jerarquía de corrutinas se desmonta limpiamente. El mismo patrón protege el
write-through best-effort de la caché en los repositorios
([§1.3 del doc de corrutinas](./corrutinas-y-microoptimizaciones.md)).

---

## 3. Accesibilidad

### 3.1 `contentDescription` descriptivo en imágenes y acciones con significado

**Archivo:** `AlbumDetailScreen.kt`

```kotlin
// :214  portada con descripción contextual (incluye el nombre del álbum)
contentDescription = stringResource(R.string.cd_album_cover_of, album.name)
// :415  botón de añadir track
contentDescription = stringResource(R.string.add_track_button_album_detail)
// :363 / :641  navegación atrás
contentDescription = stringResource(R.string.action_back)
```

TalkBack anuncia qué representa cada imagen y acción, no “imagen sin etiqueta”.

### 3.2 `contentDescription = null` en imágenes/iconos decorativos

**Archivos:** `AlbumCard.kt:58`, `MusicianRow.kt:68`, `AlbumCard.kt:99` (chevron)

La miniatura dentro de una fila clicable y el chevron de navegación se marcan
explícitamente como decorativos (`null`): el lector de pantalla no los anuncia
porque la fila ya transmite el contenido, evitando verborrea redundante.

### 3.3 Agrupar nodos con `mergeDescendants` + `role`

**Archivos:** `AlbumCard.kt:46`, `AlbumDetailScreen.kt:436`, `:519`

```kotlin
// AlbumCard: toda la tarjeta es un solo destino "Botón"
.semantics(mergeDescendants = true) { role = Role.Button }
// AlbumDetailScreen: la fila de track se lee como una unidad
.semantics(mergeDescendants = true) { contentDescription = rowDesc }
```

**Impacto:** TalkBack lee la tarjeta/fila como **un** elemento accionable con
rol “Botón” en lugar de tabular por cada `Text`/`Icon` interno.

### 3.4 Encabezados de sección marcados como `heading()`

**Archivo:** `AlbumDetailScreen.kt:232` (título), `:603` (cabeceras de sección)

```kotlin
modifier = Modifier.semantics { heading() }
```

**Impacto:** el usuario de lector de pantalla puede navegar por encabezados
(gesto de saltar entre headings) en vez de recorrer todo el contenido.

### 3.5 Áreas táctiles ≥ 48 dp

**Archivo:** `MusicianRow.kt:94-114`

El botón de añadir (y su estado *cargando*) se envuelven en
`Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` aunque el icono visible
sea de 20–24 dp, cumpliendo el mínimo de objetivo táctil recomendado. El icono
interno es decorativo (`null`) y la semántica accionable vive en el `Box`
contenedor con `role = Role.Button` + `contentDescription`.

### 3.6 Todo el texto visible vía `stringResource` / `pluralStringResource`

**Archivos:** `AlbumListScreen.kt:61` (`stringResource`), `:80-84`
(`pluralStringResource` para el contador de la lista).

Ningún literal de UI está embebido en el código: títulos, contadores y
descripciones salen de recursos, lo que habilita traducción y plurales
correctos y mantiene consistencia para los lectores de pantalla.

---

## 4. Verificación

| Dimensión | Práctica | Evidencia |
|---|---|---|
| Memoria | Caché Coil acotada (20% RAM / 2% disco) | `VinilosApplication.kt:14-30` |
| Memoria | Imágenes con tamaño fijo + `Crop` | `AlbumCard.kt:56-61`, `MusicianRow.kt:66-71` |
| Memoria | Listas virtualizadas con keys estables | `AlbumListScreen.kt`, `AlbumDetailScreen.kt` |
| Memoria | Estado `StateFlow` inmutable + `sealed`/`data object` | `MusicianDetailViewModel.kt:33-34` |
| ANR | I/O en `@IoDispatcher`; DAOs `suspend` | `*RepositoryImpl.kt`, `*Dao.kt` |
| ANR | Timeouts OkHttp 15 s | `NetworkModule.kt:30-34` |
| ANR | `viewModelScope` + cancelar job previo | `BandDetailViewModel.kt:24-31` |
| ANR | Relanzo de `CancellationException` | `DomainResult.kt:21-32` |
| A11y | `contentDescription` significativo / `null` decorativo | `AlbumDetailScreen.kt:214`, `AlbumCard.kt:58` |
| A11y | `mergeDescendants` + `role` + `heading()` | `AlbumCard.kt:46`, `AlbumDetailScreen.kt:232,436` |
| A11y | Objetivo táctil ≥ 48 dp | `MusicianRow.kt:94-114` |
| A11y | Texto vía recursos / plurales | `AlbumListScreen.kt:61,80-84` |

**Verificación en dispositivo:** estas rutas se ejercitaron en el flujo E2E
híbrido (Pixel_8 / API 34) — suite instrumentada en verde y smoke manual del
APK release shrinkado cargando datos reales end-to-end. La suite de tests
instrumentados incluye chequeos de accesibilidad sobre nodos del árbol
semántico (ver `app/src/androidTest/` y `docs/espresso/`).

---

*Generado a partir de la revisión del código de `feature/mejoras-fase-1`. Cada
fragmento corresponde al estado actual del archivo citado.*
