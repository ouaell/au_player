package app.auplayer.enhance

/**
 * Native crash isolation policy (MS-073, §17.7-1): inference runs in the
 * `:enhance` process (see enhance/src/main/AndroidManifest.xml); after two
 * native crashes the feature disables itself on the device with a message.
 * Watchdog timeouts and the bound-service plumbing are device-verified.
 */
class IsolationPolicy {
    var crashes: Int = 0
        private set
    var disabled: Boolean = false
        private set

    /** Returns true once the feature must stay disabled. */
    fun onNativeCrash(): Boolean {
        crashes++
        if (crashes >= MAX_CRASHES) disabled = true
        return disabled
    }

    fun reset() {
        crashes = 0
        disabled = false
    }
}

private const val MAX_CRASHES = 2
