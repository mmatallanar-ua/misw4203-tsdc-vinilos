# Pruebas E2E — Vinilos

Documento de referencia para las pruebas end-to-end de la app Vinilos.

---

## 1. Alcance

Las pruebas E2E (`app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt`) atacan **`MainActivity` real** con **Hilt** arrancado y un módulo de test (`FakeRepositoryModule` con `@TestInstallIn(replaces = [RepositoryModule::class])`) que reemplaza los repositorios de producción por fakes en memoria. Validan la integración UI → ViewModel → UseCase → Repositorio fake y la navegación entre pantallas.

**No se requiere backend ni Docker.** El dataset es determinista (definido en los fakes), lo que elimina el flakiness de tests que dependían de la semilla del backend o de la red.

Cobertura: features **Álbumes**, **Artistas**, **Bandas (HU012)** y **navegación entre tabs**.

---

## 2. Stack de pruebas

| Componente | Librería |
|---|---|
| Framework E2E | Jetpack Compose Test (`androidx.compose.ui:ui-test-junit4`) |
| Instrumentación | `androidx.test:runner 1.7.0` |
| DI en tests | `com.google.dagger:hilt-android-testing 2.59.2` |
| Runner custom | `com.misw4203.vinilos.HiltTestRunner` (arranca `HiltTestApplication`) |
| Sustitución de repos | `FakeRepositoryModule` (en `app/src/androidTest/.../di/`) con `@TestInstallIn(replaces = [RepositoryModule::class])` |
| Back del sistema | `androidx.test.espresso.Espresso.pressBack()` |

---

## 3. Requisitos para ejecutar

1. **Emulador Android API 33 o 34**.
   - API 35+ rompe Espresso 3.6.1 con `NoSuchMethodException: android.hardware.input.InputManager.getInstance`. Hasta que `espresso-core` stable lo soporte, usar Pixel 7/8 con Android 14 (API 34).
2. **Animaciones desactivadas** en el emulador. Sin esto, Espresso/Compose nunca llegan a `idle` y todos los tests fallan con `IllegalStateException: No compose hierarchies found`:
   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```
3. **Emulador despierto**. Una pantalla bloqueada / dormida (`mWakefulness=Asleep` en `dumpsys power`) impide que la `MainActivity` llegue a `setContent`:
   ```bash
   adb shell input keyevent KEYCODE_WAKEUP
   adb shell svc power stayon true
   ```

### Ejecución

```bash
# Todas las pruebas instrumentadas (incluye E2E + tests de componente)
./gradlew connectedAndroidTest

# Un test E2E específico
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.misw4203.vinilos.e2e.VinilosE2ETest#albumList_rendersListFromBackend
```

Reporte HTML: `app/build/reports/androidTests/connected/debug/index.html`.

---

## 4. Infraestructura añadida al proyecto

### `HiltTestRunner`
```kotlin
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?) =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
```

Configurado en `app/build.gradle.kts`:
```kotlin
testInstrumentationRunner = "com.misw4203.vinilos.HiltTestRunner"
```

### Deps añadidas en `libs.versions.toml`
```toml
androidx-test-runner = { group = "androidx.test", name = "runner", version = "1.7.0" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
```

### `FakeRepositoryModule` (sustituye al `RepositoryModule` real)

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
abstract class FakeRepositoryModule {
    @Binds @Singleton abstract fun bindAlbumRepository(impl: FakeAlbumRepository): AlbumRepository
    @Binds @Singleton abstract fun bindMusicianRepository(impl: FakeMusicianRepository): MusicianRepository
    @Binds @Singleton abstract fun bindCollectorRepository(impl: FakeCollectorRepository): CollectorRepository
    @Binds @Singleton abstract fun bindBandRepository(impl: FakeBandRepository): BandRepository
}
```

Cada `FakeXRepository` es una implementación en memoria que devuelve datasets fijos, lo que hace los tests deterministas.

### `testTag`s agregados al código de producción
| Composable | Tag |
|---|---|
| `AlbumListScreen` → `LazyColumn` | `albums_list` |
| `AlbumCard` (wrapper en lista) | `album_card_{id}` |
| `MusicianListContent` → `LazyColumn` | `artists_list` |
| `MusicianCard` (wrapper en lista) | `musician_card_{id}` |
| `BandListContent` → `LazyColumn` | `bands_list` |
| `BandCard` (wrapper en lista) | `band_card_{id}` |
| `CollectorListScreen` → `LazyColumn` | `collectors_list` |
| `CollectorCard` (wrapper en lista) | `collector_card_{id}` |
| `VinilosBottomNav` tabs | `bottom_nav_albums`, `bottom_nav_artists`, `bottom_nav_collectors` |
| `ArtistsHubScreen` sub-tabs | `artists_tab_musicians`, `artists_tab_bands` |
| `AlbumDetailScreen` root | `album_detail_root` |
| `AlbumDetailScreen` back btn | `album_detail_back` |
| `MusicianDetailScreen` body | `artist_detail_root` |
| `MusicianDetailScreen` back btn | `artist_detail_back` |
| `BandDetailScreen` root | `band_detail_root` |
| `BandDetailScreen` back btn | `band_detail_back` |
| `AddMusiciansToBandScreen` root | `add_musicians_screen_root` |
| `AddMusiciansToBandScreen` back btn | `add_musicians_back` |
| `PrizeItem` en detalle de artista | `prize_{id}` |

---

## 5. Escenarios cubiertos

La suite incluye **22 tests E2E** distribuidos en los módulos de Álbumes, Artistas, Coleccionistas, Bandas (HU012) y navegación. Cada test parte de datos deterministas inyectados por los fakes.

### Álbumes — lista, detalle y creación
| ID | Escenario | Cobertura |
|---|---|---|
| AL-01 | La pantalla principal muestra la lista de álbumes del catálogo. | ✅ E2E |
| AL-05 | Al tocar una card el usuario aterriza en el detalle. | ✅ E2E |
| AD-01 | Detalle abierto muestra imagen, título, metadatos. | ✅ E2E |
| AD-02 | Botón back del top bar regresa a la lista. | ✅ E2E |
| AD-05 | El rating de comentarios expone `contentDescription` accesible ("N de 5 estrellas"). | ✅ E2E |
| AL-07 | El FAB abre el formulario de creación de álbum. | ✅ E2E |
| AL-08 | El submit vacío en creación muestra errores de validación. | ✅ E2E |

### Artistas — lista y detalle
| ID | Escenario | Cobertura |
|---|---|---|
| ML-01 | Al cambiar al tab "Artists" se muestra la lista de músicos. | ✅ E2E |
| ML-04 | Al tocar una card de músico se abre el detalle. | ✅ E2E |
| MD-04 | Botón back regresa a la lista de artistas. | ✅ E2E |

### Coleccionistas — lista y detalle
| ID | Escenario | Cobertura |
|---|---|---|
| CL-01 | El tab "Collectors" muestra la lista. | ✅ E2E |
| CL-02 | Las cards exponen sus testTags `collector_card_*`. | ✅ E2E |
| CD-01 | El detalle muestra álbumes coleccionados y artistas favoritos. | ✅ E2E |
| CD-02 | Botón back regresa a la lista (top bar y sistema). | ✅ E2E |
| CD-05 | El rating de comentarios del coleccionista expone `contentDescription` accesible. | ✅ E2E |

### Bandas (HU012) — flujo de agregar músicos
| ID | Escenario | Cobertura |
|---|---|---|
| BD-01 | Desde el sub-tab "Bandas" se navega al detalle, se abre "Agregar músicos", se selecciona uno, se publica y aparece en integrantes. | ✅ E2E (`hu012_addMusicianToBand_flow`) |

### Navegación
| ID | Escenario | Cobertura |
|---|---|---|
| NAV-01 | La bottom nav cambia entre tabs y el título refleja el cambio. | ✅ E2E |
| NAV-03 | El back del sistema desde un detalle regresa a la lista. | ✅ E2E |

### Escenarios NO cubiertos por E2E (y dónde sí están)
| ID | Escenario | Motivo de exclusión E2E | Cobertura alterna |
|---|---|---|---|
| AL-02, ML-02 | Empty state | Los fakes inicializan con dataset poblado. Sería trivial añadir un flavor "empty" del fake si se requiere. | Unit tests de VM |
| AL-03, AL-04, AL-06, AD-03, AD-04, ML-03 | Error state (red/servidor) + retry | Los fakes actuales no exponen un toggle de error; sería trivial añadir un setter para forzar `IOException` / `HttpException` en próximos escenarios. | Unit tests de VM y repo |
| AD-03, MD-05 | NotFound (404) | Requiere deep-link a ID inexistente; la nav no expone args de test. | Unit tests de VM |
| MD-02, MD-03 | AlertDialog de premios | El dataset fake del artista base no garantiza performerPrizes. Fácil de añadir. | Unit tests de VM |

---

## 6. Casos de prueba detallados

> Esta sección documenta tests representativos. La suite completa (`VinilosE2ETest.kt`) tiene 22 tests y los no listados siguen el mismo patrón (`waitForTag` → `performClick` → assert). Para HU012 el test es `hu012_addMusicianToBand_flow`.

### 6.1 `albumList_rendersListFromBackend` — AL-01

**Objetivo**: verificar que la pantalla inicial carga álbumes del backend.

**Precondiciones**: el fake provee ≥1 álbum (true por defecto).

**Pasos**:
1. Esperar hasta que exista un nodo con `testTag("albums_list")` (timeout 10s).

**Validaciones**:
- `onNodeWithTag("albums_list").assertIsDisplayed()`.

---

### 6.2 `albumList_tapFirstCard_opensDetail_andBackReturns` — AL-05, AD-01, AD-02

**Objetivo**: flujo completo lista → detalle → regreso.

**Precondiciones**: el fake provee ≥1 álbum (true por defecto).

**Pasos**:
1. Esperar a `albums_list`.
2. Scroll al primer nodo cuyo `TestTag` empiece por `album_card_` (SemanticsMatcher custom).
3. Click sobre ese nodo.
4. Esperar a `album_detail_root`.
5. Click en `album_detail_back`.
6. Esperar a `albums_list` nuevamente.

**Validaciones**:
- `album_detail_root` visible tras el click (navegación efectiva).
- `albums_list` visible tras el back.

---

### 6.3 `artistList_rendersListFromBackend` — ML-01

**Objetivo**: el tab "Artists" carga el listado de artistas.

**Precondiciones**: el fake provee ≥1 artista (true por defecto).

**Pasos**:
1. Click en `bottom_nav_artists`.
2. Esperar a `artists_list`.

**Validaciones**:
- `artists_list` visible.

---

### 6.4 `artistList_tapFirstCard_opensDetail_andBackReturns` — ML-04, MD-01, MD-04

**Objetivo**: flujo lista de artistas → detalle → back.

**Pasos**:
1. Click en `bottom_nav_artists`.
2. Esperar a `artists_list`.
3. Click en primer nodo con tag que empieza por `musician_card_`.
4. Esperar a `artist_detail_root`.
5. Click en `artist_detail_back`.
6. Esperar a `artists_list`.

**Validaciones**:
- `artist_detail_root` visible tras el click.
- `artists_list` visible tras el back.

---

### 6.5 `bottomNav_switchesBetweenTabs` — NAV-01

**Objetivo**: la bottom nav cambia entre Albums ↔ Artists y el top bar refleja el cambio.

**Pasos**:
1. Esperar a `albums_list`; verificar que el título "Albums" esté visible.
2. Click en `bottom_nav_artists`; esperar al texto del título "Artists".
3. Click en `bottom_nav_albums`; esperar a `albums_list`.

**Validaciones**:
- Tras cada switch, el título correcto (`albums_title` / `artists_title`) está `isDisplayed`.

---

### 6.6 `systemBack_fromAlbumDetail_returnsToList` — NAV-03

**Objetivo**: el back del sistema operativo (gesto/botón) regresa al listado desde el detalle.

**Pasos**:
1. Esperar a `albums_list`, scroll y click en primer `album_card_*`.
2. Esperar a `album_detail_root`.
3. `Espresso.pressBack()`.
4. Esperar a `albums_list`.

**Validaciones**:
- `albums_list` visible nuevamente.

---

### 6.7 `albumDetail_ratingHasAccessibleContentDescription_ifCommentsPresent` — AD-05

**Objetivo**: accesibilidad — los ratings de comentarios exponen `contentDescription` legible por TalkBack ("N de 5 estrellas").

**Precondiciones**: el primer álbum del fake tiene ≥1 comentario.

**Pasos**:
1. Esperar a `albums_list`, scroll y click en el primer `album_card_*`.
2. Esperar a `album_detail_root`.
3. Buscar nodos cuyo `contentDescription` contenga `"de 5"` (substring match).
4. Si la lista viene vacía, el test es no-op (el álbum no tiene comentarios).

**Validaciones**:
- Primer nodo encontrado debe existir (`assertExists`, **no** `assertIsDisplayed` — el nodo puede estar fuera del viewport pero seguir siendo anunciado por TalkBack al hacer scroll).

---

## 7. Matchers y helpers personalizados

### `tagStartsWith(prefix: String): SemanticsMatcher`
Evita acoplar los tests a los IDs sembrados por el backend.
```kotlin
private fun tagStartsWith(prefix: String): SemanticsMatcher =
    SemanticsMatcher("TestTag starts with '$prefix'") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: return@SemanticsMatcher false
        tag.startsWith(prefix)
    }
```

Uso:
```kotlin
val firstCard = tagStartsWith("album_card_")
composeRule.onNodeWithTag("albums_list").performScrollToNode(firstCard)
composeRule.onAllNodes(firstCard)[0].performClick()
```

### `waitForTag(tag: String)` / `waitForText(text: String)`
Bloquean hasta que el nodo aparece en el semantic tree (timeout 10s). Necesarios porque los `StateFlow` pasan de `Loading → Success` asíncronamente con la red.

---

## 8. Lecciones aprendidas durante la implementación

1. **Compose ≠ Espresso clásico.** El brief original pedía `onView(withId(...))`, pero el proyecto es 100% Compose. La API correcta es `onNodeWithTag/Text/ContentDescription` + `SemanticsMatcher`. Espresso clásico no aplica salvo para `pressBack()`.
2. **Espresso 3.6.1 + API 35+ = crash.** `InputManager.getInstance()` fue removido en API 34; Espresso aún lo reflexiona. Workaround: emulador API 33/34 hasta que salga una versión stable de `espresso-core` compatible.
3. **IDs hardcodeados son frágiles.** Aunque ahora los fakes definen IDs estables, mantenemos `SemanticsMatcher` con `startsWith` para acoplar menos los tests a los datos.
4. **`assertIsDisplayed()` es estricto con viewport.** Para accesibilidad o para verificar que un nodo está en el tree, `assertExists()` es más correcto. TalkBack anuncia nodos aunque no estén en pantalla si el usuario hace scroll hacia ellos.
5. **Animaciones y wake-lock matan los tests.** Sin animaciones desactivadas el idling resource nunca settlea (`No compose hierarchies found`); sin `stayon true` la `MainActivity` no llega a `setContent`. Ambos chequeos viven en la sección 3.
6. **Fakes vs backend real.** El paso a `FakeRepositoryModule` eliminó el flakiness ligado a la semilla del backend y la latencia de red. El trade-off es que la integración Retrofit → backend → Room ya no se valida en CI; queda cubierta por unit tests del repositorio y, eventualmente, por un perfil opcional de tests con backend real.

---

## 9. Siguientes pasos (propuestos, no implementados)

- **Fakes con toggle de error**: añadir setters a los `FakeXRepository` para forzar `IOException` / `HttpException` y cubrir AL-03/04, AD-03/04, ML-03 sin tocar networking.
- **Fakes "empty"**: variante o flag para devolver listas vacías y cubrir AL-02, ML-02 (Empty state).
- **Screenshot testing (Paparazzi/Roborazzi)**: regressions visuales de cards, top bar, bottom nav. Costo: plugin adicional y PNGs en repo.
- **CI**: ejecutar `connectedAndroidTest` en GitHub Actions con emulador API 34. Sin docker (los fakes hacen la suite autosuficiente).
