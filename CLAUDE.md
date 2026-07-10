# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

An Android library (Jetpack Compose) providing `RollingAnimatedText` — odometer-style digit animation. Two Gradle modules: `:library` (the published artifact) and `:sample` (demo app). Published via JitPack as `com.github.morozione:compose-rolling-text:<git tag>`.

## Commands

On this Windows machine, `JAVA_HOME` is not set globally — prefix Gradle commands with the Android Studio JBR path (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat <task>
```

- Run library unit tests: `.\gradlew.bat :library:test`
- Run a single test class: `.\gradlew.bat :library:test --tests "io.github.morozione.rollingtext.RollingAnimatedTextHelpersTest*"` (nested classes: append e.g. `$BuildDrumTextTest`)
- Build library AAR: `.\gradlew.bat :library:assembleRelease`
- Run instrumented (E2E) tests — needs a connected device/emulator: `.\gradlew.bat :library:connectedDebugAndroidTest` (compile-only check without a device: `:library:assembleDebugAndroidTest`)
- Build sample APK: `.\gradlew.bat :sample:assembleDebug`
- Full CI-equivalent check: `.\gradlew.bat :library:test :library:assembleRelease :sample:assembleDebug`

Configuration cache, build cache, and parallel mode are enabled; the daemon JVM is pinned to 21 via `gradle/gradle-daemon-jvm.properties` (foojay auto-provisioning).

## Build setup constraints

- Uses **AGP 9 built-in Kotlin**: there is deliberately no `org.jetbrains.kotlin.android` plugin anywhere. Modules apply only `com.android.library`/`com.android.application` plus `org.jetbrains.kotlin.plugin.compose`. The `kotlin { compilerOptions { ... } }` block lives *inside* `android {}`. Do not re-add the kotlin-android plugin or `kotlinOptions`.
- `library/build.gradle.kts` has `publishing { singleVariant("release") { withSourcesJar() } }` in the `android {}` block — required for the `maven-publish` block's `components["release"]` to exist. JitPack runs `:library:publishToMavenLocal` (see `jitpack.yml`).
- Versions live in `gradle/libs.versions.toml`. Some androidx deps require compileSdk 37+.

## Releasing

Version appears in two places that must stay in sync: `version = "x.y.z"` in the `publishing` block of `library/build.gradle.kts`, and the dependency snippet in `README.md`. JitPack uses the **git tag name literally** as the version — tags are v-prefixed (`v1.1.0`), so the README coordinate is `...:v1.1.0`. Release = commit, tag `vX.Y.Z` (annotated), push branch and tag (`git push origin main vX.Y.Z` — a plain push does not send tags). JitPack builds lazily on first request at https://jitpack.io/#morozione/compose-rolling-text.

## Architecture

The entire library is one file: `library/src/main/java/io/github/morozione/rollingtext/RollingAnimatedText.kt`. Key design decisions that must be preserved when editing:

- **Odometer keying**: character slots are keyed by distance from the *end* of the string (`key(length - index)`), so `99 → 100` shifts digits like a real odometer instead of re-animating every column.
- **Fixed-width digit slots**: each digit renders in a `Box` sized to the widest of `'0'..'9'` (measured with `LineHeightStyle(Proportional, Trim.None)`), preventing horizontal jitter with proportional fonts. `measureRowWidth` for auto-sizing must stay consistent with this slot model.
- **Deferred animation reads**: `Animatable.value` is read only inside the `graphicsLayer {}` lambda (translationY), so animation frames skip recomposition entirely. Do not hoist that read into composition.
- **Rolling drum**: `buildDrumText` builds the intermediate digit column; direction is handled by inverting progress (`isIncreasing → 1f - progress`), and `calculateVerticalOffset` maps progress to pixels.
- **Non-digit characters** render statically with a `LaunchedEffect(char)` updating `previousChar` to prevent stale-digit animation when a slot changes type.
- **Auto-size** (`calculateFontSizeToFit`) binary-searches font size (0.5sp precision) with guards for non-Sp/unspecified font sizes.

Pure helpers (`buildDrumText`, `calculateVerticalOffset`, `shouldSkipAnimation`, `resolveTextColor`) are `internal` specifically so `library/src/test/.../RollingAnimatedTextHelpersTest.kt` can unit-test them on the JVM (Compose `Color`/`TextStyle` work in plain unit tests; no Robolectric needed). Keep new logic in extractable `internal` functions when possible.

Gotcha encountered in tests: use the delta overload `assertEquals(expected, actual, 0.001f)` for floats — the boxed overload fails on `-0.0f` vs `0.0f`.

## CI

`.github/workflows/ci.yml` runs on push to `main` and on PRs, two parallel jobs: `build` (JDK 21 temurin + `gradle/actions/setup-gradle@v4` caching, then the full check command above) and `e2e` (enables KVM, boots an API 34 emulator via `reactivecircus/android-emulator-runner`, runs `:library:connectedDebugAndroidTest`). Instrumented tests live in `library/src/androidTest/`; exact-text matchers only match a digit slot once its rolling animation has settled, because mid-animation the node's text is the whole multi-line drum.
