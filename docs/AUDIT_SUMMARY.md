# AUDIT_SUMMARY (HEAD `d04e689`, branch `audit/2026-10-10`, read-only)

## 1. HEAD + run ids
- HEAD `d04e689` "docs: full suite green". Fresh re-runs ATTEMPTED 2026-10-10, all scheduler-refused (billing/spending-limit):
  T1 `38070189258`, headless `38070192324`, smoke `38070197087`, emulator `38070199863` (fail in 3–6 s, no logs).
- Last greens, identical code HEAD: T1 `38021692373` (3m04s) · emulator `38017724352` (10/10) ·
  headless `38021893683` (H1+H2+H4) · smoke-release `38021902266` (R8 minified). Native rebuild NOT re-run (hours-long).
- New jobs (billing-blocked, code on branch): `audit-bughunt.yml`, `showcase.yml` + `tools/audit/*.sh`. No showcase/bughunt artifacts yet.

## 2. Engine
- Real? YES in CI (E1 green every emulator run; `mpv v0.41.0`, VO gpu + AO audiotrack, mediacodec-copy).
- LGPL-clean? PARTLY (pins mpv v0.41.0/FFmpeg n9.0/dav1d 1.5.4/libass 0.17.5/libplacebo v7.360.1 + `-Dgpl=false` recipe
  + CI strings/alignment gates; NO committed `.so`, NO FFmpeg `configuration:` string in repo).
- ABIs? arm64-v8a + armeabi-v7a + x86_64 staged in CI only (gitignored; 0 `.so` in repo).

## 3. Requirements R1–R15
| Req | Implemented | Status | Top gap |
|---|---|---|---|
| R1 player | Partial | E2E-VERIFIED (core) / UNIT-ONLY (rest) | No UI for tracks/speed/A-B/subs-style/PiP/bg-audio; gestures mapper unwired |
| R2 anime | Full | E2E-VERIFIED | Phone-GPU cost unproven |
| R3 standard | Partial (RAVU, not FSRCNNX) | E2E-VERIFIED load, license UNVERIFIED | In-repo license/SPDX proof missing |
| R4 reader modes | Partial (paged only) | E2E-VERIFIED (CBZ) | RTL/webtoon/double/filters/zoom/chapters unwired |
| R5 PDF | Partial (basic render) | E2E-VERIFIED | No tiles/cache/password/scanned-detect |
| R6 inputs | Partial | Partial | No folder grid; content:// copied, never fd-passed |
| R7 web source | Full-ish | E2E-VERIFIED (E6) | No reorder/selector UI; exotic sites untested |
| R8 image model | None | STUBBED | No weights, null binder, no watchdog |
| R9 legal | Partial | Partial | No committed artifact proof; notices JSON missing |
| R10 originality | Full | UNIT-ONLY | — |
| R11 l10n | Skeleton (6 strings) | UNVERIFIED | App not actually localized; no screenshot tests |
| R12 settings | Keys only | UNIT-ONLY | No Settings screen; user changes nothing |
| R13 ads | Interface only | UNIT-ONLY | No SDK, NoOp unbound, consent stub |
| R14 release | Partial | Partial | Never signed; release.yml never run; v0.1.0-ms001 |
| R15 no content | Full | E2E-VERIFIED | — |

## 4. STUBBED in release
- `FakeMpvBridge()` fallback in `AppContainer.kt:41-42` (silent fake engine if `.so` missing)
- `AuPlaybackService.onGetSession()=null`; `EnhanceService.onBind()=null`; NCNN `initVulkan()=false` + `inferTile` throws
- Dead `StubScreen`s (Player/Settings/unreached Reader/Web); `FakeLibrarySource`-equivalent production `LibrarySource`

## 5. Open bugs S1/S2/S3
- S1: none KNOWN — but monkey/negative/lifecycle never ran (absence of evidence, not evidence of absence)
- S2 N1: Settings/Diagnostics/Licenses unreachable (`Nav.kt:93-113`, zero call-sites)
- S2 N2: track/speed/A-B/sleep/sub-style/aspect/lock/PiP/bg-audio have logic, zero UI
- S2 N3: reader modes/filters/zoom/chapters/progress unwired (paged-only)
- S2 N4: release binds Fake engine silently when `.so` absent
- S3 N5: `SmokeTest` asserts `assertTrue(true)`; N6 same-renderer goldens; N7 no backup rules, sick-DB silent; N8 artifact quota/expiry

## 6. Top 10 gaps (next task + hours)
1. Settings screen reachable (R12/N1) → build Settings UI from existing keys: 4–6 h
2. Player feature UI (tracks/speed/subs-style/aspect/lock) (N2) → sheets wired to existing props: 8–12 h
3. Reader modes/filters/zoom/chapters wiring (N3) → bind existing viewers + mode memory: 6–10 h
4. Fake-engine fallback UX (N4) → fail-visible error screen + gate: 1–2 h
5. Background audio + notification + focus/noisy (R1) → wire service: 6–10 h (+device)
6. NCNN real path or explicit cut (R8) → bundle weights + JNI or remove UI: 12–20 h or 1 h to cut
7. Release signing + AAB + version (R14/P5) → owner keystore + dispatch release.yml: 1 h + owner
8. Notices JSON + licenses screen reachability (R9) → generate + link in Settings: 2–3 h
9. Real localization (R11) → extract ~200 strings × 3 locales + RTL screenshots: 6–10 h
10. Bug-hunt + showcase green (CI) → restore billing, run new jobs, fix findings: 2 h + findings

## 7. Unverifiable on emulator (mitigation?)
- Mobile-GPU shader cost/thermal → YES (governor + rung-skip + 1440p gate)
- Vendor decoder quirks → PARTIAL (software-decoder key exists, wiring partial)
- Vulkan driver crashes → NO watchdog (process split declared only)
- Touch feel/haptics → NO; headset/noisy → NO; CJK fonts → NO (nothing shipped)
- Shader-compile stalls → YES (async prep + cache dir); Doze/bg-audio → N/A (no bg audio)

## 8. Go/No-Go
1. Real engine LGPL-clean + E1 on release variant: PARTIAL (debug only; no FFmpeg config string)
2. Anime + Standard visibly differ, no GLSL errors: PARTIAL (H2 clean-compile + E3 load/play; no off-vs-enhanced assert)
3. Zero S1/S2 open: FAIL (S2 N1–N4; S1 unknown)
4. No STUBBED in release: FAIL (see §4)
5. Signed AAB: FAIL (never built; no keystore)
6. Pre-launch path: FAIL (billing failing; no Play/Firebase)
OVERALL: **NO-GO** for 1.0. GitHub Beta possible after N1 fixed-or-cut + billing restored + bughunt/showcase green.

## 9. Owner actions still needed (true blockers only)
- Fix GitHub billing/spending limit (ALL CI currently refused — nothing else can be verified until this is done)
- H8 keystore (generate once, back up offline, add `AU_KEYSTORE_*` secrets) for signed AAB
- H5/H6 Play Console + Firebase project (pre-launch report, FTL, store path)
- One phone session at the end (§17.8) — or accept indefinite Beta
