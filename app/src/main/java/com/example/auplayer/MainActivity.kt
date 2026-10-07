package com.example.auplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.auplayer.ui.theme.AuTheme

/** Single-activity (§2). Predictive back via enableOnBackInvokedCallback (manifest). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AuTheme { AuNav() } }
    }
}
