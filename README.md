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
| `data/remote/api/` | `VinilosApiService` — interfaz Retrofit con los endpoints de lectura (`GET /albums`, `GET /albums/{id}`, `GET /musicians`, `GET /musicians/{id}`, `GET /prizes/{id}`, `GET /collectors`, `GET /collectors/{id}`) y de escritura (`POST /albums`, `POST /albums/{id}/tracks`, `POST /albums/{id}/comments`). |
| `data/remote/dto/` | DTOs que modelan la respuesta JSON (`AlbumDto`, `TrackDto`, `CommentDto`, `MusicianDetailDto`, `PrizeDetailDto`, `PerformerPrizeDto`, `CollectorDto`, `CollectorDetailDto`) y los bodies de escritura (`CreateAlbumRequestDto`, `CreateTrackRequest`, `CreateCommentRequest`). Campos nullables para tolerar datos incompletos del servidor. |
| `data/repository/` | Implementaciones de repositorio con estrategia **network-first + fallback a caché** para lecturas y POST directo a red (sin caché) para escrituras. |

### Estrategia de caché

Todos los repositorios siguen el mismo patrón para **lecturas**:

1. Intenta red → si hay éxito, actualiza la caché (`replaceX` transaccional para listas, `upsert` para detalles) y retorna.
2. Si la red falla con `IOException` (offline) → retorna la caché si existe; si no, re-lanza el error.
3. Si falla con `HttpException` u otro → propaga (la UI clasifica 404, red, servidor, etc.).

Para **escrituras** (`POST /albums`, `POST /albums/{id}/tracks`, `POST /albums/{id}/comments`) el repositorio envía el request directamente al API sin tocar la caché. Tras un POST exitoso, la pantalla origen invalida su `UiState` (vía `retry()` o flag en `SavedStateHandle`) para que el siguiente `GET` refresque la caché con el dato nuevo. Las excepciones se propagan al ViewModel sin envolverlas, igual que en las lecturas.

---

### `domain/` — Capa de dominio

Es el núcleo de la aplicación. No depende de ninguna otra capa y contiene la lógica de negocio pura.

| Carpeta | Contenido |
|---|---|
| `domain/model/` | Modelos de dominio (`Album`, `AlbumDetail`, `Musician`, `MusicianSummary`, `MusicianPrize`, `Track`, `Performer`, `Comment`, `CollectorSummary`, `CollectorDetail`, `CollectorAlbum`, `CollectorComment`, `CreateAlbumInput`). Sin dependencias de Android. |
| `domain/repository/` | Interfaces de repositorio (`AlbumRepository` con `getAlbums`, `getAlbumById`, `createAlbum`, `addTrack`, `addComment`; `MusicianRepository`; `CollectorRepository`). |
| `domain/usecase/` | Casos de uso de lectura (`GetAlbumsUseCase`, `GetAlbumDetailUseCase`, `GetMusiciansUseCase`, `GetMusicianDetailUseCase`, `GetCollectorsUseCase`, `GetCollectorDetailUseCase`) y de escritura (`CreateAlbumUseCase`, `AddTrackUseCase`, `AddCommentUseCase`). |

---

### `presentation/` — Capa de presentación

Contiene todo lo relacionado con la interfaz de usuario y el estado de la pantalla.

| Carpeta | Contenido |
|---|---|
| `presentation/navigation/` | `VinilosNavHost` + `Destinations` (rutas: `album_list`, `album_detail/{albumId}`, `create_album`, `album/{albumId}/track/add`, `album/{albumId}/comment/add/{collectorId}`, `artists`, `artist/{id}`, `collectors`, `collector/{collectorId}`). |
| `presentation/viewmodel/` | ViewModels con `@HiltViewModel`. Para listas/detalle exponen `StateFlow<UiState>` (`Loading / Success / Empty\|NotFound / Error(isNetworkError)`) y clasifican excepciones (`IOException` → red, `HttpException` 404 → NotFound, otros → servidor). Para formularios POST (`CreateAlbumViewModel`, `AddTrackViewModel`, `AddCommentViewModel`) separan el form state del submit state (`Idle / Loading / Success / Error`). Todos re-lanzan `CancellationException` para preservar structured concurrency. |
| `presentation/ui/screens/` | Composables por entidad: `album/` (`AlbumListScreen`, `AlbumDetailScreen`, `CreateAlbumScreen`, `AddTrackScreen`, `AddCommentScreen`), `artist/` (`MusicianListScreen`, `MusicianDetailScreen`), `collector/` (`CollectorListScreen`, `CollectorDetailScreen`). |
| `presentation/ui/components/` | `AlbumCard`, `MusicianCard`, `CollectorCard`, `LoadingState`, `EmptyState`, `ErrorState`, `VinilosTopBar`, `VinilosBottomNav`, `SearchBarStatic`, `ListCounter`, `RatingBar`. |
| `presentation/ui/theme/` | Material 3: `Color.kt`, `Theme.kt`, `Type.kt`. |

---

### `di/` — Inyección de dependencias

Módulos de Hilt instalados en `SingletonComponent`.

| Archivo | Contenido |
|---|---|
| `NetworkModule` | Provee `OkHttpClient` (con `HttpLoggingInterceptor` solo en debug), `Retrofit` y `VinilosApiService`. |
| `DatabaseModule` | Provee `VinilosDatabase` (con `fallbackToDestructiveMigration` — la caché es descartable) y los DAOs (`AlbumDao`, `MusicianDao`, `CollectorDao`). |
| `RepositoryModule` | `@Binds` de las interfaces de dominio a sus implementaciones: `AlbumRepository → AlbumRepositoryImpl`, `MusicianRepository → MusicianRepositoryImpl`, `CollectorRepository → CollectorRepositoryImpl`. |

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
