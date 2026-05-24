# Mejoras Fase 3 (M1 + A2 parcial) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** (M1) volver puramente abstractas las interfaces de repositorio que hoy tienen métodos `= Unit` por defecto (`AlbumRepository`, `CollectorRepository`, `BandRepository`) y dar una clase base de fake reutilizable en el source set de test; (A2, parcial) introducir un helper `runCatchingDomain` en presentación que centraliza el único `try/catch` de clasificación de error con orden canónico, y migrar **solo los ViewModels homogéneos** (rutas de carga list/detail que mapean a `Error(isNetworkError: Boolean)` / `NotFound` / `Empty`). Los formularios POST heterogéneos (Error con `message`/eventos) quedan para **Fase 4**.

**Decisiones tomadas (usuario):** A2 = helper compartido en presentación (no modelo de error en `domain`, no se tocan repos Impl). M1 = los 3 repos con defaults. Faseado: Fase 3 = infra + M1 + VMs homogéneos; Fase 4 = resto.

**Architecture / contrato a preservar:** Los repo Impl interceptan `IOException` SOLO en métodos de lectura para servir caché (network-first); cuando la caché está vacía relanzan `IOException`, y `HttpException` siempre se propaga. **No se tocan los Impl** — el helper vive en presentación y reproduce el orden canónico. Comportamiento observable de los VMs migrados: idéntico (mismo `UiState` para cada excepción); además se **corrige la deriva** GROUP-A/GROUP-B (todos pasan a orden canónico `Cancellation → Http(404 split) → IO → Exception`), lo cual NO cambia el `UiState` resultante porque `HttpException` e `IOException` son disjuntos.

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1 (new DSL — usar `testDebugUnitTest`, NO `test --tests`), Coroutines 1.11.0, JUnit4 + MockK + Turbine.

**Comandos base** (PowerShell; `./gradlew` vía Bash):
- Suite unitaria: `./gradlew testDebugUnitTest`
- Una clase: `./gradlew testDebugUnitTest --tests "<FQN>"`
- Compilar instrumentados (sin emulador): `./gradlew assembleDebugAndroidTest`

**Scope map (verificado, fuente de verdad para los barridos):**
- Interfaces con `= Unit`: `AlbumRepository` (`removeTrack`, `removeComment`, `addMusicianToAlbum`, `addBandToAlbum` — líneas 19,20,22,23); `CollectorRepository` (`removeFavoriteMusician`, `removeFavoriteBand`, `removeAlbumFromCollector` — 15,16,17); `BandRepository` (`addAlbumToBand`, `addPrizeToBand` — 13,14).
- VMs homogéneos a migrar (solo ruta de carga): `AlbumListViewModel`, `MusicianListViewModel`, `CollectorListViewModel`, `BandListViewModel`, `PrizesViewModel` (list, `Empty`, sin 404); `AlbumDetailViewModel.load()`, `MusicianDetailViewModel.loadMusician()`, `CollectorDetailViewModel.load()`, `BandDetailViewModel.loadBand()` (detail, `NotFound` en 404). **9 clases VM.** Las rutas `removeTrack/removeComment/removeFavorite/removeAlbum` y todos los `Add*ViewModel`/`Create*ViewModel` quedan para Fase 4.

---

## Task 1 (M1): Interfaces puramente abstractas + base-fakes reutilizables

**Atómico** (quitar defaults de una interfaz no compila a medias) → 1 commit. Mecánico pero transversal (~21 fakes).

**Files:**
- Modify: `domain/repository/AlbumRepository.kt`, `CollectorRepository.kt`, `BandRepository.kt` (quitar los `= Unit` y el comentario que los justifica).
- Create: `app/src/test/java/com/misw4203/vinilos/testsupport/FakeAlbumRepositoryBase.kt`, `FakeCollectorRepositoryBase.kt`, `FakeBandRepositoryBase.kt` (clases `open` con TODOS los métodos implementados con no-op/`error(...)` explícito y `open`, sobrescribibles).
- Modify (sweep): cada fake que hoy depende de un default eliminado.

Contexto: separar `src/test` y `src/androidTest` impide compartir una clase entre ambos. Estrategia: las clases base viven en `src/test/.../testsupport/`; los fakes anidados de unit-test que dependían de defaults reciben **overrides explícitos no-op** (aditivo, sin cambio de comportamiento — NO se reescriben para heredar la base, para acotar riesgo); los 5 fakes standalone Hilt de `androidTest/.../di/` reciben overrides explícitos no-op. Las clases base se entregan como herramienta sancionada para tests futuros (cumple el backlog M1: "clase base de fake con no-ops explícitos sobrescribibles").

- [ ] **Step 1: Crear las 3 clases base en `src/test/.../testsupport/`**

`FakeAlbumRepositoryBase.kt` — `open class FakeAlbumRepositoryBase : AlbumRepository` con cada método `open override`. Métodos abstractos de negocio → cuerpo `error("override in test: <name>")`. Los 4 ex-defaults (`removeTrack`, `removeComment`, `addMusicianToAlbum`, `addBandToAlbum`) → cuerpo no-op explícito (`{ }` / `= Unit`) `open`, comentados como "no-op de test, sobrescribir si se ejercita". Firmas EXACTAS de la interfaz (incluye `addTrack(albumId: Long, track: NewTrack): Track`). Análogo para `FakeCollectorRepositoryBase` (3 ex-defaults no-op) y `FakeBandRepositoryBase` (2 ex-defaults no-op).

- [ ] **Step 2: Quitar los `= Unit` de las 3 interfaces**

`AlbumRepository.kt`: borrar ` = Unit` de las 4 firmas (líneas ~19,20,22,23) y el comentario "Removal operations. Default no-op...". Quedan `suspend fun removeTrack(albumId: Long, trackId: Long)` etc. (abstractas). Igual en `CollectorRepository.kt` (3) y `BandRepository.kt` (2), borrando sus comentarios "Default no-op...".

- [ ] **Step 3: Compilar para enumerar los fakes rotos**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew compileDebugUnitTestKotlin 2>&1 | tail -40`
Expected: FALLA con errores "Class 'Fake…' is not abstract and does not implement abstract member …" listando cada fake. Usar esa lista (más el scope map abajo) como checklist.

Scope map de fakes a tocar (añadir override explícito no-op SOLO de los métodos que antes heredaban el default):

- **AlbumRepository — añadir los 4 (`removeTrack`,`removeComment`,`addMusicianToAlbum`,`addBandToAlbum`):**
  `androidTest/.../di/FakeAlbumRepository.kt`; unit nested: `AlbumListViewModelTest`, `AddTrackViewModelTest`, `AddCommentViewModelTest`, `CreateAlbumViewModelTest`, `AddAlbumToBandViewModelTest`, `AddAlbumToMusicianViewModelTest`.
- **AlbumRepository — añadir solo `removeTrack`+`removeComment`:** `AddPerformerToAlbumViewModelTest` (ya tiene `addMusicianToAlbum`/`addBandToAlbum`).
- **AlbumRepository — añadir solo `addMusicianToAlbum`+`addBandToAlbum`:** `AlbumDetailViewModelTest` (ya tiene `removeTrack`/`removeComment`).
- **CollectorRepository — añadir los 3 (`removeFavoriteMusician`,`removeFavoriteBand`,`removeAlbumFromCollector`):**
  `androidTest/.../di/FakeCollectorRepository.kt`; unit nested: `CollectorListViewModelTest`, `AddFavoritePerformerViewModelTest`. (`CollectorDetailViewModelTest` ya los implementa los 3 — no tocar.)
- **BandRepository — añadir los 2 (`addAlbumToBand`,`addPrizeToBand`):**
  `androidTest/.../di/FakeBandRepository.kt`; unit nested: `BandListViewModelTest`, `BandDetailViewModelTest`, `AddMusiciansToBandViewModelTest`, `AddFavoritePerformerViewModelTest`, `AddPerformerToAlbumViewModelTest`.
- **BandRepository — añadir solo `addPrizeToBand`:** `AddAlbumToBandViewModelTest` (ya tiene `addAlbumToBand`).
- **BandRepository — añadir solo `addAlbumToBand`:** `AddPrizeToBandViewModelTest` (ya tiene `addPrizeToBand`).
- Fakes que son `object : XRepository` anónimos en `AddPrizeToMusicianViewModelTest` implementan `MusicianRepository`/`PrizeRepository` (sin defaults) → **no requieren cambios**.

Cada override añadido: cuerpo no-op explícito, p.ej. `override suspend fun removeTrack(albumId: Long, trackId: Long) {}`. Firmas EXACTAS de la interfaz. No cambiar cuerpos existentes.

- [ ] **Step 4: Aplicar el barrido y recompilar hasta verde**

Iterar Step 3/Step 4 hasta que `./gradlew compileDebugUnitTestKotlin` pase. Confirmar que las firmas no-op coinciden con la interfaz (tipos `Long` vs `Int` exactos: `addBandToAlbum(albumId: Long, bandId: Int)`, `addAlbumToBand(bandId: Int, albumId: Long)`, `addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String)`, etc.).

- [ ] **Step 5: Regresión unit + compilación androidTest**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest`
Expected: ambos `BUILD SUCCESSFUL`. Sin cambio de comportamiento (overrides no-op == antiguos defaults `= Unit`).

- [ ] **Step 6: Verificar que no quedan defaults en las interfaces**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && git grep -nE "= Unit$" -- "app/src/main/java/com/misw4203/vinilos/domain/repository"; echo "exit:$?"`
Expected: sin coincidencias (`exit:1`).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/domain/repository/ app/src/test/java/com/misw4203/vinilos/testsupport/ app/src/test/java/com/misw4203/vinilos/ app/src/androidTest/java/com/misw4203/vinilos/
git commit -m "refactor(domain): interfaces de repositorio puramente abstractas (M1)

M1: quita los métodos no-op '= Unit' de AlbumRepository (4),
CollectorRepository (3) y BandRepository (2). La decisión de no-op
deja de vivir en la interfaz: ahora es explícita en cada fake.
Añade clases base de fake reutilizables en src/test/testsupport.
Barrido aditivo de ~21 fakes; sin cambio de comportamiento.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2 (A2 infra): helper `runCatchingDomain` + `DomainResult`

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/common/DomainResult.kt`
- Test: `app/src/test/java/com/misw4203/vinilos/presentation/common/RunCatchingDomainTest.kt` (crear)

Contexto: único punto del módulo que importa `retrofit2.HttpException` + `java.io.IOException` (en vez de 16 VMs). Orden canónico fijo. `DomainResult` vive en `presentation` (no en `domain`) por la decisión de "helper compartido".

- [ ] **Step 1: Escribir el test que falla (TDD)**

Crear `RunCatchingDomainTest.kt`:

```kotlin
package com.misw4203.vinilos.presentation.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RunCatchingDomainTest {
    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test fun `ok wraps value`() = runTest {
        assertEquals(DomainResult.Ok(42), runCatchingDomain { 42 })
    }

    @Test fun `404 maps to NotFound`() = runTest {
        assertEquals(DomainResult.NotFound, runCatchingDomain { throw http(404) })
    }

    @Test fun `non-404 http maps to Server`() = runTest {
        assertEquals(DomainResult.Server, runCatchingDomain { throw http(500) })
    }

    @Test fun `IOException maps to Network`() = runTest {
        assertEquals(DomainResult.Network, runCatchingDomain { throw IOException("offline") })
    }

    @Test fun `generic Exception maps to Server`() = runTest {
        assertEquals(DomainResult.Server, runCatchingDomain { throw IllegalStateException("x") })
    }

    @Test fun `CancellationException is rethrown not swallowed`() = runTest {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { runCatchingDomain { throw CancellationException() } }
        }
    }
}
```

- [ ] **Step 2: Run → FAIL** (no compila: `DomainResult`/`runCatchingDomain` no existen)

Run: `./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.presentation.common.RunCatchingDomainTest"`

- [ ] **Step 3: Implementar `DomainResult.kt`**

```kotlin
package com.misw4203.vinilos.presentation.common

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Resultado clasificado de una operación de datos, sin filtrar tipos de red a los VMs. */
sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>
    data object Network : DomainResult<Nothing>
    data object NotFound : DomainResult<Nothing>
    data object Server : DomainResult<Nothing>
}

/**
 * Ejecuta [block] y clasifica el fallo en el orden canónico único:
 * Cancellation (relanzada) → HttpException (404 ⇒ NotFound, resto ⇒ Server)
 * → IOException (Network) → Exception (Server).
 * Centraliza el try/catch que antes estaba duplicado y con deriva en los VMs.
 */
suspend inline fun <T> runCatchingDomain(block: () -> T): DomainResult<T> =
    try {
        DomainResult.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() == 404) DomainResult.NotFound else DomainResult.Server
    } catch (e: IOException) {
        DomainResult.Network
    } catch (e: Exception) {
        DomainResult.Server
    }
```

- [ ] **Step 4: Run → PASS** (6 tests). Luego `./gradlew testDebugUnitTest` completo verde.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/presentation/common/DomainResult.kt app/src/test/java/com/misw4203/vinilos/presentation/common/RunCatchingDomainTest.kt
git commit -m "feat(presentation): helper runCatchingDomain + DomainResult (A2)

A2 infra: centraliza la clasificación de error de red en un único punto
con orden canónico (Cancellation->Http(404)->IO->Exception), en vez de
duplicarla con deriva en 16 VMs. +6 tests.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3 (A2 migración): migrar los 9 VMs homogéneos al helper

**Files (modify):** los 9 VMs del scope map (solo su ruta de carga). Sus tests existentes NO se modifican (comportamiento preservado) — son la red de seguridad.

Patrón de migración (idéntico para los 5 list y los 4 detail, ajustando el mapeo):

**List VM** (ej. `AlbumListViewModel.load()`):
```kotlin
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
// QUITAR: import retrofit2.HttpException ; import java.io.IOException ; import kotlinx.coroutines.CancellationException (si ya no se usa en el archivo)

private fun load() {
    _uiState.value = AlbumListUiState.Loading
    viewModelScope.launch {
        _uiState.value = when (val r = runCatchingDomain { getAlbums() }) {
            is DomainResult.Ok ->
                if (r.value.isEmpty()) AlbumListUiState.Empty
                else AlbumListUiState.Success(r.value)
            DomainResult.Network -> AlbumListUiState.Error(isNetworkError = true)
            DomainResult.NotFound -> AlbumListUiState.Error(isNetworkError = false)
            DomainResult.Server -> AlbumListUiState.Error(isNetworkError = false)
        }
    }
}
```
(`MusicianListViewModel`, `CollectorListViewModel`, `BandListViewModel`, `PrizesViewModel` idénticos cambiando tipo de UiState/usecase. Para `PrizesViewModel`, que hoy NO tiene rama Http: el resultado para `HttpException` pasa de `Exception→isNetworkError=false` a `Server→isNetworkError=false` — **mismo UiState**, sin regresión.)

**Detail VM** (ej. `AlbumDetailViewModel.load()` — SOLO el método `load()`, NO tocar `removeTrack/removeComment`):
```kotlin
_uiState.value = when (val r = runCatchingDomain { getAlbumDetail(albumId) }) {
    is DomainResult.Ok -> AlbumDetailUiState.Success(r.value)
    DomainResult.Network -> AlbumDetailUiState.Error(isNetworkError = true)
    DomainResult.NotFound -> AlbumDetailUiState.NotFound
    DomainResult.Server -> AlbumDetailUiState.Error(isNetworkError = false)
}
```
(`MusicianDetailViewModel`, `CollectorDetailViewModel.load()`, `BandDetailViewModel` idénticos. Conservar el patrón `loadJob?.cancel()` donde exista — `runCatchingDomain` va DENTRO del `launch`, sin alterar la cancelación.)

Importante: quitar `import retrofit2.HttpException` y `import java.io.IOException` SOLO de los 9 VMs migrados. En los detail con bloques `remove*` que se difieren a Fase 4 (`AlbumDetailViewModel`, `CollectorDetailViewModel`), esos imports SIGUEN usándose en los `catch` de `removeTrack/removeComment/removeFavorite/removeAlbum` → **NO quitarlos** en esos dos archivos hasta Fase 4. Quitar `CancellationException` import solo si deja de usarse en el archivo.

- [ ] **Step 1: Migrar los 5 list VMs**

`AlbumListViewModel`, `MusicianListViewModel`, `CollectorListViewModel`, `BandListViewModel`, `PrizesViewModel`. Estos no tienen bloques `remove*` → sí quitar imports retrofit/io. Mantener `Empty` para lista vacía.

- [ ] **Step 2: Run tests de los 5 list VMs**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.AlbumListViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.CollectorListViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.BandListViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.PrizesViewModelTest"`
Expected: PASS sin tocar los tests (comportamiento idéntico).

- [ ] **Step 3: Migrar los 4 detail VMs (solo `load`/`loadMusician`/`loadBand`)**

`AlbumDetailViewModel.load()`, `MusicianDetailViewModel.loadMusician()`, `CollectorDetailViewModel.load()`, `BandDetailViewModel.loadBand()`. Mapear 404→`NotFound`. NO tocar bloques `remove*`. En `MusicianDetailViewModel`/`BandDetailViewModel` (sin `remove*`) sí quitar imports retrofit/io; en `AlbumDetailViewModel`/`CollectorDetailViewModel` **conservar** esos imports (los usan `remove*`, Fase 4).

- [ ] **Step 4: Run tests de los 4 detail VMs**

Run: `./gradlew testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.MusicianDetailViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.CollectorDetailViewModelTest" --tests "com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModelTest"`
Expected: PASS sin modificar los tests.

- [ ] **Step 5: Regresión completa + compilación androidTest**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew testDebugUnitTest && ./gradlew assembleDebugAndroidTest`
Expected: ambos `BUILD SUCCESSFUL`.

- [ ] **Step 6: Verificar reducción de la fuga Clean Arch**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && git grep -lE "import retrofit2.HttpException" -- "app/src/main/java/com/misw4203/vinilos/presentation/viewmodel" | wc -l`
Expected: bajó de 15 a 8 (los 7 list/detail sin `remove*` migrados — `MusicianListVM`,`AlbumListVM`,`CollectorListVM`,`BandListVM`,`PrizesVM`,`MusicianDetailVM`,`BandDetailVM` ya no lo importan; `AlbumDetailVM` y `CollectorDetailVM` lo conservan por `remove*`/Fase 4). Documentar el número real obtenido.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/
git commit -m "refactor(presentation): VMs homogéneos usan runCatchingDomain (A2)

A2 parcial: 9 VMs list/detail migrados al helper; eliminan el try/catch
duplicado y la deriva GROUP-A/GROUP-B (orden canónico unificado). 7 VMs
dejan de importar retrofit2.HttpException. Sin cambio de comportamiento
(tests existentes intactos y verdes). Forms POST -> Fase 4.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Cierre de Fase 3

- [ ] **Step final:** añadir sección `## Fase N — Backlog Fase 3 (M1 + A2 parcial)` a `MEJORAS.md` (gitignored, NO commitear) con los 3 commits, decisiones (helper vs modelo de dominio; faseado; M1 = 3 repos), métricas (+6 tests helper; 7 VMs sin retrofit; ~21 fakes saneados) y el pendiente Fase 4.

## Fase 4 (roadmap, no detallada aquí)

- **A2 resto:** migrar al helper (o a una variante con callbacks para eventos) los bloques `remove*` de `AlbumDetailViewModel`/`CollectorDetailViewModel` y los `Add*/Create*ViewModel` (Error con `message`/`isNetworkError`+campos extra, éxito vía `SharedFlow`/terminal). Incluir M3 (homogeneizar éxito de forms a evento one-shot) si se aborda junto.

## Self-review

- **Cobertura:** M1 → Task 1 (3 interfaces, scope map exhaustivo de fakes). A2 infra → Task 2 (TDD). A2 homogéneo → Task 3 (9 VMs). Heterogéneo explícitamente diferido a Fase 4 con criterio (forma de `UiState`/éxito distinta).
- **Sin cambio de comportamiento:** Task 1 overrides no-op == defaults previos; Task 3 mapea cada excepción al mismo `UiState` que antes y NO modifica los tests de VM (red de seguridad). El reordenamiento canónico no altera el resultado porque Http/IO son disjuntos.
- **Contrato preservado:** no se tocan los repo Impl → la caché network-first intacta.
- **Riesgos acotados:** imports retrofit/io NO se quitan de `AlbumDetailViewModel`/`CollectorDetailViewModel` (los siguen usando sus `remove*` de Fase 4) — evita romper compilación.
