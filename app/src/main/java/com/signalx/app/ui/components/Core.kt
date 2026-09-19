package com.signalx.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.signalx.app.domain.model.SignalQuality
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor
import com.signalx.app.ui.theme.SxMono

/** Section label: 11sp, 1.2sp tracking, uppercase. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.padding(start = Sx.s1, bottom = Sx.s2)
)

/** Base card: 16dp radius, hairline outline, no drop shadow. */
@Composable
fun SxCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(Sx.rMd)
        .background(MaterialTheme.colorScheme.surface)
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), Sx.rMd)
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(Sx.s4),
        content = content
    )
}

/** Label/value row. A null value renders the unavailability copy, never a fake number. */
@Composable
fun InfoRow(label: String, value: String?, mono: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .semantics { contentDescription = "$label: ${value ?: "not available"}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Sx.s4))
        Text(
            value ?: "Not available",
            style = if (mono) SxMono else MaterialTheme.typography.bodyLarge,
            color = if (value == null) SxColor.TextTertiary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Status chip - colour AND text, so colour is never the only signal carrier. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(Sx.rPill)
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), Sx.rPill)
            .padding(horizontal = Sx.s3, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(Sx.s2))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Five-bar meter that animates its heights on refresh. Bars + dBm + text label. */
@Composable
fun SignalBars(quality: SignalQuality, modifier: Modifier = Modifier, barHeight: Int = 28) {
    val color = quality.color()
    Row(modifier.height(barHeight.dp), verticalAlignment = Alignment.Bottom) {
        (1..5).forEach { i ->
            val filled = i <= quality.bars
            val h by animateDpAsState(
                targetValue = ((barHeight * (0.34f + 0.165f * i))).dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bar$i"
            )
            Box(
                Modifier
                    .padding(end = 3.dp)
                    .width(6.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (filled) color else SxColor.Outline)
            )
        }
    }
}

@Composable
fun SignalQuality.color(): Color = when (this) {
    SignalQuality.EXCELLENT -> SxColor.Success
    SignalQuality.GOOD -> SxColor.Cyan
    SignalQuality.FAIR -> SxColor.Warning
    SignalQuality.POOR -> SxColor.Danger
    SignalQuality.UNKNOWN -> SxColor.TextTertiary
}

/** Expandable card so the details screen stays scannable. */
@Composable
fun ExpandableCard(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    SxCard {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sx.touchMin)
                .clickable { expanded = !expanded }
                .semantics { contentDescription = "$title, ${if (expanded) "expanded" else "collapsed"}" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.rotate(rotation), tint = SxColor.TextSecondary)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(Sx.s2))
                Divider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(Sx.s2))
                content()
            }
        }
    }
}

/** Shared empty / error / permission state. */
@Composable
fun StateMessage(
    title: String,
    body: String,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(Sx.s6),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SignalXMark(size = 44)
        Spacer(Modifier.height(Sx.s5))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Sx.s2))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(Sx.s6))
            Button(onClick = onPrimary, shape = Sx.rPill, modifier = Modifier.heightIn(min = Sx.touchMin)) {
                Text(primaryLabel)
            }
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel, color = SxColor.TextSecondary) }
        }
    }
}
