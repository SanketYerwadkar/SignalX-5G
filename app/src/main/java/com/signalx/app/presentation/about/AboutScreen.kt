package com.signalx.app.presentation.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.signalx.app.ui.components.BrandTagline
import com.signalx.app.ui.components.SignalXLogo
import com.signalx.app.ui.components.SxCard
import com.signalx.app.ui.theme.Sx
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit, onFeedback: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Sx.s6),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Sx.s6))
            com.signalx.app.ui.components.SignalXAppIcon(size = 80.dp)
            Spacer(Modifier.height(Sx.s3))
            SignalXLogo(fontSize = 32)
            Spacer(Modifier.height(Sx.s3))
            BrandTagline()
            Spacer(Modifier.height(Sx.s2))
            Text("Version $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Sx.s6))
            Text(
                "View mobile network information and quickly access Android network configuration.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Sx.s8))
            SxCard {
                TextButton(onPrivacy, Modifier.fillMaxWidth()) { Text("Privacy Policy") }
                TextButton(onLicenses, Modifier.fillMaxWidth()) { Text("Open Source Licenses") }
                TextButton(onFeedback, Modifier.fillMaxWidth()) { Text("Contact / Feedback") }
            }
        }
    }
}

/** Placeholder scaffolding for the two legal screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPageScreen(title: String, body: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Sx.s5)) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
