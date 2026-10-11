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
  Demo appears). Chapter header shows plural "1 chapter", not "Chapters" — flow fixed.
- Earlier root causes, both fixed: (1) seed CBZ pushed to wrong dir (adb space-split; fixed +
  verified with ls); (2) correct local base is /sdcard/AU Player/local (uses app_name).
