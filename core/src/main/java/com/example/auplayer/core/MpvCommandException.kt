package com.example.auplayer.core

/** Thrown when a native mpv command fails (bridge implementations). */
class MpvCommandException(message: String) : RuntimeException(message)
