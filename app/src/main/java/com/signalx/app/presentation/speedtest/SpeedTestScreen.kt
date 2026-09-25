package com.signalx.app.presentation.speedtest

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalx.app.ui.theme.SignalX
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

// ─── State ────────────────────────────────────────────────────────────────────

private enum class Phase { IDLE, PING, DOWNLOAD, UPLOAD, DONE, ERROR }

private data class SpeedResult(
    val ping: Double? = null,
    val download: Double? = null,
    val upload: Double? = null
)

private val FillStart = Color(0xFFFF2D55)
private val FillEnd   = Color(0xFFFF6B35)
private val FillMid   = Color(0xFFFF4560)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SpeedTestScreen() {
    val sx = SignalX.colors
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var result by remember { mutableStateOf(SpeedResult()) }
    val liveSpeed = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun runTest() {
        phase = Phase.PING
        result = SpeedResult()
        scope.launch {
            try {
                liveSpeed.snapTo(0f)

                // 1. Ping
                val ping = withContext(Dispatchers.IO) { measurePing() }
                result = result.copy(ping = ping)

                // 2. Download
                phase = Phase.DOWNLOAD
                val dl = withContext(Dispatchers.IO) {
                    measureDownload { spd ->
                        scope.launch(Dispatchers.Main) {
                            liveSpeed.animateTo(spd.toFloat(), tween(250, easing = LinearEasing))
                        }
                    }
                }
                result = result.copy(download = dl)
                liveSpeed.animateTo(0f, tween(450))

                // 3. Upload
                phase = Phase.UPLOAD
                val ul = withContext(Dispatchers.IO) {
                    measureUpload { spd ->
                        scope.launch(Dispatchers.Main) {
                            liveSpeed.animateTo(spd.toFloat(), tween(250, easing = LinearEasing))
                        }
                    }
                }
                result = result.copy(upload = ul)
                liveSpeed.animateTo(0f, tween(450))

                phase = Phase.DONE
            } catch (_: Exception) {
                liveSpeed.animateTo(0f, tween(300))
                phase = Phase.ERROR
            }
        }
    }

    val canStart = phase == Phase.IDLE || phase == Phase.DONE || phase == Phase.ERROR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sx.background)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SPEED\nTEST",
            color = sx.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speedometer
            SpeedometerGauge(
                modifier = Modifier
                    .weight(0.55f)
                    .aspectRatio(1f),
                speed = liveSpeed.value,
                ping = result.ping,
                phase = phase
            )

            // Stat cards
            Column(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedCard(
                    label = "Download",
                    value = result.download,
                    unit = "Mbps",
                    iconChar = "↑",
                    active = phase == Phase.DOWNLOAD
                )
                SpeedCard(
                    label = "Upload",
                    value = result.upload,
                    unit = "Mbps",
                    iconChar = "↓",
                    active = phase == Phase.UPLOAD
                )
                PingCard(ping = result.ping, active = phase == Phase.PING)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Phase label
        val statusText = when (phase) {
            Phase.PING     -> "Measuring ping…"
            Phase.DOWNLOAD -> "Testing download…"
            Phase.UPLOAD   -> "Testing upload…"
            Phase.DONE     -> "Test complete"
            Phase.ERROR    -> "Test failed — check your connection"
            Phase.IDLE     -> ""
        }
        if (statusText.isNotEmpty()) {
            Text(
                text = statusText,
                color = if (phase == Phase.ERROR) sx.bad else sx.textMuted,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // Start / restart button
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(if (canStart) FillMid else sx.surfaceHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (canStart) {
                IconButton(onClick = { runTest() }, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Refresh, "Start test", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            } else {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ─── Speedometer ──────────────────────────────────────────────────────────────

@Composable
private fun SpeedometerGauge(
    modifier: Modifier = Modifier,
    speed: Float,
    ping: Double?,
    phase: Phase
) {
    val sx = SignalX.colors
    val textMeasurer = rememberTextMeasurer()
    val maxSpeed = 160f
    val arcStart = 150f
    val arcSweep = 240f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(cx, cy) * 0.80f
            val trackW = r * 0.09f
            val tl = Offset(cx - r, cy - r)
            val sz = Size(r * 2, r * 2)

            // Glow background
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(FillMid.copy(alpha = 0.08f * (speed / maxSpeed).coerceIn(0f, 1f)), Color.Transparent),
                    center = Offset(cx, cy), radius = r * 1.1f
                ),
                radius = r * 1.1f, center = Offset(cx, cy)
            )

            // Track arc
            drawArc(
                color = sx.outline, startAngle = arcStart, sweepAngle = arcSweep,
                useCenter = false, topLeft = tl, size = sz,
                style = Stroke(trackW, cap = StrokeCap.Round)
            )

            // Fill arc
            val frac = (speed / maxSpeed).coerceIn(0f, 1f)
            if (frac > 0.01f) {
                drawArc(
                    brush = Brush.linearGradient(
                        listOf(FillStart, FillEnd),
                        start = Offset(tl.x, cy),
                        end = Offset(tl.x + sz.width, cy)
                    ),
                    startAngle = arcStart, sweepAngle = arcSweep * frac,
                    useCenter = false, topLeft = tl, size = sz,
                    style = Stroke(trackW, cap = StrokeCap.Round)
                )
            }

            // Tick marks + labels
            val labels = listOf(0, 20, 40, 60, 80, 100, 120, 140, 160)
            labels.forEachIndexed { idx, value ->
                val frac2 = idx / (labels.size - 1).toFloat()
                val angleDeg = arcStart + arcSweep * frac2
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val tickOuter = r * 1.05f
                val tickInner = r * 0.93f
                drawLine(
                    color = sx.textMuted.copy(alpha = 0.5f),
                    start = Offset(cx + cosA * tickInner, cy + sinA * tickInner),
                    end = Offset(cx + cosA * tickOuter, cy + sinA * tickOuter),
                    strokeWidth = 1.5f
                )

                // Label text
                val labelR = r * 1.22f
                val textLayout = textMeasurer.measure(
                    AnnotatedString(value.toString()),
                    style = TextStyle(
                        fontSize = (r * 0.10f).sp,
                        color = sx.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
                drawText(
                    textLayout,
                    topLeft = Offset(
                        cx + cosA * labelR - textLayout.size.width / 2f,
                        cy + sinA * labelR - textLayout.size.height / 2f
                    )
                )
            }
        }

        // Center overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val displayVal = when {
                speed > 0.5f -> "%.1f".format(speed)
                phase == Phase.IDLE || phase == Phase.DONE -> "--"
                else -> "…"
            }
            Text(
                text = displayVal,
                color = sx.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Text("Mbps", color = sx.textMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            if (ping != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ping  ${ping.toLong()} ms",
                    color = FillMid,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Stat cards ───────────────────────────────────────────────────────────────

@Composable
private fun SpeedCard(
    label: String,
    value: Double?,
    unit: String,
    iconChar: String,
    active: Boolean
) {
    val sx = SignalX.colors
    Surface(
        color = sx.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(iconChar, color = FillMid, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(label, color = sx.textMuted, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(4.dp))
            if (active && value == null) {
                CircularProgressIndicator(
                    color = FillMid,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Text(
                    text = if (value != null) "%.1f".format(value) else "--",
                    color = sx.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(unit, color = sx.textMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun PingCard(ping: Double?, active: Boolean) {
    val sx = SignalX.colors
    Surface(
        color = sx.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("〜", color = FillMid, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Ping", color = sx.textMuted, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(4.dp))
            if (active && ping == null) {
                CircularProgressIndicator(color = FillMid, strokeWidth = 1.5.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(4.dp))
            } else {
                Text(
                    text = if (ping != null) ping.toLong().toString() else "--",
                    color = sx.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("ms", color = sx.textMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
        }
    }
}

// ─── Network measurement ──────────────────────────────────────────────────────

private fun measurePing(): Double {
    val samples = LongArray(5)
    for (i in samples.indices) {
        val t0 = System.currentTimeMillis()
        try {
            val conn = URL("https://1.1.1.1").openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.instanceFollowRedirects = false
            conn.connect()
            conn.responseCode
            samples[i] = System.currentTimeMillis() - t0
            conn.disconnect()
        } catch (_: Exception) {
            samples[i] = 9_999L
        }
        Thread.sleep(100)
    }
    return samples.sorted().drop(1).dropLast(1).average()
}

private fun measureDownload(onProgress: (Double) -> Unit): Double {
    val conn = URL("https://speed.cloudflare.com/__down?bytes=25000000")
        .openConnection() as HttpURLConnection
    conn.connectTimeout = 10_000
    conn.readTimeout   = 30_000
    conn.connect()

    val t0 = System.currentTimeMillis()
    var total = 0L
    var lastReport = t0
    val buf = ByteArray(65_536)
    val stream = conn.inputStream
    try {
        while (true) {
            val n = stream.read(buf)
            if (n == -1) break
            total += n
            val now = System.currentTimeMillis()
            if (now - lastReport >= 300L && now - t0 > 500L) {
                val mbps = total * 8.0 / ((now - t0) / 1000.0) / 1_000_000.0
                onProgress(mbps)
                lastReport = now
            }
        }
    } finally {
        stream.close()
        conn.disconnect()
    }
    val elapsed = (System.currentTimeMillis() - t0) / 1000.0
    return total * 8.0 / elapsed / 1_000_000.0
}

private fun measureUpload(onProgress: (Double) -> Unit): Double {
    val size = 10 * 1024 * 1024 // 10 MB
    val payload = ByteArray(size) { (it and 0xFF).toByte() }

    val conn = URL("https://speed.cloudflare.com/__up")
        .openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setFixedLengthStreamingMode(size)
    conn.setRequestProperty("Content-Type", "application/octet-stream")
    conn.connectTimeout = 10_000
    conn.readTimeout   = 30_000
    conn.connect()

    val t0 = System.currentTimeMillis()
    var sent = 0
    var lastReport = t0
    val chunk = 65_536
    val out = conn.outputStream
    try {
        while (sent < size) {
            val end = minOf(sent + chunk, size)
            out.write(payload, sent, end - sent)
            sent = end
            val now = System.currentTimeMillis()
            if (now - lastReport >= 300L && now - t0 > 500L) {
                val mbps = sent * 8.0 / ((now - t0) / 1000.0) / 1_000_000.0
                onProgress(mbps)
                lastReport = now
            }
        }
        out.flush()
    } finally {
        out.close()
        try { conn.responseCode } catch (_: Exception) {}
        conn.disconnect()
    }
    val elapsed = (System.currentTimeMillis() - t0) / 1000.0
    return sent * 8.0 / elapsed / 1_000_000.0
}
