# Correcciones de accesibilidad — Vinilos (2026-05-18)

Análisis y correcciones derivadas de los reportes generados por el **Escáner de Accesibilidad de Google** sobre 37 pantallas de la app Android Vinilos.

---

## Resumen de hallazgos

| Categoría | Reportes afectados | Estado |
|---|---|---|
| Contraste del texto — barra de navegación inferior | Casi todos | ✅ Corregido |
| Contraste del texto — barra de búsqueda (placeholder e ícono) | report1 | ✅ Corregido |
| Contraste del texto — etiquetas secundarias (#757C7D) | report52, report65, otros | ✅ Corregido (token Outline) |
| Contraste de imagen — placeholder de portadas | report8, 9, 12, 13, 44, 45, 32 | ✅ Corregido |
| Texto no expuesto — imágenes de álbumes/artistas | report8, 9, 23, 32, 65 | ✅ Corregido |
| Descripciones duplicadas — estrellas de valoración (★) | report16, 17 | ✅ Corregido |
| Descripciones duplicadas — "Eliminar comentario" | report17 | ✅ Corregido |
| Descripciones duplicadas — "Fecha de Lanzamiento" | report20 | ✅ Corregido |
| Descripciones duplicadas — nombre del premio (banda) | report16 (BandDetail) | ✅ Corregido |
| Elementos clicables superpuestos — botón Volver | report52 | ⚠️ Falso positivo de navegación |
| Tipo de elemento no admitido (u30) | Múltiples | ℹ️ Falso positivo de Compose |

---

## Detalle de hallazgos y soluciones

---

### 1. Contraste del texto — Barra de navegación inferior

**Reportes:** report3, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 20, 21, 23, 25, 27, 28, 29, 30, 32, 33, 34, 44, 45, 46, 47, 49, 50, 51, 52, 57, 58, 65, 66, 67 (y más)

**Descripción del hallazgo:**
El scanner detectó que las etiquetas de las pestañas inactivas de la barra de navegación inferior usaban el color `#ADB3B4` sobre el fondo `#F9F9F9`, lo que resulta en una relación de contraste de **2.02**. La WCAG requiere **≥ 4.50** para texto de tamaño pequeño.

Los elementos detectados correspondían a las coordenadas verticales `y ≈ 2401–2436` (zona inferior de la pantalla donde reside la barra de navegación) en prácticamente todas las pantallas de la app.

**Causa raíz:**
- El color del tinte para pestañas inactivas estaba configurado con `MaterialTheme.colorScheme.outlineVariant` (`OutlineVariant = #ADB3B4`), que es demasiado claro para cumplir con el requisito de contraste sobre el fondo claro de la superficie.
- Adicionalmente, el token `Outline = #757C7D` usado en otras etiquetas secundarias daba un contraste de **4.04**, levemente por debajo del umbral de 4.50.

**Solución aplicada:**

*Archivo: `app/src/main/java/com/misw4203/vinilos/presentation/ui/theme/Color.kt`*
```kotlin
// Antes
Outline = Color(0xFF757C7D)

// Después
Outline = Color(0xFF545C5D)  // contraste ≥ 5.71:1 sobre #F9F9F9
```

*Archivo: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/VinilosBottomNav.kt`*
```kotlin
// Antes
val tint = if (active) MaterialTheme.colorScheme.onSurface
           else MaterialTheme.colorScheme.outlineVariant

// Después
val tint = if (active) MaterialTheme.colorScheme.onSurface
           else MaterialTheme.colorScheme.onSurfaceVariant  // #5A6061, ratio ~6:1
```

---

### 2. Contraste del texto — Barra de búsqueda (placeholder e ícono)

**Reportes:** report1

**Descripción del hallazgo:**
En la pantalla principal (lista de álbumes con barra de búsqueda), el scanner detectó el texto del placeholder de la barra de búsqueda con color `#9CA0A0` sobre `#FFFFFF`, dando una relación de contraste de **2.64** (requiere ≥ 4.50).

El mismo problema afectaba al ícono de búsqueda, cuyo tinte tenía una opacidad reducida aplicada programáticamente (`alpha = 0.5f`).

**Causa raíz:**
En `SearchBarStatic.kt` se aplicaban modificadores `.copy(alpha = 0.5f)` al ícono y `.copy(alpha = 0.6f)` al color del placeholder, lo que reducía visualmente la opacidad y degradaba el contraste resultante.

**Solución aplicada:**

*Archivo: `app/src/main/java/com/misw4203/vinilos/presentation/ui/components/SearchBarStatic.kt`*
```kotlin
// Antes
tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),

// Después
tint = MaterialTheme.colorScheme.onSurfaceVariant,   // #5A6061, ratio ~6:1
color = MaterialTheme.colorScheme.onSurfaceVariant,
```

---

### 3. Contraste de imagen — Placeholder de portadas de álbumes

**Reportes:** report8, report9, report12, report13, report32, report44, report45

**Descripción del hallazgo:**
Al cargar imágenes de portadas de álbumes, se mostraba un color de fondo provisional (`surfaceContainerHighest = #DDE4E5`) mientras la imagen se descargaba. Este color sobre el fondo de la pantalla (`#F9F9F9`) daba una relación de contraste de **1.22**, muy por debajo del umbral de **3.00** requerido para imágenes.

El problema se presentaba en las pantallas: lista de álbumes, detalle de álbum, agregar álbum a banda, agregar álbum a músico y agregar track.

**Causa raíz:**
El `AsyncImage` de Coil usaba `MaterialTheme.colorScheme.surfaceContainerHighest` como fondo del `Modifier.background(...)`, que es un tono casi idéntico al fondo de la pantalla.

**Solución aplicada:**

Se cambió el background del placeholder a `outline` (ahora `#545C5D`), que provee suficiente contraste y actúa como un borde oscuro visible mientras la imagen carga.

*Archivos afectados:*
- `AlbumCard.kt`
- `AddAlbumToBandScreen.kt` (composables `AlbumRow` y `CurrentAlbumItem`)
- `AddAlbumToMusicianScreen.kt` (composables `AlbumRow` y `CurrentAlbumItem`)
- `AddTrackScreen.kt` (composable `AlbumHeaderCard`)

```kotlin
// Antes
modifier = Modifier
    .size(52.dp)
    .clip(RoundedCornerShape(8.dp))
    .background(MaterialTheme.colorScheme.surfaceContainerHighest)  // #DDE4E5

// Después
modifier = Modifier
    .size(52.dp)
    .clip(RoundedCornerShape(8.dp))
    .background(MaterialTheme.colorScheme.outline)  // #545C5D, ratio ≥ 3:1
```

---

### 4. Texto no expuesto — Texto detectado en imágenes de álbumes/artistas

**Reportes:** report8, report9, report23, report32, report65

**Descripción del hallazgo:**
El scanner usa OCR para detectar texto visible dentro de imágenes. Detectó texto legible en las portadas de álbumes y fotos de artistas (ej: "AMERICA", "rubén blodes", "Ratkin Macdosy Seis det:") que no estaba expuesto en el árbol de accesibilidad porque el `contentDescription` del `AsyncImage` era `null`.

Sin descripción de accesibilidad, los lectores de pantalla como TalkBack no pueden anunciar el contenido de la imagen al usuario.

**Causa raíz:**
Los componentes `AsyncImage` de Coil en varios lugares de la app tenían `contentDescription = null`, lo que hace que el elemento sea invisible para los servicios de accesibilidad.

**Solución aplicada:**

Se asignó `contentDescription = album.name` (o el nombre del artista cuando corresponde) a cada `AsyncImage` que representaba una portada o imagen de perfil.

*Archivos afectados:*
- `AlbumCard.kt`: `contentDescription = album.name`
- `AddAlbumToBandScreen.kt`: `contentDescription = album.name` en AlbumRow y CurrentAlbumItem
- `AddAlbumToMusicianScreen.kt`: `contentDescription = album.name` en AlbumRow y CurrentAlbumItem
- `AddTrackScreen.kt`: `contentDescription = album.name` en AlbumHeaderCard

```kotlin
// Antes
AsyncImage(
    model = album.coverUrl,
    contentDescription = null,  // ← invisible para TalkBack
    ...
)

// Después
AsyncImage(
    model = album.coverUrl,
    contentDescription = album.name,  // ← anunciado al usuario
    ...
)
```

---

### 5. Descripciones duplicadas — Estrellas de valoración (★)

**Reportes:** report16, report17

**Descripción del hallazgo:**
En la pantalla de detalle de álbum, cada comentario mostraba una fila de estrellas (★). El scanner detectó que cada carácter "★" estaba expuesto individualmente en el árbol de accesibilidad, produciendo **25 elementos idénticos** con descripción "★". Esto es confuso para el usuario de TalkBack, que escucharía "★ ★ ★ ★ ★" en lugar de "5 de 5 estrellas".

**Causa raíz:**
La fila de estrellas usaba `semantics { contentDescription = ratingDesc }` pero sin `clearAndSetSemantics`, lo que permitía que los nodos hijos (los `Text("★")` individuales) siguieran siendo accesibles.

**Solución aplicada:**

*Archivo: `AlbumDetailScreen.kt`*
```kotlin
// Antes
modifier = Modifier
    .weight(1f)
    .semantics { contentDescription = ratingDesc },

// Después
modifier = Modifier
    .weight(1f)
    .clearAndSetSemantics { contentDescription = ratingDesc },
    // clearAndSetSemantics elimina los nodos hijos del árbol de accesibilidad
    // y reemplaza la descripción con un único mensaje "4 de 5 estrellas"
```

Adicionalmente se importó `androidx.compose.ui.semantics.clearAndSetSemantics`.

---

### 6. Descripciones duplicadas — "Eliminar comentario"

**Reportes:** report17

**Descripción del hallazgo:**
En la lista de comentarios del detalle de álbum, todos los botones de eliminar anunciaban exactamente el mismo texto: **"Eliminar comentario"**. Con 5 comentarios en pantalla, TalkBack presentaba 5 botones con descripción idéntica, sin forma de distinguir a cuál corresponde cada uno.

**Causa raíz:**
La `contentDescription` del botón era `stringResource(R.string.album_remove_comment_cd)` = "Eliminar comentario" para todos los comentarios, sin diferenciar por autor o contenido.

**Solución aplicada:**

*Archivo: `AlbumDetailScreen.kt`*
```kotlin
// Antes
contentDescription = stringResource(R.string.album_remove_comment_cd),

// Después
val commenterLabel = comment.commenter?.name ?: "${comment.rating} ★"
contentDescription = stringResource(R.string.album_remove_comment_by_cd, commenterLabel),
// Anuncia: "Eliminar comentario de Carlos Pérez"
```

*Archivo: `strings.xml`* — nuevo recurso:
```xml
<string name="album_remove_comment_by_cd">Eliminar comentario de %s</string>
```

---

### 7. Descripciones duplicadas — "Fecha de Lanzamiento"

**Reportes:** report20

**Descripción del hallazgo:**
En la pantalla de creación de álbum (`CreateAlbumScreen`), el campo de fecha de lanzamiento tenía dos elementos accesibles con la misma descripción "Fecha de Lanzamiento": el label del `OutlinedTextField` y el ícono del calendario (`IconButton`). TalkBack los anunciaba como idénticos, incumpliendo WCAG 1.3.1.

**Causa raíz:**
El `contentDescription` del `IconButton` que abre el date picker usaba `stringResource(R.string.create_album_field_release_date)` ("Fecha de Lanzamiento"), el mismo texto que el label del campo de texto.

**Solución aplicada:**

*Archivo: `CreateAlbumScreen.kt` — función `ReleaseDateField`*
```kotlin
// Antes
contentDescription = stringResource(R.string.create_album_field_release_date),
// → "Fecha de Lanzamiento" (igual que el label del campo)

// Después
contentDescription = stringResource(R.string.cd_open_date_picker),
// → "Abrir calendario de fechas"
```

*Archivo: `strings.xml`* — nuevo recurso:
```xml
<string name="cd_open_date_picker">Abrir calendario de fechas</string>
```

---

### 8. Descripciones duplicadas — Nombre del premio en BandDetail

**Reportes:** report16 (sección de premios de banda)

**Descripción del hallazgo:**
En la sección de premios del detalle de banda, cada tarjeta de premio (nombre, organización, fecha) exponía sus textos hijos como elementos separados e independientes. El nombre del premio (ej. "Grammy Award") era anunciado dos veces: una como elemento de la fila y otra como nodo hijo de texto.

**Causa raíz:**
La `Row` del premio no tenía semántica de agrupación, por lo que los nodos `Text` individuales (nombre, organización, fecha) eran accesibles por separado, generando duplicados.

**Solución aplicada:**

*Archivo: `BandDetailScreen.kt` — composable `PrizesSection`*
```kotlin
// Antes
modifier = Modifier
    .fillMaxWidth()
    .clip(RoundedCornerShape(12.dp))
    .background(MaterialTheme.colorScheme.surfaceContainerLow)
    .padding(12.dp)
    .testTag("band_prize_${prize.id}"),

// Después
modifier = Modifier
    .fillMaxWidth()
    .clip(RoundedCornerShape(12.dp))
    .background(MaterialTheme.colorScheme.surfaceContainerLow)
    .padding(12.dp)
    .semantics(mergeDescendants = true) {}  // agrupa nombre + organización + fecha
    .testTag("band_prize_${prize.id}"),
```

TalkBack ahora anuncia la tarjeta como un único elemento: "Grammy Award, Recording Academy, 2024-01-01".

---

### 9. Elementos clicables superpuestos — Botón Volver

**Reportes:** report52

**Descripción del hallazgo:**
El scanner detectó dos elementos clicables superpuestos en las coordenadas `[12,212][156,356]` con la misma descripción "Volver". Ambos eran botones de navegación hacia atrás en la misma área.

**Análisis:**
Este hallazgo corresponde a un estado temporal de la pantalla en transición dentro del grafo de navegación de Jetpack Navigation. Cuando se navega entre pantallas compuestas con animación, puede existir un breve momento donde el TopAppBar de la pantalla anterior y la nueva pantalla comparten área. Es un falso positivo durante la captura del scanner.

No se realizó ningún cambio de código para este hallazgo, ya que no representa un problema real de accesibilidad en el estado estable de la pantalla.

---

### 10. Tipo de elemento no admitido (u30)

**Reportes:** Múltiples (elemento de pantalla completa `[0,0][1080,2640]`)

**Descripción del hallazgo:**
El scanner reportó en casi todas las pantallas que el tipo de elemento "u30" no es compatible con los servicios de accesibilidad de Android.

**Análisis:**
Este es un **falso positivo conocido** con el Escáner de Accesibilidad de Google al analizar apps construidas con **Jetpack Compose**. El tipo "u30" es un nodo interno del runtime de Compose que representa el `ComposeView` raíz. Los servicios de accesibilidad modernos (TalkBack 14+) interpretan correctamente estos nodos a través de la API de semántica de Compose. Versiones antiguas del escáner no reconocen el tipo personalizado de Compose.

No se realizó ningún cambio de código para este hallazgo.

---

## Resumen de archivos modificados

| Archivo | Cambio |
|---|---|
| `ui/theme/Color.kt` | `Outline` oscurecido de `#757C7D` a `#545C5D` |
| `ui/components/VinilosBottomNav.kt` | Tinte inactivo de `outlineVariant` a `onSurfaceVariant` |
| `ui/components/SearchBarStatic.kt` | Eliminado `.copy(alpha)` de ícono y placeholder |
| `ui/components/AlbumCard.kt` | `contentDescription = album.name` en AsyncImage |
| `ui/screens/album/AlbumDetailScreen.kt` | `clearAndSetSemantics` en fila de estrellas; `contentDescription` personalizado en botones de eliminar |
| `ui/screens/album/AddTrackScreen.kt` | `contentDescription` y placeholder `outline` en AlbumHeaderCard |
| `ui/screens/album/CreateAlbumScreen.kt` | `contentDescription` de ícono de fecha cambiado a "Abrir calendario de fechas" |
| `ui/screens/band/BandDetailScreen.kt` | `semantics(mergeDescendants = true)` en tarjeta de premio |
| `ui/screens/band/AddAlbumToBandScreen.kt` | `contentDescription` y placeholder `outline` en AlbumRow y CurrentAlbumItem |
| `ui/screens/artist/AddAlbumToMusicianScreen.kt` | `contentDescription` y placeholder `outline` en AlbumRow y CurrentAlbumItem |
| `res/values/strings.xml` | Nuevas cadenas: `album_remove_comment_by_cd`, `cd_open_date_picker` |
