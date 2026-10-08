package com.example.auplayer.monetization

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdProviderTest {
    @Test
    fun `noop never shows`() = runBlocking {
        val ads: AdProvider = NoOpAdProvider()
        ads.init()
        assertFalse(ads.isReady)
        assertEquals(AdResult.Unavailable, ads.showInterstitial())
        assertEquals(AdResult.Unavailable, ads.showRewarded())
    }

    @Test
    fun `consent starts unknown`() = runBlocking {
        assertEquals(ConsentStatus.UNKNOWN, StubConsentManager().requestConsent())
    }
}
