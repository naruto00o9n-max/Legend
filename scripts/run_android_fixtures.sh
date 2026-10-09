#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/runtime
capture_diagnostics() {
  adb logcat -d -v threadtime > build/runtime/logcat.txt 2>&1 || true
  adb exec-out screencap -p > build/runtime/emulator-screen.png 2>/dev/null || true
  adb shell uiautomator dump /sdcard/cookies-ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/cookies-ui.xml build/runtime/cookies-ui.xml >/dev/null 2>&1 || true
}
trap capture_diagnostics EXIT
adb logcat -c
# Testing is entirely offline and never signs in to the original developer service.
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global verifier_verify_adb_installs 0
adb install --no-streaming -r build/cookies-reference-test.apk
adb install --no-streaming -r build/cookies-fixtures-test.apk
adb shell am instrument -w -r com.cookies.editor.fixtures/com.cookies.editor.fixtures.FixtureRunner > build/runtime/instrumentation.txt
if ! python3 -c "from pathlib import Path; assert 'INSTRUMENTATION_CODE: -1' in Path('build/runtime/instrumentation.txt').read_text()"; then
  cat build/runtime/instrumentation.txt
  exit 1
fi
adb pull /sdcard/Android/data/com.cookies.editor/files/reference-fixtures build/runtime/reference-fixtures
python3 - <<'PY'
import json,pathlib
p=pathlib.Path('build/runtime/reference-fixtures')
r=json.loads((p/'verification.json').read_text())
assert r['result']=='pass'
assert r['renderCases']>=20
assert len(json.loads((p/'typography-android.json').read_text()))>=100
print('Actual original Android renderer cases:',r['renderCases'])
print('iOS image/glyph parity has not been evaluated by this Android test.')
PY

# Observe the actual launcher offline; this does not sign in or bypass its gates.
adb shell am start -W -n com.cookies.editor/com.oneguystudio.ytyper.ui.dashboard.SplashActivity > build/runtime/launcher-start.txt
sleep 3
