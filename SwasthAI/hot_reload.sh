#!/usr/bin/env bash
# Hot reload for SwasthAI.
#
# Watches Kotlin/Compose sources in app/src; on every save it incrementally
# rebuilds, reinstalls, and relaunches the app on the connected device.
#
# Usage:
#   ./hot_reload.sh                   # deploy to the first connected device
#   ./hot_reload.sh emulator-5554     # deploy to a specific device
set -euo pipefail
cd "$(dirname "$0")"

DEVICE=""
if [[ $# -gt 0 ]]; then DEVICE="$1"; fi

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi

echo "▸ SwasthAI hot reload starting… (Ctrl+C to stop)"
if [[ -n "$DEVICE" ]]; then
  echo "▸ Device: $DEVICE"
  exec ./gradlew :app:hotReloadDebug --continuous -Pdevice="$DEVICE"
else
  echo "▸ No device detected — deploying to the default adb target."
  exec ./gradlew :app:hotReloadDebug --continuous
fi