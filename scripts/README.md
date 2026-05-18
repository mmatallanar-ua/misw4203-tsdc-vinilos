# Monkey — exploración aleatoria sistemática

Inyecta eventos pseudo-aleatorios de UI sobre la app instalada para detectar
crashes/ANRs navegando estados que los tests escritos a mano no cubren.

- **Aleatorio pero reproducible**: matriz de semillas fijas (`-s <seed>`). Misma
  semilla + mismo nº de eventos + mismo throttle ⇒ misma secuencia de eventos.
- **Recolectar y continuar**: NO se detiene ante un crash/ANR (`--ignore-*`);
  los registra todos y reporta al final. No es un gate (siempre `exit 0`).
- **Acotado a la app**: `--pct-syskeys 0` evita salir de la app.

## Requisito

Un emulador/dispositivo **ya arrancado** y la app instalada. Usa el flujo
híbrido E2E (ver `CLAUDE.md`):

```bash
android emulator start Pixel_8
adb shell input keyevent KEYCODE_WAKEUP && adb shell svc power stayon true
```

## Uso

```bash
# bash / Linux / macOS / Git-Bash
scripts/monkey.sh                                  # defaults
scripts/monkey.sh --events 1000 --seeds "1 42" --throttle 300
scripts/monkey.sh --install                        # build+installDebug primero
```

```powershell
# PowerShell (Windows)
scripts\monkey.ps1
scripts\monkey.ps1 -Events 1000 -Seeds "1 42" -Throttle 300 -Install
```

```bash
# Gradle (multiplataforma; detecta el SO y llama al script correcto)
./gradlew monkeyTest
./gradlew monkeyTest -PmonkeyEvents=1000 -PmonkeySeeds="1 42 123" -PmonkeyThrottle=300
```

## Parámetros

| Script (`--`) | PS (`-`) | Gradle (`-P`) | Default | Significado |
|---|---|---|---|---|
| `--events` | `-Events` | `monkeyEvents` | `500` | Nº de eventos por semilla |
| `--seeds` | `-Seeds` | `monkeySeeds` | `1 42 123 2024 7777` | Matriz de semillas |
| `--throttle` | `-Throttle` | `monkeyThrottle` | `200` | ms entre eventos (deja asentar Compose) |
| `--package` | `-Package` | — | `com.misw4203.vinilos` | applicationId |
| `--device` | `-Device` | — | (auto) | serial adb si hay varios |
| `--install` | `-Install` | — | off | `./gradlew installDebug` antes |

Duración ≈ `eventos × throttle` por semilla. Defaults: 500×200 ms ≈ 100 s/semilla
→ ~8 min para las 5 semillas.

## Resultados

`scripts/monkey-results/<yyyyMMdd-HHmmss>/` (gitignored):

- `seed-<n>.log` — salida verbosa de Monkey por semilla.
- `summary.txt` — tabla `SEED | EVENTS | STATUS | CRASHES | ANRs | LOG` y totales.

`STATUS`: `CLEAN` (terminó sin hallazgos) · `FINDINGS` (crash/ANR/native) ·
`INCOMPLETE` (no llegó a "Monkey finished").
