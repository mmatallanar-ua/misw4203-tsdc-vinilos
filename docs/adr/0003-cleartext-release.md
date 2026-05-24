# ADR 0003 — Cleartext permitido en release para el backend del curso

- **Estado:** Aceptada (deuda)
- **Fecha:** 2026-05-18
- **Contexto del curso:** MISW4203 (TSDC) — Vinilos

## Contexto

El build de release apunta al backend del curso
`http://backvynils.duckdns.org:3000/` (`BASE_URL` en `release {}` de
`app/build.gradle.kts`). Endurecer el release exige decidir si ese tráfico
puede viajar cifrado (HTTPS) o si hay que aceptar cleartext.

Se sondeó el host antes de decidir:

- No hay TLS en el puerto `:3000` ni en `:443` (ningún listener TLS responde).
- Sobre HTTP plano el servidor responde (devuelve `404` en `/`), es decir el
  backend **está arriba pero solo sobre cleartext**.

Conclusión del sondeo: el backend del curso es **HTTP-only**; no existe un
endpoint HTTPS al que migrar `BASE_URL`. En paralelo, el manifest aún traía
`android:usesCleartextTraffic="true"`, un flag global más permisivo que
`network_security_config.xml` (la fuente de verdad real de cleartext).

## Decisión

1. **Eliminar** `android:usesCleartextTraffic="true"` del `<application>` en
   `AndroidManifest.xml`. El manifest **no** lleva ningún flag global de
   cleartext; la única fuente de verdad es `network_security_config.xml`.
2. **Permitir cleartext únicamente** para `backvynils.duckdns.org` (host de
   release) más los hosts de desarrollo (`10.0.2.2`, `localhost`,
   `127.0.0.1`), vía la `domain-config` de `network_security_config.xml`.
3. **Mantener** `BASE_URL` de release en `http://backvynils.duckdns.org:3000/`
   (Opción 2 del fork): no se migra a `https` porque el backend no tiene TLS.
4. Aceptar esta excepción como **deuda documentada**, acotada al alcance del
   curso.

## Consecuencias

**Positivas**
- El manifest deja de exponer un flag global de cleartext: la superficie de
  cleartext queda explícita y acotada a una allowlist de hosts conocida.
- El release sigue funcionando contra el único backend disponible del curso
  sin introducir un proxy TLS ni infraestructura adicional fuera de alcance.
- Coherente con la nota de seguridad de `CLAUDE.md`: cleartext sólo en hosts
  enumerados, nunca global.

**Negativas / deuda asumida**
- El tráfico de release hacia `backvynils.duckdns.org` viaja **sin cifrar**
  (sin confidencialidad ni integridad en tránsito). Aceptable estrictamente
  dentro del alcance del curso, donde no hay datos sensibles ni identidad de
  usuario (ver ADR 0001).
- Reabrir esta decisión es obligatorio si el backend gana TLS: en ese caso
  cambiar `BASE_URL` de release a `https://...` y **quitar**
  `backvynils.duckdns.org` de la allowlist de `network_security_config.xml`
  (quedaría solo dev).

## Alternativas consideradas

- **Opción 1 — `BASE_URL` release a `https://` y sacar el host del cleartext
  allowlist:** preferida y coherente con la postura de seguridad documentada.
  Descartada porque el sondeo confirmó que el backend del curso **no expone
  TLS** (ni en `:3000` ni en `:443`); migrar a `https` rompería el release sin
  un endpoint cifrado real al que apuntar.

## Referencias

- `docs/superpowers/plans/2026-05-17-mejoras-fase-7.md` → Task 2 (A4),
  USER FORK A4, Opción 2.
- Backlog interno: ítem **A4** ("Endurecer el release: cleartext coherente +
  R8/shrink").
- `app/src/main/res/xml/network_security_config.xml` → `domain-config` con
  `backvynils.duckdns.org` + hosts dev.
- `app/build.gradle.kts` → `release { buildConfigField BASE_URL }`.
- `CLAUDE.md` → sección "Network security".
