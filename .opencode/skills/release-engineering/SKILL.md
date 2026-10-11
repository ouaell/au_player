# release-engineering

`tools/gen-keystore.sh` (owner runs once; prints secret names + backup instructions);
keystore backed up to a local path, path printed in report. Release CI: signed AAB + APK,
versionCode from run number, R8 with keep rules for all JNI classes (`native` methods,
callbacks, `MpvEvents*`, `LibmpvBridge*`, `JniNcnnBridge`), `bundletool validate` + `apksigner
verify`, targetSdk 36, applicationId `app.auplayer` (never `com.example.*`), 64-bit `.so`
16KB alignment check, ABI splits if it meaningfully shrinks downloads.
