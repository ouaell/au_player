# visual-qa-loop

Run `.github/workflows/visual.yml` (dispatch, optional `scenarios` input), then:
`gh run watch <id>` → `gh run download <id> -n visual-<run>` → open the ImageMagick
`montage` contact sheets and LOOK at every panel. Append 3–6 lines per screen to
`docs/visual-log.md` (what is on screen / what is wrong / what you fixed).
FAIL = empty or black content area, system-bars-only shot, default-looking Material
controls, unmirrored RTL, missing dark/light parity. Never pass a screen you did not view.
