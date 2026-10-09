#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/ui-runtime
capture_diagnostics() {
  adb logcat -d -v threadtime > build/ui-runtime/logcat.txt 2>&1 || true
  adb exec-out screencap -p > build/ui-runtime/final-screen.png 2>/dev/null || true
  adb pull /sdcard/Android/data/com.cookies.editor/files/cookies-ui-evidence.zip build/ui-runtime/ >/dev/null 2>&1 || true
  adb pull /sdcard/Android/data/com.cookies.editor/files/ui-evidence build/ui-runtime/partial-evidence >/dev/null 2>&1 || true
}
trap capture_diagnostics EXIT
adb logcat -c
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global verifier_verify_adb_installs 0
adb install --no-streaming -r build/cookies-reference-test.apk
adb install --no-streaming -r build/cookies-fixtures-test.apk
timeout 360 adb shell am instrument -w -r com.cookies.editor.fixtures/com.cookies.editor.fixtures.UiRunner > build/ui-runtime/instrumentation.txt
adb pull /sdcard/Android/data/com.cookies.editor/files/cookies-ui-evidence.zip build/ui-runtime/
python3 - <<'PY'
import json,pathlib,zipfile
p=pathlib.Path('build/ui-runtime')
with zipfile.ZipFile(p/'cookies-ui-evidence.zip') as z:
    assert z.testzip() is None
    z.extractall(p/'screenshots')
report=json.loads((p/'screenshots/ui-verification.json').read_text())
print('Actual captured screenshots:',report['screenshots'])
for c in report['checks']:print(c['status'],c['name'],c.get('error',''))
PY
python3 scripts/create_ui_gallery.py build/ui-runtime/screenshots
python3 - <<'PY'
import json,pathlib,zipfile
p=pathlib.Path('build/ui-runtime');folder=p/'screenshots'
with zipfile.ZipFile(p/'cookies-ui-gallery.zip','w',compression=zipfile.ZIP_DEFLATED) as z:
    for f in sorted(folder.glob('*')):
        if f.is_file():z.write(f,f.name)
report=json.loads((folder/'ui-verification.json').read_text())
assert report['status']=='pass','UI checks failed; gallery, screenshots and logcat retained for diagnosis'
PY
