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

# Native-to-Java callbacks (MS-063 proof: R8 renamed these and
# GetMethodID("onEvent") failed on first launch of the minified build).
# The MpvEventCallback interface, its onEvent, and the LibmpvBridge holder
# of the anonymous implementation must keep their names.
-keep class com.example.auplayer.player.MpvEvents { *; }
-keep class com.example.auplayer.player.MpvEvents$MpvEventCallback { *; }
-keep class com.example.auplayer.player.LibmpvBridge { *; }
-keep class com.example.auplayer.player.LibmpvBridge$* { *; }
