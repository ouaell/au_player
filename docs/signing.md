# Signing + keys (MS-053, owner action H8)

Release builds work without secrets (debug key). For shippable artifacts:

1. Generate ONE upload keystore (keep forever — direct-APK users need the
   same key for updates; with Play App Signing this is the *upload* key):
   `keytool -genkeypair -keystore au-upload.keystore -alias au -keyalg RSA -keysize 4096 -validity 10000`
2. Back it up encrypted OFFLINE (plus a second offline copy). Losing it for
   direct APKs means users must reinstall.
3. Add GitHub secrets: `AU_KEYSTORE_B64` (base64 of the keystore),
   `AU_KEYSTORE_PASSWORD`, `AU_KEY_ALIAS`, `AU_KEY_PASSWORD`.
4. Dispatch the `release` workflow; artifacts are signed when secrets exist.
5. Play Console: enroll in Play App Signing with this upload key; complete
   Data Safety, content rating, ads + foreground-service declarations (§13).
