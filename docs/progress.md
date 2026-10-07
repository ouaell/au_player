# progress — evidence log (MS-001 owns this file's creation)

## MS-001 — Repo, Gradle KTS + version catalog, modules + flavors, SDK 36/26
- Status: DONE
- Sections used: §2, §4, §14, §15 (MS-001 row), §17.1 (T1)
- Placeholder identity (H4 undecided): AU Player / com.example.auplayer
- Flavors: direct (WEB_SOURCE_ENABLED=true), play (false)
- ABIs: release arm64-v8a + armeabi-v7a; debug adds x86_64 for CI emulator (§2, §17.2)
- Toolchain (verified in CI): AGP 8.13.2 + Gradle 8.13 + Kotlin 2.4.20 + JDK 17, compileSdk/targetSdk 36, minSdk 26
- CI: android-t1 green, run id 37686518140 (assembleDirectDebug, artifact direct-debug-apk)
- Fixes during task: preinstalled-runner-SDK install; kotlinOptions→compilerOptions (KGP 2.4); compose runtime added to :player/:reader/:library
- Tests added: none (scaffold only; JVM/unit gates land in MS-002)
- UNVERIFIED-on-device: everything (no device session yet; install + first T3 at Day-1 gate)
- Files changed: 34 scaffold files (settings, root build, version catalog, 7 module builds, manifest, MainActivity/AuApp/AppContainer/Nav/Tokens, 6 module stubs, AGENTS.md, MASTER_PLAN.md, docs, T1 workflow)

## MS-002 — CI gates: lint, detekt, ktlint, unit tests, license allowlist, 16KB, forbidden grep
- Status: in progress
- Sections used: §12.3, §15 (MS-002 row), §17.1 (T1), §17.5 (16KB minimal; full verifier in MS-062), §13 (allowlist basis)
- detekt 1.23.8 (stable 1.x; 2.0 still pre-release) + ktlint-gradle 14.2.0, both version-pinned in catalog
- Tests added: 6 JVM unit tests (Diag, FakePlayerController, FakeEnhanceEngine, FakePageProvider, FakeWebSourceExtractor, FakeLibrarySource); :app has no pure-logic unit yet (needs Context/Robolectric, T2)
- Tokens.kt renamed to §10 lowercase semantic names (also satisfies detekt TopLevelPropertyNaming); file-level MagicNumber suppress with rationale
- UNVERIFIED-on-device: everything (unchanged)
