#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_SDK="$(xcrun --sdk iphoneos --show-sdk-path)"
TASK_OUTPUT="$PWD/.work/ios-components"
mkdir -p "$TASK_OUTPUT" build/ios-components
TASK_FLAGS=(-target arm64-apple-ios15.0 -isysroot "$TASK_SDK" -O2 -DPNG_ARM_NEON_OPT=0 -I ios/Core/Vendor/libpng)
for TASK_SOURCE in ios/Core/PixelCore.c ios/Core/Vendor/libpng/*.c; do
  TASK_OBJECT="$TASK_OUTPUT/$(basename "${TASK_SOURCE%.c}").o"
  xcrun --sdk iphoneos clang "${TASK_FLAGS[@]}" -c "$TASK_SOURCE" -o "$TASK_OBJECT"
done
xcrun --sdk iphoneos ar rcs build/ios-components/libCookiesPixelCore.a "$TASK_OUTPUT"/*.o
xcrun swiftc -target arm64-apple-ios15.0 -sdk "$TASK_SDK" -parse-as-library -whole-module-optimization \
  -module-name CookiesTypesetting -emit-module -emit-object ios/Engine/Typesetter.swift ios/Engine/CoreTextMeasure.swift \
  -emit-module-path build/ios-components/CookiesTypesetting.swiftmodule \
  -o build/ios-components/CookiesTypesetting.o
python3 - <<'PY'
import json,pathlib
pathlib.Path('build/ios-components/validation.json').write_text(json.dumps({
    'result':'compiled', 'sdk':'iphoneos', 'architecture':'arm64',
    'minimumIOS':'15.0', 'components':['PNG C core','Swift typography algorithm'],
    'appBuilt':False, 'ipaBuilt':False, 'iPhoneRuntimeVerified':False,
    'scope':'Port components only; this is not the requested complete editor.'
},indent=2))
PY
