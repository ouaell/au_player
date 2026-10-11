# mpv-android-engine

Options: `vo=gpu gpu-context=android opengl-es=yes hwdec=mediacodec-copy`
(`mediacodec_embed` forbidden: kills shaders + software fallback),
`ao=audiotrack,opensles video-sync=audio`, shader-cache dir set, `tls-verify=yes`,
bundled fonts dir, `scale=lanczos dscale=mitchell correct-downscaling=yes linear-downscaling=no`
under enhancement else bilinear. Surface: `wid` = Surface jobject address parked on "0"
across swaps (S1-001), global ref lives for the whole mpv use, `loadfile` gated on armed
surface, all bridge calls on one single-thread dispatcher. Shaders: patched `//!WHEN`
removal, apply via `change-list glsl-shaders set`, "Preparing enhancement…" + shader cache,
probe via forced redraws + `vo-passes`, reactive governor (drops≥3 ×3 samples or thermal≥MODERATE,
never step up), hold-to-compare snapshot/restore. Subtitles: bundled OFL Latin+Arabic+CJK,
`sub-codepage` for legacy encodings, sidecar discovery under scoped storage.
