# R8 rules (MS-053). App code is reflection-free; Compose/Media3/DataStore/Room
# ship their own consumer rules. Only native entry points need keeps.

# NCNN JNI holder (methods bind when the native lib lands with the build).
-keep class com.example.auplayer.enhance.JniNcnnBridge { *; }

# Future self-built mpv wrapper (package recorded in docs/decisions.md when
# the native build lands in MS-053+): keep all JNI entry points and any
# class/method looked up from native code (verified by tools in MS-062).
-keepclasseswithmembernames class * {
    native <methods>;
}
