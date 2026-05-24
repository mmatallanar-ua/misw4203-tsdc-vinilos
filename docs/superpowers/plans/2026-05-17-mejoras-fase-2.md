# Mejoras Fase 2 (A3) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quitar el DTO de red `CreateTrackRequest` de la frontera de dominio. `AlbumRepository.addTrack` y `AddTrackUseCase` deben hablar un modelo de dominio puro `NewTrack`; el mapeo a `CreateTrackRequest` (formato de cable Retrofit) queda confinado dentro de `AlbumRepositoryImpl`.

**Architecture:** Esto es un **único refactor atómico**: cambiar una firma pública de interfaz no compila a medias, así que todo aterriza en un solo commit. El DTO `CreateTrackRequest` **NO se elimina** — sigue siendo el contrato de red del `VinilosApiService`; solo deja de aparecer en `domain/`. Sin cambio de comportamiento: la suite existente (unit + androidTest) es la red de seguridad que prueba que nada se rompió. La única conducta nueva digna de aserción explícita es "`AlbumRepositoryImpl.addTrack` mapea `NewTrack` → `CreateTrackRequest` antes de llamar al API".

**Ripple medido (18 archivos):**
- Producción (5): `domain/model/NewTrack.kt` (crear), `domain/repository/AlbumRepository.kt`, `domain/usecase/AddTrackUseCase.kt`, `presentation/viewmodel/AddTrackViewModel.kt`, `data/repository/AlbumRepositoryImpl.kt`.
- Tests unitarios (8): `AlbumRepositoryImplTest`, `AddTrackUseCaseTest`, `AddTrackViewModelTest`, `AlbumDetailViewModelTest`, `AlbumListViewModelTest`, `AddCommentViewModelTest`, `AddPerformerToAlbumViewModelTest`, `CreateAlbumViewModelTest`, `AddAlbumToBandViewModelTest`, `AddAlbumToMusicianViewModelTest` — (10 archivos, todos con fake inline que implementa `AlbumRepository`).
- Tests instrumentados (3): `di/FakeAlbumRepository.kt` (fake compartido Hilt), `screens/album/AlbumListScreenTest.kt`, `screens/album/CreateAlbumScreenTest.kt`.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1 (new DSL — `./gradlew test --tests` NO funciona; usar `testDebugUnitTest --tests`), Room 2.8.4, Coroutines 1.11.0, JUnit 4 + MockK + Turbine.

**Comandos base** (shell: PowerShell; `./gradlew` vía Bash):
- Suite unitaria completa: `./gradlew testDebugUnitTest`
- Una clase: `./gradlew testDebugUnitTest --tests "<FQN>"`
- Compilar (sin correr) los tests instrumentados: `./gradlew assembleDebugAndroidTest`
- Build debug: `./gradlew assembleDebug`

---

## Task 1 (A3): Sustituir `CreateTrackRequest` por `NewTrack` en la frontera de dominio

**Files (en orden de edición):**
- Create: `app/src/main/java/com/misw4203/vinilos/domain/model/NewTrack.kt`
- Modify (prod): `domain/repository/AlbumRepository.kt`, `domain/usecase/AddTrackUseCase.kt`, `presentation/viewmodel/AddTrackViewModel.kt`, `data/repository/AlbumRepositoryImpl.kt`
- Modify (test): los 13 archivos del ripple

Contexto: `CreateTrackRequest` es `data class CreateTrackRequest(@SerializedName("name") name: String, @SerializedName("duration") duration: String)`. `NewTrack` tendrá **los mismos nombres de propiedad** (`name`, `duration`) — esto hace que los cuerpos de fake que leen `request.name`/`request.duration` no cambien, solo el tipo del parámetro. El mapeo inline `CreateTrackRequest(track.name, track.duration)` dentro del impl replica el patrón existente de `addComment` (que construye `CreateCommentRequest` inline).

- [ ] **Step 1: Ajustar el test de mapeo primero (TDD — define el contrato nuevo)**

En `app/src/test/java/com/misw4203/vinilos/data/repository/AlbumRepositoryImplTest.kt`:

1. Cambiar el import (línea 9): `import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest` → mantenerlo (el test stubea `api.addTrack` que SIGUE recibiendo `CreateTrackRequest`) y **añadir** `import com.misw4203.vinilos.domain.model.NewTrack`.
2. Reescribir `addTrack returns mapped Track from API` (líneas 95-105) para que pruebe explícitamente el mapeo dominio→DTO:

```kotlin
    @Test
    fun `addTrack maps NewTrack to CreateTrackRequest and returns mapped Track`() = runTest {
        val sentToApi = slot<CreateTrackRequest>()
        coEvery { api.addTrack(100L, capture(sentToApi)) } returns TrackDto(1L, "Get Lucky", "04:08")

        val result = repository.addTrack(100L, NewTrack("Get Lucky", "04:08"))

        assertEquals(CreateTrackRequest("Get Lucky", "04:08"), sentToApi.captured)
        assertEquals(1L, result.id)
        assertEquals("Get Lucky", result.name)
        assertEquals("04:08", result.duration)
    }
```

3. En las líneas 110, 118, 216, 230: reemplazar cada `CreateTrackRequest("X", "01:00")` / `CreateTrackRequest("Karma Police", "04:21")` por `NewTrack(...)` con los mismos valores (son la entrada al `repository.addTrack`, que ahora toma `NewTrack`). Los stubs `api.addTrack(..., any())` no cambian.

`slot` ya está importado (se usa en línea 213); si no, añadir `import io.mockk.slot`.

- [ ] **Step 2: Run el test para verificar que falla por compilación**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.data.repository.AlbumRepositoryImplTest"`
Expected: FAIL de compilación — `NewTrack` no existe aún y `repository.addTrack` aún toma `CreateTrackRequest`.

- [ ] **Step 3: Crear el modelo de dominio `NewTrack`**

Crear `app/src/main/java/com/misw4203/vinilos/domain/model/NewTrack.kt`:

```kotlin
package com.misw4203.vinilos.domain.model

/**
 * Datos de entrada para crear un track. Modelo de dominio puro:
 * la capa de datos lo mapea a su DTO de red.
 */
data class NewTrack(
    val name: String,
    val duration: String,
)
```

- [ ] **Step 4: Cambiar la firma en `AlbumRepository`**

En `app/src/main/java/com/misw4203/vinilos/domain/repository/AlbumRepository.kt`:
- Quitar `import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest`
- Añadir `import com.misw4203.vinilos.domain.model.NewTrack`
- Línea 13: `suspend fun addTrack(albumId: Long, request: CreateTrackRequest): Track` → `suspend fun addTrack(albumId: Long, track: NewTrack): Track`

- [ ] **Step 5: Cambiar `AddTrackUseCase`**

En `app/src/main/java/com/misw4203/vinilos/domain/usecase/AddTrackUseCase.kt`:
- Quitar `import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest`
- Añadir `import com.misw4203.vinilos.domain.model.NewTrack`
- `suspend operator fun invoke(albumId: Long, request: CreateTrackRequest): Track = repository.addTrack(albumId, request)` → parámetro `track: NewTrack`, cuerpo `repository.addTrack(albumId, track)`

- [ ] **Step 6: Mapear `NewTrack` → `CreateTrackRequest` dentro de `AlbumRepositoryImpl`**

En `app/src/main/java/com/misw4203/vinilos/data/repository/AlbumRepositoryImpl.kt` (mantener el import de `CreateTrackRequest` — ahora se usa internamente). Línea 51-53:

```kotlin
    override suspend fun addTrack(albumId: Long, request: CreateTrackRequest): Track =
        withContext(Dispatchers.IO) {
            val dto = api.addTrack(albumId, request)
```
→
```kotlin
    override suspend fun addTrack(albumId: Long, track: NewTrack): Track =
        withContext(Dispatchers.IO) {
            val dto = api.addTrack(albumId, CreateTrackRequest(track.name, track.duration))
```
Añadir `import com.misw4203.vinilos.domain.model.NewTrack`. El resto del método (write-through cache) no cambia.

- [ ] **Step 7: Actualizar `AddTrackViewModel`**

En `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AddTrackViewModel.kt`:
- Quitar `import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest`
- Añadir `import com.misw4203.vinilos.domain.model.NewTrack`
- Línea 48: `addTrack(albumId, CreateTrackRequest(name.trim(), duration.trim()))` → `addTrack(albumId, NewTrack(name.trim(), duration.trim()))`

- [ ] **Step 8: Barrer los 9 fakes inline restantes (unit) + `AddTrackUseCaseTest`**

En cada archivo cambiar el tipo del parámetro de `addTrack` de `...data.remote.dto.CreateTrackRequest` a `com.misw4203.vinilos.domain.model.NewTrack` (los cuerpos `error("...")` o `result.getOrThrow()` o `Track(1L, request.name, request.duration)` NO cambian — `NewTrack` tiene `name`/`duration`):

- `AddTrackViewModelTest.kt:6` — cambiar el import corto `CreateTrackRequest` → `import com.misw4203.vinilos.domain.model.NewTrack`; firma en `:41` usa el nombre simple `NewTrack`.
- `AlbumDetailViewModelTest.kt:42-43` — FQN inline → `com.misw4203.vinilos.domain.model.NewTrack` (cuerpo `Track(1L, request.name, request.duration)` intacto).
- `AlbumListViewModelTest.kt:37`, `AddCommentViewModelTest.kt:41`, `AddPerformerToAlbumViewModelTest.kt:47`, `CreateAlbumViewModelTest.kt:33`, `AddAlbumToBandViewModelTest.kt:50`, `AddAlbumToMusicianViewModelTest.kt:50` — FQN inline → `com.misw4203.vinilos.domain.model.NewTrack`.
- `AddTrackUseCaseTest.kt`: import línea 3 → `import com.misw4203.vinilos.domain.model.NewTrack`; líneas 32/45/53 `CreateTrackRequest(...)` → `NewTrack(...)`; línea 39 `coVerify { repository.addTrack(100L, request) }` sigue válido (ahora `request` es `NewTrack`).

- [ ] **Step 9: Run la suite unitaria completa**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. Incluye el test de mapeo nuevo + toda la regresión.

- [ ] **Step 10: Barrer los 3 fakes instrumentados y compilarlos**

- `app/src/androidTest/java/com/misw4203/vinilos/di/FakeAlbumRepository.kt`: import línea 9 `CreateTrackRequest` → `import com.misw4203.vinilos.domain.model.NewTrack`; firma `:20` parámetro `NewTrack`; cuerpo `Track(id = 999L, name = request.name, duration = request.duration)` intacto.
- `screens/album/AlbumListScreenTest.kt:31` y `screens/album/CreateAlbumScreenTest.kt:37`: FQN inline `...data.remote.dto.CreateTrackRequest` → `com.misw4203.vinilos.domain.model.NewTrack` (cuerpos `error("unused")` intactos).

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew assembleDebugAndroidTest`
Expected: `BUILD SUCCESSFUL` (compila los androidTest sin emulador).

- [ ] **Step 11: Verificar que `domain/` ya no importa el DTO de red**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && git grep -n "data.remote.dto.CreateTrackRequest" -- "app/src/main/java/com/misw4203/vinilos/domain" "app/src/main/java/com/misw4203/vinilos/presentation"; echo "exit:$?"`
Expected: sin coincidencias (`exit:1`). El DTO solo debe vivir en `data/`.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/domain/model/NewTrack.kt \
        app/src/main/java/com/misw4203/vinilos/domain/repository/AlbumRepository.kt \
        app/src/main/java/com/misw4203/vinilos/domain/usecase/AddTrackUseCase.kt \
        app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AddTrackViewModel.kt \
        app/src/main/java/com/misw4203/vinilos/data/repository/AlbumRepositoryImpl.kt \
        app/src/test/java/com/misw4203/vinilos/ \
        app/src/androidTest/java/com/misw4203/vinilos/
git commit -m "refactor(domain): NewTrack en vez del DTO de red en AlbumRepository.addTrack

A3: introduce domain/model/NewTrack; AlbumRepository/AddTrackUseCase ya
no exponen CreateTrackRequest. El mapeo NewTrack -> CreateTrackRequest
queda confinado en AlbumRepositoryImpl (el DTO sigue siendo el contrato
de red de VinilosApiService). Barrido de 13 fakes de test. Sin cambio
de comportamiento; +1 aserción explícita del mapeo.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Self-review

- **Cobertura del spec:** A3 → Task 1 único (refactor atómico, no divisible: una firma de interfaz no compila a medias).
- **Sin placeholders:** cada step incluye el código/edición exacta y comando con salida esperada.
- **Consistencia de tipos:** `NewTrack` reusa los nombres `name`/`duration` de `CreateTrackRequest` → cuerpos de fake intactos, solo cambia el tipo del parámetro. `CreateTrackRequest` sigue siendo data class → la igualdad estructural del `slot.captured` y de los `coVerify` se mantiene. Firmas públicas de `AlbumDetailUiState`/eventos intactas.
- **Frontera Clean Arch:** Step 11 verifica por grep que `domain/` y `presentation/` ya no importan el DTO de red.
- **Red de seguridad:** Step 9 (unit) + Step 10 (compilación androidTest) prueban ausencia de regresión; Step 1 añade la única aserción de comportamiento nuevo (mapeo).

## Fases posteriores (roadmap)

- **Fase 3 — M1 + A2 juntas.** M1 (eliminar los `= Unit` por defecto en `AlbumRepository`) solo es seguro tras crear una clase base de fake reutilizable en el source set de test; conviene con A2 (modelo de error de dominio + helper `runCatchingDomain`) porque tocan el mismo conjunto de ViewModels y fakes.
