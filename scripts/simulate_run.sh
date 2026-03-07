#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_ROUTE="$SCRIPT_DIR/../tools/simulation/sample_run_loop.csv"
ADB_BIN="${ADB:-adb}"
DEVICE_SERIAL=""
ROUTE_FILE="$DEFAULT_ROUTE"
SPEED_MULTIPLIER="1.0"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--route <csv>] [--serial <device>] [--speed <multiplier>]

CSV format:
  elapsed_seconds,latitude,longitude

Examples:
  $(basename "$0")
  $(basename "$0") --route tools/simulation/sample_run_loop.csv --speed 2.0
  $(basename "$0") --serial emulator-5554
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --route)
      ROUTE_FILE="$2"
      shift 2
      ;;
    --serial)
      DEVICE_SERIAL="$2"
      shift 2
      ;;
    --speed)
      SPEED_MULTIPLIER="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ! -f "$ROUTE_FILE" ]]; then
  echo "Route file not found: $ROUTE_FILE" >&2
  exit 1
fi

ADB_CMD=("$ADB_BIN")
if [[ -n "$DEVICE_SERIAL" ]]; then
  ADB_CMD+=("-s" "$DEVICE_SERIAL")
fi

"${ADB_CMD[@]}" wait-for-device >/dev/null

if [[ "$("${ADB_CMD[@]}" shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
  echo "Connected device is not an emulator. This script uses 'adb emu geo fix'." >&2
  exit 1
fi

echo "Using route: $ROUTE_FILE"
echo "Speed multiplier: $SPEED_MULTIPLIER"

prev_elapsed=""
line_no=0

while IFS=, read -r elapsed latitude longitude; do
  line_no=$((line_no + 1))

  if [[ -z "$elapsed" || "$elapsed" == \#* ]]; then
    continue
  fi

  if [[ -n "$prev_elapsed" ]]; then
    sleep_seconds="$(awk -v prev="$prev_elapsed" -v curr="$elapsed" -v speed="$SPEED_MULTIPLIER" 'BEGIN {
      delta = curr - prev
      if (delta < 0) delta = 0
      if (speed <= 0) speed = 1
      printf "%.3f", delta / speed
    }')"
    sleep "$sleep_seconds"
  fi

  echo "[$line_no] geo fix lon=$longitude lat=$latitude t=$elapsed"
  "${ADB_CMD[@]}" emu geo fix "$longitude" "$latitude" >/dev/null
  prev_elapsed="$elapsed"
done < "$ROUTE_FILE"

echo "Route playback complete."
