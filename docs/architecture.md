# Architecture

SyncCore extends the existing two-module Kotlin Multiplatform template. `shared` contains common code, Android and iOS ARM64 targets (device and simulator); `androidApp` hosts the Android activity. SwiftUI embeds the shared Compose controller. No persistence, transport or serialization library existed in the template. Existing tests were template arithmetic checks.

## Preserved build

Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20, Compose Multiplatform 1.12.1, Material3 1.12.0-alpha03. Daemon criteria require Java 21 without a vendor restriction. Android SDK 37, minimum 29, JVM bytecode 11. The preceding build fix is retained.

## Shared pipeline

`domain` owns entity snapshots, changes, conflicts, policies, plans and results. `engine` owns normalization, three-way detection, conflict classification, explicit resolution, deterministic planning and guarded in-memory execution. `simulator` owns reproducible scenarios and replay. `serialization` owns portable codecs. `storage` defines a baseline repository; `benchmark` measures the pure pipeline. Compose displays real engine output and never implements synchronization rules.

Base is the last accepted synchronized snapshot, not an authoritative server. Local and remote independently evolve. Identity is a stable non-empty string. Absence represents deletion; comparison is against the base, so deleting a never-synced entity is indistinguishable from no addition. Unresolved conflicts preserve both inputs and their old baseline entries. A planner returns data, never network or filesystem effects.

## Boundaries

All synchronization code lives in `commonMain` and depends only on Kotlin standard APIs. Android/iOS host the same Compose app and common engine. Platform storage may implement the baseline interface later. No cloud, database or filesystem synchronization is introduced. Tests live in commonTest with small Android/iOS entry-point checks. Execution and benchmarks are deferred until all implementation phases are complete.
