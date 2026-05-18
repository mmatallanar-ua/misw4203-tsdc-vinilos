# Mejoras Fase 5 (M2 · M7 · B5 · B4 · M8 · B6) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Checkbox (`- [ ]`) steps.

**Goal:** Cluster de calidad/testabilidad de bajo riesgo: 6 items aislados, mayormente mecánicos, comportamiento preservado salvo donde se indique. Cada task = 1 commit con revisión spec + calidad.

**Decisiones de ingeniería (tomadas, no son fork de usuario):**
- **B5**: abstracción propia `AppLogger` (interfaz + impl `android.util.Log`), sin nueva dependencia (Timber); testeable con fake no-op.
- **M2**: añadir `const val ArtistDetailArg = "id"` a `Destinations` y usarlo en NavHost + VM (elimina el literal `"id"` de paso).
- **M7**: nuevo `di/DispatcherModule.kt` (`object` + `@Provides`, estilo `NetworkModule`) + qualifier `@IoDispatcher`; los tests de repo pasan `UnconfinedTestDispatcher()` por constructor (preserva el comportamiento eager actual de esos tests, que no usan `advanceUntilIdle`).
- **B6**: crear `docs/adr/0001-sin-concepto-de-sesion.md` (no existe carpeta ADR; `CLAUDE.md`/`MEJORAS.md` gitignored no sirven).

**Orden / dependencias:** M7 antes que B4 (el nuevo test de Prize debe construir el Impl con el dispatcher inyectado) y antes que B5 (B5 parte de los constructores ya modificados por M7, evitando doble-churn en los mismos 5 `*Impl` + tests). Orden: **M2 → M7 → B5 → B4 → M8 → B6**.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1 (`./gradlew testDebugUnitTest`, NO `test --tests`; `--tests "<FQN>"`; `assembleDebugAndroidTest`), Hilt+KSP, Room, Retrofit+Gson, Compose, JUnit4+MockK+Turbine+`kotlinx-coroutines-test`.

**Overlap de archivos (sólo informativo):** M7 y B5 tocan los 5 `data/repository/*Impl.kt` y sus tests → por eso M7 va antes y B5 asume el constructor ya con dispatcher. M2 toca `Destinations.kt`/`VinilosNavHost.kt`; B6 sólo añade un doc + (opcional) una línea de KDoc.

---

## Task 1 (M2): `MusicianDetailViewModel` a `SavedStateHandle`

**Files:** `presentation/viewmodel/MusicianDetailViewModel.kt`, `presentation/navigation/Destinations.kt`, `presentation/navigation/VinilosNavHost.kt`, `presentation/ui/screens/artist/MusicianDetailScreen.kt`, `test/.../MusicianDetailViewModelTest.kt`.

Contexto: hoy `MusicianDetailViewModel(getMusicianDetail)` expone `loadMusician(id)` público + `loadJob`/`currentId`; el screen llama `loadMusician` en dos `LaunchedEffect` (inicial + refresh). Patrón objetivo = `AlbumDetailViewModel` (SavedStateHandle + `init{ load() }` + `retry()=load()`). Ruta NavHost = `"artist/{id}"`, arg crudo `"id"` (no hay constante).

- [ ] **Step 1:** `Destinations.kt`: añadir `const val ArtistDetailArg = "id"` (junto a los otros `*DetailArg`). Mantener el resto.
- [ ] **Step 2:** Reescribir `MusicianDetailViewModel`:
  - Constructor `@Inject constructor(private val getMusicianDetail: GetMusicianDetailUseCase, savedStateHandle: SavedStateHandle)`.
  - `private val musicianId: Int = checkNotNull(savedStateHandle[Destinations.ArtistDetailArg])`.
  - Borrar `loadJob`, `currentId`, `loadMusician(id)`. Añadir `private fun load()` (cuerpo = el `when(runCatchingDomain{ getMusicianDetail(musicianId) })` actual, mismas ramas Ok/Network/NotFound/Server). `init { load() }`. `fun retry() { load() }`. Añadir `fun refresh() { load() }` para la ruta de refresh del screen.
  - Imports: añadir `SavedStateHandle`, `Destinations`; quitar `Job` si queda sin uso.
- [ ] **Step 3:** `VinilosNavHost.kt` ruta `"artist/{id}"`: usar `navArgument(Destinations.ArtistDetailArg)`; ya no extraer `id` para pasarlo al VM (el VM lo lee de SSH). `id` aún se necesita para los callbacks `addAlbumToMusician(id)`/`addPrizeToMusician(id)` → mantener `val id = backStackEntry.arguments?.getInt(Destinations.ArtistDetailArg) ?: return@composable` sólo para esos callbacks. `MusicianDetailScreen(...)` ya no recibe `musicianId` para cargar.
- [ ] **Step 4:** `MusicianDetailScreen.kt`: quitar el parámetro `musicianId: Int` del uso de carga y los dos `LaunchedEffect{ viewModel.loadMusician(...) }`. El inicial desaparece (lo hace `init`). El de refresh pasa a `LaunchedEffect(refreshKey){ if (refreshKey){ viewModel.refresh(); onRefreshHandled() } }`. Mantener `viewModel: MusicianDetailViewModel = hiltViewModel()`. Si los callbacks de navegación necesitan el id, recibirlo aún como parámetro desde NavHost (sólo para callbacks), pero NO usarlo para cargar.
- [ ] **Step 5:** Reescribir `MusicianDetailViewModelTest.kt` con el patrón de `AlbumDetailViewModelTest`: `buildViewModel(repo, musicianId=2) = MusicianDetailViewModel(GetMusicianDetailUseCase(repo), SavedStateHandle(mapOf(Destinations.ArtistDetailArg to musicianId)))`. Donde antes se llamaba `viewModel.loadMusician(2)` ahora la carga ocurre en `init`; usar `advanceUntilIdle()` + `uiState.test{}` como en `AlbumDetailViewModelTest`. Preservar TODA la cobertura (Loading→Success, NotFound 404, Error network/server, retry). El caso de "cambio de id"/cancelación (`loadJob`) ya no aplica — si había un test de cancelación de carga previa, eliminarlo documentando que el patrón SavedStateHandle no recarga con id cambiante (un id distinto = nueva entrada de nav = nuevo VM).
- [ ] **Step 6:** `./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.MusicianDetailViewModelTest"` → PASS. Luego `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` → verdes (chequea que ningún test instrumentado/E2E rompa por la firma del screen).
- [ ] **Step 7: Commit** `refactor(presentation): MusicianDetailViewModel a SavedStateHandle (M2)` (Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>).

---

## Task 2 (M7): inyectar `@IoDispatcher`

**Files:** crear `di/DispatcherModule.kt` + `di/IoDispatcher.kt` (qualifier); modificar los 5 `data/repository/*Impl.kt`; modificar los tests de repo existentes que construyen los Impl.

- [ ] **Step 1:** Crear el qualifier `app/src/main/java/com/misw4203/vinilos/di/IoDispatcher.kt`:
```kotlin
package com.misw4203.vinilos.di
import javax.inject.Qualifier
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
```
- [ ] **Step 2:** Crear `app/src/main/java/com/misw4203/vinilos/di/DispatcherModule.kt` (estilo `NetworkModule`: `@Module @InstallIn(SingletonComponent::class) object`):
```kotlin
@Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
```
- [ ] **Step 3:** En cada `*Impl` (`Album/Musician/Band/Collector/Prize`): añadir parámetro de constructor `@IoDispatcher private val ioDispatcher: CoroutineDispatcher` (último param). Reemplazar TODOS los `withContext(Dispatchers.IO)` por `withContext(ioDispatcher)`. Quitar `import kotlinx.coroutines.Dispatchers` si queda sin uso; añadir `import kotlinx.coroutines.CoroutineDispatcher` + `import com.misw4203.vinilos.di.IoDispatcher`. `RepositoryModule` usa `@Binds` → Hilt cablea automáticamente con el `@Provides` nuevo; NO cambia `RepositoryModule`.
- [ ] **Step 4:** Actualizar los tests de repo que construyen los Impl directamente (`AlbumRepositoryImplTest`, `MusicianRepositoryImplTest`, `BandRepositoryImplTest`, `CollectorRepositoryImplTest` — y cualquier otro): pasar `UnconfinedTestDispatcher()` como último arg del constructor. Añadir `import kotlinx.coroutines.test.UnconfinedTestDispatcher` y `@OptIn(ExperimentalCoroutinesApi::class)` si el archivo lo requiere (seguir el estilo ya presente en otros tests del repo). No cambiar la lógica de los tests.
- [ ] **Step 5:** `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` → verdes. Verificar `git grep -n "withContext(Dispatchers.IO)" -- app/src/main/java/com/misw4203/vinilos/data/repository; echo "exit:$?"` → sin coincidencias (exit:1).
- [ ] **Step 6: Commit** `refactor(data): inyectar @IoDispatcher en los repos (M7)`.

---

## Task 3 (B5): seam de logging para excepciones tragadas

**Files:** crear `core/logging/AppLogger.kt` + impl + módulo Hilt; modificar `Album/Musician/Band/CollectorRepositoryImpl.kt` (PrizeImpl no tiene catch). Parte del estado post-M7 (constructores ya con `ioDispatcher`).

- [ ] **Step 1:** Crear interfaz `app/src/main/java/com/misw4203/vinilos/core/logging/AppLogger.kt`:
```kotlin
interface AppLogger { fun w(tag: String, message: String, throwable: Throwable? = null) }
```
+ impl `AndroidAppLogger` (`@Inject constructor()`) usando `android.util.Log.w(tag, message, throwable)`.
- [ ] **Step 2:** Bind vía Hilt. Crear `di/LoggingModule.kt` (`@Module @InstallIn(SingletonComponent::class) abstract class` con `@Binds @Singleton abstract fun bindAppLogger(impl: AndroidAppLogger): AppLogger`) — estilo `RepositoryModule`.
- [ ] **Step 3:** Inyectar `private val logger: AppLogger` (último param, tras `ioDispatcher`) en los 4 Impl con catches best-effort. En CADA `catch (_: Exception)`/`catch (e: IOException)` que hoy es silencioso (lista del scope map: `MusicianRepositoryImpl:95`, `BandRepositoryImpl:91,108`, `CollectorRepositoryImpl:65,89,213,232`, `AlbumRepositoryImpl.invalidateDetailCache` IOException): cambiar `catch (_: Exception)` → `catch (e: Exception)` y añadir `logger.w("<ClassName>", "<acción> best-effort falló", e)` ANTES del comentario `// best-effort`. NO cambiar la política (sigue siendo best-effort, no se relanza). Mantener el rethrow de `CancellationException` intacto.
- [ ] **Step 4:** Tests de repo (construyen los Impl): pasar un fake `object : AppLogger { override fun w(...) {} }` (o un `RecordingLogger` simple en testsupport) como último arg. Donde un test quiera aseverar que se logueó (opcional, no obligatorio), usar el recorder. Actualizar las construcciones de los 4 Impl en sus tests.
- [ ] **Step 5:** `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` → verdes.
- [ ] **Step 6: Commit** `feat(data): logger para excepciones best-effort tragadas (B5)`.

---

## Task 4 (B4): `PrizeRepositoryImplTest`

**Files:** crear `app/src/test/java/com/misw4203/vinilos/data/repository/PrizeRepositoryImplTest.kt`. Sólo añade test; sin cambio de producción. Parte del estado post-M7 (constructor `PrizeRepositoryImpl(api, ioDispatcher)`).

Contexto: `PrizeRepositoryImpl` sin DAO/caché/catch — el más simple. Plantilla = `BandRepositoryImplTest` (mockk `api`, `runTest`, construcción directa, sin `MainDispatcherRule`).

- [ ] **Step 1:** Crear el test mirroreando el estilo del repo (mockk `api: VinilosApiService`; `PrizeRepositoryImpl(api, UnconfinedTestDispatcher())`):
  - `getPrizes returns mapped list` (coEvery `api.getPrizes()` → `listOf(PrizeDetailDto(...))`; assert size + mapeo `name/organization/description/id`).
  - `getPrizes propagates IOException` (`@Test(expected = IOException::class)` o try/fail).
  - `getPrizes propagates HttpException` (Response.error 500).
  - `createPrize posts mapped request and returns mapped Prize` (`coEvery api.createPrize(any())` → DTO; assert resultado; `coVerify { api.createPrize(CreatePrizeRequest("name","desc","org")) }`).
  - `createPrize propagates IOException`.
- [ ] **Step 2:** `./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.data.repository.PrizeRepositoryImplTest"` → PASS; luego suite completa verde.
- [ ] **Step 3: Commit** `test(data): PrizeRepositoryImplTest (B4)`.

---

## Task 5 (M8): `AlbumDetailContent` a `LazyColumn`

**Files:** `presentation/ui/screens/album/AlbumDetailScreen.kt`. **Riesgo medio (layout visual; sin test instrumentado existente para esta pantalla → la verificación visual NO es automatizable; reportar como DONE_WITH_CONCERNS y pedir validación visual manual).**

Contexto: `AlbumDetailContent` (línea ~179) usa `Box > Column(verticalScroll) > [cover Box height=CoverHeight] + [Surface(offset -CardOverlap) > Column(padding) > título/chips/desc/botón/PerformersSection/TracksSection/CommentsSection/label]` + botón Back overlay. `TracksSection`/`CommentsSection` hacen `forEach{ key(id){ Row } }` (eager). Sin `AlbumDetailScreenTest` instrumentado; testTags a preservar: `album_detail_root`, `album_detail_add_comment`, `album_detail_add_performer`, `album_detail_no_performers`, `album_remove_track_{id}`, `album_remove_comment_{id}`, `album_detail_back`, `album_remove_confirm`.

- [ ] **Step 1:** Convertir el `Column(verticalScroll)` en un `LazyColumn` (estado de scroll propio). El diseño visual (cover de `CoverHeight` + tarjeta con `offset(y=-CardOverlap)` solapando el cover) debe preservarse: estructurar el `LazyColumn` con `item {}` para (a) el cover Box, (b) la Surface-tarjeta cuyo contenido superior (título→descripción→botón add comment→PerformersSection) va en `item {}`s o un único `item {}`, (c) `items(album.tracks, key={it.id}){ TrackRow }` para tracks, (d) `items(album.comments, key={it.id}){ CommentCard }` para comments, (e) `item {}` para record label + Spacer final. El `offset`/solape de la tarjeta sobre el cover debe mantenerse (envolver cover+tarjeta de forma que el solape visual no se rompa: p.ej. el cover como primer item y la tarjeta como items siguientes con el mismo `offset` negativo aplicado al primer bloque de la tarjeta). Mantener `Box(testTag("album_detail_root"))` como raíz y el botón Back overlay fuera del `LazyColumn` (en el `Box`). `TracksSection`/`CommentsSection` se reemplazan por `items(...)` directos o se refactorizan a extensiones `LazyListScope` (`fun LazyListScope.tracksSection(...)`). PRESERVAR todos los testTags y la lógica de remove/confirm dialog intacta.
- [ ] **Step 2:** `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` → verdes (compila los instrumentados/E2E; `VinilosE2ETest` puede tocar la pantalla — debe seguir compilando y, si corre por testTag, seguir localizándolos).
- [ ] **Step 3:** Verificación NO automatizable: revisar visualmente (o pedir al usuario) que el cover + solape de tarjeta + scroll perezoso se ven igual. Reportar explícitamente esta limitación.
- [ ] **Step 4: Commit** `perf(presentation): AlbumDetailContent a LazyColumn (M8)`.

---

## Task 6 (B6): documentar la deuda de "sesión/collector actual"

**Files:** crear `docs/adr/0001-sin-concepto-de-sesion.md`; opcional: una línea de `@see` en el KDoc de `Destinations.DefaultCollectorId` apuntando al ADR.

Contexto: `Destinations.DefaultCollectorId = 100` ya tiene un KDoc explicando la deuda; `AddCommentViewModel.collectorId` lo recibe vía SavedStateHandle desde la ruta (hardcodeado 100). No existe carpeta ADR ni `DECISIONS.md` committeado.

- [ ] **Step 1:** Crear `docs/adr/0001-sin-concepto-de-sesion.md` (ADR corto): contexto (HU09 requiere `collector` en el POST; el curso no incluye auth), decisión (usar `DefaultCollectorId=100` como referencia válida; no introducir `SessionRepository`), consecuencias (sólo afecta `AddComment`; los demás `collectorId` provienen de navegación real), estado: **Deuda aceptada** mientras el alcance del curso no incluya autenticación. Referenciar `Destinations.kt` (DefaultCollectorId) y `AddCommentViewModel`.
- [ ] **Step 2 (opcional):** En el KDoc de `Destinations.DefaultCollectorId` añadir `@see docs/adr/0001-sin-concepto-de-sesion.md` (una línea; sin cambiar el código).
- [ ] **Step 3:** Sin tests (doc). `./gradlew testDebugUnitTest` debe seguir verde si se tocó el KDoc (cambio trivial).
- [ ] **Step 4: Commit** `docs(adr): registrar deuda aceptada de sesión/collector (B6)`.

---

## Cierre de Fase 5

- [ ] **Step final:** sección `## Fase P — Backlog Fase 5 (M2·M7·B5·B4·M8·B6)` en `MEJORAS.md` (gitignored, NO commitear): commits, decisiones de ingeniería tomadas, métricas (repos testeables con dispatcher inyectado; +1 test de repo; excepciones best-effort ahora logueadas; detalle de álbum perezoso; M2 consistente; ADR de deuda). Confirmar `git grep -n "withContext(Dispatchers.IO)" -- app/src/main` = sin coincidencias.

## Self-review
- **Aislamiento:** cada task es independiente y commiteable; orden M7→B4/B5 respeta la dependencia del constructor. M2 sólo presentación; B4 sólo test; B6 sólo doc.
- **Riesgo:** M2/M7/B4/B5/B6 sin cambio de comportamiento observable (M7/B5 mecánicos; M2 preserva estados; B5 sólo añade logging sin alterar la política). **M8 es el único con cambio de UI** y SIN verificación automatizable → DONE_WITH_CONCERNS + validación visual manual explícita.
- **Red de seguridad:** suite unitaria + `assembleDebugAndroidTest` en cada task; tests de VM/repo existentes intactos salvo las construcciones (M7) y la reescritura justificada de `MusicianDetailViewModelTest` (M2, patrón AlbumDetail).
- **Sin nueva dependencia:** B5 usa abstracción propia + `android.util.Log` (no Timber).

## Fase 6+ (roadmap)
Cluster de navegación: M4 (rutas type-safe / centralizar literales), M5 (helper de nav-result), B2 (partir NavHost), M6 (ids Int↔Long / value class). Y A4 (hardening release: cleartext + R8) en su propia fase con validación manual del APK. B3 (estabilidad Compose) medición opcional.
