<#
.SYNOPSIS
  Monkey — pruebas de exploración aleatoria sistemática para Vinilos.

.DESCRIPTION
  Recorre la app inyectando eventos pseudo-aleatorios de UI con una MATRIZ de
  semillas fijas (aleatorio pero reproducible). Modo "recolectar y continuar":
  NO se detiene ante un crash/ANR; los registra todos y reporta al final.
  Siempre termina con código 0 (no es un gate); revisa el resumen.

  Requiere un emulador/dispositivo ya arrancado (ver el flujo híbrido E2E en
  CLAUDE.md: `android emulator start Pixel_8`).

.EXAMPLE
  scripts\monkey.ps1
  scripts\monkey.ps1 -Events 1000 -Seeds "1 42 123" -Throttle 300 -Install
#>
param(
  [int]$Events = 500,
  [string]$Seeds = "1 42 123 2024 7777",
  [int]$Throttle = 200,
  [string]$Package = "com.misw4203.vinilos",
  [string]$Device = "",
  [switch]$Install
)
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
$AdbArgs   = @()
if ($Device) { $AdbArgs = @("-s", $Device) }

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
  Write-Error "'adb' no está en PATH."; exit 2
}

# Dispositivo conectado
if (-not $Device) {
  $online = (& adb devices | Select-Object -Skip 1 |
    Where-Object { $_ -match "\sdevice$" } | Select-Object -First 1)
  if (-not $online) {
    Write-Host "ERROR: no hay emulador/dispositivo conectado." -ForegroundColor Red
    Write-Host "Arranca la AVD primero (flujo híbrido E2E): android emulator start Pixel_8"
    exit 2
  }
}

# App instalada (o instalar)
if ($Install) {
  Write-Host "Instalando debug APK con Gradle..."
  & "$RepoRoot\gradlew.bat" installDebug
  if ($LASTEXITCODE -ne 0) { Write-Error "installDebug falló."; exit 2 }
} else {
  $pkgLines = & adb @AdbArgs shell pm list packages $Package 2>$null
  $installed = @($pkgLines | Where-Object { $_.Trim() -eq "package:$Package" })
  if ($installed.Count -eq 0) {
    Write-Host "ERROR: el paquete '$Package' no está instalado." -ForegroundColor Red
    Write-Host "Re-ejecuta con -Install, o corre: .\gradlew.bat installDebug"
    exit 2
  }
}

$RunId  = Get-Date -Format "yyyyMMdd-HHmmss"
$OutDir = Join-Path $ScriptDir "monkey-results\$RunId"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$Summary = Join-Path $OutDir "summary.txt"

$header = @(
  "Monkey run $RunId",
  "package=$Package events=$Events throttle=${Throttle}ms seeds=[$Seeds]",
  "modo=recolectar-y-continuar (no gate)",
  "------------------------------------------------------------",
  ("{0,-8} {1,-8} {2,-10} {3,-8} {4,-6} {5}" -f "SEED","EVENTS","STATUS","CRASHES","ANRs","LOG")
)
$header | Tee-Object -FilePath $Summary
$header | Out-Null

$totalCrashes = 0
$totalAnrs = 0
foreach ($seed in ($Seeds -split '\s+' | Where-Object { $_ })) {
  $log = Join-Path $OutDir "seed-$seed.log"
  & adb @AdbArgs shell monkey -p $Package -s $seed `
    --throttle $Throttle --pct-syskeys 0 `
    --ignore-crashes --ignore-timeouts --ignore-security-exceptions `
    --monitor-native-crashes -v -v $Events *> $log

  $content = Get-Content $log -Raw -ErrorAction SilentlyContinue
  if (-not $content) { $content = "" }
  $crashes = ([regex]::Matches($content, "// CRASH")).Count
  $anrs    = ([regex]::Matches($content, "// NOT RESPONDING")).Count
  $native  = $content -match "native crash|Monkey aborted"
  $finished = $content -match "Monkey finished"

  if ($crashes -gt 0 -or $anrs -gt 0 -or $native) { $status = "FINDINGS" }
  elseif ($finished) { $status = "CLEAN" }
  else { $status = "INCOMPLETE" }
  $totalCrashes += $crashes
  $totalAnrs += $anrs

  $row = ("{0,-8} {1,-8} {2,-10} {3,-8} {4,-6} {5}" -f `
    $seed, $Events, $status, $crashes, $anrs, (Split-Path $log -Leaf))
  $row | Tee-Object -FilePath $Summary -Append
}

$footer = @(
  "------------------------------------------------------------",
  "TOTAL: crashes=$totalCrashes anrs=$totalAnrs",
  "Artefactos: $OutDir"
)
$footer | Tee-Object -FilePath $Summary -Append

# Modo recolectar-y-continuar: no es un gate, siempre exit 0.
exit 0
