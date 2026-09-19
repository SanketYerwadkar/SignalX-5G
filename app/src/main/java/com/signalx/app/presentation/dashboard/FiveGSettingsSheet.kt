package com.signalx.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor

/**
 * The honesty gate. Android routes preferred-network changes through system
 * settings, so we say that plainly instead of implying the app can do it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiveGSettingsSheet(onDismiss: () -> Unit, onOpen: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SxColor.Outline) }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(Sx.s6).padding(bottom = Sx.s8),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Set Network to 5G", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Sx.s3))
            Text(
                "This opens your phone's hidden Phone Info (Radio Info) screen. " +
                    "From there, find 'Set Preferred Network Type' and select 'NR only' or 'NR/LTE' to force 5G.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Sx.s6))
            Button(
                onClick = onOpen,
                shape = Sx.rPill,
                modifier = Modifier.fillMaxWidth().heightIn(min = Sx.touchMin)
            ) { Text("Open Phone Info") }
            Spacer(Modifier.height(Sx.s2))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = Sx.touchMin)
            ) { Text("Cancel", color = SxColor.TextSecondary) }
        }
    }
}
