package com.example.auplayer.player

/** UI talks only to this interface (§4). Full state machine lands in MS-011. */
interface PlayerController {
    fun play()
    fun pause()
}

/** Fake for unit tests (§4: every interface has a fake). */
class FakePlayerController : PlayerController {
    var plays = 0
    var pauses = 0
    override fun play() { plays++ }
    override fun pause() { pauses++ }
}
