# Serialization

`SyncCodec` serializes states, inputs, changes, conflicts, plans, results and replay records as version 1 JSON documents (`version`, `type`, `data`). Stable entity and field order gives canonical engine output. Fields and policy overrides are represented as lists so duplicate domain keys can be rejected. Decoders reject wrong type/version, unknown fields, invalid enums, duplicate identities, negative revisions and configured size/depth violations. This is a local showcase format, not an authenticated transport protocol. Decoded plans still require executor validation.

The project had no serialization dependency. Added kotlinx.serialization JSON 1.11.0 and the serialization compiler plugin using the **existing** Kotlin 2.4.20 version. Upstream release 1.11.0 is based on Kotlin 2.3.20, publishes Android/JVM and iOS ARM64 artifacts, and is Apache-2.0 licensed. No upstream code was copied. DTOs separate the JSON schema from domain classes. Source: https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.11.0 and https://github.com/Kotlin/kotlinx.serialization/blob/v1.11.0/LICENSE.txt.

Limits: 32 Mi UTF-16 code units per document, nesting depth 32, 100,000 entities per state, 1,000 fields per entity, 4,096 code units per identity/field name, 1 Mi code units per field value. Parsing allocates within the bounded document; this is not a streaming codec. Persisted data should be trusted local data. Version migrations and signatures are outside scope.

Platform testing additionally uses AndroidX Test runner 1.7.0 (Apache-2.0) from the official Google Maven repository; this is a test-only runtime dependency, not an engine dependency.
