#!/usr/bin/env bash
# Optional device checks; an emulator is not required to build the Android APK.
set -euo pipefail
NURAYA_PROJECT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$NURAYA_PROJECT/scripts/env.sh"
NURAYA_API="${NURAYA_API:-28}"
if (( NURAYA_API >= 30 )); then NURAYA_DEFAULT_ABI=x86_64; else NURAYA_DEFAULT_ABI=x86; fi
NURAYA_ABI="${NURAYA_ABI:-$NURAYA_DEFAULT_ABI}"
NURAYA_AVD="${NURAYA_AVD:-nuraya_api$NURAYA_API}"
NURAYA_PORT="${NURAYA_PORT:-5556}"
NURAYA_SERIAL="emulator-$NURAYA_PORT"
NURAYA_LOG="$NURAYA_TOOLS/emulator-$NURAYA_API.log"
NURAYA_IMAGE="system-images;android-$NURAYA_API;default;$NURAYA_ABI"
if ! command -v emulator >/dev/null || [[ ! -f "$ANDROID_HOME/system-images/android-$NURAYA_API/default/$NURAYA_ABI/package.xml" ]]; then
    sdkmanager 'emulator' "$NURAYA_IMAGE"
fi
if [[ ! -f "$ANDROID_AVD_HOME/$NURAYA_AVD.ini" ]]; then
    printf 'no\n' | avdmanager create avd --name "$NURAYA_AVD" --package "$NURAYA_IMAGE" --device pixel_2
    python3 - "$ANDROID_AVD_HOME/$NURAYA_AVD.avd/config.ini" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
values = {'hw.lcd.width':'480','hw.lcd.height':'800','hw.lcd.density':'200',
          'hw.ramSize':'2048','hw.cpu.ncore':'2','hw.gpu.enabled':'yes',
          'hw.gpu.mode':'swiftshader_indirect','showDeviceFrame':'no',
          'skin.dynamic':'yes','skin.name':'480x800'}
lines = [s for s in p.read_text().splitlines() if s.split('=',1)[0].strip() not in values]
p.write_text('\n'.join(lines + [k+'='+v for k,v in values.items()])+'\n')
PY
fi
adb start-server
if ! adb devices | awk 'NR > 1 { print $1 }' | rg -qx "$NURAYA_SERIAL"; then
    NURAYA_ACCEL=off
    if [[ -r /dev/kvm && -w /dev/kvm ]]; then NURAYA_ACCEL=on; fi
    nohup emulator -avd "$NURAYA_AVD" -accel "$NURAYA_ACCEL" \
        -no-window -no-audio -gpu swiftshader_indirect -feature -Vulkan \
        -no-snapshot -no-boot-anim -memory 2048 -cores 2 -port "$NURAYA_PORT" > "$NURAYA_LOG" 2>&1 &
    printf '%s\n' "$!" > "$NURAYA_TOOLS/emulator-$NURAYA_API.pid"
fi
printf 'Waiting for Android boot. Software emulation can take several minutes. Log: %s\n' "$NURAYA_LOG"
NURAYA_DEADLINE=$((SECONDS + 660))
until [[ "$(timeout 10 adb -s "$NURAYA_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do
    if (( SECONDS >= NURAYA_DEADLINE )); then
        printf 'Android did not complete boot. Inspect %s; no device-test pass is implied.\n' "$NURAYA_LOG" >&2
        exit 1
    fi
    sleep 3
done
adb -s "$NURAYA_SERIAL" shell settings put global window_animation_scale 0
adb -s "$NURAYA_SERIAL" shell settings put global transition_animation_scale 0
adb -s "$NURAYA_SERIAL" shell settings put global animator_duration_scale 0
adb -s "$NURAYA_SERIAL" shell input keyevent 82
printf 'Android boot completed. Test with ANDROID_SERIAL=%s bash scripts/gradle.sh connectedDebugAndroidTest\n' "$NURAYA_SERIAL"
