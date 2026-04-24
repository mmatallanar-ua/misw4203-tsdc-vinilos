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
│       │   └── artist/
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
| `data/remote/api/` | `VinilosApiService` — interfaz Retrofit con los endpoints (`GET /albums`, `GET /albums/{id}`, `GET /musicians`, `GET /musicians/{id}`, `GET /prizes/{id}`). |
| `data/remote/dto/` | DTOs que modelan la respuesta JSON. Campos nullables para tolerar datos incompletos del servidor. |
| `data/repository/` | Implementaciones de repositorio con estrategia **network-first + fallback a caché**. |

### Estrategia de caché

Todos los repositorios siguen el mismo patrón:

1. Intenta red → si hay éxito, actualiza la caché (`replaceX` transaccional para listas, `upsert` para detalles) y retorna.
2. Si la red falla con `IOException` (offline) → retorna la caché si existe; si no, re-lanza el error.
3. Si falla con `HttpException` u otro → propaga (la UI clasifica 404, red, servidor, etc.).

---

### `domain/` — Capa de dominio

Es el núcleo de la aplicación. No depende de ninguna otra capa y contiene la lógica de negocio pura.

| Carpeta | Contenido |
|---|---|
| `domain/model/` | Modelos de dominio (`Album`, `AlbumDetail`, `Musician`, `MusicianSummary`, `MusicianPrize`, `Track`, `Performer`, `Comment`). Sin dependencias de Android. |
| `domain/repository/` | Interfaces de repositorio (`AlbumRepository`, `MusicianRepository`). |
| `domain/usecase/` | Casos de uso (`GetAlbumsUseCase`, `GetAlbumDetailUseCase`, `GetMusiciansUseCase`, `GetMusicianDetailUseCase`). |

---

### `presentation/` — Capa de presentación

Contiene todo lo relacionado con la interfaz de usuario y el estado de la pantalla.

| Carpeta | Contenido |
|---|---|
| `presentation/navigation/` | `VinilosNavHost` + `Destinations` (rutas y argumentos de navegación). |
| `presentation/viewmodel/` | ViewModels con `@HiltViewModel`. Exponen `StateFlow<UiState>` y clasifican excepciones (`IOException` → red, `HttpException` 404 → NotFound, otros → servidor). Re-lanzan `CancellationException` para preservar structured concurrency. |
| `presentation/ui/screens/` | Composables por entidad (`album/`, `artist/`). |
| `presentation/ui/components/` | `AlbumCard`, `MusicianCard`, `LoadingState`, `EmptyState`, `ErrorState`, `VinilosTopBar`, `VinilosBottomNav`, `SearchBarStatic`. |
| `presentation/ui/theme/` | Material 3: `Color.kt`, `Theme.kt`, `Type.kt`. |

---

### `di/` — Inyección de dependencias

Módulos de Hilt instalados en `SingletonComponent`.

| Archivo | Contenido |
|---|---|
| `NetworkModule` | Provee `OkHttpClient` (con `HttpLoggingInterceptor` solo en debug), `Retrofit` y `VinilosApiService`. |
| `DatabaseModule` | Provee `VinilosDatabase` (con `fallbackToDestructiveMigration` — la caché es descartable) y los DAOs (`AlbumDao`, `MusicianDao`). |
| `RepositoryModule` | `@Binds` de las interfaces de dominio a sus implementaciones. |

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
| `app/src/androidTest/` | Compose UI tests (instrumentados) | `ui-test-junit4`, `ui-test-manifest` |

**Convenciones**:
- ViewModel tests usan **fake repos inline** (clases anidadas que implementan la interfaz) para control explícito de resultados y conteo de llamadas.
- Repository tests usan **MockK** sobre `VinilosApiService` y los DAOs.
- Use-case tests mockean el repositorio y verifican delegación.
- Compose UI tests reciben el VM como parámetro (los screens aceptan `viewModel: VM = hiltViewModel()` con default), evitando montar Hilt en tests.

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
