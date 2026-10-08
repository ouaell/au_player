# Play internal-track upload (MS-061', pre-launch reports without Firebase)

CI (`release.yml`, job `play-upload`, runs only when the
`PLAY_SERVICE_JSON` secret exists) uploads the signed release AAB to the
internal track via the Play Developer API. Every upload is then exercised
automatically on real devices in Google's lab (crashes, ANRs, performance,
accessibility, security) — results land in Play Console → Pre-launch report
and are copied to `docs/prelaunch.md` by whoever reads them.

## Owner setup (one time)

1. Play developer account (also required to publish at all).
2. Play Console → Setup → API access → create/link a Google Cloud project,
   create a service account, grant it (at least) "Release to internal track"
   rights, download the JSON key.
3. GitHub repo → Settings → Secrets → `PLAY_SERVICE_JSON` = the JSON key
   contents. Also complete `docs/signing.md` (H8) so the AAB is signed.
4. Dispatch the `release` workflow (or push a tag); the `play-upload` job
   picks up the AAB artifact and uploads it.

## No secret? Manual path (≤ 6 clicks)

1. Dispatch `release` workflow → download the `direct-release-aab` artifact.
2. Play Console → the app → Internal testing → Create release.
3. Upload the AAB → review → Start rollout to internal testing.
4. Wait for the pre-launch report → paste findings into `docs/prelaunch.md`.
5. Each finding becomes a failing E-test (MS-060 harness), then a fix.

## Report ingestion

`docs/prelaunch.md` (created on first report): device, Android version,
finding, severity, linked E-test, fix commit. Treat every finding as a
failing test: reproduce in E-tests, fix, re-upload.
