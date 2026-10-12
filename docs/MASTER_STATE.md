# MASTER_STATE — single source of truth

## Clock
- Run start (T+0): 2026-10-11 ~03:45 UTC. Now: Phase B mid (~T+17:00 wall, CI-parallelized).

## Phase
- B IN PROGRESS. mihon-ci GREEN (assembleDebug): dep ports (flexible-adapter/davidea, imagedecoder v16, injekt kohesive + vendored registrar), rebrand (app.auplayer, AU Player, amber icon, About→repo), manifest strip, Videos/Web tabs, local-scan action.
- mihon-visual running: Maestro tabs/scan/reader + contact sheet (run 38173282244).

## Done
- Phase A gates: agents+skills, visual.yml green on legacy, dossier + gap-matrix seeded, recon (Mihon v0.20.4, REX usable primary).
- legacy-v1 tag; native artifacts present (no rebuild).
- mihon-line: clean object import (31beac7); working tree verified after a zero-byte corruption incident (rebuilt via git objects, no file copies).

## Next
1. Full gate run 38195812584 (ordered flows, wake+strict pixel gate with format checks).
2. Phase B gate: green + contact sheet review → promote to main → Phase C (REX).

## Open defects
- S2-001 (legacy WebView INTERNET crash) — dies with old UI.
- Maestro texts guessed (More options/Scan/Chapters/Demo) — screenshots will confirm.

## CI run ids (mihon-line)
- mihon-ci 38172687343 green (4m01s, rebrand+tabs+scan).
- mihon-visual 38173282244 running.
