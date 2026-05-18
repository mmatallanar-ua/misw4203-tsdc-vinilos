# Vinilos - MISW4203

Aplicación Android desarrollada con **Jetpack Compose** y arquitectura **MVVM + Clean Architecture**.

---

## Estructura del proyecto

```
app/src/main/java/com/misw4203/vinilos/
├── data/
│   ├── local/
│   │   ├── converter/   # TypeConverters de Room (listas ↔ JSON vía Gson)
│   │   ├── dao/         # Interfaces DAO
│   │   ├── database/    # VinilosDatabase (RoomDatabase)
│   │   └── entity/      # Entidades @Entity
│   ├── remote/
│   │   ├── api/         # Retrofit interfaces
│   │   └── dto/         # DTOs JSON
│   └── repository/      # Implementaciones de repositorios
├── domain/
│   ├── model/           # Modelos de dominio (sin deps de Android)
│   ├── repository/      # Interfaces de repositorio
│   └── usecase/         # Casos de uso (uno por acción)
├── presentation/
│   ├── navigation/      # NavHost + Destinations
│   ├── viewmodel/       # ViewModels + UiStates
│   └── ui/
│       ├── screens/
│       │   ├── album/
│       │   ├── artist/
│       │   ├── band/
│       │   └── collector/
│       ├── components/  # Composables reutilizables
│       └── theme/       # Material 3 theme
└── di/                  # Módulos de Hilt
```

---

## Descripción de cada capa

### `data/` — Capa de datos

Responsable de obtener y persistir datos, ya sea desde la red o desde la base de datos local.

| Carpeta | Contenido |
|---|---|
| `data/local/converter/` | `Converters` de Room. Serializa/deserializa listas anidadas (tracks, performers, comments, etc.) a JSON vía Gson para evitar normalizar cada entidad. |
| `data/local/dao/` | Interfaces DAO de Room con operaciones `@Upsert`, `@Query`, y transacciones (`replaceX` = clear + upsert). |
| `data/local/database/` | `VinilosDatabase` — clase abstracta `RoomDatabase` que declara entidades y expone los DAOs. |
| `data/local/entity/` | Entidades `@Entity` con mappers `toDomain()` / `fromDomain()`. |
| `data/remote/api/` | `VinilosApiService` — interfaz Retrofit. **Lectura:** `GET /albums`, `GET /albums/{id}`, `GET /musicians`, `GET /musicians/{id}`, `GET /prizes`, `GET /prizes/{id}`, `GET /performerprizes`, `GET /collectors`, `GET /collectors/{id}`, `GET /collectors/{id}/albums`, `GET /bands`, `GET /bands/{id}`. **Escritura (POST):** `POST /albums`, `POST /prizes`, `POST /albums/{id}/tracks`, `POST /albums/{id}/comments`, `POST /bands/{bandId}/musicians/{musicianId}`, `POST /collectors/{id}/albums/{albumId}`, `POST /collectors/{id}/musicians\|bands/{performerId}`, `POST /musicians/{id}/albums/{albumId}`, `POST /prizes/{id}/musicians\|bands/{performerId}`, `POST /albums/{id}/musicians\|bands/{performerId}`, `POST /bands/{id}/albums/{albumId}`. **Borrado (DELETE):** `DELETE /collectors/{id}/musicians\|bands/{performerId}`, `DELETE /collectors/{id}/albums/{albumId}`, `DELETE /albums/{id}/tracks/{trackId}`, `DELETE /albums/{id}/comments/{commentId}`. |
| `data/remote/dto/` | DTOs que modelan la respuesta JSON (`AlbumDto`, `TrackDto`, `CommentDto`, `MusicianDetailDto`, `PrizeDetailDto`, `PerformerPrizeDto`, `CollectorDto`, `CollectorDetailDto`, `BandDto`, `BandDetailDto`) y los bodies de escritura (`CreateAlbumRequestDto`, `CreateTrackRequest`, `CreateCommentRequest` con `CollectorRef`). Campos nullables para tolerar datos incompletos del servidor. |
| `data/repository/` | Implementaciones de repositorio con estrategia **network-first + fallback a caché** para lecturas y POST directo a red (sin caché) para escrituras. |

### Estrategia de caché

Todos los repositorios siguen el mismo patrón para **lecturas**:

1. Intenta red → si hay éxito, actualiza la caché (`replaceX` transaccional para listas, `upsert` para detalles) y retorna.
2. Si la red falla con `IOException` (offline) → retorna la caché si existe; si no, re-lanza el error.
3. Si falla con `HttpException` u otro → propaga (la UI clasifica 404, red, servidor, etc.).

Para **escrituras** (`POST /albums`, `POST /albums/{id}/tracks`, `POST /albums/{id}/comments`, `POST /bands/{bandId}/musicians/{musicianId}`) el repositorio aplica **write-through cache best-effort**:

- `createAlbum`: tras la respuesta exitosa hace `dao.upsert(AlbumEntity)` para insertar el nuevo álbum en la lista local.
- `addTrack` / `addComment`: si existe un `AlbumDetailEntity` cacheado para ese `albumId`, se hace read-modify-write apilando el nuevo track/comentario y `upsertDetail` lo persiste; si el detalle aún no está en caché, se omite la escritura local (el próximo `getAlbumById` la repoblará).
- `addMusicianToBand`: enriquece el detalle de banda en caché con el nuevo integrante. La enriquecimiento va envuelto en `try/catch` (re-lanzando `CancellationException`) para que un fallo del cache no convierta un POST exitoso en error visible.
- **Asociaciones** (`addAlbumToBand`, `addMusicianToAlbum`, `addBandToAlbum`, `addAlbumToCollector`, favoritos, `addPrizeToMusician\|Band`): tras el POST exitoso refrescan/parchean el detalle en caché best-effort (refetch del recurso autoritativo o read-modify-write); ante `IOException` la caché se deja intacta y se reconcilia en la siguiente lectura.
- **Borrados** (`removeTrack`, `removeComment`, `removeFavoriteMusician\|Band`, `removeAlbumFromCollector`): hacen el DELETE y luego un *prune* del detalle cacheado (filtran el ítem). El ViewModel aplica además **borrado optimista** sobre el `UiState` y restaura desde red si el DELETE falla.

Adicionalmente, la pantalla origen invalida su `UiState` vía `retry()` o un flag en `SavedStateHandle`. Esto asegura coherencia inmediata aunque se produzca una desconexión justo después del POST. Las excepciones se propagan al ViewModel sin envolverlas, igual que en las lecturas.

---

### `domain/` — Capa de dominio

Es el núcleo de la aplicación. No depende de ninguna otra capa y contiene la lógica de negocio pura.

| Carpeta | Contenido |
|---|---|
| `domain/model/` | Modelos de dominio (`Album`, `AlbumDetail`, `Musician`, `MusicianSummary`, `MusicianPrize`, `Track`, `Performer` (con `kind: PerformerKind` — `MUSICIAN`/`BAND`/`UNKNOWN`, inferido de `creationDate`/`birthDate` para elegir el endpoint correcto al quitar favoritos), `PerformerKind`, `Comment` (con `commenter: CollectorSummary?`), `CollectorSummary`, `CollectorDetail`, `CollectorAlbum`, `CollectorComment`, `Band` (incluye `prizes: List<MusicianPrize>`), `BandSummary`, `CreateAlbumInput`). Sin dependencias de Android. |
| `domain/repository/` | Interfaces de repositorio. `AlbumRepository` (`getAlbums`, `getAlbumById`, `createAlbum`, `addTrack`, `addComment`, `removeTrack`, `removeComment`, `addMusicianToAlbum`, `addBandToAlbum`); `MusicianRepository`; `CollectorRepository` (`...addFavorite/addAlbum`, `removeFavoriteMusician`, `removeFavoriteBand`, `removeAlbumFromCollector`); `BandRepository` (`getBands`, `getBandDetail`, `addMusicianToBand`, `addAlbumToBand`, `addPrizeToBand`). Los métodos mutadores nuevos tienen implementación por defecto no-op en la interfaz para que los fakes de test no rompan al evolucionar el contrato. |
| `domain/usecase/` | Casos de uso de lectura (`GetAlbumsUseCase`, `GetAlbumDetailUseCase`, `GetMusiciansUseCase`, `GetMusicianDetailUseCase`, `GetCollectorsUseCase`, `GetCollectorDetailUseCase`, `GetBandsUseCase`, `GetBandDetailUseCase`), de escritura (`CreateAlbumUseCase`, `AddTrackUseCase`, `AddCommentUseCase`, `AddMusicianToBandUseCase`, `AddAlbumToBandUseCase`, `AddMusicianToAlbumUseCase`, `AddBandToAlbumUseCase`, `AddPrizeToBandUseCase`) y de borrado (`RemoveTrackUseCase`, `RemoveCommentUseCase`, `RemoveFavoriteMusicianUseCase`, `RemoveFavoriteBandUseCase`, `RemoveAlbumFromCollectorUseCase`). |

---

### `presentation/` — Capa de presentación

Contiene todo lo relacionado con la interfaz de usuario y el estado de la pantalla.

| Carpeta | Contenido |
|---|---|
| `presentation/navigation/` | `VinilosNavHost` + `Destinations` (rutas: `album_list`, `album_detail/{albumId}`, `create_album`, `album/{albumId}/track/add`, `album/{albumId}/comment/add/{collectorId}`, `album/{albumId}/performers/add`, `artists` (hub con sub-tabs Músicos / Bandas), `artist/{id}`, `band/{bandId}`, `band/{bandId}/musicians/add`, `band/{bandId}/albums/add`, `band/{bandId}/prizes/add`, `collectors`, `collector/{collectorId}`, `collector/{collectorId}/favorites/add`). El refresh post-mutación se propaga vía flags en `SavedStateHandle` (`refresh_*_detail`). |
| `presentation/viewmodel/` | ViewModels con `@HiltViewModel`. Para listas/detalle exponen `StateFlow<UiState>` (`Loading / Success / Empty\|NotFound / Error(isNetworkError)`) y clasifican excepciones (`IOException` → red, `HttpException` 404 → NotFound, otros → servidor). Para formularios POST (`CreateAlbumViewModel`, `AddTrackViewModel`, `AddCommentViewModel`, `AddMusiciansToBandViewModel`) separan el form state del submit state (`Idle / Loading / Success / Error`) y emiten eventos one-shot (`MutableSharedFlow<Event>`) para snackbars de éxito/fallo, de modo que la rotación de pantalla no los re-dispare. Todos re-lanzan `CancellationException` para preservar structured concurrency. |
| `presentation/ui/screens/` | Composables por entidad: `album/` (`AlbumListScreen`, `AlbumDetailScreen`, `CreateAlbumScreen`, `AddTrackScreen`, `AddCommentScreen`, `AddPerformerToAlbumScreen`), `artist/` (`ArtistsHubScreen` con `MusicianListContent` + `BandListContent`, `MusicianDetailScreen`, `AddAlbumToMusicianScreen`, `AddPrizeToMusicianScreen`), `band/` (`BandDetailScreen`, `AddMusiciansToBandScreen`, `AddAlbumToBandScreen`, `AddPrizeToBandScreen`), `collector/` (`CollectorListScreen`, `CollectorDetailScreen`, `AddAlbumToCollectorScreen`, `AddFavoritePerformerScreen`). El borrado de favoritos/contenido usa diálogo de confirmación + `SnackbarHost` embebido; los detalles muestran las secciones (tracks, comentarios, performers, álbumes, premios) siempre visibles con su CTA. |
| `presentation/ui/components/` | `AlbumCard`, `MusicianCard`, `BandCard`, `CollectorCard`, `MusicianRow`, `EmptyMembersState`, `PerformerHeader`, `LoadingState`, `EmptyState`, `ErrorState`, `VinilosTopBar`, `VinilosBottomNav`, `SearchBarStatic`, `ListCounter`, `RatingBar`. |
| `presentation/ui/theme/` | Material 3: `Color.kt`, `Theme.kt`, `Type.kt`. |

---

### `di/` — Inyección de dependencias

Módulos de Hilt instalados en `SingletonComponent`.

| Archivo | Contenido |
|---|---|
| `NetworkModule` | Provee `OkHttpClient` (con `HttpLoggingInterceptor` solo en debug), `Retrofit` y `VinilosApiService`. |
| `DatabaseModule` | Provee `VinilosDatabase` v7 (con `fallbackToDestructiveMigration` — la caché es descartable; se sube la versión al cambiar el esquema serializado, p. ej. `Performer.kind` y `Band.prizes`) y los DAOs (`AlbumDao`, `MusicianDao`, `CollectorDao`, `BandDao`). |
| `RepositoryModule` | `@Binds` de las interfaces de dominio a sus implementaciones: `AlbumRepository → AlbumRepositoryImpl`, `MusicianRepository → MusicianRepositoryImpl`, `CollectorRepository → CollectorRepositoryImpl`, `BandRepository → BandRepositoryImpl`. |

---

## Stack tecnológico

| Tecnología | Uso |
|---|---|
| Jetpack Compose | Framework de UI declarativo |
| ViewModel + StateFlow | Gestión del estado de la UI |
| Hilt + KSP | Inyección de dependencias |
| Room | Persistencia local (caché offline) |
| Retrofit + Gson | Comunicación con el API REST |
| OkHttp | HTTP client + logging |
| Navigation Compose | Navegación entre pantallas |
| Coroutines | Operaciones asíncronas |
| Coil | Carga de imágenes |
| Material 3 | Sistema de diseño |

---

## Testing

| Ubicación | Tipo | Herramientas |
|---|---|---|
| `app/src/test/` | Unit tests JVM | JUnit 4, MockK, Turbine, `kotlinx-coroutines-test` |
| `app/src/androidTest/` | Compose UI tests (instrumentados) | `ui-test-junit4`, `ui-test-manifest`, `hilt-android-testing` |
| `app/src/androidTest/.../e2e/` | Pruebas E2E contra `MainActivity` real con datos fake | Compose Test + Hilt + `FakeRepositoryModule` |

**Convenciones**:
- ViewModel tests usan **fake repos inline** (clases anidadas que implementan la interfaz) para control explícito de resultados y conteo de llamadas.
- Repository tests usan **MockK** sobre `VinilosApiService` y los DAOs.
- Use-case tests mockean el repositorio y verifican delegación.
- Compose UI tests de componentes reciben el VM como parámetro (los screens aceptan `viewModel: VM = hiltViewModel()` con default), evitando montar Hilt.
- **Tests E2E** (`VinilosE2ETest`): atacan `MainActivity` real con Hilt. Un módulo `@TestInstallIn` (`FakeRepositoryModule`) reemplaza los repositorios de producción por implementaciones en memoria, por lo que **no requieren backend ni Docker**. Solo necesitan el emulador.

---

## Comandos

```bash
# Compilar APK debug
./gradlew assembleDebug

# Compilar APK release (firmado con vinilos-release.jks)
./gradlew assembleRelease
# APK generado en: app/build/outputs/apk/release/app-release.apk

# Tests unitarios
./gradlew test

# Tests instrumentados (requiere emulador/dispositivo)
# IMPORTANTE: usar emulador API 33 o 34. API 35+ rompe Espresso 3.6.1
# (NoSuchMethodException: InputManager.getInstance).
# No se requiere backend ni Docker — los repositorios están reemplazados por fakes.
./gradlew connectedAndroidTest

# Solo compilar tests instrumentados (sin ejecutar)
./gradlew assembleDebugAndroidTest

# Clean build
./gradlew clean assembleDebug

# Un test específico
./gradlew test --tests "com.misw4203.vinilos.presentation.viewmodel.AlbumListViewModelTest"
```

---

## Seguridad de red

El tráfico HTTP en texto plano está restringido a hosts de desarrollo (`10.0.2.2`, `localhost`, `127.0.0.1`) mediante `app/src/main/res/xml/network_security_config.xml`. No existe `android:usesCleartextTraffic="true"` en el manifest — la configuración basta.
