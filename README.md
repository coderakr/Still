# Still — a little less online

A native Android app for intentional phone use, built with Kotlin, Jetpack Compose, and Material 3. Android 9 (API 28) and newer.

## Included

- A calm, responsive Focus dashboard with clear setup, protected, disabled, and break states.
- Independent rules for YouTube Shorts, Instagram Reels, Facebook Reels, and TikTok (the entire TikTok app).
- Website blocklist with URL normalization, international domain support, duplicate detection, and subdomain matching.
- Timed 5-, 10-, 15-, or 30-minute breaks that pause all rules, resume automatically, and can be ended early.
- A gentle intervention screen with a route home or to a deliberate timed break.
- Real daily, weekly, and all-time interception counts, plus a clear-activity control.
- Persistent preferences, a custom launcher icon, and a prominent Accessibility disclosure.

## Run

Open this folder in Android Studio, let Gradle sync, and run the `app` configuration. The project uses JDK 21 for Gradle, Android SDK 37, and target SDK 36. The starter's compile SDK was raised to 37 to match its existing AndroidX dependencies.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
# With an emulator or Android device connected:
./gradlew :app:connectedDebugAndroidTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

1. Install and open **Still**.
2. Tap **Enable protection**, read the disclosure, and select **Agree & open settings**.
3. In Android Accessibility settings, select **Still** (sometimes under “Installed apps”) and enable it.
4. Return to Still. The status should read **Protected**.
5. Choose feed rules and add websites in **Blocks**. Use **Take a mindful break** to pause all rules temporarily.

Some devices require “Allow restricted settings” from the app's Android App info menu for a locally installed accessibility app. Android and manufacturer settings control service availability.

## How blocking works

`FocusAccessibilityService` inspects visible accessibility nodes in supported foreground apps. Feed detection checks known viewer resource IDs or an explicitly selected Shorts/Reels tab; merely seeing an unselected tab or a video mentioning “shorts” does not trigger a block. TikTok's selected rule intentionally blocks the whole app. Browser rules check only known address bar nodes, avoiding a focused address being edited. A match exits the feed player before returning to Home and presenting Still's intervention screen. Browser matches return straight to Home.

Supported browser packages: Chrome (stable and beta), Brave, Edge, Firefox, Samsung Internet, and Opera. Website rules apply to the entire domain and subdomains, including mobile subdomains. Supported short-form web paths are checked automatically when the corresponding feed rule is enabled.

This is **screen-based, best-effort blocking**, not DNS/VPN filtering. Social apps frequently change their screen structure; not every app version, localization, or experiment exposes the same viewer IDs/selected-tab labels. Facebook in particular may expose no stable identifiers. Hidden address bars, fullscreen pages, embedded browsers, and unsupported browsers may bypass website detection. Already-minimized picture-in-picture players may not expose their feed identity; disable picture-in-picture for video apps in Android app settings for more reliable coverage. Validate the actual app versions and languages on target devices before distribution. Normal feeds can also contain short video previews that are intentionally not treated as an open Shorts/Reels viewer.

The service polls once per second as well as responding to UI changes, so an open feed is reevaluated when a break expires. Break deadlines use monotonic elapsed time and the device boot count. They survive app process recreation and wall-clock changes; a device restart ends a break. No exact-alarm, battery-exemption, overlay, or network permission is needed. Disabling the Accessibility service or force-stopping the app stops enforcement.

## Privacy

No account, analytics SDK, advertising SDK, or INTERNET permission. Visible screen content and URLs are processed transiently and never persisted or transmitted. Only rules, break state, disclosure consent, daily counts (retained up to 30 days), and an all-time count are stored privately on the device. Backup is disabled. The Accessibility service is not marked as an accessibility tool for people with disabilities.

For Play distribution, complete the applicable Accessibility API declaration and store-listing disclosure, assess policy eligibility, sign a release build, and perform device coverage testing. This repository does not imply Play approval.

## Code map

- `ui/StillApp.kt`: dashboard, block controls, activity, settings, setup and break sheets.
- `ui/Artwork.kt`: vector-style Compose icons and botanical artwork.
- `blocking/BlockingRules.kt`: pure, testable feed/URL detection and break clock.
- `blocking/FocusAccessibilityService.kt`: Android enforcement.
- `data/FocusStore.kt`: observable private preferences and counts.
- `BlockedActivity.kt`: intervention screen.

Unit tests cover invalid URLs, canonical hostnames, IDNs, domain boundaries, ordinary-tab false positives, viewer detection, disabled rules, web short-form paths, and monotonic break expiry/reboot behavior. Android UI tests cover website entry/removal and validation, feed persistence, disclosure, break persistence/early exit, and activity reset. Third-party app detection still requires testing against their live UI versions.

Android references: [Accessibility services](https://developer.android.com/guide/topics/ui/accessibility/service) and [Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491).

## Validation in this workspace

- Debug APK built successfully.
- Blocking-rule unit tests and Android UI tests passed on an Android 14 emulator.
- Android lint completed with no errors; remaining warnings concern starter dependency versions, target API, optional KTX style, and unused starter resources.
- Manually verified live Chrome website interception, website availability during a five-minute break, early break completion, and YouTube Shorts interception on the emulator.
- Fixed a launcher transition race and verified that exiting the YouTube player before Home avoids creating a picture-in-picture player in the tested version.
- Instagram, Facebook, TikTok, additional browsers, other Android versions, and OEM behavior need validation on their actual installed versions.
