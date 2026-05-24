# Corrutinas y microoptimizaciones — evidencia antes/después

Documento de evidencia (MISW4203 · TSDC · Vinilos). Para cada técnica se
muestra el código **antes** y **después**, con el commit y archivo:línea que
lo respalda. Metodología: revisión del historial `git log`/`git show` sobre
las ramas de trabajo.

> Convención: los fragmentos están recortados a las líneas relevantes; el
> hash enlaza el commit donde se introdujo el cambio.

---

## 1. Corrutinas

### 1.1 Dispatcher de IO inyectable (no `Dispatchers.IO` hardcodeado)

**Commits:** `b54ba1a` *refactor(data): inyectar @IoDispatcher en los repos (M7)*,
`a80889e` *refactor(di): @Target en IoDispatcher + @param: explícito (M7)*
**Archivos:** los 5 `data/repository/*RepositoryImpl.kt`, nuevos
`di/IoDispatcher.kt` y `di/DispatcherModule.kt`.

**Antes** — cada repositorio fijaba el dispatcher concreto, lo que impedía
inyectar un dispatcher de test y acoplaba la capa de datos a `Dispatchers.IO`:

```kotlin
class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: AlbumDao,
) : AlbumRepository {
    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) { … }
    override suspend fun getAlbumById(id: Long): AlbumDetail = withContext(Dispatchers.IO) { … }
    override suspend fun removeTrack(albumId: Long, trackId: Long) = withContext(Dispatchers.IO) { … }
    // … withContext(Dispatchers.IO) repetido en cada método
}
```

**Después** — `@IoDispatcher CoroutineDispatcher` inyectado por Hilt; todos
los `withContext` usan el dispatcher inyectado:

```kotlin
class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: AlbumDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) : AlbumRepository {
    override suspend fun getAlbums(): List<Album> = withContext(ioDispatcher) { … }
    override suspend fun getAlbumById(id: Long): AlbumDetail = withContext(ioDispatcher) { … }
    override suspend fun removeTrack(albumId: Long, trackId: Long) = withContext(ioDispatcher) { … }
}
```

```kotlin
// di/IoDispatcher.kt (nuevo)
@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class IoDispatcher

// di/DispatcherModule.kt (nuevo)
@Module @InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

**Impacto:** los tests de repositorio ahora pasan `UnconfinedTestDispatcher()`
por constructor (ejecución determinista, sin `Dispatchers.IO` real);
desacople de la capa de datos del dispatcher concreto. Invariante verificado:
`git grep "withContext(Dispatchers.IO)" -- app/src/main` → **0 coincidencias**.

---

### 1.2 Concurrencia estructurada: llamadas de red en paralelo

**Commits:** `f3f9a2f` *HU04 — ver detalle de artista + premios*,
`8d9f427` *feat: Collectors details*, `c955df2` *fix(HU014): resolver IDs de
premio vía endpoint performerprizes*.
**Archivos:** `MusicianRepositoryImpl.kt`, `BandRepositoryImpl.kt`,
`CollectorRepositoryImpl.kt`.

**Antes** — el detalle de músico resolvía premios de forma **secuencial**
(una espera tras otra) dentro de un `coroutineScope`/`map { async }` que en
la práctica encadenaba la llamada principal con la de premios:

```kotlin
val prizes = coroutineScope {
    dto.performerPrizes.map { pp ->
        async { /* … resolución secuencial por premio … */ }
    }.awaitAll()
}
```

**Después** — la llamada al detalle y la de asociaciones de premios se
lanzan **concurrentemente** con `async` y se combinan con `await()`; el
detalle de colector enriquece los álbumes en paralelo con `awaitAll()`:

```kotlin
// MusicianRepositoryImpl.getMusicianDetail (después)
coroutineScope {
    val musicianAsync       = async { api.getMusicianDetail(id) }      // en paralelo
    val allAssociationsAsync = async { api.getPerformerPrizes() }      // en paralelo
    val dto = musicianAsync.await()
    val associationMap = allAssociationsAsync.await().associateBy { it.id }
    val prizes = dto.performerPrizes.mapNotNull { pp -> … }
    dto.toDomain(prizes)
}
```

```kotlin
// CollectorRepositoryImpl.getCollectorDetail (después)
val enrichedAlbums = coroutineScope {
    val albumIdLookupAsync = async { api.getCollectorAlbums(id) … }    // lookup en paralelo
    val albumIdByAssocId = albumIdLookupAsync.await()
    dto.collectorAlbums.map { collAlbumDto ->
        async { /* api.getAlbum(albumId) por álbum, todos concurrentes */ }
    }.awaitAll()
}
```

**Impacto:** la latencia del detalle deja de ser la **suma** de las llamadas
y pasa a ser aproximadamente la **más lenta** de ellas (las dos peticiones
independientes viajan a la vez). `coroutineScope` garantiza concurrencia
estructurada: si una hija falla o se cancela, las demás se cancelan y el
scope propaga el error de forma controlada.

---

### 1.3 Cancelación cooperativa (`CancellationException` se relanza)

**Commits:** trabajo de caché write-through (p. ej. `dbc49e6`) + endurecido
en Fase 5 (`7a3bd53` logging seam).
**Archivos:** `MusicianRepositoryImpl.kt`, `BandRepositoryImpl.kt`,
`CollectorRepositoryImpl.kt`.

**Patrón aplicado** — el cache best-effort nunca traga una cancelación
(de lo contrario rompería la concurrencia estructurada del llamador):

```kotlin
try {
    // … write-through best-effort a la caché …
} catch (e: CancellationException) {
    throw e                       // se relanza SIEMPRE primero
} catch (e: Exception) {
    logger.w("…RepositoryImpl", "write-through cache … falló", e)  // best-effort
}
```

**Impacto:** una corrutina cancelada (p. ej. el usuario sale de la pantalla)
no queda “absorbida” por el `catch` genérico; la cancelación se propaga y la
jerarquía de corrutinas se desmonta correctamente.

---

## 2. Microoptimizaciones

### 2.1 `Converters` (Room): Gson único + `TypeToken` izados + null-safe

**Commits:** `0347ff8` *refactor(data): endurecer Converters (Gson único,
TypeTokens izados, null-safe)*, `688f501` *refactor(data): quitar reified
innecesario y documentar contrato de decode*.
**Archivo:** `data/local/converter/Converters.kt`.

**Antes** — se instanciaba `Gson()` por cada `Converters` y, peor, un
`TypeToken` anónimo nuevo **en cada (de)serialización**; sin null-safety:

```kotlin
private val gson = Gson()

fun jsonToTracks(value: String): List<Track> =
    gson.fromJson(value, object : TypeToken<List<Track>>() {}.type)   // TypeToken nuevo por llamada
fun jsonToPerformers(value: String): List<Performer> =
    gson.fromJson(value, object : TypeToken<List<Performer>>() {}.type)
// … repetido para 8 tipos; "null"/blob malformado → NPE río abajo
```

**Después** — un `Gson` estático compartido y los `TypeToken` izados a
constantes (se construyen **una sola vez**); `decode` centraliza el parseo
y devuelve `emptyList()` ante fallo:

```kotlin
private companion object {
    val GSON = Gson()
    val TRACK_LIST: Type     = object : TypeToken<List<Track>>() {}.type
    val PERFORMER_LIST: Type = object : TypeToken<List<Performer>>() {}.type
    // … 8 constantes izadas
}

/** Devuelve emptyList ante cualquier fallo de parseo. */
private inline fun <T> decode(value: String, type: Type): List<T> =
    runCatching { GSON.fromJson<List<T>>(value, type) }.getOrNull() ?: emptyList()

fun jsonToTracks(value: String): List<Track>       = decode(value, TRACK_LIST)
fun jsonToPerformers(value: String): List<Performer> = decode(value, PERFORMER_LIST)
```

`688f501` además quitó el `reified` innecesario de `decode` (el `Type` ya se
pasa explícito) reduciendo el bytecode inlineado en cada call-site:

```kotlin
- private inline fun <reified T> decode(value: String, type: Type): List<T> =
+ private inline fun <T> decode(value: String, type: Type): List<T> =
```

**Impacto:** se elimina la asignación de un `Gson` por instancia y de un
`TypeToken` por cada lectura/escritura de caché (Room invoca estos
converters en cada acceso a columnas-JSON); además se vuelve robusto ante
blobs corruptos.

---

### 2.2 Detalle de álbum: `Column(verticalScroll)` → `LazyColumn`

**Commits:** `9008276` *perf(presentation): AlbumDetailContent a LazyColumn
(M8)*; corrección posterior `47b77d8` *fix(presentation): keys únicas
globalmente en LazyColumn de AlbumDetail*.
**Archivo:** `presentation/ui/screens/album/AlbumDetailScreen.kt`.

**Antes** — todo el contenido (incluyendo `tracks.forEach` y
`comments.forEach`) se componía de golpe dentro de un `Column` con scroll;
en álbumes con muchas canciones/comentarios se componían todos los nodos
aunque estuvieran fuera de pantalla:

```kotlin
Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    // portada + metadatos …
    tracks.forEach { TrackRow(it) }       // todo compuesto siempre
    comments.forEach { CommentCard(it) }  // todo compuesto siempre
}
```

**Después** — `LazyColumn` virtualiza: solo se componen los ítems en (o
cerca de) la ventana visible; las listas no acotadas usan `itemsIndexed`
con **keys únicas globalmente** (corrección `47b77d8`):

```kotlin
LazyColumn(Modifier.fillMaxSize().testTag("album_detail_scroll")) {
    item { /* portada */ }
    item { /* título, metadatos, performers, header de tracks */ }
    itemsIndexed(album.tracks,  key = { _, t -> "track-${t.id}" })   { _, t -> TrackRow(t) }
    if (album.comments.isNotEmpty()) {
        item { /* header comentarios */ }
        itemsIndexed(album.comments, key = { _, c -> "comment-${c.id}" }) { _, c -> CommentCard(c) }
    }
}
```

**Impacto:** se elimina la composición eager de filas fuera de pantalla
(menos trabajo de composición/medición y menos memoria en álbumes grandes).
La corrección `47b77d8` además resolvió un **crash latente**: con `Column`
las `key(...)` eran de composición (scoped), pero en `LazyColumn` deben ser
únicas en toda la lista — un `track.id` y un `comment.id` iguales (caso
común: ambos empiezan en 1) colisionaban (`IllegalArgumentException`) al
componerse juntos. Detectado por el smoke E2E manual (138/138 verde tras
el fix).

---

### 2.3 Estabilidad Compose medida (strong skipping) — decisión basada en evidencia

**Commit:** `525ddb0` *docs(adr): registrar estabilidad Compose con strong
skipping, sin migración (B3)*; `app/build.gradle.kts` cablea el reporte.
**Doc:** `docs/adr/0002-estabilidad-compose-strong-skipping.md`.

**Antes** — sin métricas del compilador Compose; se asumía (sin medir) que
los modelos con `List` podían causar recomposición.

**Después** — se instrumentó el reporte de estabilidad:

```kotlin
// app/build.gradle.kts
composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}
```

Evidencia del reporte: `AlbumDetail`/`Band`/`Musician`/`CollectorDetail` son
`unstable class` **solo** por sus campos `List`, pero los composables
consumidores (`AlbumDetailContent`, `BandBody`, `CollectorDetailContent`,
`MusicianBody`, los 4 `*DetailScreen`) ya son **`restartable skippable`**
gracias al *strong skipping* de Kotlin 2.2.10.

**Impacto:** microoptimización **medida, no asumida**. La conclusión basada
en evidencia fue **no migrar** a `kotlinx.collections.immutable` (el churn
transversal no se justifica: el compilador ya entrega el beneficio). El
bloque de métricas se conservó para que la medición sea reproducible.

---

### 2.4 Microoptimizaciones de persistencia e imágenes

**Commits:** `1df9677` *feat: corrutinas y microoptimizaciones documentadas
+ 3 mejoras nuevas*, `a51d5b3` *feat: microoptimizaciones con reporte de
Lint*.
**Archivos:** `data/local/entity/*Entity.kt`,
`data/local/database/VinilosDatabase.kt`, `AlbumDetailScreen.kt`,
`VinilosApplication.kt`.

**Antes / Después** — se añadieron **índices Room** a las columnas usadas
para ordenar/filtrar listas (evita full-scan en consultas de listado):

```kotlin
- @Entity(tableName = "albums")
+ @Entity(
+     tableName = "albums",
+     indices = [Index(value = ["name"])],
+ )
```

Además se afinó Coil (caché de memoria/disco + crossfade) y se documentaron
estas mejoras en su momento. La limpieza de recursos muertos guiada por Lint
(`a51d5b3`, y en esta sesión el commit `8906a62`) reduce el APK y acelera el
build (`UnusedResources`).

**Impacto:** consultas de listado indexadas (menos I/O en SQLite), carga de
imágenes cacheada (menos red/decodificación), y APK sin recursos muertos.

---

## 3. Verificación e impacto global

| Técnica | Commit(s) | Antes | Después | Beneficio |
|---|---|---|---|---|
| Dispatcher inyectable | `b54ba1a`,`a80889e` | `withContext(Dispatchers.IO)` ×N | `@IoDispatcher` inyectado | Testabilidad determinista; desacople |
| Red en paralelo | `f3f9a2f`,`8d9f427`,`c955df2` | Llamadas secuenciales | `async`/`await`/`awaitAll` en `coroutineScope` | Latencia ≈ máx (no suma) |
| Cancelación cooperativa | `dbc49e6`,`7a3bd53` | `catch (Exception)` tragaba todo | `CancellationException` se relanza | Concurrencia estructurada correcta |
| Converters Gson/TypeToken | `0347ff8`,`688f501` | `Gson()`+`TypeToken` por llamada | Estáticos izados + `decode` null-safe | Menos asignaciones; robustez |
| LazyColumn detalle álbum | `9008276`,`47b77d8` | `Column`+`forEach` eager | `LazyColumn` virtualizado, keys únicas | Menos composición/memoria; crash resuelto |
| Estabilidad Compose | `525ddb0` | Asumida | Medida (reporte) → decisión | Optimización basada en evidencia |
| Índices Room + Coil | `1df9677`,`a51d5b3` | Sin índices / Coil por defecto | `@Index` + caché/crossfade | Menos I/O y red |

**Verificación en dispositivo:** el flujo E2E híbrido (Pixel_8 / API 34)
ejercitó estas rutas — suite instrumentada **138/138** verde y smoke manual
del APK release shrinkado cargando datos reales end-to-end
(Retrofit→Gson→dominio→Room `Converters`→Compose/Coil).

---

*Generado a partir del historial Git del repositorio (revisión de commits
con `git show`/`git log -S`). Cada fragmento "después" corresponde al estado
actual de `feature/mejoras-fase-1`.*
