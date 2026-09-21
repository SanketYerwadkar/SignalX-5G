package com.signalx.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalx.app.domain.model.ConnectionType
import com.signalx.app.domain.model.SimInfo
import com.signalx.app.ui.theme.SignalX
import com.signalx.app.ui.theme.SignalXTheme
import com.signalx.app.ui.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

data class NetworkUiState(
    val generation: String = "5G",
    val connected: Boolean = true,
    val operator: String = "Jio",
    val radio: String = "NR",
    val dbm: Int = -78,
    val sim: String = "SIM 1",
    val data: String = "Connected",
    val voice: String = "VoNR",
    val roaming: String = "Off"
)

fun SimInfo.toHomeNetworkUiState(): NetworkUiState {
    return NetworkUiState(
        generation = when (connectionType) {
            ConnectionType.NR_5G -> "5G"
            ConnectionType.LTE_4G -> "4G"
            ConnectionType.WCDMA_3G -> "3G"
            ConnectionType.GSM_2G -> "2G"
            ConnectionType.UNKNOWN -> "Unknown"
        },
        connected = connected,
        operator = operator ?: "Unknown",
        radio = radioTech.label,
        dbm = signalDbm ?: -120,
        sim = displayName,
        data = dataState.label,
        voice = voiceNetwork ?: "--",
        roaming = when (roaming) {
            true -> "On"
            false -> "Off"
            null -> "--"
        }
    )
}


/**
 * Home tab. Put it inside your existing Scaffold and pass the Scaffold's padding as [modifier].
 * (If you don't use Scaffold padding, add .statusBarsPadding() to the modifier.)
 */
@Composable
fun HomeScreen(
    state: NetworkUiState,
    onRefresh: () -> Unit,
    onSetTo5G: () -> Unit,
    onSetTo4G: () -> Unit,
    onOpen5GSettings: () -> Unit,
    onOpenSimSettings: () -> Unit,
    onOpenNetworkInfo: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = SignalX.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Header(onAbout)
        Spacer(Modifier.height(16.dp))

        HeroCard(state, onRefresh)
        Spacer(Modifier.height(16.dp))

        SetTo5GButton(onSetTo5G)
        Spacer(Modifier.height(12.dp))

        SetTo4GButton(onSetTo4G)
        Spacer(Modifier.height(28.dp))

        SectionTitle("Network details")
        Spacer(Modifier.height(12.dp))
        val details = listOf(
            "Operator" to state.operator,
            "Radio" to state.radio,
            "SIM" to state.sim,
            "Data" to state.data,
            "Voice" to state.voice,
            "Roaming" to state.roaming
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            details.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (label, value) ->
                        StatTile(label, value, Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))

        SectionTitle("Shortcuts")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(Icons.Rounded.Star, "5G settings", onOpen5GSettings, Modifier.weight(1f))
            ActionTile(Icons.Rounded.Phone, "SIM settings", onOpenSimSettings, Modifier.weight(1f))
            ActionTile(Icons.Rounded.List, "Network info", onOpenNetworkInfo, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- header ----------

@Composable
private fun Header(onAbout: () -> Unit) {
    val c = SignalX.colors
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.Medium)) { append("Signal") }
        withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.ExtraBold)) { append("X") }
        withStyle(
            SpanStyle(
                color = c.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift.Superscript
            )
        ) { append("5G") }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoMark(Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Text(wordmark, fontSize = 24.sp, letterSpacing = (-0.3).sp)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAbout) {
            Icon(Icons.Rounded.Info, contentDescription = "About", tint = c.textMuted)
        }
    }
}

/** Small version of the logo (same shapes as logo.svg). */
@Composable
private fun LogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 426f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val w = 96f * s
        drawLine(
            Brush.linearGradient(
                listOf(Color(0xFF22D3EE), Color(0xFF2F6BFF)),
                start = p(48f, 48f), end = p(378f, 378f)
            ),
            p(48f, 48f), p(378f, 378f), w, StrokeCap.Round
        )
        drawLine(
            Brush.linearGradient(
                listOf(Color(0xFF2340C8), Color(0xFF12E6E0)),
                start = p(48f, 378f), end = p(378f, 48f)
            ),
            p(48f, 378f), p(378f, 48f), w, StrokeCap.Round
        )
        drawCircle(
            Brush.linearGradient(listOf(Color(0xFF00F5DA), Color(0xFF00B4E6))),
            radius = 66f * s,
            center = p(372f, 54f)
        )
    }
}

// ---------- hero: gauge card ----------

@Composable
private fun HeroCard(state: NetworkUiState, onRefresh: () -> Unit) {
    val c = SignalX.colors
    val shape = RoundedCornerShape(32.dp)

    var spins by remember { mutableStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = spins * 360f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "refresh"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.heroTop, c.heroBottom)))
            .border(1.dp, c.outline, shape)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignalGauge(state.dbm, state.generation)
            Spacer(Modifier.height(8.dp))
            StatusChip(state.connected)
        }
        IconButton(
            onClick = { spins++; onRefresh() },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = c.accent,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun SignalGauge(dbm: Int, generation: String) {
    val c = SignalX.colors
    // -120 dBm (empty) .. -50 dBm (full)
    val target = ((dbm + 120) / 70f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "gauge"
    )
    val (quality, tint) = when {
        dbm >= -70 -> "Excellent" to c.good
        dbm >= -85 -> "Good" to c.good
        dbm >= -100 -> "Fair" to c.warn
        else -> "Poor" to c.bad
    }

    Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // soft glow behind the gauge
            drawCircle(
                Brush.radialGradient(
                    listOf(c.accent.copy(alpha = if (c.isDark) 0.16f else 0.10f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f
                ),
                radius = size.minDimension / 2f,
                center = center
            )
            // track
            drawArc(
                color = c.outline,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // value
            if (progress > 0f) {
                drawArc(
                    brush = Brush.horizontalGradient(listOf(c.accent, c.blue)),
                    startAngle = 150f,
                    sweepAngle = 240f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            // knob at the end of the value arc
            val angle = Math.toRadians((150f + 240f * progress).toDouble())
            val r = arcSize.width / 2f
            val knob = Offset(
                center.x + (r * cos(angle)).toFloat(),
                center.y + (r * sin(angle)).toFloat()
            )
            drawCircle(c.blue, radius = stroke * 0.62f, center = knob)
            drawCircle(Color.White, radius = stroke * 0.34f, center = knob)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                generation,
                color = c.textPrimary,
                fontSize = 60.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text("$dbm dBm", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(quality, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusChip(connected: Boolean) {
    val c = SignalX.colors
    val tint = if (connected) c.good else c.bad
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { alpha = if (connected) pulse else 1f }
                .background(tint, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (connected) "Connected" else "No connection",
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------- main action ----------

@Composable
private fun SetTo5GButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF2F5BFF))))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("5G", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Set network to 5G", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Opens your phone's network settings",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun SetTo4GButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SignalX.colors.surface)
            .border(1.dp, SignalX.colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(SignalX.colors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("4G", color = SignalX.colors.accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Set network to 4G", color = SignalX.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Opens your phone's mobile settings",
                color = SignalX.colors.textMuted,
                fontSize = 13.sp
            )
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = SignalX.colors.textMuted)
    }
}

// ---------- sections ----------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = SignalX.colors.textPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = SignalX.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .padding(16.dp)
    ) {
        Text(label, color = c.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = SignalX.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(c.accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = c.accent)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = c.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ---------- previews: check both themes in Android Studio ----------

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF05070A, heightDp = 900)
@Composable
private fun HomeDarkPreview() = SignalXTheme(ThemeMode.Dark) {
    HomeScreen(NetworkUiState(), {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF3F6FB, heightDp = 900)
@Composable
private fun HomeLightPreview() = SignalXTheme(ThemeMode.Light) {
    HomeScreen(NetworkUiState(), {}, {}, {}, {}, {}, {}, {})
}
