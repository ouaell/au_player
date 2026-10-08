package com.example.auplayer.monetization

/**
 * Monetization seam (MS-050, §11). The core never depends on ads: the
 * default binding is [NoOpAdProvider] and the real network lands behind a
 * build flag in a later task. Placements (native slots, ~15 min
 * interstitial cap, rewarded) are enforced by callers, never here.
 */
interface AdProvider {
    suspend fun init()
    val isReady: Boolean
    suspend fun showInterstitial(): AdResult
    suspend fun showRewarded(): AdResult
}

sealed interface AdResult {
    data object Shown : AdResult
    data object Dismissed : AdResult
    data object Unavailable : AdResult
    data object Earned : AdResult
}

/** Default: no ads, no network, every show is [AdResult.Unavailable]. */
class NoOpAdProvider : AdProvider {
    override suspend fun init() = Unit
    override val isReady: Boolean = false
    override suspend fun showInterstitial(): AdResult = AdResult.Unavailable
    override suspend fun showRewarded(): AdResult = AdResult.Unavailable
}

/** UMP consent state scaffold (MS-050); real UMP flow lands with the network. */
enum class ConsentStatus {
    UNKNOWN,
    REQUIRED,
    OBTAINED,
    NOT_REQUIRED
}

interface ConsentManager {
    suspend fun requestConsent(): ConsentStatus
}

class StubConsentManager : ConsentManager {
    override suspend fun requestConsent(): ConsentStatus = ConsentStatus.UNKNOWN
}
