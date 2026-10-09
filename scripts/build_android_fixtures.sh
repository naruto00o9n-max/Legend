#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/workspace/tooling/android-sdk}}"
TASK_TOOLS="$TASK_SDK/build-tools/36.0.0"
TASK_PLATFORM="$TASK_SDK/platforms/android-36/android.jar"
TASK_WORK="$PWD/.work/fixtures"
mkdir -p "$TASK_WORK/classes" "$TASK_WORK/dex" build
find tests/android/src -name '*.java' > "$TASK_WORK/sources.txt"
java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -classpath "$TASK_PLATFORM" -d "$TASK_WORK/classes" @"$TASK_WORK/sources.txt"
find "$TASK_WORK/classes" -name '*.class' > "$TASK_WORK/classes.txt"
"$TASK_TOOLS/d8" --min-api 24 --lib "$TASK_PLATFORM" --output "$TASK_WORK/dex" @"$TASK_WORK/classes.txt"
"$TASK_TOOLS/aapt2" link -I "$TASK_PLATFORM" --manifest tests/android/AndroidManifest.xml -o "$TASK_WORK/fixtures-base.apk" --min-sdk-version 24 --target-sdk-version 36
python3 - "$TASK_WORK" <<'PY'
import pathlib, sys, zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'fixtures-base.apk','a',compression=zipfile.ZIP_DEFLATED) as archive:
    archive.write(p/'dex/classes.dex','classes.dex')
PY
"$TASK_TOOLS/zipalign" -P 16 -f 4 "$TASK_WORK/fixtures-base.apk" build/cookies-fixtures-unsigned.apk
