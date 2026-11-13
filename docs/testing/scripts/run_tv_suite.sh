#!/bin/sh
# Runs the IPVIO TV regression suite on one device and writes $OUT/report.md (screenshots + expected result per case).
# Usage: DEVICE=<adb serial> [OUT=dir] [APK=path] [CASES="T01 T02 ..."] [DEEP_ROWS=30] [SEARCH_WORD=batman] sh run_tv_suite.sh
# Works with a real playlist (Stalker, Xtream, M3U) or the mock server (see mock_xtream.py). The playlist must already be added.
# Keep portal load low: the suite pauses between scroll steps; do not run it on two devices with the same MAC at once.
set -u
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/lib.sh"
: "${CASES:=T01 T02 T03 T04 T05 T06 T07 T08 T09 T10 T11 T12}"
: "${DEEP_ROWS:=30}"
: "${SEARCH_WORD:=batman}"
: "${SERIES_WORD:=mob}"
: "${HOME_WORD:=news}"
R="$OUT/report.md"

case_head() { printf '\n## %s %s\n\n**Expected:** %s\n\n' "$1" "$2" "$3" >> "$R"; echo "== $1 $2"; }
img() { printf -- '- %s: ![%s](%s.png)\n' "$2" "$1" "$1" >> "$R"; shot "$1" >/dev/null; }
result_line() { printf '\n**Result:** [ ] pass  [ ] fail  - notes:\n' >> "$R"; }

T01() { case_head T01 "Install and cold start" "App starts on Home, playlist from the cache (no new download), Continue watching and the Movies/Series counts are there; no crash."
  if [ -n "${APK:-}" ]; then adb -s "$DEVICE" install -r "$APK" | tail -1 >> "$R"; fi
  adb -s "$DEVICE" logcat -c; adb -s "$DEVICE" shell am force-stop "$PKG"; start_app 20; img T01-home "Home after start"; result_line; }

T02() { case_head T02 "Live TV opens and keeps focus deep in the list" "Live TV opens on Home. After $DEEP_ROWS rows down, Right focuses the first channel of that category; Down 3, Left returns to the category, Right returns to the 4th channel."
  go_live; img T02-a "Live TV start"
  repeat_key "$DEEP_ROWS" DPAD_DOWN; pause 2; img T02-b "category $DEEP_ROWS rows down"
  key DPAD_RIGHT w; img T02-c "after Right (first channel focused)"
  key DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_LEFT w; img T02-d "after Down x3, Left (category focused)"
  key DPAD_RIGHT w; img T02-e "after Right (4th channel focused again)"; result_line; }

T03() { case_head T03 "Live playback and zapping" "The focused channel plays; Channel Up switches to the next channel; Back returns to Live TV."
  key DPAD_CENTER; pause 9; img T03-a "channel playing"
  key CHANNEL_UP; pause 8; img T03-b "after Channel Up"
  key BACK w w; img T03-c "after Back"; result_line; }

T04() { case_head T04 "Movies categories: paging and Left/Right" "Categories show posters; the grid scrolls 20 rows (past 40 titles) and keeps loading; Left goes to the selected category, Right returns to the same title."
  go_movies; img T04-a "Movies tab (search pill says 'Search movies …')"
  open_categories; key DPAD_RIGHT w DPAD_RIGHT w DPAD_RIGHT w; img T04-b "first title focused"
  repeat_key 20 DPAD_DOWN 0.6; pause 3; img T04-c "20 rows down"
  key DPAD_LEFT w; img T04-d "after Left (category focused)"
  key DPAD_RIGHT w; img T04-e "after Right (same title)"; result_line; }

T05() { case_head T05 "Detail page, movie playback, resume, Back" "Detail page shows metadata; the movie plays; skipping moves forward; after Back the detail page offers Resume; Back again returns to the same title in the grid."
  key DPAD_CENTER w w w; img T05-a "detail page"
  key DPAD_CENTER; pause 15; img T05-b "movie playing"
  key DPAD_RIGHT DPAD_RIGHT; pause 5; key DPAD_CENTER; img T05-c "after skipping (position moved)"
  key BACK w w; key BACK w w; img T05-d "detail page with Resume"
  key BACK w w; img T05-e "back in the grid on the same title"; result_line; }

T06() { case_head T06 "Full category view keeps loading" "OK on a category opens the full view; scrolling near the end raises the title count (on Stalker: more titles load)."
  key DPAD_LEFT DPAD_CENTER w w w; img T06-a "full view, first title"
  repeat_key 230 DPAD_RIGHT 0.05; pause 8; img T06-b "near the end (count should have grown)"; result_line; }

T07() { case_head T07 "Series: episodes and playback" "A series opens with its seasons and episodes; an episode plays."
  go_series; img T07-a "Series tab (search pill says 'Search series …')"
  key DPAD_DOWN w DPAD_DOWN w DPAD_CENTER w w w w w; img T07-b "series detail with seasons and episodes (Play focused)"
  key DPAD_CENTER; pause 15; img T07-c "first episode playing"
  key BACK w w BACK w w; result_line; }

T08() { case_head T08 "Search from Movies" "Search opens with the Movies filter and the hint 'Search movies …'. '$SEARCH_WORD' lists only movies; the same title in two versions shows 'year · language' (or the category) under each card; 'All N' opens all hits."
  open_search_in movies; img T08-a "search opened from Movies"
  clear_search; type_search "$SEARCH_WORD"; pause 12; img T08-b "results for '$SEARCH_WORD'"
  tap 994 386; pause 4; img T08-c "All results view"
  key BACK w BACK w; result_line; }

T09() { case_head T09 "Search from Series" "Search opens with the Series filter; Recently watched shows only series; '$SERIES_WORD' lists only series."
  open_search_in series; img T09-a "search opened from Series"
  clear_search; type_search "$SERIES_WORD"; pause 10; img T09-b "results for '$SERIES_WORD'"
  key BACK w BACK w; result_line; }

T10() { case_head T10 "Search from Home (all areas)" "Search from Home starts on 'All'; hits come in Movies, Series and Live TV rows."
  open_search_in home; img T10-a "search opened from Home"
  clear_search; type_search "$HOME_WORD"; pause 10; img T10-b "results for '$HOME_WORD'"
  key BACK w BACK w; result_line; }

T11() { case_head T11 "Settings in English" "Settings and Settings > Playlists show no German text (the content-mode button reads 'Live TV + movies & series' in English)."
  go_settings; img T11-a "Settings"
  tap 456 658; pause 3; img T11-b "Settings > Playlists"
  key BACK w; result_line; }

T12() { case_head T12 "Health" "health.txt shows crash=0 anr=0 oom=0 http429=0 focusWarnings=0 for the whole run."
  health_check "after suite"; printf '\n```\n%s\n```\n' "$(tail -1 "$OUT/health.txt")" >> "$R"; result_line; }

{
  echo "# IPVIO TV test run"
  echo
  echo "- Date: $(date '+%Y-%m-%d %H:%M')"
  echo "- Device: $(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r') (Android $(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r'))"
  echo "- App: $(adb -s "$DEVICE" shell dumpsys package "$PKG" | grep -m1 versionName | tr -d ' \r')"
  echo "- Build: $(git -C "$DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo "- Cases: $CASES"
} > "$R"
for c in $CASES; do [ "$c" = T01 ] || [ "$c" = T12 ] || ensure_main; "$c"; done
echo "Report: $R"
