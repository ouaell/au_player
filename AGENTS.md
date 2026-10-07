Read MASTER_PLAN.md before any task. One task (MS id) per session; start fresh.
Build and test ONLY through GitHub Actions (never run Gradle locally).
Read the task row in §15 + the sections it cites; do nothing outside it.
No file > 600 lines. Pure logic goes in plain Kotlin with JVM tests.
Never use vo=mediacodec_embed. Never use GPL/AGPL code or non-commercial model weights.
[VERIFY] items must be confirmed in real source first; record results in docs/progress.md.
Task done = green CI + entry in docs/progress.md (CI run id, tests added, UNVERIFIED-on-device list).
If blocked or time-box exceeded: use the written fallback, note it, continue.
Commit "MS-xxx: summary" and tag MS-xxx-done. Every 3 tasks: run the full test suite and re-read the plan.
