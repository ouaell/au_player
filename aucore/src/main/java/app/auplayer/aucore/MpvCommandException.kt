package app.auplayer.aucore

/** Thrown when a native mpv command fails (bridge implementations). */
class MpvCommandException(message: String) : RuntimeException(message)
