#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_DECODED="$1"
TASK_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/workspace/tooling/android-sdk}}"
TASK_TOOLS="$TASK_SDK/build-tools/36.0.0"
TASK_PLATFORM="$TASK_SDK/platforms/android-36/android.jar"
TASK_STAGE="$PWD/.work/welcome"
TASK_APKTOOL="${COOKIES_APKTOOL:-$PWD/.work/apktool-3.0.3.jar}"
mkdir -p "$TASK_STAGE/classes" "$TASK_STAGE/dex" "$TASK_DECODED/assets/cookies"
find android/local/src -name '*.java' > "$TASK_STAGE/sources.txt"
# Android's java.lang.invoke stubs omit LambdaMetafactory; javac uses the JDK
# bootstrap classes and D8 desugars lambdas for Android API 24 afterwards.
java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -classpath "$TASK_PLATFORM" -d "$TASK_STAGE/classes" @"$TASK_STAGE/sources.txt"
find "$TASK_STAGE/classes" -name '*.class' > "$TASK_STAGE/classes.txt"
"$TASK_TOOLS/d8" --min-api 24 --lib "$TASK_PLATFORM" --output "$TASK_STAGE/dex" @"$TASK_STAGE/classes.txt"
cat > "$TASK_STAGE/AndroidManifest.xml" <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.cookies.editor.local"><application android:label="Cookies Local UI"/></manifest>
XML
"$TASK_TOOLS/aapt2" link -I "$TASK_PLATFORM" --manifest "$TASK_STAGE/AndroidManifest.xml" -o "$TASK_STAGE/local.apk" --min-sdk-version 24
python3 - "$TASK_STAGE" <<'PY'
import pathlib,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'local.apk','a',compression=zipfile.ZIP_DEFLATED) as z:z.write(p/'dex/classes.dex','classes.dex')
PY
java -jar "$TASK_APKTOOL" d --force --no-res "$TASK_STAGE/local.apk" -o "$TASK_STAGE/decoded"
python3 - "$TASK_STAGE" "$TASK_DECODED" <<'PY'
import pathlib,shutil,sys
stage,decoded=map(pathlib.Path,sys.argv[1:]);numbers=[int(p.name.removeprefix('smali_classes')) for p in decoded.glob('smali_classes*')]
dest=decoded/('smali_classes'+str(max(numbers+[1])+1));shutil.copytree(stage/'decoded/smali',dest,dirs_exist_ok=True)
PY
cp branding/cookies-logo.png "$TASK_DECODED/assets/cookies/welcome-logo.png"
python3 scripts/apply_local_mode.py "$TASK_DECODED"
