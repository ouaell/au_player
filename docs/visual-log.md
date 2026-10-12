# visual-log (every entry from a screenshot I actually viewed)

## 2026-10-11 — legacy contact sheet (run 38120789006)
- Left: home screen = 3 default purple Material buttons on black ("Open file/URL/Web page").
  Verdict: textbook AI-slop; dies with the old UI (mihon-line replaces it). No fix (legacy).
- Right: test-pattern video frame rendering (color bars + circle). The engine pipeline works;
  the chrome around it (player UI) is legacy. No fix (legacy).

## 2026-10-11 — Mihon More tab (mihon-visual 00-diag)
- Real Mihon More screen with OUR bottom nav (Library/Videos/History/Web/More) — rebrand visible.
- Wrong: header still shows the Mihon み mark (LogoHeader uses ic_mihon). Fixed: new amber
  triangle mark (ic_au_mark) + LogoHeader swap.
- The screen is otherwise indistinguishable from Mihon (it is Mihon) — correct for Phase B.

## 2026-10-11 — Library with scanned Demo series (mihon-visual 38180934854)
- Library tab shows the "Demo" card (local-folder badge) after overflow → "Scan local folder".
  The scan action works end-to-end (seed CBZ → scan → library entry). Bottom nav rebranded.
- Earlier scan-assert failures were snackbar exact-text timing, not function (reader flow proved
  Demo appears).
- Earlier root causes, both fixed: (1) seed CBZ pushed to wrong dir (adb space-split; fixed +
  verified with ls); (2) correct local base is /sdcard/AU Player/local (uses app_name).

## 2026-10-12 — "0 chapters" mystery (mihon-visual 38182752074)
- MangaScreen for the scanned Demo shows title/author/Local-source/"In library" — but chapter list
  shows "0 chapters" at screenshot time. Gave the chapter assert a 60 s timeout (rejected: this
  Maestro version has no per-command timeout); re-enter strategy reads DB-cached chapters.

## 2026-10-12 — root cause: shell-pushed files invisible to the app (scoped storage)
- In-app File probe proved it: base dir lists [local, downloads], Demo/ exists and is a dir,
  but demoList=[] while adb-shell ls shows ch1.cbz. Dir metadata readable, children of a
  shell-owned file not — classic scoped-storage ownership. getChapterList silently [].
- MANAGE_EXTERNAL_STORAGE via appops does NOT apply (undeclared; absent from appops get).
- Product is correct as designed (real users pick a SAF tree in onboarding StorageStep).
  CI fix: storage_dir pref → app-owned internal dir (file://…/files/citest), CBZ via run-as
  base64-decode. No permission needed at all.
- Maestro shots now collected from $HOME/.maestro (numbered 1.png…); repo-bundled PNGs excluded.

## 2026-10-12 — reader flow GREEN, series screen proven (mihon-visual 38192631370)
- Maestro reader flow passes end to end: scan → Demo → series ("1 chapter", ch1 row, Today,
  Start button, test-pattern cover via our ImageDecoder port) → tap ch1 → reader opens, no crash.
- BUT the reader-page screenshot is pure black (10 KB). Either screenshot-at-tap timing or a real
  blank-page render bug. Added a hard gate: adb shot after settle + stdlib pixel variance
  (var > 30), failing the job on a uniform frame. Run 38193966946 decides.
