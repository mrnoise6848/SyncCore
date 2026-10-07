# SyncCore

**Two replicas changed. Which version should survive?**

| Last accepted base | Local edit | Remote edit |
|---|---|---|
| Meeting | Important Meeting | Team Meeting |

Picking a branch silently loses one edit. SyncCore makes this a conflict, preserves both inputs and asks for a resolution policy before producing mutations.

SyncCore is a deterministic Kotlin Multiplatform synchronization engine with an Android/iOS conflict lab. It compares three snapshots, builds an ordered plan and applies it to detached states after checking that the inputs and plan are still valid.

## Explore the decision

```kotlin
val scenario = DemoScenarios.all.first()
val unresolved = SyncSimulator().run(scenario)
val resolved = SyncSimulator().run(
    scenario, ResolutionChoices(ResolutionPolicy.KEEP_BOTH)
)
```

With no resolution, the divergent branches remain unresolved. `KEEP_BOTH` gives the competing values distinct stable identities and converges both replicas to both values. `KEEP_LOCAL`, `KEEP_REMOTE` and `SKIP` offer the other explicit choices, globally or per conflict.

The classes are in `com.noise.synccore.simulator` and `com.noise.synccore.domain.policy`. The lab exposes 15 scenarios, input snapshots, conflicts, operations, results and the accepted baseline; replay reruns the shared engine.

<p align="center">
  <img src="docs/verification/ios-lab.png" width="360" alt="iOS conflict lab showing Meeting changed independently to Important Meeting and Team Meeting">
</p>

*Existing iOS simulator capture of the competing edits.*

## A plan carries its own preconditions

Detection distinguishes one-sided changes, agreed changes and divergent edits—including delete/modify conflicts. Field differences help explain a conflict; policies resolve whole entities rather than automatically merging text or fields.

Operations carry expected state and replacement payloads. Before applying them, [PlanExecutor](shared/src/commonMain/kotlin/com/noise/synccore/engine/PlanExecutor.kt) checks current snapshots against the inputs and independently recomputes the canonical plan. A stale or edited plan is rejected before results are returned.

Only converged identities advance the baseline. Skipped conflicts keep their earlier base, preserving the evidence needed for the next attempt. Platform repositories use compare-and-set publication within the process. [Sync model](docs/sync-model.md) · [Resolution policies](docs/resolution-policies.md) · [Integrity checks](docs/safety.md)

## What does that extra check cost?

The committed JVM benchmark measures planning and guarded application separately:

| Identities | Conflicts / operations | Planning median | Guarded application median |
|---:|---:|---:|---:|
| 10,000 | 2,000 / 8,000 | 16.60 ms | 31.81 ms |
| 100,000 | 20,000 / 80,000 | 149.75 ms | 347.19 ms |

Planning includes detection and resolution. Application includes canonical replanning, making the cost of the integrity check visible.

Rounded from the [measured report](docs/benchmark-report.md): Temurin 21.0.8, macOS 26.4.1 ARM64, 10 available processors, two warmups and five samples per stage; fixture generation excluded. These are in-process JVM measurements subject to GC and system load, not mobile or network throughput. With five samples, empirical p95 is the maximum. [Methodology](docs/performance.md) · [Raw samples](docs/benchmark-samples.csv)

## One engine across the hosts

`commonMain` owns the engine, simulator, JSON replay codec, repository interfaces and shared Compose lab. Platform adapters supply baseline storage and host entry points. Domain decisions avoid network, clock and randomness dependencies so identical inputs can be replayed deterministically. [Architecture](docs/architecture.md) · [KMP boundaries](docs/kmp.md)

The [evaluation record](docs/evaluation.md) reports **35 host, 35 iOS simulator and 34 Android device tests passing**, plus **395 deterministic scenario/policy combinations**. Cases include deletion conflicts, identity collisions, stale/tampered plans, codecs and baseline publication.

## Run the lab or the harness

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
./gradlew :shared:benchmarkSyncCore
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:connectedAndroidDeviceTest
```

The last command needs an Android device. For the iOS app, open `iosApp/iosApp.xcodeproj`.

The engine operates in memory. Real transport transactions, retry protocols, tombstones, multi-peer causality, rename inference and authentication belong to future adapters. Detached atomic application does not make server writes atomic; revisions do not establish cross-branch authority. Baseline publication is in-process, and physical/minimum-version iOS validation remains open. [Full evaluation scope](docs/evaluation.md)
