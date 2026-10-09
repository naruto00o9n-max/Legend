#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_WORK="${COOKIES_WORK:-$PWD/.work}"
mkdir -p "$TASK_WORK"
python3 scripts/download_reference.py "$TASK_WORK/reference.apk"
if [[ ! -f "$TASK_WORK/apktool-3.0.3.jar" ]]; then
  curl --fail --location --retry 3 https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar --output "$TASK_WORK/apktool-3.0.3.jar"
fi
if [[ ! -x "$TASK_WORK/jadx/bin/jadx" ]]; then
  curl --fail --location --retry 3 https://github.com/skylot/jadx/releases/download/v1.5.6/jadx-1.5.6.zip --output "$TASK_WORK/jadx.zip"
  unzip -q -o "$TASK_WORK/jadx.zip" -d "$TASK_WORK/jadx"
  chmod +x "$TASK_WORK/jadx/bin/jadx"
fi
java -Xmx3g -jar "$TASK_WORK/apktool-3.0.3.jar" d --force "$TASK_WORK/reference.apk" -o "$TASK_WORK/pristine"
# JADX may report non-zero status for unstructured methods. Smali still retains
# their complete instructions; record the failure instead of claiming Java parity.
TASK_JADX_STATUS=0
"$TASK_WORK/jadx/bin/jadx" --show-bad-code -d "$TASK_WORK/java" "$TASK_WORK/reference.apk" > "$TASK_WORK/jadx.log" 2>&1 || TASK_JADX_STATUS=$?
printf 'JADX exit code: %s (see .work/jadx.log)\n' "$TASK_JADX_STATUS"
python3 scripts/inventory_reference.py "$TASK_WORK/pristine" "$TASK_WORK/java/sources/com/oneguystudio/ytyper" docs
