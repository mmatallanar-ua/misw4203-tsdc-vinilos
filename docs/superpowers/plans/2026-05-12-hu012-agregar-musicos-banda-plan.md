# HU012 — Agregar músicos a una banda — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir a un coleccionista agregar músicos existentes a una banda del catálogo, manteniendo coherencia entre backend, caché local y UI.

**Architecture:** Clean Architecture en tres capas (domain/data/presentation), MVVM con Jetpack Compose. `Band` se modela como entidad de dominio aislada (approach C tras resolver R7: existe ya `data class Performer` en `AlbumDetail.kt` con shape distinto). Patrón network-first + fallback a caché para lecturas; write-through cache para `addMusicianToBand`. Filtro de búsqueda local con debounce 300ms. Sub-tabs internas dentro de la tab "Artistas" (Músicos / Bandas).

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Hilt + KSP, Room (con bump a v5), Retrofit + Gson, Navigation Compose, Coil, Material 3. Tests: JUnit4 + MockK + Turbine + `kotlinx-coroutines-test` (JVM), Compose UI Test + Hilt Testing (instrumentados).

**Reference spec:** `docs/superpowers/specs/2026-05-12-hu012-agregar-musicos-banda-design.md`

---

## File Structure

### Nuevos archivos

**Domain (`app/src/main/java/com/misw4203/vinilos/domain/`):**
- `model/BandSummary.kt` — modelo de lista
- `model/Band.kt` — modelo de detalle
- `repository/BandRepository.kt` — interfaz
- `usecase/GetBandsUseCase.kt`
- `usecase/GetBandDetailUseCase.kt`
- `usecase/AddMusicianToBandUseCase.kt`

**Data (`app/src/main/java/com/misw4203/vinilos/data/`):**
- `remote/dto/BandDto.kt` — listado
- `remote/dto/BandDetailDto.kt` — detalle (incluye `musicians: List<MusicianDetailDto>` y `albums: List<AlbumDto>`)
- `local/entity/BandListEntity.kt`
- `local/entity/BandDetailEntity.kt`
- `local/dao/BandDao.kt`
- `repository/BandRepositoryImpl.kt`

**Presentation (`app/src/main/java/com/misw4203/vinilos/presentation/`):**
- `viewmodel/BandListUiState.kt`
- `viewmodel/BandListViewModel.kt`
- `viewmodel/BandDetailUiState.kt` (incluye sealed `BandDetailUiState`)
- `viewmodel/BandDetailViewModel.kt`
- `viewmodel/AddMusiciansFormState.kt`
- `viewmodel/AddMusiciansUiState.kt`
- `viewmodel/AddMusiciansToBandViewModel.kt`
- `ui/components/BandCard.kt`
- `ui/components/MusicianRow.kt`
- `ui/components/EmptyMembersState.kt`
- `ui/components/PerformerHeader.kt`
- `ui/screens/band/BandListContent.kt`
- `ui/screens/band/BandDetailScreen.kt`
- `ui/screens/band/AddMusiciansToBandScreen.kt`
- `ui/screens/artist/ArtistsHubScreen.kt`
- `ui/screens/artist/MusicianListContent.kt` (extraído de `MusicianListScreen.kt`)

**Tests JVM (`app/src/test/`):**
- `data/repository/BandRepositoryImplTest.kt`
- `domain/usecase/GetBandsUseCaseTest.kt`
- `domain/usecase/GetBandDetailUseCaseTest.kt`
- `domain/usecase/AddMusicianToBandUseCaseTest.kt`
- `presentation/viewmodel/BandListViewModelTest.kt`
- `presentation/viewmodel/BandDetailViewModelTest.kt`
- `presentation/viewmodel/AddMusiciansToBandViewModelTest.kt`

**Tests instrumentados (`app/src/androidTest/`):**
- `presentation/ui/components/BandCardTest.kt`
- `presentation/ui/components/MusicianRowTest.kt`
- `presentation/ui/components/EmptyMembersStateTest.kt`
- `presentation/ui/screens/band/BandDetailScreenTest.kt`
- `presentation/ui/screens/band/AddMusiciansToBandScreenTest.kt`
- `di/FakeBandRepository.kt`

### Archivos a modificar

- `app/src/main/java/com/misw4203/vinilos/data/remote/api/VinilosApiService.kt` — 4 endpoints nuevos
- `app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt` — TypeConverter para `List<MusicianSummary>`
- `app/src/main/java/com/misw4203/vinilos/data/local/database/VinilosDatabase.kt` — agregar entities + DAO, bump version a 5
- `app/src/main/java/com/misw4203/vinilos/di/DatabaseModule.kt` — provee `BandDao`
- `app/src/main/java/com/misw4203/vinilos/di/RepositoryModule.kt` — bind de `BandRepository`
- `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/artist/MusicianListScreen.kt` — refactor → `MusicianListContent` (mismo archivo o nuevo)
- `app/src/main/java/com/misw4203/vinilos/presentation/navigation/Destinations.kt` — nuevas rutas
- `app/src/main/java/com/misw4203/vinilos/presentation/navigation/VinilosNavHost.kt` — registrar destinos + reemplazar `MusicianListScreen` por `ArtistsHubScreen`
- `app/src/main/res/values/strings.xml` — strings nuevos
- `app/src/androidTest/java/com/misw4203/vinilos/di/FakeRepositoryModule.kt` — bind de fake `BandRepository`
- `app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt` — caso E2E HU012

---

## Phase 1 — Domain models + DTOs (Commit 1 prep)

### Task 1: `BandSummary` domain model

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/domain/model/BandSummary.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.domain.model

data class BandSummary(
    val id: Int,
    val name: String,
    val image: String,
)
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: `Band` domain model

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/domain/model/Band.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.domain.model

data class Band(
    val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val creationDate: String,
    val members: List<MusicianSummary>,
    val albums: List<Album>,
)
```

> Nota: `creationDate` reemplaza al `birthDate` de `Musician`. El backend devuelve `creationDate` para bandas.

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: `BandDto` y `BandDetailDto`

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/data/remote/dto/BandDto.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/data/remote/dto/BandDetailDto.kt`

- [ ] **Step 1: Write `BandDto.kt`**

```kotlin
package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BandDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("creationDate") val creationDate: String?,
)
```

- [ ] **Step 2: Write `BandDetailDto.kt`**

```kotlin
package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BandDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("creationDate") val creationDate: String?,
    @SerializedName("musicians") val musicians: List<MusicianDetailDto>?,
    @SerializedName("albums") val albums: List<AlbumDto>?,
)
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Phase 2 — Data local (Room entities, DAO, converters, database)

### Task 4: `BandListEntity`

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/data/local/entity/BandListEntity.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.BandSummary

@Entity(
    tableName = "bands",
    indices = [Index(value = ["name"])],
)
data class BandListEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
) {
    fun toDomain() = BandSummary(
        id = id,
        name = name,
        image = image,
    )

    companion object {
        fun fromDomain(summary: BandSummary) = BandListEntity(
            id = summary.id,
            name = summary.name,
            image = summary.image,
        )
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: `BandDetailEntity` (con `members` y `albums` como JSON blob)

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/data/local/entity/BandDetailEntity.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.MusicianSummary

@Entity(tableName = "band_details")
data class BandDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val creationDate: String,
    val members: List<MusicianSummary>,
    val albums: List<Album>,
) {
    fun toDomain() = Band(
        id = id,
        name = name,
        image = image,
        description = description,
        creationDate = creationDate,
        members = members,
        albums = albums,
    )

    companion object {
        fun fromDomain(band: Band) = BandDetailEntity(
            id = band.id,
            name = band.name,
            image = band.image,
            description = band.description,
            creationDate = band.creationDate,
            members = band.members,
            albums = band.albums,
        )
    }
}
```

- [ ] **Step 2: Compile (Room requirá nuevo TypeConverter — esperar warning/error)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED — Room reporta missing TypeConverter para `List<MusicianSummary>` (se resuelve en Task 6).

---

### Task 6: TypeConverter para `List<MusicianSummary>` en `Converters`

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt`

- [ ] **Step 1: Agregar import y dos TypeConverters**

Añadir el import:

```kotlin
import com.misw4203.vinilos.domain.model.MusicianSummary
```

Agregar al final de la clase `Converters` (antes del `}` final):

```kotlin
    @TypeConverter
    fun musicianSummariesToJson(value: List<MusicianSummary>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToMusicianSummaries(value: String): List<MusicianSummary> =
        gson.fromJson(value, object : TypeToken<List<MusicianSummary>>() {}.type)
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (el error de Task 5 step 2 desaparece).

---

## Phase 3 — Data remote: API endpoints + Repository (TDD)

### Task 10: Cuatro endpoints nuevos en `VinilosApiService`

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/data/remote/api/VinilosApiService.kt`

- [ ] **Step 1: Agregar imports**

```kotlin
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.BandDto
import retrofit2.Response
```

- [ ] **Step 2: Agregar los cuatro endpoints antes del cierre de la interfaz**

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
    ): Response<Unit>
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 11: `BandRepository` interface (domain)

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/domain/repository/BandRepository.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary

interface BandRepository {
    suspend fun getBands(): List<BandSummary>
    suspend fun getBandDetail(id: Int): Band
    suspend fun addMusicianToBand(bandId: Int, musicianId: Int)
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 12: `BandRepositoryImpl` con tests (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/data/repository/BandRepositoryImplTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/data/repository/BandRepositoryImpl.kt`

- [ ] **Step 1: Escribir el test failing primero**

Crear `app/src/test/java/com/misw4203/vinilos/data/repository/BandRepositoryImplTest.kt`:

```kotlin
package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.BandDao
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.BandDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BandRepositoryImplTest {

    private val api: VinilosApiService = mockk()
    private val dao: BandDao = mockk(relaxed = true)
    private val repo = BandRepositoryImpl(api, dao)

    @Test
    fun `getBands network success caches and returns mapped list`() = runTest {
        coEvery { api.getBands() } returns listOf(
            BandDto(1, "Queen", "img1", "desc", "1970-01-01"),
            BandDto(2, "Aerosmith", "img2", "desc", "1970-01-01"),
        )

        val result = repo.getBands()

        assertEquals(2, result.size)
        assertEquals("Queen", result[0].name)
        coVerify { dao.replaceBands(any()) }
    }

    @Test
    fun `getBands IOException returns cache when populated`() = runTest {
        coEvery { api.getBands() } throws IOException("offline")
        coEvery { dao.getAll() } returns listOf(BandListEntity(1, "Queen", ""))

        val result = repo.getBands()

        assertEquals(1, result.size)
        assertEquals("Queen", result[0].name)
    }

    @Test
    fun `getBands IOException rethrows when cache empty`() = runTest {
        coEvery { api.getBands() } throws IOException("offline")
        coEvery { dao.getAll() } returns emptyList()

        try {
            repo.getBands()
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `getBands HttpException propagates`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        coEvery { api.getBands() } throws httpError

        try {
            repo.getBands()
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun `getBandDetail success upserts and returns mapped band`() = runTest {
        coEvery { api.getBandDetail(1) } returns BandDetailDto(
            id = 1,
            name = "Queen",
            image = "img",
            description = "desc",
            creationDate = "1970-01-01",
            musicians = listOf(
                MusicianDetailDto(10, "Freddie", "img-f", "", "1946-09-05", emptyList(), emptyList())
            ),
            albums = listOf(
                AlbumDto(100, "A Night at the Opera", "cover", "1975-01-01", null, "Rock", null, null, null, null)
            ),
        )

        val result = repo.getBandDetail(1)

        assertEquals("Queen", result.name)
        assertEquals(1, result.members.size)
        assertEquals("Freddie", result.members[0].name)
        coVerify { dao.upsertDetail(any()) }
    }

    @Test
    fun `getBandDetail IOException returns cache when available`() = runTest {
        coEvery { api.getBandDetail(1) } throws IOException("offline")
        coEvery { dao.getDetailById(1) } returns BandDetailEntity(
            id = 1, name = "Queen", image = "", description = "", creationDate = "",
            members = emptyList(), albums = emptyList(),
        )

        val result = repo.getBandDetail(1)
        assertEquals("Queen", result.name)
    }

    @Test
    fun `getBandDetail IOException rethrows when no cache`() = runTest {
        coEvery { api.getBandDetail(1) } throws IOException("offline")
        coEvery { dao.getDetailById(1) } returns null

        try {
            repo.getBandDetail(1)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `addMusicianToBand success write-through updates cached detail`() = runTest {
        val cachedBand = BandDetailEntity(
            id = 1, name = "Queen", image = "", description = "", creationDate = "",
            members = emptyList(), albums = emptyList(),
        )
        coEvery { dao.getDetailById(1) } returns cachedBand
        coEvery { api.addMusicianToBand(1, 10) } returns Response.success(Unit)
        coEvery { api.getMusicianDetail(10) } returns MusicianDetailDto(
            10, "Freddie", "img", "", "1946-09-05", emptyList(), emptyList()
        )

        repo.addMusicianToBand(1, 10)

        coVerify {
            dao.upsertDetail(match { it.members.size == 1 && it.members[0].name == "Freddie" })
        }
    }

    @Test
    fun `addMusicianToBand without cache only posts and skips local update`() = runTest {
        coEvery { dao.getDetailById(1) } returns null
        coEvery { api.addMusicianToBand(1, 10) } returns Response.success(Unit)

        repo.addMusicianToBand(1, 10)

        coVerify(exactly = 0) { dao.upsertDetail(any()) }
    }

    @Test
    fun `addMusicianToBand IOException rethrows leaving cache intact`() = runTest {
        coEvery { api.addMusicianToBand(1, 10) } throws IOException("offline")

        try {
            repo.addMusicianToBand(1, 10)
            fail("Expected IOException")
        } catch (e: IOException) {
            coVerify(exactly = 0) { dao.upsertDetail(any()) }
        }
    }
}
```

- [ ] **Step 2: Run test (debe fallar — la clase no existe aún)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.data.repository.BandRepositoryImplTest"`
Expected: Compilation FAIL — `Unresolved reference: BandRepositoryImpl`.

- [ ] **Step 3: Crear `BandRepositoryImpl`**

`app/src/main/java/com/misw4203/vinilos/data/repository/BandRepositoryImpl.kt`:

```kotlin
package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.BandDao
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.BandDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class BandRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: BandDao,
) : BandRepository {

    override suspend fun getBands(): List<BandSummary> = withContext(Dispatchers.IO) {
        try {
            val summaries = api.getBands().map { it.toSummary() }
            dao.replaceBands(summaries.map { BandListEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getBandDetail(id: Int): Band = withContext(Dispatchers.IO) {
        try {
            val dto = api.getBandDetail(id)
            val band = dto.toDomain()
            dao.upsertDetail(BandDetailEntity.fromDomain(band))
            band
        } catch (e: IOException) {
            dao.getDetailById(id)?.toDomain() ?: throw e
        }
    }

    override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = withContext(Dispatchers.IO) {
        api.addMusicianToBand(bandId, musicianId)
        // Write-through: si el detalle está cacheado, agregamos el nuevo miembro.
        val cached = dao.getDetailById(bandId)
        if (cached != null) {
            val musicianDto = api.getMusicianDetail(musicianId)
            val newMember = MusicianSummary(
                id = musicianDto.id,
                name = musicianDto.name,
                image = musicianDto.image,
                birthDate = musicianDto.birthDate,
            )
            val updated = cached.copy(members = cached.members + newMember)
            dao.upsertDetail(updated)
        }
        Unit
    }

    private fun BandDto.toSummary() = BandSummary(
        id = id,
        name = name.orEmpty(),
        image = image.orEmpty(),
    )

    private fun BandDetailDto.toDomain() = Band(
        id = id,
        name = name.orEmpty(),
        image = image.orEmpty(),
        description = description.orEmpty(),
        creationDate = creationDate.orEmpty(),
        members = musicians.orEmpty().map {
            MusicianSummary(it.id, it.name, it.image, it.birthDate)
        },
        albums = albums.orEmpty().map { it.toDomain() },
    )

    private fun AlbumDto.toDomain() = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )
}
```

- [ ] **Step 4: Run tests para verificar que pasan**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.data.repository.BandRepositoryImplTest"`
Expected: 10 tests PASS.

---

### Task 13: Binding en `RepositoryModule`

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/di/RepositoryModule.kt`

- [ ] **Step 1: Agregar imports**

```kotlin
import com.misw4203.vinilos.data.repository.BandRepositoryImpl
import com.misw4203.vinilos.domain.repository.BandRepository
```

- [ ] **Step 2: Agregar binding antes del cierre de la clase**

```kotlin
    @Binds
    @Singleton
    abstract fun bindBandRepository(impl: BandRepositoryImpl): BandRepository
```

- [ ] **Step 3: Verify compiles + Hilt resuelve dependencias**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit fase 1**

```bash
git add app/src/main/java/com/misw4203/vinilos/domain/model/BandSummary.kt \
        app/src/main/java/com/misw4203/vinilos/domain/model/Band.kt \
        app/src/main/java/com/misw4203/vinilos/domain/repository/BandRepository.kt \
        app/src/main/java/com/misw4203/vinilos/data/remote/dto/BandDto.kt \
        app/src/main/java/com/misw4203/vinilos/data/remote/dto/BandDetailDto.kt \
        app/src/main/java/com/misw4203/vinilos/data/remote/api/VinilosApiService.kt \
        app/src/main/java/com/misw4203/vinilos/data/local/entity/BandListEntity.kt \
        app/src/main/java/com/misw4203/vinilos/data/local/entity/BandDetailEntity.kt \
        app/src/main/java/com/misw4203/vinilos/data/local/dao/BandDao.kt \
        app/src/main/java/com/misw4203/vinilos/data/local/converter/Converters.kt \
        app/src/main/java/com/misw4203/vinilos/data/local/database/VinilosDatabase.kt \
        app/src/main/java/com/misw4203/vinilos/data/repository/BandRepositoryImpl.kt \
        app/src/main/java/com/misw4203/vinilos/di/DatabaseModule.kt \
        app/src/main/java/com/misw4203/vinilos/di/RepositoryModule.kt \
        app/src/test/java/com/misw4203/vinilos/data/repository/BandRepositoryImplTest.kt

git commit -m "$(cat <<'EOF'
feat(hu012): Se agregan dominio, DTOs, entidades, DAO y repositorio de Bandas

- Domain: Band, BandSummary, BandRepository
- DTOs: BandDto, BandDetailDto
- Room: BandListEntity, BandDetailEntity, BandDao + TypeConverter para
  List<MusicianSummary>. VinilosDatabase v5.
- API: 4 endpoints nuevos (/bands, /bands/{id}, /bands/{id}/musicians,
  POST /bands/{bandId}/musicians/{musicianId}).
- Repository network-first + fallback a caché; write-through cache en
  addMusicianToBand (re-fetch del músico para extender members).
- Hilt binding en RepositoryModule.
- 10 tests unitarios sobre BandRepositoryImpl cubriendo éxito, IOException,
  HttpException, cache hit/miss y write-through.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 4 — UseCases + tests

### Task 14: `GetBandsUseCase` con test (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/domain/usecase/GetBandsUseCaseTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/domain/usecase/GetBandsUseCase.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBandsUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = GetBandsUseCase(repo)

    @Test
    fun `invoke delegates to repository getBands`() = runTest {
        val expected = listOf(BandSummary(1, "Queen", "img"))
        coEvery { repo.getBands() } returns expected

        val result = useCase()

        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getBands() }
    }
}
```

- [ ] **Step 2: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.GetBandsUseCaseTest"`
Expected: Compilation FAIL — `Unresolved reference: GetBandsUseCase`.

- [ ] **Step 3: Crear el use case**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class GetBandsUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(): List<BandSummary> = repository.getBands()
}
```

- [ ] **Step 4: Run test y verificar que pasa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.GetBandsUseCaseTest"`
Expected: 1 test PASS.

---

### Task 15: `GetBandDetailUseCase` con test (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/domain/usecase/GetBandDetailUseCaseTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/domain/usecase/GetBandDetailUseCase.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBandDetailUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = GetBandDetailUseCase(repo)

    @Test
    fun `invoke delegates to repository getBandDetail with id`() = runTest {
        val expected = Band(
            id = 1, name = "Queen", image = "img", description = "desc",
            creationDate = "1970-01-01", members = emptyList(), albums = emptyList(),
        )
        coEvery { repo.getBandDetail(1) } returns expected

        val result = useCase(1)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getBandDetail(1) }
    }
}
```

- [ ] **Step 2: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.GetBandDetailUseCaseTest"`
Expected: Compilation FAIL.

- [ ] **Step 3: Crear el use case**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class GetBandDetailUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int): Band = repository.getBandDetail(bandId)
}
```

- [ ] **Step 4: Run y verificar que pasa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.GetBandDetailUseCaseTest"`
Expected: 1 test PASS.

---

### Task 16: `AddMusicianToBandUseCase` con test (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/domain/usecase/AddMusicianToBandUseCaseTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/domain/usecase/AddMusicianToBandUseCase.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddMusicianToBandUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = AddMusicianToBandUseCase(repo)

    @Test
    fun `invoke delegates bandId and musicianId to repository`() = runTest {
        coEvery { repo.addMusicianToBand(1, 10) } returns Unit

        useCase(bandId = 1, musicianId = 10)

        coVerify(exactly = 1) { repo.addMusicianToBand(1, 10) }
    }
}
```

- [ ] **Step 2: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCaseTest"`
Expected: Compilation FAIL.

- [ ] **Step 3: Crear el use case**

```kotlin
package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class AddMusicianToBandUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int, musicianId: Int) =
        repository.addMusicianToBand(bandId, musicianId)
}
```

- [ ] **Step 4: Run y verificar que pasa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCaseTest"`
Expected: 1 test PASS.

---

## Phase 5 — ViewModels + tests (Commit 2)

### Task 17: `BandListUiState` + `BandListViewModel` con tests (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/BandListViewModelTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/BandListUiState.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/BandListViewModel.kt`

- [ ] **Step 1: Crear `BandListUiState.kt`** (la sealed la creamos primero porque el test la referencia)

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.BandSummary

sealed interface BandListUiState {
    data object Loading : BandListUiState
    data class Success(val bands: List<BandSummary>) : BandListUiState
    data object Empty : BandListUiState
    data class Error(val isNetworkError: Boolean) : BandListUiState
}
```

- [ ] **Step 2: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BandListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepository : BandRepository {
        var nextResult: Result<List<BandSummary>> = Result.success(emptyList())
        var callCount = 0
        override suspend fun getBands(): List<BandSummary> {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun getBandDetail(id: Int): Band = error("not used")
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    private fun buildVm(repo: FakeBandRepository) = BandListViewModel(GetBandsUseCase(repo))

    private fun sampleBands() = listOf(
        BandSummary(1, "Queen", "img"),
        BandSummary(2, "Aerosmith", "img"),
    )

    @Test
    fun `emits Loading then Success when bands returned`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(sampleBands()) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is BandListUiState.Success)
            assertEquals(2, (state as BandListUiState.Success).bands.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when list empty`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(emptyList()) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error on IOException`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error on HttpException`() = runTest {
        val http = HttpException(Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType())))
        val repo = FakeBandRepository().apply { nextResult = Result.failure(http) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes repository`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = true), awaitItem())

            repo.nextResult = Result.success(sampleBands())
            vm.retry()

            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is BandListUiState.Success)
            assertEquals(2, repo.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.BandListViewModelTest"`
Expected: Compilation FAIL — `Unresolved reference: BandListViewModel`.

- [ ] **Step 4: Crear el ViewModel**

`app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/BandListViewModel.kt`:

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BandListViewModel @Inject constructor(
    private val getBands: GetBandsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BandListUiState>(BandListUiState.Loading)
    val uiState: StateFlow<BandListUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() { load() }

    private fun load() {
        _uiState.value = BandListUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val bands = getBands()
                if (bands.isEmpty()) BandListUiState.Empty
                else BandListUiState.Success(bands)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                BandListUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                BandListUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                BandListUiState.Error(isNetworkError = false)
            }
        }
    }
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.BandListViewModelTest"`
Expected: 5 tests PASS.

---

### Task 18: `BandDetailUiState` + `BandDetailViewModel` con tests (TDD)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/BandDetailViewModelTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/BandDetailUiState.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/BandDetailViewModel.kt`

- [ ] **Step 1: Crear `BandDetailUiState.kt`**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Band

sealed interface BandDetailUiState {
    data object Loading : BandDetailUiState
    data class Success(val band: Band) : BandDetailUiState
    data object NotFound : BandDetailUiState
    data class Error(val isNetworkError: Boolean) : BandDetailUiState
}
```

- [ ] **Step 2: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BandDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepository : BandRepository {
        var nextResult: Result<Band> = Result.success(sampleBand(1))
        var callCount = 0
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    companion object {
        fun sampleBand(id: Int) = Band(
            id = id, name = "Queen", image = "img", description = "desc",
            creationDate = "1970-01-01", members = emptyList(), albums = emptyList(),
        )
    }

    private fun buildVm(repo: FakeBandRepository) = BandDetailViewModel(GetBandDetailUseCase(repo))

    @Test
    fun `loadBand emits Loading then Success`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(sampleBand(1)) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertTrue(awaitItem() is BandDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadBand with 404 emits NotFound`() = runTest {
        val http404 = HttpException(Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType())))
        val repo = FakeBandRepository().apply { nextResult = Result.failure(http404) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertEquals(BandDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadBand with IOException emits network Error`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertEquals(BandDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes use case with last id`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.loadBand(42)
        advanceUntilIdle()
        repo.nextResult = Result.success(sampleBand(42))
        vm.retry()
        advanceUntilIdle()

        assertEquals(2, repo.callCount)
    }
}
```

- [ ] **Step 3: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModelTest"`
Expected: Compilation FAIL.

- [ ] **Step 4: Crear el ViewModel**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BandDetailViewModel @Inject constructor(
    private val getBandDetail: GetBandDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BandDetailUiState>(BandDetailUiState.Loading)
    val uiState: StateFlow<BandDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentId: Int? = null

    fun loadBand(id: Int) {
        currentId = id
        loadJob?.cancel()
        _uiState.value = BandDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                BandDetailUiState.Success(getBandDetail(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) BandDetailUiState.NotFound
                else BandDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                BandDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                BandDetailUiState.Error(isNetworkError = false)
            }
        }
    }

    fun retry() {
        currentId?.let { loadBand(it) }
    }
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModelTest"`
Expected: 4 tests PASS.

---

### Task 19: `AddMusiciansToBandViewModel` con tests (el más complejo)

**Files:**
- Test: `app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/AddMusiciansToBandViewModelTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AddMusiciansFormState.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AddMusiciansUiState.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/AddMusiciansToBandViewModel.kt`

- [ ] **Step 1: Crear `AddMusiciansFormState.kt` y `AddMusiciansUiState.kt`**

`AddMusiciansFormState.kt`:

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianSummary

data class AddMusiciansFormState(
    val query: String = "",
    val allMusicians: List<MusicianSummary> = emptyList(),
    val currentMemberIds: Set<Int> = emptySet(),
    val filteredAvailable: List<MusicianSummary> = emptyList(),
)
```

`AddMusiciansUiState.kt`:

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddMusiciansUiState {
    data object Loading : AddMusiciansUiState
    data object Ready : AddMusiciansUiState
    data class Adding(val musicianId: Int) : AddMusiciansUiState
    data class Error(val isNetworkError: Boolean, val musicianId: Int?) : AddMusiciansUiState
}

sealed interface AddMusiciansEvent {
    data class AddedSuccessfully(val musicianName: String) : AddMusiciansEvent
}
```

- [ ] **Step 2: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddMusiciansToBandViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepo : MusicianRepository {
        var allMusicians: List<MusicianSummary> = emptyList()
        override suspend fun getMusicians(): List<MusicianSummary> = allMusicians
        override suspend fun getMusicianDetail(id: Int) = error("not used")
    }

    private class FakeBandRepo : BandRepository {
        var bandResult: Result<Band> = Result.success(sampleBand(emptyList()))
        var addResult: Result<Unit> = Result.success(Unit)
        var addCallCount = 0
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = bandResult.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
            addCallCount++
            addResult.getOrThrow()
        }
    }

    companion object {
        fun sampleBand(members: List<MusicianSummary>) = Band(
            id = 1, name = "Queen", image = "", description = "",
            creationDate = "", members = members, albums = emptyList(),
        )
    }

    private fun buildVm(
        musicianRepo: FakeMusicianRepo,
        bandRepo: FakeBandRepo,
        bandId: Int = 1,
    ): AddMusiciansToBandViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("bandId" to bandId))
        return AddMusiciansToBandViewModel(
            getMusicians = GetMusiciansUseCase(musicianRepo),
            getBandDetail = GetBandDetailUseCase(bandRepo),
            addMusicianToBand = AddMusicianToBandUseCase(bandRepo),
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initial load fetches catalog and band, excludes existing members`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "Freddie Mercury", "", ""),
                MusicianSummary(11, "Brian May", "", ""),
                MusicianSummary(12, "John Deacon", "", ""),
            )
        }
        val bandRepo = FakeBandRepo().apply {
            bandResult = Result.success(sampleBand(listOf(MusicianSummary(10, "Freddie Mercury", "", ""))))
        }
        val vm = buildVm(musicianRepo, bandRepo)

        vm.form.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(3, state.allMusicians.size)
            assertEquals(setOf(10), state.currentMemberIds)
            assertEquals(2, state.filteredAvailable.size)
            assertTrue(state.filteredAvailable.none { it.id == 10 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query change with debounce filters by normalized name`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "José Pérez", "", ""),
                MusicianSummary(11, "Brian May", "", ""),
            )
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onQueryChange("jose")
        advanceTimeBy(299L)
        // Aún no debounced
        assertEquals(2, vm.form.value.filteredAvailable.size)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertEquals(1, vm.form.value.filteredAvailable.size)
        assertEquals("José Pérez", vm.form.value.filteredAvailable[0].name)
    }

    @Test
    fun `query is case and diacritic insensitive`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "José Pérez", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onQueryChange("PEREZ")
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals(1, vm.form.value.filteredAvailable.size)
    }

    @Test
    fun `add musician success transitions Adding to Ready and updates memberIds`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(AddMusiciansUiState.Ready, vm.uiState.value)
        assertTrue(vm.form.value.currentMemberIds.contains(10))
        assertEquals(0, vm.form.value.filteredAvailable.size)
        assertEquals(1, bandRepo.addCallCount)
    }

    @Test
    fun `add musician double tap ignores second call while Adding`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(1, bandRepo.addCallCount)
    }

    @Test
    fun `add musician IOException emits network Error without mutating lists`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo().apply { addResult = Result.failure(IOException("x")) }
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddMusiciansUiState.Error)
        assertTrue((state as AddMusiciansUiState.Error).isNetworkError)
        assertEquals(10, state.musicianId)
        assertFalse(vm.form.value.currentMemberIds.contains(10))
        assertEquals(1, vm.form.value.filteredAvailable.size)
    }

    @Test
    fun `add musician HttpException emits non-network Error`() = runTest {
        val http = HttpException(Response.error<Any>(409, "".toResponseBody("text/plain".toMediaType())))
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo().apply { addResult = Result.failure(http) }
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddMusiciansUiState.Error)
        assertFalse((state as AddMusiciansUiState.Error).isNetworkError)
    }
}
```

- [ ] **Step 3: Run y verificar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModelTest"`
Expected: Compilation FAIL.

- [ ] **Step 4: Crear el ViewModel**

```kotlin
package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.Normalizer
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AddMusiciansToBandViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
    private val getBandDetail: GetBandDetailUseCase,
    private val addMusicianToBand: AddMusicianToBandUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bandId: Int = savedStateHandle.get<Int>("bandId")
        ?: error("AddMusiciansToBandViewModel requires bandId in SavedStateHandle")

    private val _form = MutableStateFlow(AddMusiciansFormState())
    val form: StateFlow<AddMusiciansFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddMusiciansUiState>(AddMusiciansUiState.Loading)
    val uiState: StateFlow<AddMusiciansUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddMusiciansEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddMusiciansEvent> = _events.asSharedFlow()

    private val queryChannel = MutableStateFlow("")

    init {
        loadInitial()
        queryChannel
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { q ->
                _form.value = _form.value.copy(
                    query = q,
                    filteredAvailable = computeFiltered(_form.value.allMusicians, _form.value.currentMemberIds, q),
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        queryChannel.value = query
    }

    fun onAddMusician(musicianId: Int) {
        if (_uiState.value is AddMusiciansUiState.Adding) return
        _uiState.value = AddMusiciansUiState.Adding(musicianId)
        viewModelScope.launch {
            try {
                addMusicianToBand(bandId, musicianId)
                val musician = _form.value.allMusicians.firstOrNull { it.id == musicianId }
                _form.value = _form.value.copy(
                    currentMemberIds = _form.value.currentMemberIds + musicianId,
                    filteredAvailable = computeFiltered(
                        _form.value.allMusicians,
                        _form.value.currentMemberIds + musicianId,
                        _form.value.query,
                    ),
                )
                _uiState.value = AddMusiciansUiState.Ready
                if (musician != null) {
                    _events.tryEmit(AddMusiciansEvent.AddedSuccessfully(musician.name))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = true, musicianId = musicianId)
            } catch (e: HttpException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = musicianId)
            } catch (e: Exception) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = musicianId)
            }
        }
    }

    private fun loadInitial() {
        _uiState.value = AddMusiciansUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val musiciansAsync = async { getMusicians() }
                    val bandAsync = async { getBandDetail(bandId) }
                    val (musicians, band) = awaitAll(musiciansAsync, bandAsync)
                    @Suppress("UNCHECKED_CAST")
                    val all = musicians as List<com.misw4203.vinilos.domain.model.MusicianSummary>
                    val bandTyped = band as com.misw4203.vinilos.domain.model.Band
                    val memberIds = bandTyped.members.map { it.id }.toSet()
                    _form.value = AddMusiciansFormState(
                        query = "",
                        allMusicians = all,
                        currentMemberIds = memberIds,
                        filteredAvailable = computeFiltered(all, memberIds, ""),
                    )
                    _uiState.value = AddMusiciansUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = true, musicianId = null)
            } catch (e: HttpException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = null)
            } catch (e: Exception) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = null)
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun computeFiltered(
        all: List<com.misw4203.vinilos.domain.model.MusicianSummary>,
        excluded: Set<Int>,
        query: String,
    ): List<com.misw4203.vinilos.domain.model.MusicianSummary> {
        val available = all.filterNot { it.id in excluded }
        if (query.isBlank()) return available
        val normalizedQuery = normalize(query)
        return available.filter { normalize(it.name).contains(normalizedQuery) }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }
}
```

> Nota sobre `awaitAll` con tipos heterogéneos: Kotlin no infiere bien `awaitAll(async A, async B)` cuando los tipos difieren — la sintaxis con cast `as` aquí es el workaround más limpio sin agregar dependencias.

- [ ] **Step 5: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModelTest"`
Expected: 7 tests PASS.

- [ ] **Step 6: Commit fase 2**

```bash
git add app/src/main/java/com/misw4203/vinilos/presentation/viewmodel/ \
        app/src/main/java/com/misw4203/vinilos/domain/usecase/GetBandsUseCase.kt \
        app/src/main/java/com/misw4203/vinilos/domain/usecase/GetBandDetailUseCase.kt \
        app/src/main/java/com/misw4203/vinilos/domain/usecase/AddMusicianToBandUseCase.kt \
        app/src/test/java/com/misw4203/vinilos/domain/usecase/GetBandsUseCaseTest.kt \
        app/src/test/java/com/misw4203/vinilos/domain/usecase/GetBandDetailUseCaseTest.kt \
        app/src/test/java/com/misw4203/vinilos/domain/usecase/AddMusicianToBandUseCaseTest.kt \
        app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/BandListViewModelTest.kt \
        app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/BandDetailViewModelTest.kt \
        app/src/test/java/com/misw4203/vinilos/presentation/viewmodel/AddMusiciansToBandViewModelTest.kt

git commit -m "$(cat <<'EOF'
feat(hu012): Se agregan UseCases y ViewModels de Bandas

- UseCases: GetBands, GetBandDetail, AddMusicianToBand (3 archivos + 3 tests).
- BandListViewModel + BandListUiState (Loading/Success/Empty/Error).
- BandDetailViewModel + BandDetailUiState (Loading/Success/NotFound/Error),
  con loadJob cancelable y retry.
- AddMusiciansToBandViewModel separa AddMusiciansFormState (query, allMusicians,
  currentMemberIds, filteredAvailable) de AddMusiciansUiState (Loading/Ready/
  Adding/Error). Carga inicial paralela vía async/awaitAll, búsqueda con
  debounce(300) normalizada (case + diacritic insensitive), defensa contra
  doble envío en Adding, write-through actualiza memberIds tras éxito.
- 17 tests unitarios cubren CA02, CA03, CA04, CA05, CA06.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 6 — Strings + composables reutilizables (Commit 3)

### Task 20: Agregar strings nuevos a `strings.xml`

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Agregar el bloque al final del recurso (antes de `</resources>`)**

```xml
    <!-- HU012 - Bandas -->
    <string name="artists_tab_musicians">Músicos</string>
    <string name="artists_tab_bands">Bandas</string>
    <string name="bands_title">Bandas</string>
    <string name="band_badge">BANDA</string>
    <string name="band_detail_title">Detalle de la banda</string>
    <string name="band_not_found_title">Banda no encontrada</string>
    <string name="band_not_found_body">La banda solicitada no existe en el catálogo.</string>
    <string name="band_members_section_title">Integrantes</string>
    <string name="band_members_empty_title">Aún no hay integrantes</string>
    <string name="band_members_empty_body">Esta banda todavía no tiene integrantes registrados.</string>
    <string name="add_first_member_cta">Agregar primer integrante</string>
    <string name="add_musicians_cta">Agregar músicos</string>
    <string name="band_albums_section_title">Álbumes</string>
    <plurals name="members_record_count">
        <item quantity="one">%d integrante</item>
        <item quantity="other">%d integrantes</item>
    </plurals>
    <plurals name="bands_record_count">
        <item quantity="one">%d banda · orden alfa</item>
        <item quantity="other">%d bandas · orden alfa</item>
    </plurals>
    <string name="search_placeholder_bands">¿Qué banda buscas?</string>

    <!-- HU012 - Agregar músicos -->
    <string name="add_musicians_title">Agregar músicos</string>
    <string name="add_musicians_available_section">Músicos disponibles</string>
    <string name="add_musicians_current_section">Integrantes actuales</string>
    <string name="add_musicians_empty_filter">No se encontraron músicos con ese nombre</string>
    <string name="add_musicians_empty_catalog">No hay músicos disponibles para agregar</string>
    <string name="search_musicians_placeholder">Buscar por nombre</string>
    <string name="add_musician_success">«%s» fue agregado a la banda</string>
    <string name="add_musician_error_network">Sin conexión. Intenta de nuevo.</string>
    <string name="add_musician_error_server">No pudimos agregar el músico. Inténtalo de nuevo.</string>

    <!-- HU012 - Content descriptions -->
    <string name="cd_band_image">Imagen de la banda</string>
    <string name="cd_band_image_of">Imagen de la banda %s</string>
    <string name="cd_add_musician_to_band">Agregar %s a la banda</string>
    <string name="cd_adding_musician">Agregando músico</string>
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (R class regenerada con los nuevos strings).

---

### Task 21: `BandCard` composable con test (TDD)

**Files:**
- Test: `app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/BandCardTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/BandCard.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.BandSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BandCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = BandSummary(id = 1, name = "Queen", image = "")

    @Test
    fun rendersBandName() {
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = {}) }
        }
        composeTestRule.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun rendersBandBadge() {
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = {}) }
        }
        composeTestRule.onNodeWithText("BANDA").assertIsDisplayed()
    }

    @Test
    fun clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("Queen").performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: Run y verificar que falla (compilation)**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: Compilation FAIL — `Unresolved reference: BandCard`.

- [ ] **Step 3: Crear `BandCard.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.BandSummary

@Composable
fun BandCard(
    band: BandSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
        ) {
            AsyncImage(
                model = band.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.width(20.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = band.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.band_badge),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
```

- [ ] **Step 4: Run tests** (requiere emulador encendido API 33/34 con animaciones off)

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.presentation.ui.components.BandCardTest`
Expected: 3 tests PASS. Si no hay emulador disponible, omitir y verificar con `./gradlew :app:assembleDebugAndroidTest` que el test compila.

---

### Task 22: `MusicianRow` composable con test (TDD)

**Files:**
- Test: `app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/MusicianRowTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/MusicianRow.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.activity.ComponentActivity
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.MusicianSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MusicianRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sample = MusicianSummary(10, "Freddie Mercury", "", "1946-09-05")

    @Test
    fun rendersMusicianName() {
        composeTestRule.setContent {
            MaterialTheme { MusicianRow(musician = sample, isAdding = false, onAdd = {}) }
        }
        composeTestRule.onNodeWithText("Freddie Mercury").assertIsDisplayed()
    }

    @Test
    fun clickPlusInvokesOnAddWithMusicianId() {
        var addedId: Int? = null
        composeTestRule.setContent {
            MaterialTheme {
                MusicianRow(musician = sample, isAdding = false, onAdd = { addedId = it })
            }
        }
        val cd = composeTestRule.activity.getString(R.string.cd_add_musician_to_band, "Freddie Mercury")
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        assertEquals(10, addedId)
    }

    @Test
    fun whenAddingShowsLoadingIndicatorAndDisablesButton() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                MusicianRow(musician = sample, isAdding = true, onAdd = { clicked = true })
            }
        }
        val cd = composeTestRule.activity.getString(R.string.cd_adding_musician)
        composeTestRule.onNodeWithContentDescription(cd).assertIsDisplayed()
        // Click sobre el indicator no debe disparar onAdd
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        assertEquals(false, clicked)
    }
}
```

- [ ] **Step 2: Run y verificar que falla (compilation)**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: Compilation FAIL — `Unresolved reference: MusicianRow`.

- [ ] **Step 3: Crear `MusicianRow.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.MusicianSummary

@Composable
fun MusicianRow(
    musician: MusicianSummary,
    isAdding: Boolean,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addCd = stringResource(R.string.cd_add_musician_to_band, musician.name)
    val addingCd = stringResource(R.string.cd_adding_musician)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A)),
            ) {
                AsyncImage(
                    model = musician.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = musician.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (musician.birthDate.isNotBlank()) {
                    Text(
                        text = musician.birthDate.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        if (isAdding) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = addingCd },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable { onAdd(musician.id) }
                    .semantics {
                        role = Role.Button
                        contentDescription = addCd
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.presentation.ui.components.MusicianRowTest`
Expected: 3 tests PASS.

---

### Task 23: `EmptyMembersState` con test (TDD)

**Files:**
- Test: `app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/EmptyMembersStateTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/EmptyMembersState.kt`

- [ ] **Step 1: Escribir el test failing**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EmptyMembersStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersEmptyTitleAndBody() {
        composeTestRule.setContent {
            MaterialTheme { EmptyMembersState(onAddFirst = {}) }
        }
        val title = composeTestRule.activity.getString(R.string.band_members_empty_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun clickCtaInvokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme { EmptyMembersState(onAddFirst = { clicked = true }) }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.onNodeWithText(cta).performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: Run y verificar que falla**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: Compilation FAIL.

- [ ] **Step 3: Crear `EmptyMembersState.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.misw4203.vinilos.R

@Composable
fun EmptyMembersState(
    onAddFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.band_members_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(R.string.band_members_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(20.dp))
        Button(
            onClick = onAddFirst,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.add_first_member_cta))
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.presentation.ui.components.EmptyMembersStateTest`
Expected: 2 tests PASS.

---

### Task 24: `PerformerHeader` composable

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/PerformerHeader.kt`

- [ ] **Step 1: Crear el composable**

```kotlin
package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.misw4203.vinilos.R

@Composable
fun PerformerHeader(
    name: String,
    image: String,
    description: String,
    badgeKind: PerformerBadgeKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = when (badgeKind) {
                    PerformerBadgeKind.Band -> stringResource(R.string.band_badge)
                    PerformerBadgeKind.Artist -> stringResource(R.string.artist_badge)
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
        }
        if (description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

enum class PerformerBadgeKind { Band, Artist }
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit fase 3**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/java/com/misw4203/vinilos/presentation/ui/components/BandCard.kt \
        app/src/main/java/com/misw4203/vinilos/presentation/ui/components/MusicianRow.kt \
        app/src/main/java/com/misw4203/vinilos/presentation/ui/components/EmptyMembersState.kt \
        app/src/main/java/com/misw4203/vinilos/presentation/ui/components/PerformerHeader.kt \
        app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/BandCardTest.kt \
        app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/MusicianRowTest.kt \
        app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/components/EmptyMembersStateTest.kt

git commit -m "$(cat <<'EOF'
feat(hu012): Se agregan strings y componentes reutilizables de Bandas

- strings.xml: ~30 strings (tabs, títulos, secciones, plurals para members
  y bands, feedback de éxito/error, content descriptions accesibles).
- BandCard: análogo a MusicianCard con badge "BANDA", mergeDescendants,
  role=Button.
- MusicianRow: fila con foto + nombre + IconButton "+" para la pantalla
  de agregar; durante isAdding muestra CircularProgressIndicator y
  deshabilita el click. Touch target ≥ 48dp con sizeIn.
- EmptyMembersState: vista vacía con CTA "Agregar primer integrante"
  (CA09).
- PerformerHeader: encabezado compartido entre BandDetailScreen y
  futuras pantallas con badge configurable (Band/Artist).
- 8 tests instrumentados sobre los componentes interactivos.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 7 — Pantallas + navegación (Commit 4)

### Task 25: Refactor `MusicianListScreen` → `MusicianListContent` + nuevo `ArtistsHubScreen`

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/artist/MusicianListScreen.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/artist/MusicianListContent.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/artist/ArtistsHubScreen.kt`

- [ ] **Step 1: Crear `MusicianListContent.kt` con el cuerpo actual de `MusicianListScreen` (sin TopBar)**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.ListCounter
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.SearchBarStatic
import com.misw4203.vinilos.presentation.viewmodel.MusicianListUiState
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel

@Composable
fun MusicianListContent(
    onMusicianClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicianListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MusicianListUiState.Loading -> LoadingState()
            is MusicianListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is MusicianListUiState.Empty -> Column {
                MusicianHeaderSection()
                EmptyState()
            }
            is MusicianListUiState.Success -> LazyColumn(
                modifier = Modifier.testTag("artists_list"),
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { MusicianHeaderSection() }
                item {
                    ListCounter(
                        text = pluralStringResource(
                            R.plurals.artists_record_count,
                            state.musicians.size,
                            state.musicians.size,
                        ),
                        testTag = "artists_record_count",
                    )
                }
                items(state.musicians, key = { it.id }) { musician ->
                    MusicianCard(
                        musician = musician,
                        onClick = { onMusicianClick(musician.id) },
                        modifier = Modifier.testTag("musician_card_${musician.id}"),
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun MusicianHeaderSection() {
    Column {
        SearchBarStatic(placeholder = stringResource(R.string.search_placeholder_artists))
        Spacer(Modifier.size(8.dp))
    }
}
```

- [ ] **Step 2: Reemplazar `MusicianListScreen.kt` con un wrapper que delegue (mantiene compatibilidad mientras refactorizamos navegación en Task 29)**

Reemplazar todo el contenido de `MusicianListScreen.kt` con:

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel

@Composable
fun MusicianListScreen(
    onMusicianClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicianListViewModel = hiltViewModel(),
) {
    MusicianListContent(
        onMusicianClick = onMusicianClick,
        modifier = modifier,
        viewModel = viewModel,
    )
}
```

> Justificación: el wrapper preserva la API pública usada por `VinilosNavHost` y por tests preexistentes (`MusicianListScreenTest`). Cuando Task 29 actualice la navegación, eliminamos este wrapper y los tests apuntarán a `MusicianListContent` o a `ArtistsHubScreen`.

- [ ] **Step 3: Crear `ArtistsHubScreen.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.ui.screens.band.BandListContent

@Composable
fun ArtistsHubScreen(
    onMusicianClick: (Int) -> Unit,
    onBandClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        VinilosTopBar(title = stringResource(R.string.artists_title))
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("artists_tab_musicians"),
                text = { Text(stringResource(R.string.artists_tab_musicians)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("artists_tab_bands"),
                text = { Text(stringResource(R.string.artists_tab_bands)) },
            )
        }
        when (selectedTab) {
            0 -> MusicianListContent(onMusicianClick = onMusicianClick)
            1 -> BandListContent(onBandClick = onBandClick)
        }
    }
}
```

> `BandListContent` aún no existe — se crea en Task 26. Compilar tras Task 26.

- [ ] **Step 4: Confirmar que NO compila aún (esperando Task 26)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: Compilation FAIL — `Unresolved reference: BandListContent`. Se resuelve en Task 26.

---

### Task 26: `BandListContent`

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/band/BandListContent.kt`

- [ ] **Step 1: Crear el composable**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.BandCard
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.ListCounter
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.SearchBarStatic
import com.misw4203.vinilos.presentation.viewmodel.BandListUiState
import com.misw4203.vinilos.presentation.viewmodel.BandListViewModel

@Composable
fun BandListContent(
    onBandClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BandListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BandListUiState.Loading -> LoadingState()
            is BandListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is BandListUiState.Empty -> Column {
                BandHeaderSection()
                EmptyState()
            }
            is BandListUiState.Success -> LazyColumn(
                modifier = Modifier.testTag("bands_list"),
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { BandHeaderSection() }
                item {
                    ListCounter(
                        text = pluralStringResource(
                            R.plurals.bands_record_count,
                            state.bands.size,
                            state.bands.size,
                        ),
                        testTag = "bands_record_count",
                    )
                }
                items(state.bands, key = { it.id }) { band ->
                    BandCard(
                        band = band,
                        onClick = { onBandClick(band.id) },
                        modifier = Modifier.testTag("band_card_${band.id}"),
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun BandHeaderSection() {
    Column {
        SearchBarStatic(placeholder = stringResource(R.string.search_placeholder_bands))
        Spacer(Modifier.size(8.dp))
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — `ArtistsHubScreen` ya resuelve `BandListContent`.

---

### Task 27: `BandDetailScreen` con tests

**Files:**
- Test: `app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/screens/band/BandDetailScreenTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/band/BandDetailScreen.kt`

- [ ] **Step 1: Crear `BandDetailScreen.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.presentation.ui.components.EmptyMembersState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.PerformerBadgeKind
import com.misw4203.vinilos.presentation.ui.components.PerformerHeader
import com.misw4203.vinilos.presentation.viewmodel.BandDetailUiState
import com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandDetailScreen(
    bandId: Int,
    onBack: () -> Unit,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
    modifier: Modifier = Modifier,
    refreshKey: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: BandDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(bandId) { viewModel.loadBand(bandId) }
    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.retry()
            onRefreshHandled()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.band_detail_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("band_detail_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is BandDetailUiState.Loading -> LoadingState()
                is BandDetailUiState.NotFound -> NotFoundContent()
                is BandDetailUiState.Error -> ErrorState(
                    onRetry = viewModel::retry,
                    isNetworkError = state.isNetworkError,
                )
                is BandDetailUiState.Success -> BandBody(
                    band = state.band,
                    onMusicianClick = onMusicianClick,
                    onAddMusicians = onAddMusicians,
                )
            }
        }
    }
}

@Composable
private fun BandBody(
    band: Band,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("band_detail_root"),
    ) {
        PerformerHeader(
            name = band.name,
            image = band.image,
            description = band.description,
            badgeKind = PerformerBadgeKind.Band,
            contentDescription = stringResource(R.string.cd_band_image_of, band.name),
        )
        Spacer(Modifier.height(8.dp))

        MembersSection(
            members = band.members,
            onMusicianClick = onMusicianClick,
            onAddMusicians = onAddMusicians,
        )

        if (band.albums.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            AlbumsSection(band.albums)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MembersSection(
    members: List<MusicianSummary>,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
) {
    SectionHeader(stringResource(R.string.band_members_section_title))
    Spacer(Modifier.height(8.dp))
    if (members.isEmpty()) {
        EmptyMembersState(onAddFirst = onAddMusicians)
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            members.forEach { m ->
                MusicianCard(
                    musician = m,
                    onClick = { onMusicianClick(m.id) },
                    modifier = Modifier.testTag("current_member_${m.id}"),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAddMusicians,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp)
                .testTag("band_detail_add_musicians"),
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_musicians_cta))
        }
    }
}

@Composable
private fun AlbumsSection(albums: List<Album>) {
    SectionHeader(stringResource(R.string.band_albums_section_title))
    Spacer(Modifier.height(8.dp))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums) { album ->
            Column(modifier = Modifier.width(120.dp)) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = stringResource(R.string.cd_album_cover_of, album.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (album.releaseYear.isNotBlank()) {
                    Text(
                        text = album.releaseYear,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun NotFoundContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.band_not_found_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.band_not_found_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
```

- [ ] **Step 2: Crear test `BandDetailScreenTest.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class BandDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(val result: Result<Band>) : BandRepository {
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = result.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    private fun buildVm(repo: BandRepository) = BandDetailViewModel(GetBandDetailUseCase(repo))

    @Test
    fun successWithEmptyMembersShowsEmptyMembersStateCta() {
        val band = Band(1, "Queen", "", "", "", emptyList(), emptyList())
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.onNodeWithText(cta).assertIsDisplayed()
    }

    @Test
    fun successWithMembersShowsListAndAddButton() {
        val band = Band(
            1, "Queen", "", "", "",
            members = listOf(MusicianSummary(10, "Freddie", "", "")),
            albums = emptyList(),
        )
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        composeTestRule.onNodeWithText("Freddie").assertIsDisplayed()
        composeTestRule.onNodeWithTag("band_detail_add_musicians").assertIsDisplayed()
    }

    @Test
    fun emptyCtaInvokesOnAddMusicians() {
        var clicked = false
        val band = Band(1, "Queen", "", "", "", emptyList(), emptyList())
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {},
                    onAddMusicians = { clicked = true },
                    viewModel = vm,
                )
            }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.onNodeWithText(cta).performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun notFoundShowsMessage() {
        val http404 = HttpException(Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType())))
        val vm = buildVm(FakeRepo(Result.failure(http404)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText(
                    composeTestRule.activity.getString(R.string.band_not_found_title)
                )
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val title = composeTestRule.activity.getString(R.string.band_not_found_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Build verify**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Ejecutar tests (requiere emulador encendido)**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.presentation.ui.screens.band.BandDetailScreenTest`
Expected: 4 tests PASS. Si no hay emulador, omitir y verificar compilación.

---

### Task 28: `AddMusiciansToBandScreen` con tests

**Files:**
- Test: `app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/screens/band/AddMusiciansToBandScreenTest.kt`
- Create: `app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/band/AddMusiciansToBandScreen.kt`

- [ ] **Step 1: Crear `AddMusiciansToBandScreen.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.MusicianRow
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansEvent
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModel
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusiciansToBandScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMusiciansToBandViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_musician_success)
    val networkErrorMessage = stringResource(R.string.add_musician_error_network)
    val serverErrorMessage = stringResource(R.string.add_musician_error_server)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is AddMusiciansEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.musicianName))
            }
        }
    }
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AddMusiciansUiState.Error && state.musicianId != null) {
            val msg = if (state.isNetworkError) networkErrorMessage else serverErrorMessage
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_musicians_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_musicians_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_musicians_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                AddMusiciansUiState.Loading -> LoadingState()
                else -> Content(
                    queryValue = form.query,
                    onQueryChange = viewModel::onQueryChange,
                    available = form.filteredAvailable,
                    currentMembers = form.allMusicians.filter { it.id in form.currentMemberIds },
                    addingId = (uiState as? AddMusiciansUiState.Adding)?.musicianId,
                    onAdd = viewModel::onAddMusician,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    queryValue: String,
    onQueryChange: (String) -> Unit,
    available: List<com.misw4203.vinilos.domain.model.MusicianSummary>,
    currentMembers: List<com.misw4203.vinilos.domain.model.MusicianSummary>,
    addingId: Int?,
    onAdd: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = queryValue,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_musicians_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_musicians_search"),
            )
        }
        item {
            SectionHeader(stringResource(R.string.add_musicians_available_section))
        }
        if (available.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.add_musicians_empty_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(available, key = { it.id }) { musician ->
                MusicianRow(
                    musician = musician,
                    isAdding = addingId == musician.id,
                    onAdd = onAdd,
                    modifier = Modifier.testTag("available_musician_${musician.id}"),
                )
            }
        }
        item {
            SectionHeader(stringResource(R.string.add_musicians_current_section))
        }
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.members_record_count,
                    currentMembers.size,
                    currentMembers.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        items(currentMembers, key = { it.id }) { musician ->
            MusicianCard(
                musician = musician,
                onClick = {},
                modifier = Modifier.testTag("current_member_${musician.id}"),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.semantics { heading() },
    )
}
```

- [ ] **Step 2: Crear test `AddMusiciansToBandScreenTest.kt`**

```kotlin
package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModel
import org.junit.Rule
import org.junit.Test

class AddMusiciansToBandScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeMusicianRepo(val list: List<MusicianSummary>) : MusicianRepository {
        override suspend fun getMusicians() = list
        override suspend fun getMusicianDetail(id: Int) = error("not used")
    }

    private class FakeBandRepo(val band: Band) : BandRepository {
        var added = mutableListOf<Pair<Int, Int>>()
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = band
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
            added += bandId to musicianId
        }
    }

    private fun buildVm(
        catalog: List<MusicianSummary>,
        currentMembers: List<MusicianSummary>,
        bandId: Int = 1,
    ): AddMusiciansToBandViewModel {
        val band = Band(bandId, "Queen", "", "", "", currentMembers, emptyList())
        return AddMusiciansToBandViewModel(
            getMusicians = GetMusiciansUseCase(FakeMusicianRepo(catalog)),
            getBandDetail = GetBandDetailUseCase(FakeBandRepo(band)),
            addMusicianToBand = AddMusicianToBandUseCase(FakeBandRepo(band)),
            savedStateHandle = SavedStateHandle(mapOf("bandId" to bandId)),
        )
    }

    @Test
    fun rendersAvailableExcludingExistingMembers() {
        val catalog = listOf(
            MusicianSummary(10, "Freddie Mercury", "", ""),
            MusicianSummary(11, "Brian May", "", ""),
        )
        val members = listOf(MusicianSummary(10, "Freddie Mercury", "", ""))
        val vm = buildVm(catalog, members)

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_11")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_musician_11").assertIsDisplayed()
    }

    @Test
    fun searchFiltersList() {
        val catalog = listOf(
            MusicianSummary(10, "Freddie Mercury", "", ""),
            MusicianSummary(11, "Brian May", "", ""),
        )
        val vm = buildVm(catalog, emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_musicians_search").performTextInput("brian")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("available_musician_11").assertIsDisplayed()
    }

    @Test
    fun addingMusicianRemovesFromAvailableList() {
        val catalog = listOf(MusicianSummary(10, "Freddie Mercury", "", ""))
        val vm = buildVm(catalog, emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val cd = composeTestRule.activity.getString(
            com.misw4203.vinilos.R.string.cd_add_musician_to_band, "Freddie Mercury"
        )
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("current_member_10").assertIsDisplayed()
    }
}
```

> Imports adicionales que pueden necesitarse: `androidx.compose.ui.test.onAllNodesWithTag`, `androidx.compose.ui.test.onNodeWithContentDescription`. Si no se autocompletan, agregarlos manualmente.

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Ejecutar tests**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.presentation.ui.screens.band.AddMusiciansToBandScreenTest`
Expected: 3 tests PASS.

---

### Task 29: `Destinations.kt` y `VinilosNavHost.kt` — rutas nuevas

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/presentation/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/misw4203/vinilos/presentation/navigation/VinilosNavHost.kt`

- [ ] **Step 1: Agregar rutas a `Destinations.kt`**

Antes (final del object):
```kotlin
    fun addComment(albumId: Long, collectorId: Int = DefaultCollectorId) =
        "album/$albumId/comment/add/$collectorId"
}
```

Después: agregar antes del `}` final:
```kotlin
    const val BandDetail = "band/{bandId}"
    const val BandDetailArg = "bandId"
    const val AddMusiciansToBand = "band/{bandId}/musicians/add"
    const val RefreshBandDetailKey = "refresh_band_detail"

    fun bandDetail(bandId: Int) = "band/$bandId"
    fun addMusiciansToBand(bandId: Int) = "band/$bandId/musicians/add"
```

- [ ] **Step 2: Modificar `VinilosNavHost.kt`**

Reemplazar el bloque `composable(Destinations.ArtistList) { ... }`:

Antes:
```kotlin
                composable(Destinations.ArtistList) {
                    MusicianListScreen(
                        onMusicianClick = { id -> navController.navigate("artist/$id") },
                    )
                }
```

Después:
```kotlin
                composable(Destinations.ArtistList) {
                    ArtistsHubScreen(
                        onMusicianClick = { id -> navController.navigate("artist/$id") },
                        onBandClick = { id -> navController.navigate(Destinations.bandDetail(id)) },
                    )
                }
```

Y agregar los imports correspondientes:
```kotlin
import com.misw4203.vinilos.presentation.ui.screens.artist.ArtistsHubScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddMusiciansToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.BandDetailScreen
```

Quitar el import de `MusicianListScreen` si ya no se usa (el wrapper sigue existiendo pero la navegación ya no lo usa).

Y agregar al final del `NavHost { ... }` (antes del cierre del bloque) los dos destinos nuevos:

```kotlin
                composable(
                    route = Destinations.BandDetail,
                    arguments = listOf(navArgument(Destinations.BandDetailArg) { type = NavType.IntType }),
                ) { entry ->
                    val bandId = entry.arguments?.getInt(Destinations.BandDetailArg) ?: return@composable
                    val refreshFlag by entry.savedStateHandle
                        .getStateFlow(Destinations.RefreshBandDetailKey, false)
                        .collectAsStateWithLifecycle()
                    BandDetailScreen(
                        bandId = bandId,
                        onBack = { navController.popBackStack() },
                        onMusicianClick = { id -> navController.navigate("artist/$id") },
                        onAddMusicians = {
                            navController.navigate(Destinations.addMusiciansToBand(bandId))
                        },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshBandDetailKey] = false
                        },
                    )
                }
                composable(
                    route = Destinations.AddMusiciansToBand,
                    arguments = listOf(navArgument(Destinations.BandDetailArg) { type = NavType.IntType }),
                ) {
                    AddMusiciansToBandScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshBandDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit fase 4**

```bash
git add app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/artist/ \
        app/src/main/java/com/misw4203/vinilos/presentation/ui/screens/band/ \
        app/src/main/java/com/misw4203/vinilos/presentation/navigation/Destinations.kt \
        app/src/main/java/com/misw4203/vinilos/presentation/navigation/VinilosNavHost.kt \
        app/src/androidTest/java/com/misw4203/vinilos/presentation/ui/screens/band/

git commit -m "$(cat <<'EOF'
feat(hu012): Se agregan pantallas de Bandas y navegación

- Refactor: MusicianListScreen → ArtistsHubScreen con sub-tabs internas
  (Músicos / Bandas). MusicianListContent extraído sin cambio funcional.
- BandListContent: lista de bandas reutilizando SearchBarStatic,
  ListCounter, EmptyState/ErrorState/LoadingState.
- BandDetailScreen: PerformerHeader + sección Integrantes (con
  EmptyMembersState CTA si vacía, MusicianCards si tiene) + botón
  "Agregar músicos" + sección Álbumes opcional. RefreshBandDetailKey
  para refrescar al volver de la pantalla de agregar.
- AddMusiciansToBandScreen: header de banda (vía detalle ya en form
  state), búsqueda con debounce vía VM, sección "Disponibles" con
  MusicianRow + "+", sección "Integrantes actuales" con ListCounter.
  Snackbar de éxito ("X agregado a la banda") y de error
  (red/servidor). Sin botón "Guardar" — cada "+" persiste de inmediato.
- Destinations: BandDetail, AddMusiciansToBand, RefreshBandDetailKey.
- VinilosNavHost: reemplaza MusicianListScreen por ArtistsHubScreen y
  registra los dos destinos nuevos.
- 7 tests instrumentados (BandDetailScreenTest x4, AddMusiciansToBandScreenTest x3).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 8 — Integración E2E + verificación final (Commit 5)

### Task 30: `FakeBandRepository` para tests instrumentados

**Files:**
- Create: `app/src/androidTest/java/com/misw4203/vinilos/di/FakeBandRepository.kt`

- [ ] **Step 1: Crear el fake**

```kotlin
package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeBandRepository @Inject constructor() : BandRepository {

    private val initialMembers = mutableListOf<MusicianSummary>()
    private val bandsList = listOf(
        BandSummary(1, "Queen", ""),
        BandSummary(2, "Aerosmith", ""),
    )

    override suspend fun getBands(): List<BandSummary> = bandsList

    override suspend fun getBandDetail(id: Int): Band = Band(
        id = id,
        name = if (id == 1) "Queen" else "Aerosmith",
        image = "",
        description = "Banda legendaria.",
        creationDate = "1970-01-01",
        members = initialMembers.toList(),
        albums = emptyList(),
    )

    override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
        if (initialMembers.none { it.id == musicianId }) {
            initialMembers += MusicianSummary(musicianId, "Rubén Blades", "", "1948-07-16T00:00:00.000Z")
        }
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: Compilation FAIL — Hilt no encuentra binding (se resuelve en Task 31).

---

### Task 31: Registrar `FakeBandRepository` en `FakeRepositoryModule`

**Files:**
- Modify: `app/src/androidTest/java/com/misw4203/vinilos/di/FakeRepositoryModule.kt`

- [ ] **Step 1: Agregar imports**

```kotlin
import com.misw4203.vinilos.domain.repository.BandRepository
```

- [ ] **Step 2: Agregar binding antes del cierre de la clase**

```kotlin
    @Binds
    @Singleton
    abstract fun bindBandRepository(impl: FakeBandRepository): BandRepository
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

---

### Task 32: Caso E2E HU012 en `VinilosE2ETest`

**Files:**
- Modify: `app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt`

- [ ] **Step 1: Leer el archivo actual para entender el estilo de los casos existentes**

Run: `cat app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt`
Expected: lista de tests con `@Test` que usan `composeRule.waitUntil`, `onNodeWithTag`, `performClick`. Identificar el patrón de un test existente que cubre flujo de detalles para reutilizar el helper de espera (si existe) o copiar la estructura.

- [ ] **Step 2: Agregar el test HU012**

Agregar dentro de la clase, junto a los otros `@Test`:

```kotlin
    @Test
    fun hu012_addMusicianToBand_flow() {
        // 1. Bottom-nav → tab Artistas
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        // 2. Sub-tab "Bandas"
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("artists_tab_bands").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("artists_tab_bands").performClick()

        // 3. Tap primera banda (band_card_*)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.TestTag.let { it }, "band_card_1")
            ).fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithTag("band_card_1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("band_card_1").performClick()

        // 4. Botón "Agregar músicos" (puede ser CTA empty o botón normal)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("band_detail_root").fetchSemanticsNodes().isNotEmpty()
        }
        val addCtaText = composeRule.activity.getString(R.string.add_first_member_cta)
        composeRule.onNodeWithText(addCtaText).performClick()

        // 5. En lista de disponibles, tap "+" del primer músico (id=100 por FakeMusicianRepository)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("available_musician_100").fetchSemanticsNodes().isNotEmpty()
        }
        val addToBandCd = composeRule.activity.getString(R.string.cd_add_musician_to_band, "Rubén Blades")
        composeRule.onNodeWithContentDescription(addToBandCd).performClick()

        // 6. Verificar que el músico aparece en "Integrantes actuales"
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("current_member_100").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("current_member_100").assertExists()
    }
```

> Imports adicionales que pueden ser necesarios al inicio del archivo: `androidx.compose.ui.test.onNodeWithContentDescription`, `androidx.compose.ui.test.onAllNodesWithTag`. Verificar al compilar.

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Ejecutar E2E (requiere emulador encendido API 33/34, animaciones off)**

Preparación del emulador (una vez):
```bash
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent KEYCODE_WAKEUP
adb shell svc power stayon true
```

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.misw4203.vinilos.e2e.VinilosE2ETest#hu012_addMusicianToBand_flow`
Expected: 1 test PASS.

---

### Task 33: Verificación final + commit

- [ ] **Step 1: Build completo**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL en < 5 min.

- [ ] **Step 2: Todos los tests unitarios**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, todos los tests verdes.

- [ ] **Step 3: Compilar tests instrumentados**

Run: `./gradlew assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: (Opcional) ejecutar suite instrumentada completa**

Run: `./gradlew connectedAndroidTest`
Expected: BUILD SUCCESSFUL. Si no hay emulador disponible, omitir y dejar evidencia en el PR de que `assembleDebugAndroidTest` compila.

- [ ] **Step 5: Commit fase 5**

```bash
git add app/src/androidTest/java/com/misw4203/vinilos/di/FakeBandRepository.kt \
        app/src/androidTest/java/com/misw4203/vinilos/di/FakeRepositoryModule.kt \
        app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt

git commit -m "$(cat <<'EOF'
test(hu012): Se agrega E2E del flujo Agregar Músicos a Banda

- FakeBandRepository en memoria con dos bandas mock y mutación de members
  en addMusicianToBand (refleja el contrato del backend para que el VM
  observe el músico recién agregado al refrescar).
- FakeRepositoryModule registra el binding del fake.
- VinilosE2ETest: caso hu012_addMusicianToBand_flow cubre Bottom-nav →
  sub-tab Bandas → detalle → CTA agregar → "+" en disponibles →
  verificación de que aparece en "Integrantes actuales" (CA01 + CA02 +
  CA03 + CA09).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

**Spec coverage:** ✓ Todos los CA están cubiertos (CA01 BandDetailScreenTest + E2E; CA02 AddMusiciansScreenTest + VM test; CA03 VM test + E2E; CA04 VM test exclusión; CA05 VM test debounce + diacritics; CA06 VM test IOException + UI Snackbar; CA07 fluye desde repositorio (no requiere test específico además del E2E); CA08 out of scope; CA09 BandDetailScreenTest empty + EmptyMembersStateTest CTA). Approach C confirmado tras leer `Performer` existente.

**Placeholder scan:** Ningún paso usa "TBD" / "TODO" / "similar to". Cada step incluye código completo o comando exacto con resultado esperado.

**Type consistency:** Nombres de funciones de VM (`onAddMusician`, `onQueryChange`, `retry`, `loadBand`), de UseCases (`invoke`), DAO (`replaceBands`, `upsertDetail`), Repository (`getBands`, `getBandDetail`, `addMusicianToBand`), composables (`BandCard`, `MusicianRow`, `EmptyMembersState`, `PerformerHeader`, `BandListContent`, `BandDetailScreen`, `AddMusiciansToBandScreen`, `ArtistsHubScreen`) consistentes entre tasks. `RefreshBandDetailKey` referenciado consistentemente en VinilosNavHost (Task 29) y como flag en BandDetailScreen.

**Conocidos a vigilar durante implementación:**
- Task 28 referencia `onAllNodesWithTag` y `onNodeWithContentDescription` — confirmar imports al primer fallo de compilación.
- Task 32 usa heurística para detectar tag `band_card_1`; si la rama main tiene helpers E2E ya estructurados, alinear el estilo en el commit.


---

### Task 7: `BandDao`

**Files:**
- Create: `app/src/main/java/com/misw4203/vinilos/data/local/dao/BandDao.kt`

- [ ] **Step 1: Write the file**

```kotlin
package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity

@Dao
interface BandDao {

    @Query("SELECT * FROM bands ORDER BY name ASC")
    suspend fun getAll(): List<BandListEntity>

    @Upsert
    suspend fun upsertAll(bands: List<BandListEntity>)

    @Query("DELETE FROM bands")
    suspend fun clear()

    @Transaction
    suspend fun replaceBands(bands: List<BandListEntity>) {
        clear()
        upsertAll(bands)
    }

    @Query("SELECT * FROM band_details WHERE id = :id")
    suspend fun getDetailById(id: Int): BandDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: BandDetailEntity)
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 8: Registrar entidades + DAO en `VinilosDatabase` (bump version a 5)

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/data/local/database/VinilosDatabase.kt`

- [ ] **Step 1: Reemplazar `entities`, `version` y agregar abstract method**

Antes:
```kotlin
@Database(
    entities = [
        AlbumEntity::class,
        AlbumDetailEntity::class,
        MusicianListEntity::class,
        MusicianDetailEntity::class,
        CollectorEntity::class,
        CollectorDetailEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VinilosDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun musicianDao(): MusicianDao
    abstract fun collectorDao(): CollectorDao
}
```

Después:
```kotlin
@Database(
    entities = [
        AlbumEntity::class,
        AlbumDetailEntity::class,
        MusicianListEntity::class,
        MusicianDetailEntity::class,
        CollectorEntity::class,
        CollectorDetailEntity::class,
        BandListEntity::class,
        BandDetailEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VinilosDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun musicianDao(): MusicianDao
    abstract fun collectorDao(): CollectorDao
    abstract fun bandDao(): BandDao
}
```

Y agregar los imports correspondientes:
```kotlin
import com.misw4203.vinilos.data.local.dao.BandDao
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL (KSP genera el código de Room para `BandDao`).

---

### Task 9: Proveer `BandDao` en `DatabaseModule`

**Files:**
- Modify: `app/src/main/java/com/misw4203/vinilos/di/DatabaseModule.kt`

- [ ] **Step 1: Agregar import y método provider**

Añadir el import:

```kotlin
import com.misw4203.vinilos.data.local.dao.BandDao
```

Agregar después de `provideCollectorDao`:

```kotlin
    @Provides
    fun provideBandDao(db: VinilosDatabase): BandDao = db.bandDao()
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL
