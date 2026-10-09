# Cookies Editor task constraints

The user requested full tool/UI/behavior parity with the provided YTyper 3.8 APK for Android and iPhone, with Cookies branding, a cookie logo, black/gold colors and Arabic. They rejected the earlier independent React Native and Pro Image Editor UIs. Do not replace this request with another generic demo or report an incomplete port as finished.

The user has only the APK. The iPhone floating assistant is explicitly allowed inside the app; Android can use a system overlay. Typical target image is 800×15000. Communicate in Modern Standard Arabic.

The Android artifact here is a **reference rebuild**, not an independent service-configured release. Full editor/canvas smali and native assets remain intact; authentication and original entitlement checks remain intact. Do not claim Google sign-in works with a different package/signature. Do not repurpose the original developer's backend configuration as Cookies infrastructure.

The recovered Java is for analysis, not a buildable Gradle project. Eleven application methods are incomplete in JADX output; complete smali instructions remain available from `scripts/collect_reference.sh`. The exact input hash is in `branding/identity.json`. Do not silently substitute a different upstream version.

The iOS folder currently holds **port components only**: C PNG core and a Swift typography algorithm. There is no complete iOS editor or app entry point. Do not add a placeholder IPA or a workflow that labels a diagnostic stub as the requested matching editor. Port the recovered functionality and maintain a feature parity matrix with evidence from original/reference projects. Matching a tool name is not behavior parity.

Meaningful checks: PNG 800×15000 all-channel preservation under bounded memory, real glyph layout comparisons using original fonts on Android and CoreText, transforms/masks/blend fixtures, project round trips, editor gesture flows and lossless exports. Existing core test passes; this does not establish iPhone UI or render parity, 16-bit editing, JPEG decoding or PSD output.

The user supplied https://github.com/naruto00o9n-max/Legend.git on 2026-10-09 and authorized publishing this work and running Actions there. Use the connected naruto00o9n-max account. The shell GitHub CLI authenticates as mourad-sat and may read public build status, but do not publish into that account or replace the old Nre project. Preserve existing Legend history.

The Android instrumentation harness compiles. It captures actual original engine output and all embedded font metrics, then the macOS CoreText job compares real outputs. Neither job has run yet. Local software emulator was unstable and could not complete app installation; this was not a proven application crash. Do not mark parity passed from compiler success.
