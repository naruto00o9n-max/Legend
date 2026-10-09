# Cookies Editor — Android offline trial

Arabic, black/gold reconstruction of the user-provided **YTyper 3.8 APK**, with a new native animated welcome screen and device-local email/password profiles. The Android editor, layer models, renderer, fonts and native libraries are retained from the exact supplied reference. This is an Android trial; **there is no matching iPhone application or IPA yet**.

## Current behavior

- New welcome only: animated gold artwork, cookie identity, Arabic copy, email/password bottom sheet and guest entry. Existing editor screens keep their original structure.
- Create a profile and sign in locally. Passwords use salted PBKDF2 with 120,000 iterations, not plaintext. These are device profiles, not verified email accounts or cloud accounts. Projects share the device workspace.
- Google login, Drive backup, original community/store/download services, remote updates, analytics and notifications are disabled. Removing `INTERNET` permission enforces offline isolation. Cloud screens show explicit disabled previews.
- Original paid-feature entitlement checks remain. Offline mode does not grant premium subscriptions.
- Android floating assistant remains a system overlay. Some security-sensitive Android screens intentionally hide overlays.

## Build and verification

```bash
# Java 21, Python 3, ImageMagick; Android SDK 36 + build tools 36.
./scripts/build_android_reference.sh
./scripts/build_android_fixtures.sh
./scripts/sign_android_study.sh

# On an accelerated Android 30 emulator:
./scripts/run_android_fixtures.sh
./scripts/run_android_ui.sh

# Portable PNG core used by the unfinished iOS port:
./scripts/test_pixel_core.sh
```

GitHub Actions builds a signed Android test APK and runs both the original renderer fixtures and an actual UI walkthrough, with Wi-Fi/data disabled. The UI artifact includes screenshots, sanitized view trees, results and an offline HTML gallery. The final [Android run](https://github.com/naruto00o9n-max/Legend/actions/runs/37880268884) passed all three Android jobs, including **45 UI checks, 58 screenshots across 18 activities, and 3 native-cleaner test images**. It exercises local authentication, import, pinch gestures, Arabic text controls, drawing, layers, crop, project save/reopen, actual 800×15000 PNG export, font screens, settings, mini editor, native OpenCV cleaning and the floating assistant. Opening a panel does not establish every property combination; see the detailed evidence and limits in [verification.json](docs/verification.json).

Renderer fixtures cover 39 render cases and 235 real-font measurements. Unchanged 800×15000 opaque PNG export compares all 12,000,000 pixels; a separate export checks bottom text and unaffected rows. This does not establish every format or device memory limit. The original default canvas uses a downsampled preview; the actual export reads the full-resolution source. Full-region preview can remain gated by the original premium checks. Original Android export still uses full-size ARGB buffers; it is not bounded-memory export for unlimited lengths.

`build/cookies-reference-test.apk` is signed with an ephemeral CI test key. Signatures may differ between runs; preserve/export projects before replacing an installed build that Android refuses to update. `cookies-reference-unsigned.apk` is the unsigned build, not installable as supplied. No signing keys are committed.

The included font/effect panel controls are the original implementation. Tests cannot exhaust every property combination, paid feature, PSD/JPEG workflow or real-device behavior. Downloaded translation models and any remaining network-dependent operation cannot fetch data in this offline build.

## iPhone status

The `ios/` directory contains C PNG and Swift/CoreText typography components only. No full editor, navigation or IPA is implemented. The existing strict typography comparison matches **130/235** cases and retains **105** differences. These checks can run manually with workflow input `verify_ios=true`; Android-focused pushes defer them as requested, without removing the mismatch assertions. iPhone parity must be established through a real port and shared fixtures, not inferred from the APK. The iPhone assistant is agreed to be inside the app.

Read [the Arabic audit](docs/فحص-التطبيق.md), [the resource/property inventory](docs/reference-inventory.json) and [the font report](docs/font-parity-report.json). Repository: [naruto00o9n-max/Legend](https://github.com/naruto00o9n-max/Legend).

Actual emulator screenshots: [welcome](docs/screenshots/android-launcher.png), [email/password sheet](docs/screenshots/local-login.png), [editor](docs/screenshots/editor.png). Read the [Arabic coverage table](docs/android-ui-review.md).
