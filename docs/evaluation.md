# SyncCore final evaluation

Final status: **Complete for the specified in-memory Kotlin Multiplatform synchronization showcase.** All 22 implementation phases have separate commits. Tests/benchmarks were first executed only after the phase 22 commit. Foundational versions remain Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and the existing Compose/platform versions.

## Implemented architecture

Shared detached state model, exact three-way detection, per-branch change summaries, explicit field/entity/delete-modify conflicts, four whole-entity policies, deterministic collision-safe copies, ordered plans, stale-input checks, canonical plan validation, atomic in-memory execution, per-entity baseline publication, replay and versioned JSON. All domain decisions live in `commonMain`. The shared Compose lab displays real scenarios and real planner/executor output. Platform adapters only provide baseline storage.

## Executed verification

| Check | Actual result |
| --- | --- |
| Android/JVM correctness suite | 35 tests, 0 failures, 0 errors, 0 skipped |
| iOS ARM64 simulator suite | 35 tests, 0 failures, 0 errors, 0 skipped |
| Android 15 device suite (A063) | 34 tests, 0 failures, 0 errors, 0 skipped |
| Separate JVM benchmark suite | 1 test, 0 failures; 80 measured stage samples |
| Deterministic evaluation harness | 395/395 runs passed; 150 cases with conflicts; 335 converged and 60 safely unresolved |
| Independent divergence predicate | All 320 exhaustive grid cases matched expected conflict presence |
| Android APK build | Successful |
| Android lint | 0 errors, 4 existing launcher-resource warnings |
| iOS device + simulator debug frameworks | Both built successfully |
| Xcode app build | Successful; Xcode 26.6, iPhone 17 Pro simulator, iOS 26.5 |
| Android app smoke launch | Successful cold launch on attached device; initial shared lab screen inspected |
| iOS app smoke launch | Successful launch on simulator; shared lab screenshot inspected |
| Gradle IDE model-only flags | Configuration successful with task-registration suppression |
| Static review | No platform/network/current-time/random imports in shared domain/engine; `git diff --check` passed |

[Test summary](verification/test-summary.json), [scenario evaluation](scenario-evaluation.md), [benchmark report](benchmark-report.md), [raw benchmark samples](benchmark-samples.csv), [iOS lab](verification/ios-lab.png).

The correctness suites check no change, both one-sided edits/additions/deletions, agreed concurrent changes/deletions, same-ID independent additions, delete/modify, multiple conflicts, all four choices, copy identity collisions, disjoint field edits, field removal, deterministic replay, insertion order, all codec round-trips, Unicode/escaping, malformed data, invalid identities/revisions, overflow, stale/tampered plans, partial baseline publication and storage compare-and-set failures. Android/iOS integration also exercises native baseline repository recreation. UI screenshots and startup are smoke checks, not exhaustive automated UI interaction tests.

## Performance

The separate final JVM benchmark used two warm-ups and five samples per stage with fixed mixed inputs. Full-pipeline planning median: **16.597459 ms for 10,000 identities**, **149.753459 ms for 100,000 identities** (20,000 conflicts, 80,000 operations). Guarded application at 100,000 identities: **347.186292 ms median**, including canonical replanning. Runtime: Temurin 21.0.8, macOS 26.4.1 ARM64, 10 available processors.

Only actual timing samples are reported. This is an in-process harness; GC, system load and runtime warm-up affect values. Heap measurements are before/after samples, not peak memory or allocations. No production throughput guarantee is implied.

## Reproducible commands

```sh
./gradlew :shared:testAndroidHostTest
./gradlew :shared:benchmarkSyncCore
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :shared:connectedAndroidDeviceTest
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath build/xcode CODE_SIGNING_ALLOWED=NO build
```

## External libraries and commits

One engine runtime library was added: kotlinx.serialization JSON 1.11.0, Apache-2.0, with its compiler plugin using the existing Kotlin version. AndroidX Test runner 1.7.0 was added for instrumentation. Existing Compose/AndroidX libraries remain. No third-party synchronization implementation was copied. [All 22 phase commits](progress.md) and final repair commits `c507dd5` and `9bcb56c` document the implementation.

## Known limitations and warnings

- No real cloud/transport/filesystem synchronization, tombstones, multi-peer causality, rename heuristics, text merge or authenticated protocol. Whole-entity policies remain explicit.
- Native baseline adapters guarantee serialized in-process publication only. NSUserDefaults does not promise crash-durable fsync. The lab uses in-memory fixtures.
- JSON format has documented bounded local-input limits and no streaming parser or schema migration.
- Physical iOS device signing/deployment was not attempted. Runtime validation used iOS 26.5, not the configured minimum 18.2. The linker warns that a transitive ICU object targets iOS simulator 18.5; minimum-OS compatibility is not claimed from this run.
- Kotlin/Native emits the existing inferred framework bundle-ID warning. Gradle reports deprecations from plugins. Xcode AppIntents metadata extraction is skipped because the app has no AppIntents dependency.
- Four pre-existing Android launcher-resource warnings remain: obsolete v24/v26 qualifiers and missing monochrome icon layers. No unrelated resource/configuration refactor was performed.
