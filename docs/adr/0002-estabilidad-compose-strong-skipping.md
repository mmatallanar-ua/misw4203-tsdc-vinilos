# ADR 0002 — Estabilidad Compose con strong skipping: sin migración a colecciones inmutables

- **Estado:** Aceptada (medición + decisión basada en evidencia)
- **Fecha:** 2026-05-18
- **Contexto del curso:** MISW4203 (TSDC) — Vinilos

## Contexto

El backlog interno (ítem **B3**) plantea, como mejora **opcional y sujeta a
medición**, migrar los modelos de dominio con campos `List<...>` a
`kotlinx.collections.immutable` (`ImmutableList`) para que el compilador de
Compose los considere estables y las pantallas que los consumen sean
*skippables* (eviten recomposiciones innecesarias).

Los modelos candidatos son los `data class` de dominio con colecciones
consumidos por Compose:

- `AlbumDetail` (`tracks`, `performers`, `comments`)
- `Band` (`members`, `albums`, `prizes`)
- `Musician` (`albums`, `prizes`)
- `CollectorDetail` (`collectorAlbums`, `favoritePerformers`, `comments`)

El proyecto usa **Kotlin 2.2.10**, donde el **strong skipping** del compilador
de Compose está **activado por defecto**: incluso con parámetros de tipo
inestable, una función `@Composable` `restartable` se vuelve `skippable`
comparando esos parámetros por identidad de instancia (`===`) en lugar de por
estructura. La pregunta no es "¿son inestables los modelos?" (lo son, por
tener `List`) sino "¿son *skippables* las pantallas que los reciben?".

Para responder con datos y no a ciegas, se cableó el DSL del compilador de
Compose (el plugin `org.jetbrains.kotlin.plugin.compose` ya estaba aplicado, sin
dependencias nuevas) en `app/build.gradle.kts`:

```kotlin
composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}
```

Se generaron los reportes con `./gradlew :app:compileDebugKotlin --rerun-tasks`,
produciendo `app/build/compose_reports/app_debug-classes.txt`,
`app/build/compose_reports/app_debug-composables.txt` y
`app/build/compose_metrics/debug/app_debug-module.json` (todos bajo
`app/build/`, gitignored; por eso las líneas decisivas se citan textualmente
abajo, para preservar la evidencia en el repo).

### Evidencia: los modelos son `unstable` (por los `List`)

De `app_debug-classes.txt` (verbatim):

```
unstable class AlbumDetail {
  ...
  unstable val tracks: List<Track>
  unstable val performers: List<Performer>
  unstable val comments: List<Comment>
  <runtime stability> = Unstable
}
unstable class Band {
  ...
  unstable val members: List<MusicianSummary>
  unstable val albums: List<Album>
  unstable val prizes: List<MusicianPrize>
  <runtime stability> = Unstable
}
unstable class CollectorDetail {
  ...
  unstable val collectorAlbums: List<CollectorAlbum>
  unstable val favoritePerformers: List<Performer>
  unstable val comments: List<CollectorComment>
  <runtime stability> = Unstable
}
unstable class Musician {
  ...
  unstable val albums: List<Album>
  unstable val prizes: List<MusicianPrize>
  <runtime stability> = Unstable
}
```

Todos los demás campos de esas clases son `stable`. La inestabilidad es
**exclusivamente** atribuible a los campos `List<...>`, tal como anticipaba el
backlog.

### Evidencia: las pantallas SÍ son `restartable skippable`

De `app_debug-composables.txt` (verbatim), las funciones que consumen esos
modelos:

```
restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun AlbumDetailContent(
  unstable album: AlbumDetail
  ...
)
restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun BandBody(
  unstable band: Band
  ...
)
restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun CollectorDetailContent(
  unstable collector: CollectorDetail
  ...
)
```

Y las pantallas de entrada también: `AlbumDetailScreen`, `BandDetailScreen`,
`MusicianDetailScreen`, `CollectorDetailScreen` aparecen todas como
`restartable skippable`. Es decir: **pese a que el parámetro está marcado
`unstable`, la función es `skippable`** — el comportamiento esperado bajo strong
skipping de Kotlin 2.2.10. No hay composables `restartable` (no `skippable`)
cuya recomposición sea imputable a estos `List`.

## Decisión

**No migrar** los modelos de dominio a `kotlinx.collections.immutable`.

La evidencia medida muestra que strong skipping ya hace `skippable` a todas las
pantallas y `*Content`/`*Body` que reciben `AlbumDetail`, `Band`, `Musician` y
`CollectorDetail`. El beneficio que justificaría B3 (volver skippables esas
pantallas) **ya está obtenido por el compilador**, sin tocar una línea de
modelo.

La migración implicaría churn transversal y sin retorno medible:

- los 4–5 `data class` de dominio,
- los `.map { }` de `AlbumRepositoryImpl`, `BandRepositoryImpl`,
  `MusicianRepositoryImpl`, `CollectorRepositoryImpl`, `PrizeRepositoryImpl`
  (→ `.toImmutableList()`),
- los `*DetailEntity.toDomain()`,
- la frontera Gson de `Converters` (`TypeToken<List<...>>`),
- los `UiState`/VMs, los *fakes* de `test/`/`androidTest/`, y todos los
  `listOf(...)` → `persistentListOf(...)` de los tests.

Hacer ese cambio sin jank medido sería optimización especulativa.

**Se conserva el bloque `composeCompiler {}`** en `app/build.gradle.kts`: tiene
costo cero en runtime y hace la medición **reproducible**, de modo que esta
decisión pueda re-verificarse en cualquier momento (`./gradlew
:app:compileDebugKotlin --rerun-tasks` + leer los reportes en
`app/build/compose_*`).

## Consecuencias

**Positivas**

- Cero churn en dominio/datos/tests; se evita propagar un tipo de colección por
  las 5 capas del Clean Architecture sin beneficio medible.
- La garantía de skippability queda **documentada y reproducible**: el bloque
  `composeCompiler {}` permite regenerar el reporte y comprobar el veredicto en
  cualquier build.
- Build y suite siguen verdes; ninguna firma pública cambia.

**Negativas / deuda asumida**

- La garantía depende de que **strong skipping siga activo** (default en Kotlin
  2.2.10). Si se desactiva (p. ej. `enableStrongSkippingMode = false`) o se baja
  de versión de Kotlin/Compose, las pantallas dejarían de ser skippables y
  habría que reabrir esta decisión.
- strong skipping compara los `List` por **identidad de instancia**. Mientras
  los repositorios entreguen una instancia nueva sólo cuando los datos cambian
  (es el caso: cada `load()` crea una lista nueva y el resto del tiempo el
  `StateFlow` mantiene la misma referencia), no hay recomposición espuria. Si en
  el futuro algún flujo recreara la lista en cada emisión sin cambios reales,
  habría recomposición innecesaria; en ese escenario `ImmutableList` (estabilidad
  estructural) sí aportaría y la decisión debe reabrirse.
- **Reabrir esta decisión es obligatorio** si: (a) se desactiva strong skipping,
  o (b) un reporte futuro muestra alguno de estos `*Content`/pantallas como
  `restartable` **no** `skippable` con la recomposición imputada a un `List`, o
  (c) un perfilado (Layout Inspector / recomposition counts) evidencia jank real
  atribuible a estas listas.

## Alternativas consideradas

- **Migrar a `kotlinx.collections.immutable` (`ImmutableList`)** en los modelos
  señalados, propagando por dominio, los 5 `*RepositoryImpl`,
  `*DetailEntity.toDomain()`, la frontera `Converters`, VMs, *fakes* y tests
  (`listOf` → `persistentListOf`). Es la solución "correcta" si el reporte
  mostrara las pantallas no skippables por estos `List`. **Descartada:** esfuerzo
  M con churn transversal y **valor nulo medido** — el reporte prueba que las
  pantallas ya son skippables sin ella. Sería optimización especulativa.
- **`@Immutable`/`@Stable` en los modelos de dominio.** Descartada: contaminaría
  la capa de dominio (Kotlin puro, sin dependencias Android/Compose) con
  anotaciones de la capa de presentación, rompiendo la regla arquitectónica del
  proyecto, y resolvería un problema que el reporte demuestra inexistente.
- **No medir y "migrar por si acaso".** Descartada por definición de B3:
  la tarea es medir y decidir con evidencia, no churn a ciegas.

## Referencias

- Plan: `docs/superpowers/plans/2026-05-17-mejoras-fase-7.md` — Task 1 (B3) y
  "Decisiones de ingeniería · B3".
- Backlog interno: ítem **B3** ("medir estabilidad Compose y decidir con
  evidencia").
- `app/build.gradle.kts` → bloque `composeCompiler { metricsDestination /
  reportsDestination }` (conservado, costo cero, hace la medición
  reproducible).
- Reportes generados (bajo `app/build/`, gitignored; líneas citadas arriba):
  `app/build/compose_reports/app_debug-classes.txt`,
  `app/build/compose_reports/app_debug-composables.txt`,
  `app/build/compose_metrics/debug/app_debug-module.json`.
- Comando de regeneración: `./gradlew :app:compileDebugKotlin --rerun-tasks`.
- Precedente de formato: `docs/adr/0001-sin-concepto-de-sesion.md`.
