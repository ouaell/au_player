package app.auplayer.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background-audio service skeleton (MS-022). Session binding to a real
 * media3 Player, notification wiring, and FGS start rules land with device
 * testing (T2/T3); focus/noisy policy itself is JVM-tested ([AudioFocusHandler]).
 */
class AuPlaybackService : MediaSessionService() {
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = null
}
