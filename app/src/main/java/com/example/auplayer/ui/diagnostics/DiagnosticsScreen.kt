package com.example.auplayer.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.auplayer.core.Diag
import com.example.auplayer.core.buildDiagnosticsReport

/** Diagnostics screen (MS-025); self-test + reactive entries land in MS-065. */
@Composable
fun DiagnosticsScreen(version: String, onShare: (String) -> Unit) {
    val report = buildDiagnosticsReport(version, Diag.entries())
    Column {
        Text(text = report)
        Button(onClick = { onShare(report) }) {
            Text(text = "Share report")
        }
    }
}
