package app.auplayer.player

/** PiP aspect math (MS-023); entering PiP itself needs a device. */
object PipCalc {
    fun aspect(videoW: Int, videoH: Int): Pair<Int, Int> {
        if (videoW <= 0 || videoH <= 0) return Pair(DEFAULT_W, DEFAULT_H)
        val divisor = gcd(videoW, videoH)
        return Pair(videoW / divisor, videoH / divisor)
    }

    private tailrec fun gcd(first: Int, second: Int): Int {
        return if (second == 0) first else gcd(second, first % second)
    }
}

private const val DEFAULT_W = 16
private const val DEFAULT_H = 9
