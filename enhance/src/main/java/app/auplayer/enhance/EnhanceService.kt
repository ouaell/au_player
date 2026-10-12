package app.auplayer.enhance

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Isolated inference host (§17.7-1, MS-073). Binding + watchdog = device work. */
class EnhanceService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
