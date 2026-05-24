# Mejoras Fase 7 — Final (A4 · B3) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Cada Task = fresh implementer subagent → revisión spec → revisión calidad → follow-ups por el controller → 1 commit. Pasos con checkbox (`- [ ]`). **Última fase**; cierra el backlog de `MEJORAS.md`.

**Goal:** Cerrar el backlog. **A4** — endurecer el release: reconciliar la deriva documental de `usesCleartextTraffic` (el manifest aún lo trae pese a que CLAUDE.md/Fase F afirman lo contrario), resolver coherencia HTTP/HTTPS del host de release (fork de usuario), y activar R8/`shrinkResources` con keep rules exhaustivas para todo lo reflexivo/codegen del stack. **B3** — medir estabilidad Compose con reportes del compilador y **decidir con evidencia**: documentar "sin migración" (ADR) si strong skipping ya hace skippables las pantallas, o migrar a `kotlinx.collections.immutable` solo si el reporte muestra inestabilidad imputable a los `List`. Cada task deja build + suite verde.

## USER FORK (A4) — resolver antes de Task A4

¿`backvynils.duckdns.org:3000` sirve HTTPS o es HTTP-only?
- **Opción 1 (HTTPS, preferida):** `BASE_URL` release → `https://...`; quitar `backvynils.duckdns.org` del cleartext allowlist (queda solo dev). Coherente con la decisión documentada.
- **Opción 2 (HTTP-only):** mantener `http://`; aceptar y documentar la excepción cleartext de ese host como deuda (ADR `0003-cleartext-release.md`).
Ambas ramas terminan con el manifest **sin** `usesCleartextTraffic`.

## Decisiones de ingeniería (tomadas, no son fork)

- **A4 — Quitar `android:usesCleartextTraffic="true"` del manifest.** Hallazgo: `AndroidManifest.xml` aún lo trae pese a CLAUDE.md/Fase F. El flag del manifest es más permisivo que `network_security_config.xml` (única fuente de verdad de cleartext). Corrección de deriva, no fork.
- **A4 — `isMinifyEnabled = true` + `isShrinkResources = true` en release.** `proguardFiles(...)` ya cableado; `app/proguard-rules.pro` vacío. Keep rules dirigidas (no `-keep class ** { *; }`).
- **A4 — Verificación R8 sin firma:** no hay `keystore.properties` → release *unsigned* con warning (tolerado). Verificar con `./gradlew :app:assembleRelease` + existencia de `mapping.txt` + grep de keep rules. `gradle.properties` ya tiene `android.r8.strictFullModeForKeepRules=false`.
- **B3 — Tarea = medición + decisión, no migración a ciegas.** Kotlin 2.2.10 → strong skipping ON; sin `composeCompiler {}`. Cablear `composeCompiler { metricsDestination/reportsDestination }` (plugin Compose ya aplicado, sin nuevas deps). Commit = ADR `0002` con evidencia si las pantallas ya son skippables; migrar solo si el reporte imputa inestabilidad a los `List`.
- **B3 — Modelos candidatos:** `AlbumDetail`, `Band`, `Musician`, `CollectorDetail` (los `data class` de dominio con `List` consumidos por Compose).

## Orden / dependencias

**B3 → A4.** Independientes. B3 primero: menor riesgo, sin fork (arranca ya en paralelo a la resolución del fork con el usuario); A4 último por mayor riesgo (stripping reflexivo no auto-verificable) y por depender del fork.

**Tech Stack:** Kotlin 2.2.10 (strong skipping ON), AGP 9.2.1 (`./gradlew testDebugUnitTest`, NO `test --tests`; `:app:assembleRelease` para R8 sin firma; `assembleDebugAndroidTest` compila instrumentados), plugin `org.jetbrains.kotlin.plugin.compose` (DSL `composeCompiler {}`), Hilt+KSP, Room+`Converters` Gson/`TypeToken`, Retrofit+Gson, Coil, Compose. Branch `feature/mejoras-fase-1` local, NO push. Commits terminan en `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>`.

---

## Task 1 (B3): Medir estabilidad Compose y decidir con evidencia

**Files (build):** `app/build.gradle.kts` (bloque `composeCompiler {}`).
**Files (decisión):** `docs/adr/0002-estabilidad-compose-strong-skipping.md` (rama "sin migración"); o `gradle/libs.versions.toml` + modelos/mappers/entities/VMs/tests (rama "migrar").
**Context:** Modelos con `List` consumidos por Compose: `AlbumDetail` (tracks/performers/comments), `Band` (members/albums/prizes), `Musician` (albums/prizes), `CollectorDetail` (collectorAlbums/favoritePerformers/comments). Strong skipping ON → impacto esperado bajo. Precedente ADR: `docs/adr/0001-sin-concepto-de-sesion.md`.

- [ ] **Step 1:** `composeCompiler { metricsDestination = layout.buildDirectory.dir("compose_metrics"); reportsDestination = layout.buildDirectory.dir("compose_reports") }` en `app/build.gradle.kts`. Verificar DSL: `./gradlew :app:help`.
- [ ] **Step 2:** `./gradlew :app:assembleDebug` (o `:app:compileReleaseKotlin`); confirmar `app/build/compose_reports/*-classes.txt`/`*-composables.txt`.
- [ ] **Step 3:** Inspeccionar stability de `AlbumDetail`/`Band`/`Musician`/`CollectorDetail` y skippable/restartable de `AlbumDetailScreen`/`BandDetailScreen`/`MusicianDetailScreen`/`CollectorDetailScreen` (+ content). Registrar líneas textuales.
- [ ] **Step 4:** Decisión por evidencia. **Rama A (esperada):** pantallas skippable pese a `List` inestable → escribir `docs/adr/0002-estabilidad-compose-strong-skipping.md` (formato ADR 0001, citar líneas del reporte, conservar el bloque `composeCompiler`). **Rama B (solo si el reporte lo imputa):** `kotlinx-collections-immutable` + `List`→`ImmutableList` en los modelos señalados, propagado por dominio/5 `*RepositoryImpl`/`*Entity.toDomain`/frontera `Converters` (sin cambiar `@TypeConverter`)/VMs/fakes/tests.
- [ ] **Step 5 (verify):** Rama A: ADR cita evidencia; `testDebugUnitTest` verde; `assembleDebugAndroidTest` compila. Rama B: además build verde + reporte regenerado mostrando stable/skippable.
- [ ] **Step: Commit** — Rama A: `docs(adr): registrar estabilidad Compose con strong skipping, sin migración (B3)`. Rama B: `perf(domain): listas inmutables en modelos consumidos por Compose (B3)`.

---

## Task 2 (A4): Endurecer el release — cleartext coherente + R8/shrink con keep rules

> **PRE-REQUISITO:** A4-FORK resuelto con el usuario. El implementer recibe la opción (1 o 2).

**Files:** `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `app/src/main/res/xml/network_security_config.xml`, `app/proguard-rules.pro`, `CLAUDE.md` (sección Network security), `docs/adr/0003-cleartext-release.md` (solo Opción 2). Skill: `r8-analyzer`.
**Context:** manifest aún `usesCleartextTraffic="true"`; config ya tiene `backvynils.duckdns.org`; release `BASE_URL`=`http://backvynils.duckdns.org:3000/`; `isMinifyEnabled=false`; `proguard-rules.pro` vacío; sin keystore (release unsigned). Superficies reflexivas: Retrofit (proxy `VinilosApiService` + Gson DTOs `@SerializedName`), Gson en `Converters` (`TypeToken<List<...>>` de dominio), enum `PerformerKind` dentro de `List<Performer>`, Room/Hilt (KSP, codegen), Coil, coroutines, Compose. R8 stripping reflexivo NO lo detecta `assembleRelease`.

- [ ] **Step 1 (ambas ramas):** Quitar `android:usesCleartextTraffic="true"` de `<application>` en `AndroidManifest.xml` (conservar `android:networkSecurityConfig`).
- [ ] **Step 2 (fork):** **Opción 1:** `BASE_URL` release → `https://backvynils.duckdns.org:<puerto-tls>/`; quitar `<domain>backvynils.duckdns.org</domain>` del config. **Opción 2:** mantener `http://...`; conservar el dominio; crear `docs/adr/0003-cleartext-release.md` (formato ADR 0001: deuda aceptada, backend HTTP-only del curso, reabrir si gana TLS).
- [ ] **Step 3:** Reconciliar `CLAUDE.md` sección "Network security" con el estado real post-cambio (manifest sin flag; cleartext solo dev en Opción 1, o dev + host de release con ref al ADR 0003 en Opción 2).
- [ ] **Step 4:** `isMinifyEnabled = true` + `isShrinkResources = true` en `release {}` (mantener signingConfig/proguardFiles).
- [ ] **Step 5:** Autorar `app/proguard-rules.pro` dirigido: `-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses`; keep de campos `data.remote.dto.**` y `domain.model.**` (Converters serializa `List<...>` de dominio); `@SerializedName`; reglas Gson `TypeToken`/`TypeAdapter`; `-keepclassmembers enum ...PerformerKind { *; }`; reglas Retrofit/OkHttp 2.11 + `-keep,allowobfuscation interface ...VinilosApiService`; `-dontwarn` para okhttp/okio/coroutines/conscrypt según R8; NO duplicar consumer rules de Room/Hilt/Compose. Pasar por `r8-analyzer` para podar redundantes/amplias.
- [ ] **Step 6 (verify):** `./gradlew :app:assembleRelease` SUCCESS (unsigned tolerado; warning de firma no es fallo). Verificar `app/build/outputs/mapping/release/mapping.txt` + `seeds.txt`/`usage.txt` + APK. Grep keep rules clave. `git grep "usesCleartextTraffic" -- app/src/main/AndroidManifest.xml` vacío. `git grep "backvynils" -- network_security_config.xml app/build.gradle.kts` coherente con el fork.
- [ ] **Step 7 (DONE_WITH_CONCERNS):** R8 stripping reflexivo no detectable por `assembleRelease`. Mitigación: keep rules + `r8-analyzer` + mapping-file. Recomendar smoke manual contra el APK release shrinkado vía el flujo híbrido E2E de CLAUDE.md (AVD API 33/34, instalar APK release, recorrer Álbumes→AlbumDetail [Gson+Room Converters], Artist/Collector detail, un POST). NO gateable en CI (necesita backend real).
- [ ] **Step: Commit** — Opción 1: `build(release): https + R8/shrink con keep rules dirigidas (A4)`. Opción 2: `build(release): cleartext documentado + R8/shrink con keep rules dirigidas (A4)`.

---

## Cierre de Fase 7

- [ ] **Step final:** Sección `## Fase R — Backlog Fase 7 (A4 · B3)` en `MEJORAS.md` (gitignored, NO commitear): commits, decisión B3 (rama A con cita del reporte, o B con capas), resolución del A4-FORK, hallazgo de deriva reconciliado, artefactos R8 (mapping.txt), ADRs nuevos (`0002` siempre si B3 rama A; `0003` si A4 Opción 2), greps de erradicación, backlog **vacío — MEJORAS.md cerrado**. Commitear el plan doc (`docs(plan)` de cierre). `feature/mejoras-fase-1` local, sin push.

## Self-review

- Deriva documental (`usesCleartextTraffic`) reconciliada en ambas ramas (A4 Steps 1+3). ✓
- A4-FORK = decisión de usuario, no del planner; bloqueante de Task A4. ✓
- B3 = medición+decisión con evidencia, no churn ciego; capas de la rama B enumeradas. ✓
- Orden B3→A4 justificado (B3 sin riesgo/sin fork arranca ya; A4 último por R8+fork). ✓
- Riesgo R8 cubierto: keep rules dirigidas + `r8-analyzer` + mapping-file + DONE_WITH_CONCERNS con smoke manual prescrito. ✓
- Comandos AGP 9.2.1 correctos. ✓
