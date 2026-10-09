#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_WORK="${COOKIES_WORK:-$PWD/.work}"
mkdir -p "$TASK_WORK" build
python3 scripts/download_reference.py "$TASK_WORK/reference.apk"
TASK_TOOL="$TASK_WORK/apktool-3.0.3.jar"
if [[ ! -f "$TASK_TOOL" ]]; then
  curl --fail --location --retry 3 https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar --output "$TASK_TOOL"
fi
java -Xmx3g -jar "$TASK_TOOL" d --force "$TASK_WORK/reference.apk" -o "$TASK_WORK/decoded"
python3 scripts/brand_android.py "$TASK_WORK/decoded"
java -Xmx3g -jar "$TASK_TOOL" b "$TASK_WORK/decoded" -o build/cookies-reference-unsigned.apk
cp "$TASK_WORK/decoded/cookies-brand-changes.json" build/
python3 scripts/verify_android_reference.py "$TASK_WORK/reference.apk" build/cookies-reference-unsigned.apk build/android-validation.json
