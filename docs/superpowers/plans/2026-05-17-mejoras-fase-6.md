# Mejoras Fase 6 — Navegación completa (M4 · M5 · M6 · B2) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Cada Task = fresh implementer subagent → revisión de cumplimiento de spec → revisión de calidad de código → follow-ups aplicados por el controller → 1 commit. Pasos con checkbox (`- [ ]`).

**Goal:** Cerrar la deuda de navegación: ids tipados y consistentes (M6), centralización total de rutas/args/keys sin literales inline ni fallbacks silenciosos (M4), helper único de nav-result reemplazando ~10 duplicaciones manuales (M5), y `VinilosNavHost` partido en grafos por feature (B2). Comportamiento de navegación observable preservado; cada task deja build + suite verde.

## Decisiones de ingeniería (tomadas, no son fork de usuario)

- **M6 — Unificación a un solo tipo primitivo `Long`, NO `value class`.** El split Int/Long es consistente por entidad en TODAS las capas (Album = `Long`; Musician/Band/Collector/Prize = `Int`). El único cruce bug-prone es que `Album` se referencia como `Int` en `MusicianRepository.addAlbumToMusician(albumId: Int)`, `CollectorRepository.addAlbumToCollector/removeAlbumFromCollector(albumId: Int)`, sus `*Impl`, use cases, VMs y `@Path("albumId") Int` en `VinilosApiService` (~106, ~113, ~175). `@JvmInline value class AlbumId/...` es transversalmente intratable de forma segura: rompe Room (`@PrimaryKey` value class → `@TypeConverter`), Retrofit `@Path`, Gson `@SerializedName` en DTOs, y ~16 fixtures `SavedStateHandle(mapOf(Destinations.XArg to id))`. Alcance completo-tratable: **estandarizar a `Long` el id de Album en todas las capas**; dejar Musician/Band/Collector/Prize como `Int` (ya consistentes, sin cruce). Value-class evaluado y descartado por intratabilidad transversal.
- **M4 — Centralización total en `Destinations`, NO type-safe `@Serializable` Navigation.** `navigation-compose 2.9.8` soporta type-safe pero **`kotlinx.serialization` no está en el proyecto**. Type-safe exigiría añadir plugin+lib, reescribir 16 entradas de NavHost y romper el contrato `SavedStateHandle` (6 VMs leen `checkNotNull(savedStateHandle[Destinations.XArg])`, ~16 tests construyen `SavedStateHandle(mapOf(...))`) sin valor de negocio. Alcance completo elegido: centralizar 100% de rutas/args/keys (eliminar literales `"track_added"`, `"artist/"`, `"band/"`, `"musician/"`, `"collector/"`) y eliminar todos los fallbacks silenciosos (`?: 0L`, `?: return@composable`) → lectura fail-fast (`checkNotNull`). Type-safe registrado como descartado.
- **M5 — Helper único `core/navigation/NavResult.kt`** con extensiones sobre `NavController`/`NavBackStackEntry`. Las refresh keys se mantienen en `Destinations` (los ~16 tests de VM dependen de `Destinations.*`; mover la fuente de verdad rompería fixtures sin valor). Migrar las ~10 llamadas en `VinilosNavHost.kt` (los `Add*Screen.kt` no tocan SSH).
- **B2 — Extensiones `NavGraphBuilder` por feature** en `presentation/navigation/`: `AlbumNavGraph.kt`, `ArtistNavGraph.kt` (musician+band, comparten pestaña Artists), `CollectorNavGraph.kt`, `PrizeNavGraph.kt`. `selectedDestination` se extrae a `fun selectedTab(route: String?): VinilosDestination` pura y testeable en JVM.

## Orden / dependencias

**Orden: M6 → M4 → M5 → B2.**

1. **M6 primero** — cambia firmas de id de Album que atraviesan domain/data/VM/`Destinations`/`NavHost`; hacerlo antes deja firmas estables para M4/M5/B2.
2. **M4 segundo** — centraliza literales y elimina fallbacks en el NavHost aún monolítico, para que B2 parta de un NavHost limpio.
3. **M5 tercero** — introduce el helper y migra los ~10 call-sites antes de que B2 los mueva a los grafos.
4. **B2 último** — reubicación estructural de un NavHost ya limpio; diff casi sólo "mover".

Cada task es internamente atómica y pasa `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` antes de su commit.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1 (`./gradlew testDebugUnitTest`, NO `test --tests`; clase única `--tests "<FQN>"`; instrumentados `./gradlew assembleDebugAndroidTest`), Hilt+KSP, Navigation Compose 2.9.8 (string-routes), Room, Retrofit+Gson, JUnit4+MockK+Turbine+`kotlinx-coroutines-test`. Branch `feature/mejoras-fase-1` (local, NO push). Commits terminan en `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>`.

---

## Task 1 (M6): Unificar el id de Album a `Long` en todas las capas

**Files (producción):** `domain/repository/MusicianRepository.kt`, `domain/repository/CollectorRepository.kt`, `data/repository/MusicianRepositoryImpl.kt`, `data/repository/CollectorRepositoryImpl.kt`, `data/remote/api/VinilosApiService.kt` (`@Path("albumId") Int`→`Long` ~106/113/175), use cases que envuelven esas firmas, `AddAlbumToMusicianViewModel.kt`/`AddAlbumToCollectorViewModel.kt`, `Destinations.kt` (revisar builders).
**Files (test):** `MusicianRepositoryImplTest.kt`, `CollectorRepositoryImplTest.kt`, `AddAlbumToMusicianViewModelTest.kt`, `AddAlbumToCollectorViewModelTest.kt`, fakes en `testsupport/Fake*Base.kt` y `androidTest/di/Fake*Repository.kt`, use-case tests afectados.

- [ ] **Step 1 (TDD):** Cambiar args/literales de `albumId` a `Long` (`1L`) en los tests de repo/VM afectados. `./gradlew testDebugUnitTest --tests "...MusicianRepositoryImplTest"` → debe fallar a compilar (rojo esperado).
- [ ] **Step 2:** Firmas de dominio: `addAlbumToMusician(musicianId: Int, albumId: Long)`, `addAlbumToCollector(collectorId: Int, albumId: Long, price, status)`, `removeAlbumFromCollector(collectorId: Int, albumId: Long)`.
- [ ] **Step 3:** Propagar a `*Impl` + `VinilosApiService` `@Path("albumId")` `Int`→`Long`.
- [ ] **Step 4:** Propagar a use cases y VMs; eliminar cualquier `.toInt()` sobre album ids.
- [ ] **Step 5:** Actualizar fakes test/androidTest que overridean las 3 firmas.
- [ ] **Step 6:** `testDebugUnitTest --tests "...MusicianRepositoryImplTest"` PASS; luego `testDebugUnitTest && assembleDebugAndroidTest` verde. `git grep -n "albumId: Int\|\.toInt()" -- app/src/main` sin referencias a album ids.
- [ ] **Step 7: Commit** `refactor(domain): unificar albumId a Long en todas las capas (M6)`.

---

## Task 2 (M4): Centralizar rutas/args/keys y eliminar fallbacks silenciosos

**Files:** `presentation/navigation/Destinations.kt`, `presentation/navigation/VinilosNavHost.kt`, (nuevo test) `test/.../presentation/navigation/DestinationsTest.kt`.

Literales a eliminar en `VinilosNavHost.kt`: `"track_added"` (~116/118/165), prefijos `"artist/"`/`"band/"`/`"musician/"`/`"collector/"` (~57–60). Fallbacks: `?: 0L` (~111), `?: return@composable` (~197/233/282).

- [ ] **Step 1 (TDD):** `DestinationsTest.kt` (JVM): builders → ruta esperada; constantes de prefijo coinciden con el prefijo de su ruta; `TrackAddedKey == "track_added"`. Rojo.
- [ ] **Step 2:** En `Destinations.kt`: `const val TrackAddedKey = "track_added"`; `ArtistRoutePrefix="artist/"`, `BandRoutePrefix="band/"`, `MusicianRoutePrefix="musician/"`, `CollectorRoutePrefix="collector/"`.
- [ ] **Step 3:** En `VinilosNavHost.kt` reemplazar los 3 `"track_added"` y los prefijos del `when` por las constantes.
- [ ] **Step 4 (fail-fast):** `?: 0L` y `?: return@composable` → `checkNotNull(...) { "Falta argumento ... en ..." }` consistente con el patrón VM.
- [ ] **Step 5:** `testDebugUnitTest --tests "...DestinationsTest"` PASS. `git grep` de literales/fallbacks en `VinilosNavHost.kt` → vacío.
- [ ] **Step 6:** `testDebugUnitTest && assembleDebugAndroidTest` verde.
- [ ] **Step 7: Commit** `refactor(navigation): centralizar rutas/keys y fail-fast en args (M4)`.

---

## Task 3 (M5): Helper único de nav-result y migración de los ~10 call-sites

**Files:** (nuevo) `core/navigation/NavResult.kt`, `presentation/navigation/VinilosNavHost.kt`, (nuevo test) `test/.../core/navigation/NavResultTest.kt`. Refresh keys se mantienen en `Destinations`.

Emisores (~9): `previousBackStackEntry?.savedStateHandle?.set(K,true); popBackStack()` en AddPerformerToAlbum/AddTrack(TrackAddedKey)/AddComment/AddAlbumToMusician/AddAlbumToCollector/AddFavoritePerformer/AddMusiciansToBand/AddAlbumToBand/AddPrizeToBand/AddPrizeToMusician/CreatePrize. Receptores (~6): `getStateFlow(K,false).collectAsStateWithLifecycle()` + `onRefreshHandled` en AlbumDetail/MusicianDetail/CollectorDetail/BandDetail/Prizes + caso especial `track_added` (retry+snackbar).

- [ ] **Step 1 (TDD):** `NavResultTest.kt` (JVM, `SavedStateHandle` real): `setRefresh` deja `true`; consumo→`clearRefresh` deja `false`; `getStateFlow` emite `false` inicial. Rojo.
- [ ] **Step 2:** `core/navigation/NavResult.kt`: `fun NavController.popWithRefresh(key)`, `@Composable fun NavBackStackEntry.rememberRefreshFlag(key): State<Boolean>`, `fun NavBackStackEntry.clearRefresh(key)`. Keys siguen en `Destinations`.
- [ ] **Step 3:** Migrar los ~9 emisores → `navController.popWithRefresh(Destinations.XKey)`.
- [ ] **Step 4:** Migrar los ~5 receptores → `rememberRefreshFlag` + `clearRefresh`. `track_added` se mantiene a medida (retry+snackbar); opcional `collectRefresh(key,onResult)`.
- [ ] **Step 5:** `testDebugUnitTest --tests "...NavResultTest"` PASS. `git grep "previousBackStackEntry?.savedStateHandle?.set" -- .../VinilosNavHost.kt` vacío.
- [ ] **Step 6:** `testDebugUnitTest && assembleDebugAndroidTest` verde. **Riesgo no auto-verificable:** entrega real de nav-result no cubierta por instrumentados. Mitigación: wrapper 1:1, `NavResultTest`, compilación. Reportar **DONE_WITH_CONCERNS** + verificación manual (add track → back → lista refrescada + snackbar).
- [ ] **Step 7: Commit** `refactor(navigation): helper único de nav-result (M5)`.

---

## Task 4 (B2): Partir `VinilosNavHost` en grafos por feature

**Files:** (nuevos) `AlbumNavGraph.kt`, `ArtistNavGraph.kt`, `CollectorNavGraph.kt`, `PrizeNavGraph.kt`, `SelectedTab.kt`; `VinilosNavHost.kt` (shell); (nuevo test) `test/.../presentation/navigation/SelectedTabTest.kt`.

- [ ] **Step 1 (TDD):** `SelectedTabTest.kt` (JVM): mapea AlbumList→Albums, ArtistList/`artist/5`/`band/2`/`musician/3`→Artists, Collectors/`collector/9`→Collectors, Prizes/CreatePrize→Prizes, `null`→Albums. Rojo.
- [ ] **Step 2:** `SelectedTab.kt`: `fun selectedTab(currentRoute: String?): VinilosDestination` con el `when` actual usando los prefijos de M4. Test PASS.
- [ ] **Step 3:** 4 graph fns `NavGraphBuilder.albumGraph(navController, snackbarHostState)` / `artistGraph(navController)` / `collectorGraph(navController)` / `prizeGraph(navController)`; mover cada `composable(...)` VERBATIM + sus imports de pantalla.
- [ ] **Step 4:** `VinilosNavHost.kt` reducido a shell (~60–80 líneas): nav controller, snackbar, `selectedTab`, Scaffold + bottom nav + `NavHost { albumGraph(...); artistGraph(...); collectorGraph(...); prizeGraph(...) }`.
- [ ] **Step 5:** `testDebugUnitTest && assembleDebugAndroidTest` verde. **Riesgo no auto-verificable:** grafo ensamblado no ejercitado en CI. Mitigación: movimiento verbatim (verificable por diff), `SelectedTabTest`, compilación. Reportar **DONE_WITH_CONCERNS** + verificación manual (4 pestañas + flujo Albums→AlbumDetail→AddTrack→back).
- [ ] **Step 6: Commit** `refactor(navigation): partir VinilosNavHost en grafos por feature (B2)`.

---

## Cierre de Fase 6

- [ ] **Step final:** Sección `## Fase Q — Backlog Fase 6 (M6 · M4 · M5 · B2)` en `MEJORAS.md` (gitignored, **NO commitear**): commits por task, decisiones de ingeniería (value-class y type-safe descartados con justificación), métricas (+`DestinationsTest`/`NavResultTest`/`SelectedTabTest`; `albumId` Long punta a punta; 0 literales/fallbacks en `VinilosNavHost`; 0 `set` manuales; NavHost ~385→~70 líneas), greps de erradicación. Roadmap restante: A4, B3. Commitear el plan doc.
