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

## 2026-10-11 — scan flow failing, tabs green
- Maestro tabs flow passes all 5 tabs. Scan flow taps succeed but "Added 1 series" never appears.
- Root causes so far: (1) seed CBZ pushed to wrong dir (adb space-split; fixed + verified with ls);
  (2) Local base is /sdcard/AU Player/local (uses app_name). Added scanLocalSource logcat
  breadcrumbs (listed=/added=) to discriminate empty-dir vs denied vs exception.
