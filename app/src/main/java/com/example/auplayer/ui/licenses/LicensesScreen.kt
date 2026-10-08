package com.example.auplayer.ui.licenses

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.auplayer.core.LicenseEntry

/** Licenses screen (MS-051); render coverage is T2-tested. */
@Composable
fun LicensesScreen(entries: List<LicenseEntry>) {
    LazyColumn {
        items(entries) { entry ->
            Text(text = entry.name + " — " + entry.license)
            Text(text = entry.url)
        }
    }
}
