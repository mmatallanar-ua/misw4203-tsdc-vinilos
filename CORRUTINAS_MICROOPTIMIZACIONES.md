# Corrutinas y microoptimizaciones — Vinilos

Este documento describe el uso de **Kotlin Coroutines** y las **microoptimizaciones** aplicadas en el módulo `app/` del proyecto Vinilos. Cada sección explica el concepto, dónde está implementado (con `archivo:línea`) y por qué.

> Branch: `feature/corrutinas_microoptimizaciones`
> Stack: Kotlin 2.0 · Coroutines 1.9.0 · Jetpack Compose · Room 2.6.1 · Retrofit 2.11 · Coil 2.7 · Hilt 2.52

---

## 1. Corrutinas

### 1.1 ¿Qué son y por qué importan?

Las **corrutinas** son la herramienta canónica de Kotlin para concurrencia cooperativa. Permiten escribir código asíncrono con sintaxis secuencial (`suspend fun`) sin bloquear el hilo principal. En Android son críticas para tres cosas:

1. **Sacar trabajo de I/O del hilo de UI** (red, disco) → la app no se congela.
2. **Vincular el ciclo de vida** (a `viewModelScope`, `lifecycleScope`) → cuando la pantalla muere, el trabajo se cancela automáticamente y no hay leaks.
3. **Componer concurrencia estructurada** (`coroutineScope`, `async/awaitAll`) → si una hija falla, todas se cancelan; no hay tareas huérfanas.

### 1.2 Patrones aplicados

#### `viewModelScope.launch` + `StateFlow` con clasificación de excepciones

Todos los ViewModels disparan trabajo en `viewModelScope` y exponen el resultado vía `StateFlow<UiState>`. La clasificación de excepciones está estandarizada:

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/CollectorListViewModel.kt:32
private fun load() {
    _uiState.value = CollectorListUiState.Loading
    viewModelScope.launch {
        _uiState.value = try {
            val collectors = getCollectors()
            if (collectors.isEmpty()) CollectorListUiState.Empty
            else CollectorListUiState.Success(collectors)
        } catch (e: CancellationException) { throw e }   // (1)
        catch (e: IOException)   { CollectorListUiState.Error(isNetworkError = true)  }
        catch (e: HttpException) { CollectorListUiState.Error(isNetworkError = false) }
        catch (e: Exception)     { CollectorListUiState.Error(isNetworkError = false) }
    }
}
```

(1) **Re-lanzar `CancellationException`** es esencial para preservar concurrencia estructurada. Si un `catch (e: Exception)` la atrapa, una cancelación legítima (cambio de pantalla, retry rápido) se convierte en falso `Error` y se rompe la cadena de cancelación del scope.

Mismo patrón en: `AlbumListViewModel.kt:35`, `AlbumDetailViewModel.kt:39`, `MusicianListViewModel.kt:34`, `MusicianDetailViewModel.kt:40`, `CollectorDetailViewModel.kt:48`, `CreateAlbumViewModel.kt:28`, `AddTrackViewModel.kt:50`, `AddCommentViewModel.kt:54`.

#### Cancelación manual de la corrutina previa cuando cambia el argumento

Cuando el usuario navega rápidamente entre detalles distintos, el ViewModel se reutiliza y el último load gana arbitrariamente. Para evitarlo, `MusicianDetailViewModel` mantiene un `Job` y lo cancela antes de relanzar:

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/MusicianDetailViewModel.kt:33
private var loadJob: Job? = null
private var currentId: Int? = null

fun loadMusician(id: Int) {
    currentId = id
    loadJob?.cancel()                                  // (1)
    _uiState.value = MusicianDetailUiState.Loading
    loadJob = viewModelScope.launch { /* ... */ }
}
```

(1) `viewModelScope` cancela todo al `onCleared()`, pero **no** cancela trabajos previos cuando el argumento cambia. Sin esta línea, el último resultado en resolver "gana" — comportamiento no determinista.

#### `withContext(Dispatchers.IO)` en repositorios

Retrofit ya hace I/O en su propio executor, pero los repositorios envuelven todo en `Dispatchers.IO` para asegurar que el trabajo de mapping (DTO → domain), las llamadas a Room y los caminos de fallback no toquen el hilo principal. Es una garantía de capa, independiente del cliente HTTP.

```kotlin
// app/src/main/java/com/misw4203/vinilos/data/repository/AlbumRepositoryImpl.kt:29
override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
    try {
        val albums = api.getAlbums().map { it.toAlbum() }
        dao.replaceAlbums(albums.map { AlbumEntity.fromDomain(it) })
        albums
    } catch (e: IOException) {
        val cached = dao.getAll()
        if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
    }
}
```

Aplicado en los 9 métodos de `AlbumRepositoryImpl`, los 2 de `MusicianRepositoryImpl` y los 2 de `CollectorRepositoryImpl`.

#### `coroutineScope { async { … }.awaitAll() }` para paralelizar llamadas independientes

El detalle del músico requiere N llamadas a `/prizes/{id}` (una por premio). En vez de iterarlas secuencialmente (`forEach { api.getPrizeDetail(it) }`), se lanzan todas en paralelo y se espera el conjunto:

```kotlin
// app/src/main/java/com/misw4203/vinilos/data/repository/MusicianRepositoryImpl.kt:41
val prizes = coroutineScope {
    dto.performerPrizes.map { pp ->
        async {
            val prizeDto = api.getPrizeDetail(pp.id)
            MusicianPrize(
                id = prizeDto.id,
                name = prizeDto.name,
                organization = prizeDto.organization,
                description = prizeDto.description,
                premiationDate = pp.premiationDate,
            )
        }
    }.awaitAll()
}
```

Para un músico con 5 premios y latencia de 200 ms por request, baja de **~1 s** (secuencial) a **~200 ms** (paralelo). `coroutineScope` garantiza que si una llamada falla, las demás se cancelan — no hay requests huérfanos.

Mismo patrón en `CollectorRepositoryImpl.kt:48` para enriquecer cada `collectorAlbum` con su `Album` completo desde `/albums/{id}`.

#### `collectAsStateWithLifecycle` (lifecycle-aware Flow collection)

En vez de `collectAsState()`, las pantallas usan la variante lifecycle-aware. Cuando la actividad pasa a STOPPED, la colección del flow se pausa automáticamente — no se procesan emisiones que la UI no va a ver, ahorrando CPU y batería.

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/album/AlbumListScreen.kt:50
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

Aplicado en todas las pantallas: `AlbumListScreen`, `AlbumDetailScreen`, `MusicianListScreen`, `MusicianDetailScreen`, `CollectorListScreen`, `CollectorDetailScreen`, `CreateAlbumScreen`, `AddTrackScreen`, `AddCommentScreen`.

#### `LaunchedEffect` + `SavedStateHandle` para refresh entre destinos

Tras un POST exitoso (HU08, HU09), el detalle del álbum debe refrescarse. Se usa un flag en el `SavedStateHandle` del back-stack, observado vía `LaunchedEffect`:

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/album/AlbumDetailScreen.kt:79
LaunchedEffect(refreshKey) {
    if (refreshKey) {
        viewModel.retry()
        onRefreshHandled()
    }
}
```

`LaunchedEffect` se cancela y relanza cuando cambia la `key`. La corrutina hereda el scope del Composable, así que al salir de pantalla se cancela. Es la primitiva canónica para side effects con vida atada al composable.

#### `SavedStateHandle` como puente entre navegación y ViewModel

Hilt inyecta `SavedStateHandle` automáticamente, y los ViewModels leen los argumentos de navegación de allí. Eso evita pasar `id` por parámetro y sobrevive a recreación de proceso:

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/CollectorDetailViewModel.kt:27
@HiltViewModel
class CollectorDetailViewModel @Inject constructor(
    private val getCollectorDetail: GetCollectorDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val collectorId: Int = savedStateHandle[Destinations.CollectorDetailArg]
        ?: error("collectorId required")
    /* ... */
}
```

Mismo patrón en `AlbumDetailViewModel`, `AddTrackViewModel`, `AddCommentViewModel`.

---

## 2. Microoptimizaciones

### 2.1 Compose

#### `key` en `LazyColumn` / `LazyRow`

Sin `key`, Compose identifica los items por su posición. Si el orden cambia (insertar al inicio, eliminar uno) recompone TODOS. Con `key = { it.id }`, Compose emparejaa cada item con su Composable previo y solo recompone los que realmente cambiaron.

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/collector/CollectorListScreen.kt:70
items(state.collectors, key = { it.id }) { collector ->
    CollectorCard(/* ... */)
}
```

Aplicado también en `AlbumListScreen.kt:88`, `MusicianListScreen.kt:70`, `AlbumDetailScreen.kt:342` (LazyRow de performers), `CollectorDetailScreen.kt:254` (LazyRow de albums) y `:327` (LazyRow de performers).

#### `key()` sobre items dentro de `Column`/`forEach` (agregado en este sprint)

La sección de tracks y comentarios en `AlbumDetailScreen` usa `forEachIndexed` dentro de un `Column` (no `LazyColumn`, porque el contenedor ya es `verticalScroll` global). Sin `key`, Compose no tiene forma de mantener identidad estable de cada `TrackRow`/`CommentCard` cuando se inserta uno nuevo (HU08, HU09). Se envuelve cada item con la función `key()`:

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/album/AlbumDetailScreen.kt:299
tracks.forEachIndexed { index, track ->
    key(track.id) {
        TrackRow(index = index + 1, track = track)
        Spacer(Modifier.height(8.dp))
    }
}

// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/album/AlbumDetailScreen.kt:381
comments.forEach { comment ->
    key(comment.id) {
        CommentCard(comment)
    }
}
```

Beneficio concreto: cuando se agrega un track vía HU08, solo se inserta el nodo nuevo; los anteriores conservan su slot table y no se recomponen.

#### `rememberLazyListState()`

Preserva el scroll position cuando recompone. Aplicado en `CollectorListScreen.kt:39`.

#### Constantes extraídas a top-level

Valores como `CoverHeight`, `CardOverlap`, `CardRadius` están extraídos a `private val` en el archivo, no en el cuerpo del Composable. Evita re-instanciar el `Dp` en cada recomposición.

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/album/AlbumDetailScreen.kt:62
private val CoverHeight = 300.dp
private val CardOverlap = 32.dp
private val CardRadius = 24.dp
```

### 2.2 Room

#### `@Transaction` para operaciones compuestas

`replaceX = clear() + upsertAll()` debe ser atómico. Si la app crashea entre el `DELETE` y el `INSERT`, no queremos perder la caché. Room serializa la operación en una sola transacción SQLite:

```kotlin
// app/src/main/java/com/misw4203/vinilos/data/local/dao/AlbumDao.kt:22
@Transaction
suspend fun replaceAlbums(albums: List<AlbumEntity>) {
    clear()
    upsertAll(albums)
}
```

Mismo patrón en `MusicianDao.kt:22` y `CollectorDao.kt:22`.

#### `@Upsert` en lugar de `@Insert(OnConflict = REPLACE)`

`@Upsert` (Room 2.5+) maneja insert/update en una sola anotación y genera SQL más eficiente que `INSERT OR REPLACE` (este último elimina y reinserta, perdiendo `rowid`).

#### `@Index` sobre columnas usadas en `ORDER BY` (agregado en este sprint)

Las queries `SELECT * FROM albums ORDER BY name ASC`, `… FROM musicians …` y `… FROM collectors …` se ejecutan en cada `getAll()` del repositorio. Sin índice, SQLite hace **full scan + sort en memoria** (O(N log N)). Con índice sobre `name`, lee la tabla en orden de índice (O(N), sin sort).

```kotlin
// app/src/main/java/com/misw4203/vinilos/data/local/entity/AlbumEntity.kt:9
@Entity(
    tableName = "albums",
    indices = [Index(value = ["name"])],
)
data class AlbumEntity(/* ... */)
```

Aplicado también en `MusicianListEntity.kt:9` y `CollectorEntity.kt:9`. Schema cambió → `VinilosDatabase.version` subió de 3 → 4. Como la DB usa `fallbackToDestructiveMigration()` (la caché es descartable), Room recrea la base sin necesidad de escribir `Migration`.

Costo: el índice ocupa espacio adicional en disco (proporcional a `2 × N` strings de nombres) y cada `INSERT/UPDATE/DELETE` debe actualizar el índice. Para volúmenes pequeños (<10k filas) es despreciable; el beneficio en lecturas ordenadas supera ampliamente el costo.

#### `fallbackToDestructiveMigration()`

La caché Room es **descartable** (los datos viven en el backend). Si el schema cambia, en lugar de escribir migraciones complejas, se recrea la DB. La pérdida es invisible: el siguiente `GET` la repuebla.

```kotlin
// app/src/main/java/com/misw4203/vinilos/di/DatabaseModule.kt:23
Room.databaseBuilder(context, VinilosDatabase::class.java, "vinilos.db")
    .fallbackToDestructiveMigration()
    .build()
```

#### Listas anidadas como JSON via `TypeConverters`

Los detalles tienen listas (tracks, performers, comments, collectorAlbums, prizes). En vez de normalizar cada una en su propia tabla con relaciones, se serializan a JSON con Gson y se guardan como `TEXT`:

```kotlin
// app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt
@TypeConverter fun fromTrackList(value: List<Track>): String = gson.toJson(value)
@TypeConverter fun toTrackList(value: String): List<Track> = gson.fromJson(value, listType)
```

Trade-off explícito: no se puede consultar por campos anidados (`WHERE track.name = …`) pero la caché no necesita esas queries — siempre se lee un detalle completo por id. Resultado: schema simple, menos tablas, menos joins.

### 2.3 Retrofit / OkHttp

#### `HttpLoggingInterceptor` solo en debug

```kotlin
// app/src/main/java/com/misw4203/vinilos/di/NetworkModule.kt:23
val logging = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
}
```

Razones:
- En release, loggear el body de cada request/response es **CPU + I/O desperdiciado** y puede filtrar datos sensibles a `logcat`.
- El compilador elimina (DCE) la rama muerta.

#### Timeouts conservadores (15s)

```kotlin
// app/src/main/java/com/misw4203/vinilos/di/NetworkModule.kt:32
.connectTimeout(15, TimeUnit.SECONDS)
.readTimeout(15, TimeUnit.SECONDS)
```

15 s es suficiente para 3G lento y evita que un servidor caído deje requests pendientes indefinidamente. Sin timeout explícito, OkHttp usa 10 s por defecto pero queremos consistencia.

### 2.4 Coil (carga de imágenes)

#### `ImageLoader` global con caché en memoria + disco + crossfade (agregado en este sprint)

Por defecto cada `AsyncImage` crea su request sin coordinar caché. Se proveyó un `ImageLoader` global vía `ImageLoaderFactory` en la `Application`:

```kotlin
// app/src/main/java/com/misw4203/vinilos/VinilosApplication.kt:11
@HiltAndroidApp
class VinilosApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)                                    // (1)
        .memoryCache {
            MemoryCache.Builder(this).maxSizePercent(0.20).build()   // (2)
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02).build()               // (3)
        }
        .respectCacheHeaders(false)                         // (4)
        .build()
}
```

(1) **Crossfade** suaviza el "flash" cuando la imagen llega: 100 ms más agradables a la vista, sin tartamudeo.
(2) **MemoryCache** = 20% de la RAM disponible para la app. Hits en memoria son instantáneos; ideal cuando volvés a una pantalla recién visitada.
(3) **DiskCache** = 2% del almacenamiento del cache dir. Sobrevive a kill del proceso. Las portadas de álbumes se reusan entre sesiones.
(4) **`respectCacheHeaders(false)`**: el backend del curso no envía `Cache-Control` apropiados; se confía en la heurística de Coil para no re-bajar la misma URL.

Beneficio observable: scroll por la lista de álbumes ya cacheada → 0 requests de red, las portadas aparecen instantáneamente.

#### `contentScale = ContentScale.Crop` y `Modifier.size()` constraint

Coil necesita conocer las dimensiones objetivo para downsamplear el bitmap antes de decodificarlo. Cargar una portada de 1024×1024 en un slot de 72×72 sin resize quema **memoria 200×**.

```kotlin
// app/src/main/java/com/misw4203/vinilos/presentation/ui/components/AlbumCard.kt
AsyncImage(
    model = album.coverUrl,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
)
```

Aplicado en `AlbumCard`, `MusicianCard`, `PerformerChip` (`AlbumDetailScreen.kt:358`) y `CollectorDetailScreen` performers/albums.

---

## 3. Resumen de optimizaciones agregadas en este sprint

| # | Optimización | Archivo(s) | Beneficio |
|---|---|---|---|
| 1 | `@Index(value = ["name"])` en `AlbumEntity`, `MusicianListEntity`, `CollectorEntity` | `data/local/entity/*Entity.kt` | `ORDER BY name ASC` evita full-table sort. DB v3 → v4 con destructive migration. |
| 2 | `key(track.id)` y `key(comment.id)` sobre items dentro de `Column.forEach` | `presentation/ui/screens/album/AlbumDetailScreen.kt` | Identidad estable: insertar un track/comment via HU08/HU09 no recompone los anteriores. |
| 3 | `ImageLoader` global con `crossfade(true)` + memory/disk cache | `VinilosApplication.kt` | Carga de imágenes uniforme con caché de 2 niveles + transición visual. Aplica a todos los `AsyncImage`. |
| 4 | **Write-through cache** en `createAlbum`, `addTrack`, `addComment` | `AlbumRepositoryImpl.kt`, `AlbumDao.kt` | El POST actualiza la caché local en el mismo paso (insert para listas, read-modify-write para detalles). Evita lecturas stale entre el POST y el siguiente refetch, y mantiene consistencia offline tras escribir online. |

Verificación:
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (5 tests nuevos para write-through cache, 0 failures)

---

## 4. Cómo medir / verificar

### Concurrencia y corrutinas

- **Inspección de transitions**: en Android Studio, ver el panel "Profiler → CPU" durante una operación; las llamadas paralelas vía `async/awaitAll` deben mostrar threads concurrentes en `OkHttp Dispatcher`.
- **Cancelación correcta**: rotar el dispositivo durante un load lento; con `viewModelScope` el log debería mostrar `JobCancellationException` y no `Error` en la UI.
- **Tests unitarios**: la suite verifica `Loading → Success/Error/Empty` para cada VM con `Turbine` + `MainDispatcherRule`.

### Compose

- **Recomposition counts**: añadir `Modifier.recompose()` (Layout Inspector) sobre `TrackRow` y verificar que solo se cuenta 1 cuando se inserta un nuevo track (con `key`).
- **Frame rate**: `adb shell dumpsys gfxinfo com.misw4203.vinilos framestats` antes y después; lista de 100 álbumes con scroll debería mantenerse en 60fps.

### Room

- **Logging de queries**: habilitar `setQueryCallback` temporalmente en debug y observar el `EXPLAIN QUERY PLAN` de los `SELECT … ORDER BY name`. Con índice debe decir `USING INDEX` en vez de `USE TEMP B-TREE FOR ORDER BY`.

### Coil

- **Cache hit rate**: pintar la lista de álbumes dos veces seguidas; el segundo render no debería disparar requests (verificable con `HttpLoggingInterceptor.Level.BASIC` en debug). Hit en memoria es <1 ms; hit en disco ~10 ms; miss de red ~200-500 ms.
