# Cookies Editor — reconstruction workbench

Arabic, black/gold identity for the user-provided YTyper 3.8 reference. **This repository currently contains a reproducible Android reference rebuild and verified image/typography port components. It is not a finished independent editor or a matching iPhone app.**

The reference APK is native Kotlin/Android. Its Canvas, TextPaint, StaticLayout and Android services cannot be built with Xcode. All original Android editor bytecode is retained for the reference rebuild; Android authentication and original feature entitlements are also retained. New package/signature requires independent authentication configuration before a functional Cookies release can be claimed.

Read [the Arabic audit](docs/فحص-التطبيق.md) and [the screen/property inventory](docs/reference-inventory.json).

```bash
# Rebuild the exact reference with resource/default branding.
# Java 21, Python 3 and ImageMagick are required.
./scripts/build_android_reference.sh

# Verify 800 × 15000 without downscaling or hidden-alpha corruption.
./scripts/test_pixel_core.sh

# Compile the recovered typography algorithm's platform-independent checks.
mkdir -p .work
swiftc ios/Engine/Typesetter.swift tests/typography_checks.swift -o .work/test-typesetter
.work/test-typesetter
```

`build/cookies-reference-unsigned.apk` is a research artifact. Its compilation must not be confused with verified editor runtime access or Google sign-in. No iPhone app archive has been built from this Android package.

The floating assistant stays an Android system overlay and an in-app panel on iOS, as agreed with the user.

PNG core is for 8-bit inputs and preserves original size and RGBA values when unedited. libpng licenses are retained in `ios/Core/Vendor/libpng/LICENSE`. The C core was reused from the existing image pipeline; the previously rejected app UI was not reused.

`tests/android` captures the APK's actual engine output through instrumentation. CI compares Android font formatting to CoreText using the exact embedded font files. Android execution passed 39 render cases and actual 800×15000 exports, comparing all 12,000,000 opaque pixels and checking bottom text. iPhone arm64 port components compiled against the Apple SDK. Font formatting matches 130/235 cases (99/235 with raw fractional CoreText); the strict parity job intentionally fails on the remaining differences. No full editor gesture flows or iPhone app/IPA have been verified. Read [the validation results](docs/verification.json) and [the font report](docs/font-parity-report.json).

GitHub repository: [naruto00o9n-max/Legend](https://github.com/naruto00o9n-max/Legend). [Verified run](https://github.com/naruto00o9n-max/Legend/actions/runs/37872638113). Download `cookies-reference-test.apk` from the `cookies-android-reference-study` artifact for the Android study; it still uses the original sign-in gate and is not an independent service-configured release. `ios-port-components` contains libraries and modules, not an IPA.
