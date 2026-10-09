#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/runtime
# Testing is entirely offline and never signs in to the original developer service.
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global verifier_verify_adb_installs 0
adb install --no-streaming -r build/cookies-reference-test.apk
adb install --no-streaming -r build/cookies-fixtures-test.apk
adb shell am instrument -w -r com.cookies.editor.fixtures/com.cookies.editor.fixtures.FixtureRunner > build/runtime/instrumentation.txt
if ! rg -q 'INSTRUMENTATION_CODE: -1' build/runtime/instrumentation.txt; then
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
