#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/workspace/tooling/android-sdk}}"
TASK_TOOLS="$TASK_SDK/build-tools/36.0.0"
TASK_KEY="${COOKIES_TEST_KEYSTORE:-$PWD/.work/cookies-ci-test.jks}"
mkdir -p .work build
if [[ ! -f "$TASK_KEY" ]]; then
  keytool -genkeypair -keystore "$TASK_KEY" -storepass android -keypass android -alias cookies-test -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Cookies Editor Test Only'
fi
for TASK_NAME in cookies-reference cookies-fixtures; do
  "$TASK_TOOLS/zipalign" -P 16 -f 4 "build/$TASK_NAME-unsigned.apk" ".work/$TASK_NAME-aligned.apk"
  "$TASK_TOOLS/apksigner" sign --ks "$TASK_KEY" --ks-key-alias cookies-test --ks-pass pass:android --key-pass pass:android --out "build/$TASK_NAME-test.apk" ".work/$TASK_NAME-aligned.apk"
  "$TASK_TOOLS/apksigner" verify "build/$TASK_NAME-test.apk"
done
