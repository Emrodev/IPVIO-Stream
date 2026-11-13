#!/bin/sh
# Shared helpers for the TV test scripts. Source this file; needs DEVICE (adb serial, e.g. 192.168.x.y:5555 or emulator-5554).
# Screen coordinates are for a 1920x1080 TV with the sidebar on the left (default layout).
: "${DEVICE:?set DEVICE to the adb serial of the TV or emulator}"
: "${OUT:=./tv-test-out}"
: "${PKG:=com.ipvio.stream}"
mkdir -p "$OUT"

pause() { perl -e "select(undef,undef,undef,$1)"; }

# key NAME... : send key events (DPAD_DOWN, BACK, CHANNEL_UP ...); "w" waits one second
key() { for k in "$@"; do if [ "$k" = w ]; then pause 1; else adb -s "$DEVICE" shell input keyevent "KEYCODE_$k"; pause 0.35; fi; done; }

# repeat_key N NAME [DELAY] : press a key N times
repeat_key() { i=0; while [ $i -lt "$1" ]; do adb -s "$DEVICE" shell input keyevent "KEYCODE_$2"; pause "${3:-0.1}"; i=$((i+1)); done; }

tap() { adb -s "$DEVICE" shell input tap "$1" "$2"; pause 1; }

# shot NAME : screenshot to $OUT/NAME.png
shot() { pause 0.8; adb -s "$DEVICE" exec-out screencap -p > "$OUT/$1.png"; echo "$OUT/$1.png"; }

# ui_texts : visible texts and descriptions from uiautomator (empty while a video plays, the dump then fails)
ui_texts() { adb -s "$DEVICE" exec-out uiautomator dump /dev/tty 2>/dev/null | tr '>' '\n' | grep -o 'text="[^"]*"' | grep -v 'text=""'; }

# ensure_main : press Back until a screen with the sidebar is shown (leaves the player and detail pages).
# Every case starts with this so one case's end screen does not break the next case.
ensure_main() {
  n=0
  while [ $n -lt 6 ]; do
    t=$(ui_texts)
    # empty dump = player; "Trailer" = detail page. (The full results view keeps the sidebar; Back does not leave it
    # after a tap, so the next case leaves it through the sidebar.)
    if [ -z "$t" ] || printf '%s' "$t" | grep -q 'text="Trailer"'; then key BACK w; n=$((n+1)); else return 0; fi
  done
  echo "ensure_main: still on the player or a detail page after 6x Back" >&2
}

# Sidebar and header targets
go_home()     { tap 54 154; pause 2; }
go_live()     { tap 54 230; pause 3; }
go_movies()   { tap 54 306; pause 3; }
go_series()   { tap 54 384; pause 3; }
go_settings() { tap 54 768; pause 3; }
# open_search_in SECTION : opens search from home, movies or series (Home first, then the section, then the search pill in
# the header) and checks that the search keyboard is shown. A tap that lands while the page is still changing can open the
# focused card instead; then it goes back and tries again (2 retries).
open_search_in() {
  for attempt in 1 2 3; do
    go_home; [ "$1" = home ] || "go_$1"; tap 340 46; pause 3
    ui_texts | grep -q 'text="Space"' && return 0
    echo "open_search_in $1: keyboard not shown (attempt $attempt)" >&2; ensure_main
  done
  return 1
}
open_categories() { tap 650 46; pause 3; }  # "Categories" pill on Movies / Series

# clear_search : presses "Clear" on the search keyboard (the search keeps its text between visits)
clear_search() { adb -s "$DEVICE" shell input tap 770 1024; pause 1; }

# type_search WORD : types lowercase letters on the app's on-screen search keyboard
type_search() {
  w=$1; i=0
  while [ $i -lt ${#w} ]; do
    ch=$(printf '%s' "$w" | cut -c$((i+1)))
    case $ch in
      q) x=108;y=391;; w) x=141;y=391;; e) x=174;y=391;; r) x=207;y=391;; t) x=240;y=391;; y) x=273;y=391;; u) x=306;y=391;; i) x=339;y=391;; o) x=372;y=391;; p) x=405;y=391;;
      a) x=127;y=431;; s) x=159;y=431;; d) x=192;y=431;; f) x=225;y=431;; g) x=258;y=431;; h) x=290;y=431;; j) x=322;y=431;; k) x=355;y=431;; l) x=387;y=431;;
      z) x=156;y=471;; x) x=190;y=471;; c) x=223;y=471;; v) x=257;y=471;; b) x=290;y=471;; n) x=323;y=471;; m) x=357;y=471;;
      ' ') x=132;y=512;;
      *) echo "type_search: unsupported character '$ch'" >&2; i=$((i+1)); continue;;
    esac
    adb -s "$DEVICE" shell input tap $((x*2)) $((y*2))
    i=$((i+1))
  done
}

start_app() { adb -s "$DEVICE" shell input keyevent KEYCODE_WAKEUP; adb -s "$DEVICE" shell am start -n "$PKG/.MainActivity" >/dev/null; pause "${1:-12}"; }

# health_check LABEL : appends crash / ANR / rate-limit / focus-warning counts and memory to $OUT/health.txt
health_check() {
  log=$(adb -s "$DEVICE" logcat -d)
  crash=$(adb -s "$DEVICE" logcat -d -b crash | grep -c "$PKG")
  anr=$(printf '%s\n' "$log" | grep -c "ANR in $PKG")
  oom=$(printf '%s\n' "$log" | grep -c "OutOfMemoryError")
  r429=$(printf '%s\n' "$log" | grep -c "HTTP 429")
  focus=$(printf '%s\n' "$log" | grep -c "FocusRequester is not initialized")
  pss=$(adb -s "$DEVICE" shell dumpsys meminfo "$PKG" | grep -m1 "TOTAL PSS" | awk '{print $3}')
  echo "$1: crash=$crash anr=$anr oom=$oom http429=$r429 focusWarnings=$focus pssKB=$pss" | tee -a "$OUT/health.txt"
}
