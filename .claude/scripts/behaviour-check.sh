#!/usr/bin/env bash
# Checks that every code marker listed in docs/BEHAVIOUR-REGISTER.md is still in its file.
# Read only. Exit 1 if a marker is missing, 2 if the register cannot be read, 0 otherwise.
#
# Usage: .claude/scripts/behaviour-check.sh
#
# A marker line in the register looks like:  - Marker: `text` in `path`
# It proves that the code is still there. It does NOT prove that the behaviour works: device tests stay manual.

set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

REG='docs/BEHAVIOUR-REGISTER.md'
if [ ! -f "$REG" ]; then
  echo "behaviour-check: $REG not found" >&2
  exit 2
fi

total=0
missing=0
while IFS= read -r line; do
  if [[ "$line" =~ ^-\ Marker:\ \`(.+)\`\ in\ \`(.+)\`$ ]]; then
    text="${BASH_REMATCH[1]}"
    file="${BASH_REMATCH[2]}"
    total=$((total + 1))
    if [ ! -f "$file" ]; then
      echo "MISSING FILE  $file  (marker: $text)"
      missing=$((missing + 1))
    elif ! grep -qF -- "$text" "$file"; then
      echo "MISSING MARKER  $text  in $file"
      missing=$((missing + 1))
    fi
  fi
done < "$REG"

if [ "$total" -eq 0 ]; then
  echo "behaviour-check: no markers found in $REG" >&2
  exit 2
fi

echo "behaviour-check: $total markers, $missing missing"
[ "$missing" -eq 0 ] && exit 0 || exit 1
