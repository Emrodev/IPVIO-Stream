#!/bin/sh
# Prepares an Android TV emulator for run_tv_suite.sh with the mock server: starts mock_xtream.py, installs the APK fresh,
# and sends the mock playlist through the app's phone page (same as adding it from a phone).
# Usage: DEVICE=emulator-5554 APK=app/build/outputs/apk/debug/app-debug.apk [VIDEO=test.mkv] [CODE=1234] sh setup_emulator.sh
# CODE = the 4-digit code on the add-playlist screen; when empty the script reads it from the screen (uiautomator).
set -u
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/lib.sh"
: "${APK:?set APK to the debug APK}"
: "${PORT:=8098}"
VIDEO_ARG=""; [ -n "${VIDEO:-}" ] && VIDEO_ARG="--video $VIDEO"
nohup python3 "$DIR/mock_xtream.py" $VIDEO_ARG --port "$PORT" > "$OUT/mock.log" 2>&1 &
echo $! > "$OUT/mock.pid"; pause 1
curl -s "http://127.0.0.1:$PORT/get.php?username=demo&password=demo" | grep -c '^#EXTINF' | sed 's/^/mock entries: /'
adb -s "$DEVICE" uninstall "$PKG" >/dev/null 2>&1
adb -s "$DEVICE" install "$APK" | tail -1
start_app 10
shot setup-add-playlist >/dev/null
if [ -z "${CODE:-}" ]; then
  adb -s "$DEVICE" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  CODE=$(adb -s "$DEVICE" shell cat /sdcard/ui.xml | grep -oE 'text="[0-9]{4}"' | head -1 | grep -oE '[0-9]{4}')
fi
[ -n "$CODE" ] || { echo "No code found: look at $OUT/setup-add-playlist.png and rerun with CODE=xxxx"; exit 1; }
adb -s "$DEVICE" forward tcp:18765 tcp:8765 >/dev/null
curl -s -X POST http://127.0.0.1:18765/send --data-urlencode "code=$CODE" \
  --data-urlencode "url=http://10.0.2.2:$PORT/get.php?username=demo&password=demo&type=m3u_plus" | sed 's/<[^>]*>/ /g' | tr -s ' ' | head -2
pause 15
shot setup-loaded
echo "Mock playlist loaded (check $OUT/setup-loaded.png). Stop the mock later with: kill \$(cat $OUT/mock.pid)"
