# HU012 — Agregar músicos a una banda — Diseño técnico

| Campo | Valor |
|---|---|
| **Fecha** | 2026-05-12 |
| **Rama** | `feature/HU12_Agregar_Musicos_Banda` |
| **HU origen** | HU012 — Agregar músicos a una banda |
| **Estado** | Aprobado por usuario (brainstorming completado) |
| **Próximo paso** | Plan de implementación (`writing-plans`) |

## 1. Resumen ejecutivo

Esta HU permite a un coleccionista agregar músicos existentes a una banda del catálogo, manteniendo actualizada la composición de la agrupación para todos los usuarios.

El proyecto Android actualmente **no tiene el concepto de Banda** en su dominio. Esta HU introduce:

1. El dominio `Band` alineado al modelo del backend (hermana de `Musician` bajo `Performer`, herencia de tabla única).
2. Cuatro endpoints nuevos en `VinilosApiService` (`GET /bands`, `GET /bands/{id}`, `GET /bands/{id}/musicians`, `POST /bands/{bandId}/musicians/{musicianId}`).
3. Tres pantallas nuevas: lista de bandas (como sub-tab dentro de "Artistas"), detalle de banda y "Agregar músicos".
4. Cobertura de pruebas unitarias, instrumentadas y E2E para los CA01–CA09 (excepto CA08, fuera de scope sin auth).

## 2. Decisiones tomadas durante el brainstorming

| # | Decisión | Razón |
|---|---|---|
| D1 | El backend expone `/bands` y `/musicians` como recursos hermanos bajo `Performer`. | Confirmado por el usuario con la documentación del backend. |
| D2 | Scope strict de la HU: **sin** delete de integrantes, **sin** roles/instrumentos. | El diseño visual local contradice el spec funcional; gana el spec. |
| D3 | Estilo visual **Material3** (consistente con la app), no el "Brutalist Draftsman" del mockup. | Consistencia con el resto de pantallas existentes. |
| D4 | Pack completo de pantallas: lista de bandas + detalle de banda + agregar músicos. | Para que el flujo cumpla CA01–CA09 end-to-end. |
| D5 | Tab "Artistas" se convierte en hub con **sub-tabs internas** ("Músicos" / "Bandas"). | Mantiene el bottom-nav existente intacto (3 tabs) y agrupa por afinidad de dominio. |
| D6 | Arquitectura: `Performer` como **sealed interface común**, con `Musician` y `Band` como implementaciones. | Refleja el modelo del backend; permite refactor a un `PerformerHeader` compartido. |

### Asunciones por defecto (no preguntadas explícitamente)

- **CA08 — restricción de rol:** la app actualmente no tiene auth (`DefaultCollectorId = 100`). Asumimos siempre coleccionista → la opción "Agregar músicos" siempre se ve. Documentado como deuda técnica.
- **CA09 — estado vacío:** incluye CTA "Agregar primer integrante" reutilizando el patrón de `EmptyState`.
- **CA05 — búsqueda:** filtro **local** con `debounce(300ms)`, insensible a mayúsculas/minúsculas y tildes (`Normalizer.normalize(NFD)`).
- **Confirmación de éxito:** `Snackbar` (patrón consistente con `AddCommentScreen` / `AddTrackScreen`).

## 3. Arquitectura — dominio y datos

### 3.1 Dominio (`domain/model/`)

```
Performer (sealed interface)
├─ id: Int
├─ name: String
├─ image: String
├─ description: String
├─ birthDate: String
└─ implementaciones:
    ├─ Musician (data class)              [existente, ajustar para implementar Performer]
    │   ├─ albums: List<Album>
    │   └─ prizes: List<MusicianPrize>
    └─ Band (data class)                  [nuevo]
        ├─ members: List<MusicianSummary>
        └─ albums: List<Album>

MusicianSummary (data class)              [existente, se reutiliza para members]
BandSummary (data class)                  [nuevo: id, name, image]
```

**Riesgo R7:** existe ya un archivo `domain/model/Performer.kt`. Antes de implementar, verificar si su contenido permite refactor a interfaz común. Si hay colisión irresoluble, degradar a la opción C (Band aislada con header propio, sin abstracción común), sin cambiar el resto del diseño.

### 3.2 Data — Remote (Retrofit)

DTOs nuevos en `data/remote/dto/`:
- `BandDto` — para lista (`id`, `name`, `image`, `description`).
- `BandDetailDto` — para detalle (campos anteriores + `musicians: List<MusicianDetailDto>` + `albums: List<AlbumDto>`).

Endpoints nuevos en `VinilosApiService`:

```kotlin
@GET("bands")
suspend fun getBands(): List<BandDto>

@GET("bands/{id}")
suspend fun getBandDetail(@Path("id") id: Int): BandDetailDto

@GET("bands/{id}/musicians")
suspend fun getBandMembers(@Path("id") id: Int): List<MusicianDetailDto>

@POST("bands/{bandId}/musicians/{musicianId}")
suspend fun addMusicianToBand(
    @Path("bandId") bandId: Int,
    @Path("musicianId") musicianId: Int,
): retrofit2.Response<Unit>
```

### 3.3 Data — Local (Room)

- `BandEntity` (mismo patrón que `MusicianEntity`): los `members` se serializan como JSON blob via `Converters` (Gson) — consistente con el patrón existente para `tracks`/`performers`/`comments`.
- `BandDao` con `replaceBands(...)` (clear + upsert transaccional), `upsertDetail(...)`, `getBands()`, `getBandDetail(id)`.
- Agregar `BandEntity` a `VinilosDatabase` — bump de versión. `fallbackToDestructiveMigration` ya está activo (la caché es expendable).

### 3.4 Repository

- `domain/repository/BandRepository` (interfaz):
  - `suspend fun getBands(): List<BandSummary>`
  - `suspend fun getBandDetail(id: Int): Band`
  - `suspend fun addMusicianToBand(bandId: Int, musicianId: Int)`
- `data/repository/BandRepositoryImpl` con la estrategia **network-first + fallback a caché** para lecturas (idéntica a `MusicianRepositoryImpl`) y **write-through cache** para `addMusicianToBand` (POST → si el detalle local existe, read-modify-write para agregar el músico a `members`).

Binding en `RepositoryModule` con `@Binds`.

## 4. Capa de presentación

### 4.1 UseCases nuevos (`domain/usecase/`)

- `GetBandsUseCase(repo)` — `suspend operator fun invoke(): List<BandSummary>`
- `GetBandDetailUseCase(repo)` — `suspend operator fun invoke(bandId: Int): Band`
- `AddMusicianToBandUseCase(repo)` — `suspend operator fun invoke(bandId: Int, musicianId: Int)`
- (Reutilizamos `GetMusiciansUseCase` existente para el catálogo en la pantalla de agregar.)

### 4.2 ViewModels nuevos

**`BandListViewModel`** (`@HiltViewModel`)

```kotlin
sealed interface BandListUiState {
    data object Loading : BandListUiState
    data object Empty : BandListUiState
    data class Success(val bands: List<BandSummary>) : BandListUiState
    data class Error(val isNetworkError: Boolean) : BandListUiState
}
```

`init { load() }`, `retry()`. Excepciones clasificadas: `CancellationException` rethrow, `IOException` → red, otros → servidor.

**`BandDetailViewModel`** (`@HiltViewModel`, recibe `SavedStateHandle`)

```kotlin
sealed interface BandDetailUiState {
    data object Loading : BandDetailUiState
    data class Success(val band: Band) : BandDetailUiState
    data object NotFound : BandDetailUiState
    data class Error(val isNetworkError: Boolean) : BandDetailUiState
}
```

- `loadJob` cancelable (patrón `MusicianDetailViewModel`).
- `retry()` re-ejecuta `GetBandDetailUseCase`.
- `RefreshBandDetailKey` en `SavedStateHandle`: cuando vuelve de `AddMusiciansToBandScreen`, refresca el detalle.

**`AddMusiciansToBandViewModel`** (separa form state de submit state)

```kotlin
data class AddMusiciansFormState(
    val query: String = "",
    val allMusicians: List<MusicianSummary> = emptyList(),
    val currentMemberIds: Set<Int> = emptySet(),
    val filteredAvailable: List<MusicianSummary> = emptyList(),
)

sealed interface AddMusiciansUiState {
    data object Loading : AddMusiciansUiState
    data object Ready : AddMusiciansUiState
    data class Adding(val musicianId: Int) : AddMusiciansUiState
    data class Error(val isNetworkError: Boolean, val musicianId: Int?) : AddMusiciansUiState
}
```

**Flujo:**
1. Carga inicial en paralelo (`async`/`awaitAll`): `getMusicians()` + `getBandDetail(bandId)`.
2. `MutableStateFlow<String>("")` para `query`, con `.debounce(300L).distinctUntilChanged()` → recalcula `filteredAvailable` excluyendo `currentMemberIds` y filtrando por nombre normalizado (`Normalizer.normalize(NFD)` + lowercase + remover diacritics).
3. `onAddMusician(id)`: si state ya es `Adding(id)`, ignorar (debounce de doble envío). Si no, `Adding(id)` → POST → al volver:
   - Éxito: agregar `id` a `currentMemberIds`, removerlo de `filteredAvailable`, volver a `Ready`, emitir evento de Snackbar de éxito.
   - `IOException`: `Error(isNetworkError = true, musicianId = id)`. La lista no se mutó (CA06).
   - `HttpException`: `Error(isNetworkError = false, musicianId = id)`. Mensaje de error genérico de servidor; si retorna 4xx por duplicado, mensaje específico (defensa en profundidad para CA04).
   - `CancellationException`: rethrow.

### 4.3 Navegación

`Destinations.kt` agrega:

```kotlin
const val BandDetail = "band/{bandId}"
const val BandDetailArg = "bandId"
const val AddMusiciansToBand = "band/{bandId}/musicians/add"
const val RefreshBandDetailKey = "refresh_band_detail"

fun bandDetail(bandId: Int) = "band/$bandId"
fun addMusiciansToBand(bandId: Int) = "band/$bandId/musicians/add"
```

`VinilosNavHost` registra dos composable destinations nuevas. El bottom-nav existente no cambia.

### 4.4 Pantallas

**Organización por carpetas** (consistente con `album/`, `artist/`, `collector/` existentes):

- `presentation/ui/screens/artist/` — mantiene `MusicianDetailScreen`; renombra `MusicianListScreen` a `ArtistsHubScreen` y extrae su contenido a `MusicianListContent` (en la misma carpeta).
- `presentation/ui/screens/band/` (nueva carpeta) — `BandListContent`, `BandDetailScreen`, `AddMusiciansToBandScreen`.

**Refactor: `MusicianListScreen` → hub con sub-tabs**

- Renombra el composable a `ArtistsHubScreen`. Su contenido actual se extrae a `MusicianListContent` (sin cambio funcional). Agrega un `TabRow` Material3 con dos `Tab`s: "Músicos" y "Bandas".
- La tab seleccionada se persiste con `rememberSaveable`. Cada tab inyecta su VM con `hiltViewModel()`.

**`BandListContent`** (sub-tab "Bandas")
- Reutiliza `SearchBarStatic`, `ListCounter`, `LoadingState`/`EmptyState`/`ErrorState`.
- `LazyColumn` con `BandCard`. Click → `bandDetail(id)`.

**`BandDetailScreen`** (patrón `MusicianDetailScreen`)
- `Scaffold` + `TopAppBar` (back + title con `heading()`).
- `PerformerHeader` reutilizable: imagen circular + nombre (heading) + badge "BANDA" + descripción.
- Sección **Integrantes**:
  - `SectionHeader("Integrantes")` con `heading()`.
  - Si `members.isEmpty()`: `EmptyMembersState` con CTA "Agregar primer integrante" → `addMusiciansToBand(bandId)` (CA09).
  - Si tiene: lista con `MusicianCard`. Click navega al detalle del músico.
  - Botón "Agregar músicos" (siempre visible per D-CA08) → `addMusiciansToBand(bandId)` (CA01).
- Sección **Álbumes** (si vienen del backend): `LazyRow` igual que en `MusicianDetailScreen`.

**`AddMusiciansToBandScreen`** (el corazón de la HU)
- `Scaffold` + `TopAppBar "Agregar músicos"` (heading) + back. `SnackbarHost`.
- `LazyColumn` con:
  1. Header de banda (Card con foto + nombre + badge).
  2. `OutlinedTextField` con icono `Search`, placeholder "Buscar por nombre". Vinculado al `query` del VM.
  3. Sección **"Músicos disponibles"** (`SectionHeader` con `heading()`):
     - Lista `filteredAvailable`.
     - Cada row: `MusicianRow` (composable nuevo: foto + nombre + `IconButton("+")` a la derecha).
     - Durante `Adding(thisId)`, el "+" se reemplaza por `CircularProgressIndicator(16dp)` y está deshabilitado (previene doble envío).
     - Lista vacía tras filtro: `EmptyState` "No se encontraron músicos".
  4. Sección **"Integrantes actuales"** (`SectionHeader` con `heading()`):
     - `ListCounter` con plural ("3 integrantes" / "1 integrante").
     - Lista compacta de los integrantes (sin acción, scope strict).
- **No** hay botón "Guardar cambios" — cada "+" hace POST inmediato porque el endpoint asocia uno-a-uno. Difiere del mockup pero respeta el backend.

### 4.5 Componentes nuevos (`ui/components/`)

- **`BandCard`** — análogo a `MusicianCard` pero con badge "BANDA". `mergeDescendants = true`, `role = Role.Button`.
- **`PerformerHeader`** — header compartido (foto + nombre con heading + badge opcional + descripción).
- **`MusicianRow`** — fila con `IconButton("+")` para la pantalla de agregar.
- **`EmptyMembersState`** — `EmptyState` parametrizado con CTA opcional para CA09.

### 4.6 Microoptimizaciones (consistencia con el proyecto)

- `derivedStateOf` para `filteredAvailable` (sólo recompone cuando cambian sus inputs).
- `key()` por `musicianId` en `LazyColumn` (recomposición selectiva por row).
- `remember(bandId)` en `LaunchedEffect`.
- `collectAsStateWithLifecycle` (no `collectAsState`).
- `stringResource` cacheado fuera de `forEach` cuando es invariante.

### 4.7 Strings nuevos (`strings.xml`)

Aproximadamente 18 strings. Tematizados:

- Tabs: `artists_tab_musicians`, `artists_tab_bands`.
- Bandas: `bands_title`, `band_badge`, `band_detail_title`, `band_not_found_title`, `band_not_found_body`, `band_members_section_title`, `band_members_empty_title`, `band_members_empty_body`, `add_first_member_cta`.
- Agregar: `add_musicians_title`, `add_musicians_available_section`, `add_musicians_current_section`, `add_musicians_empty_filter`, `search_musicians_placeholder`.
- Counters: `<plurals name="members_record_count">` (one/other).
- Feedback: `add_musician_success` ("'{nombre}' agregado a la banda"), `add_musician_error_network`, `add_musician_error_server`, `add_musician_error_duplicate`.
- Content descriptions: `cd_band_image`, `cd_band_image_of` (`"Imagen de la banda %s"`), `cd_add_musician_to_band` (`"Agregar %s a la banda"`).

### 4.8 Accesibilidad (consistente con la HU previa de a11y)

- Todos los títulos y `SectionHeader`s con `Modifier.semantics { heading() }`.
- Imágenes con `contentDescription` contextual o `null` si decorativas.
- Touch targets ≥ 48dp en el botón "+" (sizeIn).
- `Role.Button` + `mergeDescendants` en cards/rows interactivos.
- `liveRegion` (Polite/Assertive) en estados Loading/Error/Empty.
- `Snackbar` con texto descriptivo y `stateDescription` cuando aplique.

## 5. Testing

### 5.1 Unit tests (`app/src/test/`)

**ViewModels** — JUnit4 + MockK + Turbine + `kotlinx-coroutines-test`. Fakes inline.

`BandListViewModelTest`:
- `init` → `Loading → Success(list)` con lista no vacía.
- Lista vacía → `Loading → Empty`.
- `IOException` → `Error(isNetworkError = true)`.
- `HttpException` → `Error(isNetworkError = false)`.
- `retry()` re-ejecuta el fetch.

`BandDetailViewModelTest`:
- `bandId` válido → `Loading → Success(band)`.
- `HttpException 404` → `NotFound`.
- Cambio de `bandId` cancela el job previo.
- `retry()` después de error → recupera.

`AddMusiciansToBandViewModelTest`:
- Carga inicial paralela → `filteredAvailable` excluye `currentMemberIds` (CA02).
- Debounce de búsqueda: `advanceTimeBy(299)` no actualiza; `advanceTimeBy(300)` sí (CA05).
- Búsqueda case/diacritic-insensitive: `"jose"` matchea `"José"`, `"PEREZ"` matchea `"Pérez"` (CA05).
- `onAddMusician(id)` éxito → `Adding(id)` → `Ready`; `currentMemberIds` incluye `id`; `filteredAvailable` lo excluye (CA03).
- Doble click al "+" en `Adding(id)`: segundo se ignora, POST no se llama dos veces.
- `IOException` en POST → `Error(isNetworkError = true, musicianId = id)`. La lista no se mutó (CA06).
- `HttpException 4xx` → `Error(isNetworkError = false, ...)` con mensaje diferenciado (CA04 defensa).
- `CancellationException` se rethrows (structured concurrency).

**UseCases** — `mockk` del repositorio, `coVerify` de delegación. Tres tests (uno por UseCase nuevo).

**Repository** — `BandRepositoryImplTest`:
- `getBands()` success → `replaceBands(...)` llamada con la lista mapeada → retorna lista.
- `getBands()` con `IOException` y caché poblada → retorna caché; sin caché → rethrows.
- `getBands()` con `HttpException` → propaga sin tocar caché.
- `getBandDetail(id)` análogo (upsert vs read-from-cache).
- `addMusicianToBand(bandId, musicianId)` éxito: si hay detalle cacheado, se mutó `members` (write-through). Si no, se omite mutación local.
- `addMusicianToBand` con error de red: rethrows, caché intacta (CA06).

### 5.2 Compose UI tests (`app/src/androidTest/`)

Composables pure (`createComposeRule()` + `MaterialTheme`):
- `BandCardTest`: muestra nombre + badge, click invoca `onClick`, semantics correctas.
- `MusicianRowTest`: muestra nombre, click "+" invoca `onAdd`. Estado `Adding(thisId)`: "+" deshabilitado + spinner.
- `EmptyMembersStateTest`: muestra título + CTA, click CTA invoca callback.

Screens (`createAndroidComposeRule<ComponentActivity>()`):
- `BandDetailScreenTest`:
  - Loading → `LoadingState`.
  - Success con `members.isEmpty()` → `EmptyMembersState` con CTA (CA09).
  - Success con `members.isNotEmpty()` → lista + botón "Agregar músicos" (CA01).
  - `NotFound` → mensaje + back.
  - Error de red → `ErrorState` con retry, click invoca VM.
- `AddMusiciansToBandScreenTest`:
  - Loading → `LoadingState`.
  - Ready → header + buscador + disponibles + integrantes (CA02).
  - Integrantes ya asociados no aparecen en disponibles (CA04 UI).
  - Click "+": durante `Adding`, spinner reemplaza al "+" y se deshabilita.
  - Tras éxito: músico desaparece de disponibles y aparece en integrantes; `Snackbar` (CA03).
  - Error de red: `Snackbar` con mensaje de red; el músico no se movió (CA06).
  - Búsqueda: filtrar por texto reduce la lista visible (testTags `available_musician_{id}`).

### 5.3 E2E (`app/src/androidTest/.../e2e/VinilosE2ETest.kt`)

Flujo completo HU012 usando `FakeRepositoryModule`:

1. Bottom-nav → "Artistas" → sub-tab "Bandas".
2. Tap primera `BandCard` (matcher `startsWith("band_card_")`).
3. En detalle, tap "Agregar músicos".
4. En lista de disponibles, tap "+" del primer músico.
5. `composeRule.waitUntil(timeout) { ... }` hasta ver Snackbar de éxito.
6. Verificar que el músico ahora aparece en "Integrantes actuales".
7. Back → en detalle de banda, verificar que `members` incluye el músico (CA03 + CA07).

Convenciones del proyecto:
- `HiltTestRunner` ya configurado.
- `testTag` prefixes: `band_card_*`, `available_musician_*`, `current_member_*`, `add_musician_button_*`, `add_musicians_screen_root`.
- Sync vía `waitUntil`, no asume composiciones síncronas.
- Workarounds: API 33/34, animaciones off, `stayon true`.

### 5.4 Cobertura objetivo

DoD ≥ 80% sobre el código nuevo. Alcanzable porque:
- UseCases: cobertura completa (delegación + propagación).
- ViewModels: flows de éxito/error/empty/loading + edge cases (debounce, doble envío).
- Repository: red/caché/error.
- Screens: renders por UiState + interacciones clave.

## 6. Mapeo CA ↔ verificación

| CA | Verificado por |
|---|---|
| CA01 — botón visible y lista de integrantes | `BandDetailScreenTest` (states Success con/sin members) |
| CA02 — catálogo con nombre, imagen, info | `AddMusiciansToBandScreenTest` (rows + exclusión de integrantes) |
| CA03 — agregar exitoso + confirmación + reflejo | VM test + Screen test (Snackbar) + E2E flujo completo |
| CA04 — prevenir duplicados | VM test (exclusión local) + manejo de 4xx |
| CA05 — búsqueda con debounce, case/diacritic-insensitive | VM test específico (normalización + timing) |
| CA06 — error de red con retry, estado consistente | VM test (estado tras IOException) + Screen test retry |
| CA07 — visibilidad para todos los usuarios | E2E (cualquier ruta hacia detalle muestra integrantes) |
| CA08 — restricción de rol | **Out of scope** sin auth — deuda técnica documentada |
| CA09 — empty state con CTA | `BandDetailScreenTest` con `members.isEmpty()` + `EmptyMembersStateTest` |

Nota CA02: la HU pide "nacionalidad", pero `MusicianDetailDto` no expone ese campo (solo `birthDate`). Reemplazamos por `birthDate` (consistente con `MusicianCard` existente). Divergencia documentada.

## 7. Riesgos y mitigaciones

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| R1 | Posible colisión de IDs entre `/musicians` y `/bands`. | Medio | El backend usa herencia de tabla única (`Performer`) — IDs únicos globales. Verificar con un fetch real antes de cablear navegación. |
| R2 | Concurrencia: dos coleccionistas agregando integrantes a la vez. | Bajo | `RefreshBandDetailKey` refresca al volver. Tras cada add exitoso, recargamos `bandDetail` desde red. |
| R3 | API actual no autentica al coleccionista (CA08 no verificable). | Bajo | Asumimos siempre coleccionista. Documentado como deuda. |
| R4 | Filtro local caro para catálogos grandes. | Bajo | Catálogo del curso < 100 músicos. `derivedStateOf` + normalización barata. TODO de paginación documentado. |
| R5 | Migración de Room destructiva (caché borrada). | Bajo | Patrón ya establecido (`fallbackToDestructiveMigration`). Caché es expendable. |
| R6 | Refactor de `MusicianListScreen` puede romper tests UI existentes. | Medio | Extraer contenido actual a `MusicianListContent` sin cambio funcional; tests existentes apuntan a ese composable. |
| R7 | Posible colisión con `domain/model/Performer.kt` existente. | Medio | Verificar antes de implementar. Si hay colisión irresoluble, degradar a Band aislada con header propio (sin abstracción común). |

## 8. Plan de entrega

Una sola PR estructurada en 5 commits ordenados:

1. **Domain + Data + tests unitarios de UseCases y Repository** — modelos, DTOs, entity, DAO, API endpoints, repository, useCases, bindings Hilt. Cierra con tests JVM verdes.
2. **ViewModels + tests** — `BandListViewModel`, `BandDetailViewModel`, `AddMusiciansToBandViewModel`. Tests cubren debounce, doble envío, casos de error.
3. **Strings + componentes reutilizables + tests UI de componentes** — strings.xml, `BandCard`, `MusicianRow`, `EmptyMembersState`, `PerformerHeader`. Tests de cada composable.
4. **Pantallas + navegación + tests UI de pantallas** — refactor `MusicianListScreen → ArtistsHubScreen`, `BandDetailScreen`, `AddMusiciansToBandScreen`, `Destinations.kt`, `VinilosNavHost`. Ajustes de tests preexistentes.
5. **E2E + verificación de build** — extender `VinilosE2ETest`. `./gradlew assembleDebug && ./gradlew test && ./gradlew assembleDebugAndroidTest`.

## 9. Out of scope (futuras HUs)

- Auth + restricción de rol funcional (CA08).
- Eliminar integrantes (DELETE existe en el backend, no se usa).
- Roles/instrumentos por integrante (no soportado por el backend).
- Crear/editar bandas (solo asociar).
- Paginación del catálogo de músicos.
- Sincronización en tiempo real entre coleccionistas.

## 10. Patrones del proyecto aplicados

- ✅ Network-first + fallback a caché (lecturas).
- ✅ Write-through cache (POST `addMusicianToBand`).
- ✅ `StateFlow<UiState>` con sealed UiStates.
- ✅ Clasificación de excepciones (`CancellationException` rethrow, IO → red, HTTP → servidor).
- ✅ `collectAsStateWithLifecycle`, `rememberSaveable`.
- ✅ `MainDispatcherRule` + `runTest` + `advanceUntilIdle()` en tests.
- ✅ Fake repos inline (clases anidadas) en VM tests.
- ✅ Microoptimizaciones: `derivedStateOf`, `key()`, recomposición selectiva.
- ✅ Accesibilidad: `heading()` en títulos/secciones, `mergeDescendants`, `Role.Button`, touch targets ≥ 48dp, `liveRegion`.

---

**Aprobación**: usuario aprobó el diseño completo el 2026-05-12. Próximo paso: invocar `writing-plans` para construir el plan de implementación detallado.
