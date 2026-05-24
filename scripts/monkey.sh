#!/usr/bin/env bash
# Monkey — pruebas de exploración aleatoria sistemática para Vinilos.
#
# Recorre la app inyectando eventos pseudo-aleatorios de UI con una MATRIZ de
# semillas fijas (aleatorio pero reproducible). Modo "recolectar y continuar":
# NO se detiene ante un crash/ANR; los registra todos y reporta al final.
# Siempre sale con código 0 (no es un gate); revisa el resumen.
#
# Requiere un emulador/dispositivo ya arrancado (ver el flujo híbrido E2E en
# CLAUDE.md: `android emulator start Pixel_8`).
#
# Uso:
#   scripts/monkey.sh [--events N] [--seeds "1 42 123"] [--throttle MS]
#                     [--package ID] [--device SERIAL] [--install]
set -uo pipefail

PACKAGE="com.misw4203.vinilos"
SEEDS="1 42 123 2024 7777"
EVENTS=30000
THROTTLE=200
DEVICE=""
INSTALL=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --events)   EVENTS="$2"; shift 2;;
    --seeds)    SEEDS="$2"; shift 2;;
    --throttle) THROTTLE="$2"; shift 2;;
    --package)  PACKAGE="$2"; shift 2;;
    --device)   DEVICE="$2"; shift 2;;
    --install)  INSTALL=1; shift;;
    *) echo "Argumento desconocido: $1" >&2; exit 2;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ADB=(adb)
[[ -n "$DEVICE" ]] && ADB=(adb -s "$DEVICE")

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: 'adb' no está en PATH." >&2; exit 2
fi

# Dispositivo conectado
if [[ -z "$DEVICE" ]]; then
  online="$(adb devices | awk 'NR>1 && $2=="device" {print $1}' | head -1)"
  if [[ -z "$online" ]]; then
    echo "ERROR: no hay emulador/dispositivo conectado." >&2
    echo "Arranca la AVD primero (flujo híbrido E2E): android emulator start Pixel_8" >&2
    exit 2
  fi
fi

# App instalada (o instalar)
if [[ "$INSTALL" -eq 1 ]]; then
  echo "Instalando debug APK con Gradle..."
  ( cd "$REPO_ROOT" && ./gradlew installDebug ) || { echo "ERROR: installDebug falló." >&2; exit 2; }
elif ! "${ADB[@]}" shell pm list packages 2>/dev/null | grep -q "package:${PACKAGE}$"; then
  echo "ERROR: el paquete '$PACKAGE' no está instalado en el dispositivo." >&2
  echo "Re-ejecuta con --install, o corre: ./gradlew installDebug" >&2
  exit 2
fi

RUN_ID="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$SCRIPT_DIR/monkey-results/$RUN_ID"
mkdir -p "$OUT_DIR"
SUMMARY="$OUT_DIR/summary.txt"

{
  echo "Monkey run $RUN_ID"
  echo "package=$PACKAGE events=$EVENTS throttle=${THROTTLE}ms seeds=[$SEEDS]"
  echo "modo=recolectar-y-continuar (no gate)"
  echo "------------------------------------------------------------"
  printf "%-8s %-8s %-10s %-8s %-6s %s\n" "SEED" "EVENTS" "STATUS" "CRASHES" "ANRs" "LOG"
} | tee "$SUMMARY"

total_crashes=0
total_anrs=0
for seed in $SEEDS; do
  log="$OUT_DIR/seed-${seed}.log"
  "${ADB[@]}" shell monkey -p "$PACKAGE" -s "$seed" \
    --throttle "$THROTTLE" --pct-syskeys 0 \
    --ignore-crashes --ignore-timeouts --ignore-security-exceptions \
    --monitor-native-crashes -v -v "$EVENTS" >"$log" 2>&1

  crashes="$(grep -c "// CRASH" "$log" 2>/dev/null || true)"; crashes="${crashes:-0}"
  anrs="$(grep -c "// NOT RESPONDING" "$log" 2>/dev/null || true)"; anrs="${anrs:-0}"
  native=0; grep -qE "native crash|Monkey aborted" "$log" 2>/dev/null && native=1
  finished=0; grep -q "Monkey finished" "$log" 2>/dev/null && finished=1

  if [[ "$crashes" -gt 0 || "$anrs" -gt 0 || "$native" -eq 1 ]]; then
    status="FINDINGS"
  elif [[ "$finished" -eq 1 ]]; then
    status="CLEAN"
  else
    status="INCOMPLETE"
  fi
  total_crashes=$((total_crashes + crashes))
  total_anrs=$((total_anrs + anrs))

  printf "%-8s %-8s %-10s %-8s %-6s %s\n" \
    "$seed" "$EVENTS" "$status" "$crashes" "$anrs" "$(basename "$log")" | tee -a "$SUMMARY"
done

{
  echo "------------------------------------------------------------"
  echo "TOTAL: crashes=$total_crashes anrs=$total_anrs"
  echo "Artefactos: $OUT_DIR"
} | tee -a "$SUMMARY"

# Modo recolectar-y-continuar: no es un gate, siempre exit 0.
exit 0
