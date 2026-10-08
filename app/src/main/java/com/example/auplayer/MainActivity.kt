package com.example.auplayer

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.auplayer.player.PipCalc
import com.example.auplayer.ui.theme.AuTheme

/** Single-activity (§2). Predictive back via enableOnBackInvokedCallback (manifest). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AuTheme { AuNav() } }
    }

    /** PiP entry (MS-023); behavior verified on device (Day-1 gate T3). */
    fun enterAuPip(videoW: Int, videoH: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val (width, height) = PipCalc.aspect(videoW, videoH)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(width, height))
            .build()
        enterPictureInPictureMode(params)
    }
}
