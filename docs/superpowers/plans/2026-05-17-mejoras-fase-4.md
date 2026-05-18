# Mejoras Fase 4 (A2 resto + M3) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Checkbox (`- [ ]`) steps.

**Goal:** Terminar A2 (migrar todos los bloques `try/catch` de clasificación de error restantes al helper `runCatchingDomain`) y hacer M3 (los 4 forms con `Success` terminal vía `StateFlow` pasan a evento one-shot). Decisiones del usuario: **opción C** (los 4 forms M3 quedan con `UiState = Idle|Submitting|Error(category)`, los strings de error viven en la pantalla) y **M3 incluido en esta fase**.

**Arquitectura objetivo de los 4 forms M3** (`CreateAlbumViewModel`, `CreatePrizeViewModel`, `AddTrackViewModel`, `AddCommentViewModel`):
- `XUiState = Idle | Submitting | Error(val category: DomainFailure)` (sin `Success`). `CreatePrize` además conserva su ruta de carga: `LoadingPrizes | Ready(existingPrizes)`.
- Éxito = evento one-shot vía `MutableSharedFlow<XEvent>(extraBufferCapacity = 1)`; el screen navega al recibirlo (sin payload — ningún screen usa el `Track`/`Comment` devuelto, solo navega).
- Validación de cliente **fuera** de `Error(category)` (que es solo para fallo de red/submit): se conserva el mecanismo existente de cada VM (field-errors mutableState en AddTrack; `_form.descriptionError: FormError` en AddComment; flags en el screen en CreateAlbum). `CreatePrize` (hoy mete validación en `Error(message)`) pasa a un evento tipado `CreatePrizeEvent.ValidationFailed(reason: PrizeValidation)` con `enum PrizeValidation { BLANK_FIELDS, DUPLICATE }`; el screen mapea `reason`→string.
- La **pantalla** mapea `DomainFailure`→`stringResource`. Strings nuevos en `strings.xml` (es-only, como el resto del proyecto).
- Se unifica el nombre del estado de envío a `Submitting` (AddTrack/AddComment hoy usan `Loading`).

**Sin modelo de error en dominio, sin tocar repos Impl** (decisión Fase 3 vigente): el helper sigue en presentación.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1 (usar `testDebugUnitTest`, NO `test --tests`), Compose, Hilt. `./gradlew testDebugUnitTest` / `--tests "<FQN>"` / `./gradlew assembleDebugAndroidTest`.

**Scope (verificado):** 14 archivos VM aún con clasificación manual. Grupos:
- **A** (event+rollback, sin cambio de screen): `AlbumDetailViewModel.removeTrack/removeComment`, `CollectorDetailViewModel.removeFavorite/removeAlbum` (+ helper `restore*`).
- **C** (Add\* event-based, éxito ya one-shot, sin cambio de screen): `AddMusiciansToBandViewModel`, `AddAlbumToCollectorViewModel`, `AddAlbumToMusicianViewModel`, `AddAlbumToBandViewModel`, `AddFavoritePerformerViewModel`, `AddPerformerToAlbumViewModel`, `AddPrizeToMusicianViewModel`, `AddPrizeToBandViewModel`.
- **B/M3** (los 4 forms): `CreateAlbumViewModel`, `CreatePrizeViewModel`, `AddTrackViewModel`, `AddCommentViewModel` — tocan VM+UiState+Screen+tests.

Ningún UiState de los 4 forms se referencia fuera de su propio VM/Screen/test (ripple cero entre features — verificado).

---

## Task 1 (infra): `DomainFailure` + `failureOrNull`

**Files:** Modify `app/src/main/java/com/misw4203/vinilos/presentation/common/DomainResult.kt`; Modify `app/src/test/java/com/misw4203/vinilos/presentation/common/RunCatchingDomainTest.kt`.

- [ ] **Step 1 (TDD):** añadir a `RunCatchingDomainTest.kt` 4 asserts:
  `DomainResult.Ok(1).failureOrNull()` == `null`; `DomainResult.Network.failureOrNull()` == `DomainFailure.NETWORK`; `.NotFound` == `NOT_FOUND`; `.Server` == `SERVER`.
- [ ] **Step 2:** run → FAIL (símbolos no existen).
- [ ] **Step 3:** en `DomainResult.kt` añadir:
```kotlin
/** Categoría de fallo expuesta a la UI (sin filtrar tipos de red). */
enum class DomainFailure { NETWORK, NOT_FOUND, SERVER }

fun DomainResult<*>.failureOrNull(): DomainFailure? = when (this) {
    is DomainResult.Ok -> null
    DomainResult.Network -> DomainFailure.NETWORK
    DomainResult.NotFound -> DomainFailure.NOT_FOUND
    DomainResult.Server -> DomainFailure.SERVER
}
```
- [ ] **Step 4:** run el test class → PASS; luego `./gradlew testDebugUnitTest` completo verde (nada más cambió).
- [ ] **Step 5: Commit** `feat(presentation): DomainFailure + failureOrNull (A2/M3 infra)` (Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>).

---

## Task 2 (Grupo A): `remove*` de AlbumDetail/CollectorDetail al helper

Mecánico, **sin cambio de comportamiento ni de screen/tests**. Atómico.

**Files:** `presentation/viewmodel/AlbumDetailViewModel.kt`, `CollectorDetailViewModel.kt`.

Transformación (idéntica a Fase 3 pero para el bloque del `viewModelScope.launch` de cada `remove*`): el `try { useCase(...); _events.tryEmit(...Removed...) } catch(Cancellation) {throw} catch(Http){restore(..,false)} catch(IO){restore(..,true)} catch(Exception){restore(..,false)}` pasa a:
```kotlin
when (runCatchingDomain { removeTrackUseCase(albumId, track.id) }) {
    is DomainResult.Ok -> _events.tryEmit(AlbumDetailEvent.Removed(track.name))
    DomainResult.Network -> restoreTrack(track, isNetworkError = true)
    DomainResult.NotFound -> restoreTrack(track, isNetworkError = false)
    DomainResult.Server -> restoreTrack(track, isNetworkError = false)
}
```
(equivale exactamente: Http→false, IO→true, Exception→false; el optimistic-remove `_uiState.update{}` previo y `restore*` quedan intactos).

- [ ] **Step 1:** migrar `AlbumDetailViewModel.removeTrack()` y `removeComment()`. Como `load()` ya está migrado, **eliminar** `import retrofit2.HttpException`, `import java.io.IOException` (y `CancellationException` si ya no se usa en el archivo).
- [ ] **Step 2:** migrar `CollectorDetailViewModel.removeFavorite()` y `removeAlbum()`. **Preservar** el guard pre-`try` `PerformerKind.UNKNOWN` de `removeFavorite` (no es clasificación de excepción — va antes del `runCatchingDomain`). Eliminar imports retrofit/io/Cancellation si quedan sin uso.
- [ ] **Step 3:** `./gradlew testDebugUnitTest --tests "...AlbumDetailViewModelTest" --tests "...CollectorDetailViewModelTest"` → PASS con tests **sin modificar**.
- [ ] **Step 4:** `./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest` → verdes.
- [ ] **Step 5: Commit** `refactor(presentation): remove* de detail VMs usan runCatchingDomain (A2)`.

---

## Task 3 (Grupo C): 8 Add\* event-based al helper

Mecánico, **sin cambio de comportamiento, éxito ya one-shot (no M3), sin cambio de screen/tests**. Atómico.

**Files (8):** `AddMusiciansToBandViewModel`, `AddAlbumToCollectorViewModel`, `AddAlbumToMusicianViewModel`, `AddAlbumToBandViewModel`, `AddFavoritePerformerViewModel`, `AddPerformerToAlbumViewModel`, `AddPrizeToMusicianViewModel`, `AddPrizeToBandViewModel`.

Para CADA uno, en `loadInitial()` y en `onAdd*/onConfirm()`: reemplazar el `try{ ... }catch(Cancellation/IO/Http/Exception)` por `when (runCatchingDomain { <misma llamada suspend> }) { is DomainResult.Ok -> <misma rama éxito (incluye emitir AddedSuccessfully y/o setear Ready)>; DomainResult.Network -> <rama IO actual: Error(isNetworkError=true, ...) [+ AddFailed(true) en onAdd]>; DomainResult.NotFound -> <misma que rama no-IO: Error(isNetworkError=false,...)>; DomainResult.Server -> <misma que rama no-IO> }`. Conservar exactamente: campos extra del `Error` (`musicianId`/`albumId`/`type`/`performerId`/`prizeId`), helper `emitAddError`, el evento `AddFailed(isNetworkError)`. `isNetworkError = (cat == NETWORK)`; `NotFound`/`Server` → `isNetworkError=false` (idéntico a hoy: Http/Exception→false). Eliminar imports `retrofit2.HttpException`/`java.io.IOException`/`CancellationException` (si quedan sin uso) de los 8.

- [ ] **Step 1:** migrar los 8 (leer cada archivo antes; replicar la rama de éxito y los campos extra del Error verbatim).
- [ ] **Step 2:** `./gradlew testDebugUnitTest` (corre todos sus *Test) → PASS con tests **sin modificar**. Si algún test falla, la rama mapeada divergió → corregir el VM, NO el test.
- [ ] **Step 3:** `./gradlew assembleDebugAndroidTest` → verde.
- [ ] **Step 4:** `git grep -lE "import retrofit2.HttpException" -- "app/src/main/java/.../presentation/viewmodel" | wc -l` → registrar (baja a 4: los 4 forms M3 que quedan para Tasks 4-7).
- [ ] **Step 5: Commit** `refactor(presentation): 8 Add* VMs usan runCatchingDomain (A2)`.

---

## Tasks 4–7 (B/M3): los 4 forms — un VM por task

Patrón común por form (detalles específicos por task):
1. **UiState file**: quitar `Success`; `Error(message: String)`→`Error(val category: DomainFailure)`; renombrar `Loading`→`Submitting` donde aplique; añadir `sealed interface XEvent { data object Submitted : XEvent }` (+ casos validación donde aplique). Importar `DomainFailure`.
2. **VM**: añadir `MutableSharedFlow<XEvent>(extraBufferCapacity = 1)` + `events: SharedFlow` (`asSharedFlow()`). `submit()`: validación cliente igual que hoy (mecanismo existente, no en `Error(category)`); luego `_uiState.value = Submitting`; `when (runCatchingDomain { useCase(...) }) { is Ok -> _events.tryEmit(Submitted) ; else -> _uiState.value = Error(failureOrNull()!!) }`. Quitar imports retrofit/io. Ajustar/!eliminar `resetState`/`resetError` según el screen.
3. **Screen**: dejar de ramificar `uiState` en `Success`/`Error(message)`. Recolectar `events` con `LaunchedEffect` + collect → en `Submitted` llamar el callback de navegación (`onAlbumCreated`/`onSuccess`). En `uiState is Error` → snackbar con `stringResource` mapeado desde `category` (`NETWORK`→…, `NOT_FOUND`→…, `SERVER`→…). `isSubmitting = uiState is Submitting`.
4. **strings.xml**: añadir los ids nuevos por categoría (ver cada task). Reusar los `R.string.*` existentes que ya estaban (p.ej. `add_comment_error_network/server`).
5. **Unit test**: actualizar los tests que aseveraban `Success`/`Error(message)` → aseverar emisión del evento (`viewModel.events.test { ... }` con Turbine, ya usado en el repo) y `Error(category = DomainFailure.X)`. Mantener cobertura (éxito, network, server, notfound donde aplique, validación).
6. **Instrumented test** (solo CreateAlbum/CreatePrize): ajustar asserts de UI que dependían del estado terminal; preservar las verificaciones de validación de campos.

Verificación por task: `./gradlew testDebugUnitTest --tests "<VMTest FQN>"` verde, luego suite completa + `assembleDebugAndroidTest`. Cada task = 1 commit.

### - [ ] Task 4: `AddCommentViewModel` (el más cercano al objetivo — hacer primero como patrón)
- `AddCommentUiState`: `Idle | Submitting | Error(category: DomainFailure)`. `AddCommentFormState`/`FormError` se conservan tal cual (validación ya separada en `_form`). Añadir `sealed interface AddCommentEvent { data object Submitted : AddCommentEvent }`.
- VM: ya clasificaba IO→net / else→server (sin rama Http). `runCatchingDomain` lo unifica: `Network→Error(NETWORK)`, `NotFound/Server→Error(SERVER)` (idéntico: 404 y otros caían a server/false). Éxito → `_events.tryEmit(Submitted)` (descartar el `Comment` devuelto). `resetError()` se mantiene (limpia `Error`→`Idle`) — el screen lo sigue llamando tras el snackbar.
- Screen: `Success→onSuccess()` pasa a collV de `events`; el mapeo `isNetworkError`→string pasa a `category`: `NETWORK`→`R.string.add_comment_error_network`, `SERVER`→`R.string.add_comment_error_server`, `NOT_FOUND`→`R.string.add_comment_error_server` (mismo texto; 404 no aplica a POST de comentario). Reusar los `R.string.*` ya existentes (no crear nuevos).
- Tests: `AddCommentViewModelTest` — los asserts de `Success(comment)` pasan a `events` (Turbine) emite `Submitted`; `Error(isNetworkError=true)`→`Error(DomainFailure.NETWORK)`; `Error(isNetworkError=false)`→`Error(DomainFailure.SERVER)`. `resetError` test ajustado al nuevo `Error(category)`. Sin instrumented test (no existe).
- Commit: `refactor(presentation): AddComment a Error(category)+evento (A2/M3)`.

### - [ ] Task 5: `AddTrackViewModel`
- `AddTrackUiState`: `Idle | Submitting | Error(category: DomainFailure)` (quitar `Success(track)` y `Loading`→`Submitting`). `name/nameError/duration/durationError` mutableState se conservan (validación de campo intacta). Añadir `sealed interface AddTrackEvent { data object Submitted : AddTrackEvent }`.
- VM: el 404 distinto ("Álbum no encontrado") se preserva vía `category`: `runCatchingDomain` ya mapea 404→`NotFound`. Éxito→`tryEmit(Submitted)`. Quitar imports retrofit/io. Sin `resetState` (no existe; el screen navega).
- Screen `AddTrackScreen`: `Success→onSuccess()` pasa a collect de `events`. snackbar desde `category`: `NOT_FOUND`→nuevo `R.string.add_track_error_not_found` ("Álbum no encontrado"), `NETWORK`→`add_track_error_network` ("Sin conexión. Intenta de nuevo"), `SERVER`→`add_track_error_server` ("Error al agregar track"). Botón `enabled = uiState !is Submitting`.
- strings.xml: añadir `add_track_error_not_found`, `add_track_error_network`, `add_track_error_server` (reusar `add_track_*` existentes si calzan). 
- Tests `AddTrackViewModelTest`: `Success.track`→`events` emite `Submitted`; `404 → Error(DomainFailure.NOT_FOUND)`; `IO → Error(NETWORK)`; `genérico → Error(SERVER)`; validación (nameError/durationError) sin cambios. No instrumented test (no existe).
- Commit: `refactor(presentation): AddTrack a Error(category)+evento (A2/M3)`.

### - [ ] Task 6: `CreateAlbumViewModel`
- `CreateAlbumUiState`: `Idle | Submitting | Error(category: DomainFailure)` (quitar `Success`). Añadir `sealed interface CreateAlbumEvent { data object Submitted : CreateAlbumEvent }`. Validación ya vive en el screen (flags) — no cambia.
- VM: `submit` → `Submitting`; `runCatchingDomain { createAlbum(input) }`: Ok→`tryEmit(Submitted)`; else→`Error(failureOrNull()!!)`. `resetState()` se conserva (screen lo llama tras snackbar/nav) → vuelve a `Idle`. Quitar imports retrofit/io.
- `CreateAlbumScreen`: el `LaunchedEffect(uiState)` actual maneja `Success`/`Error`. Separar: collect de `events` → `Submitted`: `onAlbumCreated(); viewModel.resetState()`. `LaunchedEffect(uiState)` mantiene solo `Error`→`snackbar(category→string)`; `resetState()` tras mostrar. `isSubmitting` igual.
- strings.xml: `create_album_error_network` ("Sin conexión a internet. Intenta de nuevo."), `create_album_error_server` ("Error del servidor. Verifica los datos." — **sin el código**, decisión C), `create_album_error_unexpected` ("Ocurrió un error inesperado."). Mapeo: NETWORK→network, NOT_FOUND→server, SERVER→server. (NOT_FOUND no aplica a POST crear; same texto que server.)
- Tests: `CreateAlbumViewModelTest` — `Success`→`events` emite `Submitted`; `Error(message~conexión)`→`Error(NETWORK)`; `Error(~422)`→`Error(SERVER)` (ya no hay código en el mensaje; el assert pasa a la categoría); genérico→`Error(SERVER)`; `resetState` tests ajustados (`Error(category)`→`Idle`). `CreateAlbumScreenTest` instrumentado: la validación de campos requeridos no cambia; ajustar cualquier assert que dependa del estado terminal `Success` (usa lambda `onAlbumCreated`, ya es callback — verificar que sigue verde; el test actual no asevera snackbar de red).
- Commit: `refactor(presentation): CreateAlbum a Error(category)+evento (A2/M3/C)`.

### - [ ] Task 7: `CreatePrizeViewModel` (el más complejo: tiene `loadPrizes` + validación en UiState)
- `CreatePrizeUiState`: `LoadingPrizes | Ready(existingPrizes) | Submitting | Error(category: DomainFailure)` (quitar `Success`; `Error(message)`→`Error(category)`). Añadir `sealed interface CreatePrizeEvent { data object Submitted : CreatePrizeEvent ; data class ValidationFailed(val reason: PrizeValidation) : CreatePrizeEvent }` y `enum class PrizeValidation { BLANK_FIELDS, DUPLICATE }`.
- VM `submit`: validación blank/duplicado → `_events.tryEmit(CreatePrizeEvent.ValidationFailed(BLANK_FIELDS|DUPLICATE))` y `return` (ya NO `_uiState=Error(message)`). Luego `Submitting`; `runCatchingDomain { createPrize(...) }`: Ok→`tryEmit(Submitted)`; else→`Error(failureOrNull()!!)`. `loadPrizes()`: migrar a `when (runCatchingDomain { getPrizes() }) { is Ok -> Ready(value); else -> Ready(emptyList()) }` (preserva el silent-swallow actual: cualquier fallo → `Ready(emptyList())`). `resetState()` sigue llamando `loadPrizes()`. Quitar imports retrofit/io.
- `CreatePrizeScreen`: collect de `events`: `Submitted`→`onSuccess(); resetState()`; `ValidationFailed(reason)`→snackbar con `reason`→string (`BLANK_FIELDS`→`R.string.create_prize_error_required_all`, `DUPLICATE`→`R.string.create_prize_error_duplicate` que **ya existe**). `LaunchedEffect(uiState)`: solo `Error(category)`→snackbar (network/server) con acción "Reintentar" preservada (al reintentar re-llama `submit`); dismiss→`resetState()`. `RegisteredPrizesSection` (rama `LoadingPrizes`/`Ready`) sin cambios.
- strings.xml: `create_prize_error_required_all` ("Todos los campos son obligatorios."), `create_prize_error_network` ("Sin conexión a internet. Inténtalo de nuevo."), `create_prize_error_server` ("Error del servidor. Verifica los datos." — sin código). Reusar `create_prize_error_duplicate` existente. `Reintentar` → nuevo `create_prize_retry` (hoy hardcoded en screen — moverlo de paso).
- Tests: `CreatePrizeViewModelTest` — `Success`→`events` `Submitted`; blank/duplicado→`events` `ValidationFailed(reason)` (ya no `Error(message)`); IO→`Error(NETWORK)`; Http→`Error(SERVER)`; el test "error state set before resetState" se ajusta a `Error(category)`. `CreatePrizeScreenTest` instrumentado: `submitWithAllFieldsCallsOnSuccess` sigue usando lambda `onSuccess` (verificar verde con el evento); ajustar asserts dependientes del estado terminal.
- Commit: `refactor(presentation): CreatePrize a Error(category)+eventos (A2/M3/C)`.

---

## Cierre de Fase 4

- [ ] **Step final:** sección `## Fase O — Backlog Fase 4 (A2 resto + M3)` en `MEJORAS.md` (gitignored, NO commitear): commits, decisiones (C en 4 forms, M3 incluido, validación fuera de Error(category), strings a UI), métricas (0 imports retrofit/io en `presentation/viewmodel` — todos en `DomainResult.kt`; +N tests). Confirmar el conteo final `git grep -lE "import retrofit2.HttpException" -- presentation/viewmodel` = 0.

## Self-review
- **A2 cerrado:** tras Task 7, ningún VM clasifica excepciones a mano; `retrofit2.HttpException`/`java.io.IOException` solo en `DomainResult.kt`.
- **M3 cerrado:** los 4 forms entregan éxito por evento one-shot (no se re-emite al rotar).
- **Riesgo acotado por task:** Tasks 2-3 mecánicas, sin tocar screens/tests (comportamiento idéntico, tests intactos = red de seguridad). Tasks 4-7 aisladas por VM (ripple cero entre forms, verificado), cada una con su revisión spec+calidad.
- **Decisión C respetada:** los strings de error/validación de los 4 forms viven en la pantalla; el VM solo expone `DomainFailure`/razón tipada. Único cambio de comportamiento visible: el mensaje de servidor de CreateAlbum/CreatePrize pierde el `(código)` (aprobado).
- **Contrato preservado:** repos Impl intactos; `loadPrizes` mantiene el silent-swallow; validación de cliente preservada (mecanismo por VM).

## Fase 5 (roadmap)
- M2 (estandarizar detail VMs a `SavedStateHandle`), M4–M8, B2–B6, A4. Detallar en su plan.
