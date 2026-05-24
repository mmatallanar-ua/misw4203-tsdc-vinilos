# Mejoras Fase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar las tres mejoras aisladas y de bajo riesgo del backlog (A1 secretos del keystore, B1 robustez de `Converters`, M9 actualización optimista atómica) sin alterar contratos públicos.

**Architecture:** Cada tarea es autocontenida y commiteable por separado. A1 toca solo Gradle/VCS; B1 toca solo la capa `data/local/converter` + un test JVM nuevo; M9 toca solo `AlbumDetailViewModel` y su test existente. Ninguna cambia firmas de interfaces, por lo que no hay ripple a otros módulos ni a los fakes de test.

**Tech Stack:** Kotlin 2.2.10, Gradle KTS + AGP 9.2.1, Room 2.8.4 (TypeConverters + Gson), Coroutines 1.11.0 (`StateFlow`), JUnit 4 + Turbine + `kotlinx-coroutines-test`.

**Comandos base** (shell: PowerShell; el wrapper `./gradlew` corre vía Bash/Git-Bash):
- Test completo: `./gradlew test`
- Una clase: `./gradlew test --tests "<FQN>"`
- Build debug (no requiere keystore): `./gradlew assembleDebug`

---

## Task 1 (A1): Externalizar credenciales del keystore

**Files:**
- Modify: `app/build.gradle.kts` (bloque `signingConfigs` líneas 22-29, e import al inicio)
- Modify: `.gitignore` (sección `# Keystore files`)
- Create: `keystore.properties.example`
- VCS: dejar de trackear `app/vinilos-release.jks` (permanece en disco)

Contexto verificado: `app/vinilos-release.jks` **está trackeado** por git; `.gitignore` ya tiene `#*.jks` / `#*.keystore` comentados; el `buildType` `debug` NO referencia el signing de release, así que `assembleDebug` y `test` siguen funcionando sin `keystore.properties`.

- [ ] **Step 1: Reemplazar el bloque `signingConfigs` por carga externa**

En `app/build.gradle.kts`, añadir imports al inicio del archivo (antes de `plugins {`):

```kotlin
import java.io.FileInputStream
import java.util.Properties
```

Reemplazar el bloque actual (líneas 22-29):

```kotlin
    signingConfigs {
        create("release") {
            storeFile = file("vinilos-release.jks")
            storePassword = "vinilos123"
            keyAlias = "vinilos"
            keyPassword = "vinilos123"
        }
    }
```

por:

```kotlin
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
    }
    fun keystoreValue(propKey: String, envKey: String): String? =
        keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

    signingConfigs {
        create("release") {
            val storePw = keystoreValue("storePassword", "VINILOS_STORE_PASSWORD")
            val keyPw = keystoreValue("keyPassword", "VINILOS_KEY_PASSWORD")
            val alias = keystoreValue("keyAlias", "VINILOS_KEY_ALIAS")
            val storePath = keystoreValue("storeFile", "VINILOS_STORE_FILE")
                ?: "vinilos-release.jks"
            if (storePw != null && keyPw != null && alias != null) {
                storeFile = file(storePath)
                storePassword = storePw
                keyAlias = alias
                keyPassword = keyPw
            }
        }
    }
```

- [ ] **Step 2: Crear `keystore.properties.example`**

Crear `keystore.properties.example` en la raíz del repo:

```properties
# Copia este archivo a keystore.properties (gitignored) y rellena con los valores reales.
# Alternativamente exporta las variables de entorno VINILOS_STORE_PASSWORD / VINILOS_KEY_PASSWORD /
# VINILOS_KEY_ALIAS / VINILOS_STORE_FILE.
storePassword=changeme
keyPassword=changeme
keyAlias=vinilos
storeFile=vinilos-release.jks
```

- [ ] **Step 3: Actualizar `.gitignore`**

En `.gitignore`, reemplazar la sección:

```
# Keystore files
# Uncomment the following lines if you do not want to check your keystore files in.
#*.jks
#*.keystore
```

por:

```
# Keystore files
*.jks
*.keystore
keystore.properties
```

- [ ] **Step 4: Dejar de trackear el `.jks` (sigue en disco)**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && git rm --cached app/vinilos-release.jks`
Expected: `rm 'app/vinilos-release.jks'`. El archivo NO se borra del disco (solo del índice).

- [ ] **Step 5: Verificar que el build debug sigue verde sin keystore**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. (debug no usa el signing de release).

- [ ] **Step 6: Verificar que no quedan secretos en texto plano**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && git grep -n "vinilos123" -- app/build.gradle.kts; echo "exit:$?"`
Expected: sin coincidencias (`exit:1` de grep = no matches).

- [ ] **Step 7: Commit**

```bash
git add app/build.gradle.kts .gitignore keystore.properties.example
git rm --cached app/vinilos-release.jks
git commit -m "fix(security): externalizar credenciales del keystore fuera del build script

A1: mueve storePassword/keyPassword/keyAlias a keystore.properties
(gitignored) o variables de entorno; deja de trackear el .jks.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2 (B1): Endurecer `Converters` (Gson único, TypeTokens izados, null-safety)

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt`
- Test: `app/src/test/java/com/misw4203/vinilos/data/local/converter/ConvertersTest.kt` (crear)

Contexto: hoy `Converters` crea `Gson()` por instancia y un `object : TypeToken` nuevo en cada llamada; `jsonToX("null")` o un blob malformado devuelve `null` donde el código espera `List`. La firma pública de cada `@TypeConverter` no cambia (Room sigue compilando igual).

- [ ] **Step 1: Escribir el test que falla**

Crear `app/src/test/java/com/misw4203/vinilos/data/local/converter/ConvertersTest.kt`:

```kotlin
package com.misw4203.vinilos.data.local.converter

import com.misw4203.vinilos.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `tracks round trip preserves data`() {
        val tracks = listOf(
            Track(1L, "Decisiones", "5:30"),
            Track(2L, "Desapariciones", "6:10"),
        )
        val json = converters.tracksToJson(tracks)
        assertEquals(tracks, converters.jsonToTracks(json))
    }

    @Test
    fun `jsonToTracks returns empty list for empty list json`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks(converters.tracksToJson(emptyList())))
    }

    @Test
    fun `jsonToTracks returns empty list for literal null`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks("null"))
    }

    @Test
    fun `jsonToTracks returns empty list for blank string`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks(""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test --tests "com.misw4203.vinilos.data.local.converter.ConvertersTest"`
Expected: FAIL — `jsonToTracks returns empty list for literal null` y `for blank string` lanzan/retornan `null` (la implementación actual no tiene null-safety).

- [ ] **Step 3: Reescribir `Converters.kt` con Gson único, TypeTokens izados y null-safety**

Reemplazar el contenido completo de `app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt` por:

```kotlin
package com.misw4203.vinilos.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import java.lang.reflect.Type

class Converters {

    private companion object {
        val GSON = Gson()
        val TRACK_LIST: Type = object : TypeToken<List<Track>>() {}.type
        val PERFORMER_LIST: Type = object : TypeToken<List<Performer>>() {}.type
        val COMMENT_LIST: Type = object : TypeToken<List<Comment>>() {}.type
        val ALBUM_LIST: Type = object : TypeToken<List<Album>>() {}.type
        val PRIZE_LIST: Type = object : TypeToken<List<MusicianPrize>>() {}.type
        val COLLECTOR_ALBUM_LIST: Type = object : TypeToken<List<CollectorAlbum>>() {}.type
        val COLLECTOR_COMMENT_LIST: Type = object : TypeToken<List<CollectorComment>>() {}.type
        val MUSICIAN_SUMMARY_LIST: Type = object : TypeToken<List<MusicianSummary>>() {}.type
    }

    private inline fun <reified T> decode(value: String, type: Type): List<T> =
        runCatching { GSON.fromJson<List<T>>(value, type) }.getOrNull() ?: emptyList()

    @TypeConverter
    fun tracksToJson(value: List<Track>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToTracks(value: String): List<Track> = decode(value, TRACK_LIST)

    @TypeConverter
    fun performersToJson(value: List<Performer>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToPerformers(value: String): List<Performer> = decode(value, PERFORMER_LIST)

    @TypeConverter
    fun commentsToJson(value: List<Comment>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToComments(value: String): List<Comment> = decode(value, COMMENT_LIST)

    @TypeConverter
    fun albumsToJson(value: List<Album>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToAlbums(value: String): List<Album> = decode(value, ALBUM_LIST)

    @TypeConverter
    fun prizesToJson(value: List<MusicianPrize>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToPrizes(value: String): List<MusicianPrize> = decode(value, PRIZE_LIST)

    @TypeConverter
    fun collectorAlbumsToJson(value: List<CollectorAlbum>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToCollectorAlbums(value: String): List<CollectorAlbum> =
        decode(value, COLLECTOR_ALBUM_LIST)

    @TypeConverter
    fun collectorCommentsToJson(value: List<CollectorComment>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToCollectorComments(value: String): List<CollectorComment> =
        decode(value, COLLECTOR_COMMENT_LIST)

    @TypeConverter
    fun musicianSummariesToJson(value: List<MusicianSummary>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToMusicianSummaries(value: String): List<MusicianSummary> =
        decode(value, MUSICIAN_SUMMARY_LIST)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test --tests "com.misw4203.vinilos.data.local.converter.ConvertersTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Regresión — toda la suite unitaria sigue verde**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test`
Expected: `BUILD SUCCESSFUL` (las firmas `@TypeConverter` no cambiaron; entidades Room sin cambios).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt app/src/test/java/com/misw4203/vinilos/data/local/converter/ConvertersTest.kt
git commit -m "refactor(data): endurecer Converters (Gson único, TypeTokens izados, null-safe)

B1: una instancia Gson + Type constantes en companion; jsonToX ahora
devuelve emptyList ante null/blob malformado en vez de propagar null.
Firmas @TypeConverter intactas. +4 tests JVM.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3 (M9): Actualización optimista atómica con rollback por id+índice

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModel.kt`
- Test: `app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModelTest.kt:230` (añadir test de regresión antes de `private fun sampleDetail()`)

Contexto: hoy `removeTrack`/`removeComment` hacen `_uiState.value = ...` (no atómico) y en fallo restauran un snapshot `current` completo capturado antes; dos remociones rápidas con fallo restauran un estado stale (pierden la 2ª remoción). El fix: `_uiState.update {}` + rollback idempotente reinsertando solo el elemento en su índice original.

- [ ] **Step 1: Escribir el test de regresión que falla**

En `app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModelTest.kt`, insertar este test justo antes de la línea `}` que cierra la clase (antes de la línea 231, después del test `failed track removal restores state and emits network failure`):

```kotlin
    @Test
    fun `two failing removals each restore their own track at original position`() = runTest {
        val repo = FakeAlbumRepository().apply { removeError = IOException("offline") }
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val album = (viewModel.uiState.value as AlbumDetailUiState.Success).album
        val track1 = album.tracks[0]
        val track2 = album.tracks[1]

        viewModel.removeTrack(track1)
        viewModel.removeTrack(track2)
        advanceUntilIdle()

        val restored = (viewModel.uiState.value as AlbumDetailUiState.Success).album.tracks
        assertEquals(2, restored.size)
        assertEquals(track1.id, restored[0].id)
        assertEquals(track2.id, restored[1].id)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test --tests "com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModelTest"`
Expected: FAIL en `two failing removals...` — con el snapshot-restore actual la lista queda con 1 track (la 2ª remoción se pierde), `assertEquals(2, restored.size)` falla.

- [ ] **Step 3: Reescribir `AlbumDetailViewModel.kt` con `update {}` + rollback por id+índice**

Reemplazar el contenido completo de `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModel.kt` por:

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveCommentUseCase
import com.misw4203.vinilos.domain.usecase.RemoveTrackUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface AlbumDetailEvent {
    data class Removed(val name: String) : AlbumDetailEvent
    data class RemoveFailed(val isNetworkError: Boolean) : AlbumDetailEvent
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumDetail: GetAlbumDetailUseCase,
    private val removeTrackUseCase: RemoveTrackUseCase,
    private val removeCommentUseCase: RemoveCommentUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Destinations.AlbumDetailArg])

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AlbumDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AlbumDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun removeTrack(track: Track) {
        val album = (_uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        val index = album.tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)?.let {
                AlbumDetailUiState.Success(
                    it.album.copy(tracks = it.album.tracks.filterNot { t -> t.id == track.id }),
                )
            } ?: state
        }
        viewModelScope.launch {
            try {
                removeTrackUseCase(albumId, track.id)
                _events.tryEmit(AlbumDetailEvent.Removed(track.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restoreTrack(track, index, isNetworkError = false)
            } catch (e: IOException) {
                restoreTrack(track, index, isNetworkError = true)
            } catch (e: Exception) {
                restoreTrack(track, index, isNetworkError = false)
            }
        }
    }

    fun removeComment(comment: Comment) {
        val album = (_uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        val index = album.comments.indexOfFirst { it.id == comment.id }
        if (index < 0) return
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)?.let {
                AlbumDetailUiState.Success(
                    it.album.copy(comments = it.album.comments.filterNot { c -> c.id == comment.id }),
                )
            } ?: state
        }
        viewModelScope.launch {
            try {
                removeCommentUseCase(albumId, comment.id)
                _events.tryEmit(AlbumDetailEvent.Removed(comment.description))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restoreComment(comment, index, isNetworkError = false)
            } catch (e: IOException) {
                restoreComment(comment, index, isNetworkError = true)
            } catch (e: Exception) {
                restoreComment(comment, index, isNetworkError = false)
            }
        }
    }

    private fun restoreTrack(track: Track, index: Int, isNetworkError: Boolean) {
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)
                ?.takeIf { s -> s.album.tracks.none { it.id == track.id } }
                ?.let { s ->
                    val list = s.album.tracks.toMutableList()
                    list.add(index.coerceIn(0, list.size), track)
                    AlbumDetailUiState.Success(s.album.copy(tracks = list))
                } ?: state
        }
        _events.tryEmit(AlbumDetailEvent.RemoveFailed(isNetworkError))
    }

    private fun restoreComment(comment: Comment, index: Int, isNetworkError: Boolean) {
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)
                ?.takeIf { s -> s.album.comments.none { it.id == comment.id } }
                ?.let { s ->
                    val list = s.album.comments.toMutableList()
                    list.add(index.coerceIn(0, list.size), comment)
                    AlbumDetailUiState.Success(s.album.copy(comments = list))
                } ?: state
        }
        _events.tryEmit(AlbumDetailEvent.RemoveFailed(isNetworkError))
    }

    private fun load() {
        _uiState.value = AlbumDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                AlbumDetailUiState.Success(getAlbumDetail(albumId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) AlbumDetailUiState.NotFound
                else AlbumDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                AlbumDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                AlbumDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
```

Nota: se elimina el import ahora innecesario de `AlbumDetail` (el `restore` viejo lo usaba); `Comment` y `Track` siguen importados.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test --tests "com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModelTest"`
Expected: PASS — los 9 tests previos + `two failing removals each restore their own track at original position`. En particular siguen verdes `removeTrack calls repository and removes optimistically` (1 track restante), `removeComment ...` (0 comments) y `failed track removal restores state and emits network failure` (2 tracks).

- [ ] **Step 5: Regresión — suite unitaria completa**

Run: `cd "C:/Users/alejo/Documents/Proyectos/misw4203-tsdc-vinilos" && ./gradlew test`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModel.kt app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/AlbumDetailViewModelTest.kt
git commit -m "fix(vm): actualización optimista atómica en AlbumDetailViewModel

M9: usa MutableStateFlow.update {} y rollback idempotente por id+índice
en vez de reemplazar un snapshot completo capturado antes. Evita perder
remociones concurrentes cuando varias fallan. +1 test de regresión.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Cierre de Fase 1

- [ ] **Step final: actualizar el historial en `MEJORAS.md`**

Añadir bajo `# Mejoras aplicadas` (después de la última fase, antes del "Resumen cuantitativo") una sección `## Fase L — Backlog Fase 1 (A1, B1, M9)` describiendo los tres commits, decisiones (no se cambiaron firmas públicas; A3/M1 reprogramados) y métricas (+5 tests JVM). `MEJORAS.md` está gitignored — no se commitea.

---

## Fases posteriores (roadmap, aún no detalladas en tareas)

No forman parte de la ejecución de este plan; se detallarán en su propio plan cuando se aborden.

- **Fase 2 — A3 (quitar el DTO del dominio).** Introducir `domain/model/NewTrack`, cambiar la firma `AlbumRepository.addTrack(albumId, NewTrack)`, mapear a `CreateTrackRequest` dentro de `AlbumRepositoryImpl`, y actualizar los ~13 archivos de test con fakes inline que implementan `AlbumRepository`. Mecánico pero transversal — requiere su propio plan TDD.
- **Fase 3 — M1 + A2 juntas.** M1 (eliminar los `= Unit` por defecto en la interfaz) sólo es seguro tras crear una **clase base de fake reutilizable** en el source set de test; conviene hacerlo junto con A2 (modelo de error de dominio + helper `runCatchingDomain`) porque ambas tocan el mismo conjunto de ViewModels y fakes.

## Self-review

- **Cobertura del spec:** A1 → Task 1; B1 → Task 2; M9 → Task 3. A3/M1 explícitamente reprogramados con justificación (ripple real medido).
- **Sin placeholders:** cada step de código incluye el código completo y comandos con salida esperada.
- **Consistencia de tipos:** `restoreTrack`/`restoreComment` (nombres definidos en Task 3) usados consistentemente; firmas `@TypeConverter` sin cambios (Task 2 no rompe Room); `AlbumDetailEvent` y `AlbumDetailUiState` intactos (tests existentes siguen compilando).
</content>
