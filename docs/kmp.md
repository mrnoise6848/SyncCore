# KMP platform boundaries

`shared/commonMain` owns all domain rules, the planner, executor, replay, JSON DTOs, baseline interfaces and portable benchmarks. It imports no Android, Java, Apple or UIKit types. Android Activity and iOS SwiftUI/ComposeUIViewController both invoke `App()` and use the same `SyncSimulator`. No separate synchronization algorithm exists in either host.

Platform baseline adapters implement only read/compare-and-set storage: Android SharedPreferences and Apple NSUserDefaults. They serialize publication within a process via a shared lock; neither promises cross-process atomicity. Android reports synchronous commit failures. Apple defaults schedules disk persistence and does not promise crash-durable fsync. The UI lab intentionally operates on scenario snapshots rather than persisting fixture baselines to user storage.

Common tests are compiled for Android host and iOS simulator. A real Android instrumentation test exercises shared replay plus SharedPreferences repository recreation. An iOS test exercises shared replay plus NSUserDefaults repository recreation. Both native storage tests executed successfully in final verification. Android instrumentation requires the explicit AndroidX runner 1.7.0 runtime dependency; a successful Gradle exit without a positive test count is not accepted as evidence. Native ARM64 framework compilation and simulator runtime checks are tracked separately: compilation is not runtime validation.

Final commands: `./gradlew :shared:testAndroidHostTest`, `./gradlew :shared:connectedAndroidDeviceTest` with an attached target, `./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosArm64`, and an Xcode simulator build. Build versions remain unchanged.

Verified: 35 tests on the iOS 26.5 ARM64 simulator, 34 tests on Android 15 (A063), Android APK and lint, device/simulator frameworks, Xcode simulator app build, and app launch on both targets. Physical iOS installation/signing and iOS 18.2 runtime were not tested. See [evaluation](evaluation.md).
