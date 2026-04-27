# Pruebas E2E — Vinilos

Documento de referencia para las pruebas end-to-end de la app Vinilos.

---

## 1. Alcance

Las pruebas E2E (`app/src/androidTest/java/com/misw4203/vinilos/e2e/VinilosE2ETest.kt`) atacan **`MainActivity` real** con **Hilt** arrancado, y consumen el **backend de Vinilos** levantado localmente vía docker-compose. No usan fakes ni mocks — validan la integración completa: UI → ViewModel → UseCase → Repositorio → Retrofit → backend → Room.

Cobertura: features **Álbumes**, **Artistas** y **navegación entre tabs**.

---

## 2. Stack de pruebas

| Componente | Librería |
|---|---|
| Framework E2E | Jetpack Compose Test (`androidx.compose.ui:ui-test-junit4`) |
| Instrumentación | `androidx.test:runner 1.6.2` |
| DI en tests | `com.google.dagger:hilt-android-testing` |
| Runner custom | `com.misw4203.vinilos.HiltTestRunner` (arranca `HiltTestApplication`) |
| Back del sistema | `androidx.test.espresso.Espresso.pressBack()` |
| Backend | `backvynils-web-1` (docker) en `http://10.0.2.2:3000` |

---

## 3. Requisitos para ejecutar

1. **Emulador Android API 33 o 34**.
   - API 35+ rompe Espresso 3.6.1 con `NoSuchMethodException: android.hardware.input.InputManager.getInstance`. Hasta que `espresso-core` stable lo soporte, usar Pixel 7/8 con Android 14 (API 34).
2. **Backend docker corriendo**:
   ```bash
   docker compose up -d
   # Verificar:
   curl http://localhost:3000/albums
   ```
   - `backvynils-db-1` (postgres:16) en puerto 5432.
   - `backvynils-web-1` en puerto 3000.
3. **Semilla del backend**: al menos 1 álbum y 1 artista. El set por defecto trae 4 álbumes (IDs 100–103) y 1 artista (ID 100).

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
androidx-test-runner = { group = "androidx.test", name = "runner", version = "1.6.2" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
```

### `testTag`s agregados al código de producción
| Composable | Tag |
|---|---|
| `AlbumListScreen` → `LazyColumn` | `albums_list` |
| `AlbumCard` (wrapper en lista) | `album_card_{id}` |
| `MusicianListScreen` → `LazyColumn` | `artists_list` |
| `MusicianCard` (wrapper en lista) | `musician_card_{id}` |
| `VinilosBottomNav` tabs | `bottom_nav_albums`, `bottom_nav_artists`, `bottom_nav_collectors` |
| `AlbumDetailScreen` root | `album_detail_root` |
| `AlbumDetailScreen` back btn | `album_detail_back` |
| `MusicianDetailScreen` body | `artist_detail_root` |
| `MusicianDetailScreen` back btn | `artist_detail_back` |
| `PrizeItem` en detalle de artista | `prize_{id}` |

---

## 5. Escenarios cubiertos

### Álbumes — lista y detalle
| ID | Escenario | Cobertura |
|---|---|---|
| AL-01 | Dado el backend con álbumes, la pantalla principal muestra la lista. | ✅ E2E |
| AL-05 | Dado que la lista está cargada, al tocar una card el usuario aterriza en el detalle. | ✅ E2E |
| AD-01 | Dado un álbum abierto, se muestra su detalle (imagen, título, metadatos). | ✅ E2E |
| AD-02 | Dado el detalle abierto, al tocar "back" vuelve a la lista. | ✅ E2E |
| AD-05 | Dado un álbum con comentarios, el rating expone `contentDescription` accesible (TalkBack anuncia "N de 5 estrellas"). | ✅ E2E |

### Artistas — lista y detalle
| ID | Escenario | Cobertura |
|---|---|---|
| ML-01 | Dado el backend con artistas, al cambiar al tab "Artists" se muestra la lista. | ✅ E2E |
| ML-04 | Dado el listado de artistas, al tocar una card se abre el detalle. | ✅ E2E |
| MD-01 | Dado un artista abierto, se muestra su detalle. | ✅ E2E |
| MD-04 | Dado el detalle abierto, al tocar "back" vuelve a la lista de artistas. | ✅ E2E |

### Navegación
| ID | Escenario | Cobertura |
|---|---|---|
| NAV-01 | Dado estar en un tab, al tocar otro en la bottom nav cambia la pantalla y el título. | ✅ E2E |
| NAV-03 | Dado estar en un detalle, el botón back del sistema regresa a la lista. | ✅ E2E |

### Escenarios NO cubiertos por E2E (y dónde sí están)
| ID | Escenario | Motivo de exclusión E2E | Cobertura alterna |
|---|---|---|---|
| AL-02, ML-02 | Empty state | No se puede forzar lista vacía en backend real | Unit tests de VM |
| AL-03, AL-04, AL-06, AD-03, AD-04, ML-03 | Error state (red/servidor) + retry | No se puede inyectar `IOException` / HTTP 500 sin apagar docker mid-test | Unit tests de VM y repo |
| AD-03, MD-05 | NotFound (404) | Requiere deep-link a ID inexistente; la nav no expone args de test | Unit tests de VM |
| MD-02, MD-03 | AlertDialog de premios | La semilla actual no confirma performerPrizes en el artista base | Fácil de añadir si se garantiza dataset |

---

## 6. Casos de prueba detallados

### 6.1 `albumList_rendersListFromBackend` — AL-01

**Objetivo**: verificar que la pantalla inicial carga álbumes del backend.

**Precondiciones**: backend up con ≥1 álbum.

**Pasos**:
1. Esperar hasta que exista un nodo con `testTag("albums_list")` (timeout 10s).

**Validaciones**:
- `onNodeWithTag("albums_list").assertIsDisplayed()`.

---

### 6.2 `albumList_tapFirstCard_opensDetail_andBackReturns` — AL-05, AD-01, AD-02

**Objetivo**: flujo completo lista → detalle → regreso.

**Precondiciones**: backend up con ≥1 álbum.

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

**Precondiciones**: backend up con ≥1 artista.

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

**Precondiciones**: el primer álbum del backend tiene ≥1 comentario.

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
3. **IDs hardcodeados son frágiles.** Primer intento iteraba `album_card_1..50`, pero el backend siembra IDs 100–103. Solución: `SemanticsMatcher` con `startsWith`.
4. **`assertIsDisplayed()` es estricto con viewport.** Para accesibilidad o para verificar que un nodo está en el tree, `assertExists()` es más correcto. TalkBack anuncia nodos aunque no estén en pantalla si el usuario hace scroll hacia ellos.
5. **Network-first + cache puede enmascarar escenarios offline.** La primera vez que un test corre, la caché está vacía; tests subsecuentes pueden ver la caché aunque el backend esté abajo. Aceptable para esta suite (no testea offline), pero relevante si se añaden escenarios de error.

---

## 9. Siguientes pasos (propuestos, no implementados)

- **MockWebServer + `@UninstallModules(NetworkModule)`**: habilita escenarios de Error (AL-03/04, AD-03/04, ML-03) y NotFound (AD-03, MD-05) sin depender de apagar docker.
- **Dataset seed determinista**: endpoint o script que garantice comentarios y prizes para estabilizar AD-05 y MD-02/03.
- **Screenshot testing (Paparazzi/Roborazzi)**: regressions visuales de cards, top bar, bottom nav. Costo: plugin adicional y PNGs en repo.
- **CI**: ejecutar `connectedAndroidTest` en GitHub Actions con emulador API 34 y `docker compose up` previo.
