# MASTER_STATE — single source of truth (updated every 30–45 min + after each phase)

## Clock
- Run start (T+0): 2026-10-11 ~03:45 UTC. Now: Phase A.

## Phase
- A setup IN PROGRESS (agents+skills created; visual.yml written; dossier kickoff next).

## Done
- `legacy-v1` tag pushed (old app archived).
- Native engine artifacts present: release `native-20261009` (arm64/armv7l/x86_64 TGZ) — no rebuild needed.
- S1-001 fix proven (5× emulator green); E1–E9+Smoke+S1 suite green.

## Next
1. Commit Phase A files, dispatch visual skeleton run (trivial-app proof).
2. Background: competitor dossier (Task agent) + Mihon/REX recon.
3. Phase B: Mihon orphan branch, rebrand `app.auplayer`, strip online machinery, tabs, build + visual gate.

## Open defects
- S2-001: WebView INTERNET crash (legacy app; dies with the old UI unless ported — Web tab rewrite covers it).
- Showcase/bughunt scripts: fixed package resolution (done).

## CI run ids
- (append newest last; old ledger in docs/progress.md)
