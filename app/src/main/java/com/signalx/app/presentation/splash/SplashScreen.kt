package com.signalx.app.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.signalx.app.ui.theme.ThemeMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Cyan = Color(0xFF22D3EE)
private val RingBlue = Color(0xFF22D3EE)

/**
 * Animated launch screen.
 *
 * Timeline (~2.9 s):
 *   0.0s  glow fades in, two signal rings draw themselves
 *   0.25s "\" stroke of the X draws, then "/" weaves over it
 *   1.05s 5G badge pops in with a bounce + ripple
 *   1.15s "SignalX5G" wordmark slides up, tagline fades in
 *   2.5s  everything fades out and [onFinished] is called
 */
@Composable
fun SplashScreen(themeMode: ThemeMode = ThemeMode.DARK, onFinished: () -> Unit) =
    SignalXSplash(themeMode = themeMode, onFinished = onFinished)

@Composable
fun SignalXSplash(themeMode: ThemeMode = ThemeMode.DARK, onFinished: () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
    }
    val Bg    = if (isDark) Color(0xFF05070A) else Color(0xFFF0F5FF)
    val Muted = if (isDark) Color(0xFF8A94A6) else Color(0xFF64748B)
    val titleBase = if (isDark) Color.White else Color(0xFF0F172A)
    val glow = remember { Animatable(0f) }
    val outerRing = remember { Animatable(0f) }
    val innerRing = remember { Animatable(0f) }
    val strokeA = remember { Animatable(0f) }
    val strokeB = remember { Animatable(0f) }
    val badge = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { glow.animateTo(1f, tween(700)) }
            launch { outerRing.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
            launch {
                delay(120)
                innerRing.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
            launch {
                delay(250)
                strokeA.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                strokeB.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            }
            launch {
                delay(1050)
                launch { ripple.animateTo(1f, tween(750, easing = LinearOutSlowInEasing)) }
                badge.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            launch {
                delay(1150)
                word.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
            launch {
                delay(1450)
                tag.animateTo(1f, tween(500))
            }
        }
        delay(500) // hold the finished logo
        exit.animateTo(1f, tween(350))
        onFinished()
    }

    val title = remember(isDark) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = titleBase, fontWeight = FontWeight.Medium)) { append("Signal") }
            withStyle(SpanStyle(color = Color(0xFF3FB4FF), fontWeight = FontWeight.ExtraBold)) { append("X") }
            withStyle(
                SpanStyle(
                    color = Cyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    baselineShift = BaselineShift.Superscript
                )
            ) { append("5G") }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .graphicsLayer {
                val e = exit.value
                alpha = 1f - e
                scaleX = 1f + 0.06f * e
                scaleY = 1f + 0.06f * e
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SignalXAnimatedMark(
                glow = { glow.value },
                outerRing = { outerRing.value },
                innerRing = { innerRing.value },
                strokeA = { strokeA.value },
                strokeB = { strokeB.value },
                badge = { badge.value },
                ripple = { ripple.value },
                backgroundColor = Bg
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 40.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = word.value
                    translationY = (1f - word.value) * 24.dp.toPx()
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Modern 5G Network Dashboard",
                color = Muted,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer { alpha = tag.value }
            )
        }
    }
}

/** The logo, drawn on a 1024-unit design grid so it matches logo.svg exactly. */
@Composable
private fun SignalXAnimatedMark(
    glow: () -> Float,
    outerRing: () -> Float,
    innerRing: () -> Float,
    strokeA: () -> Float,
    strokeB: () -> Float,
    badge: () -> Float,
    ripple: () -> Float,
    backgroundColor: Color = Color(0xFF05070A),
    modifier: Modifier = Modifier
) {
    val markSize = 260.dp
    val k = markSize.value / 1024f // dp per design unit

    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "spin"
    )

    Box(modifier = modifier.size(markSize)) {

        // Layer 1: glow + signal rings
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width / 1024f
            val c = Offset(512f * s, 530f * s)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.20f * glow()), Color.Transparent),
                    center = c,
                    radius = 420f * s
                ),
                radius = 420f * s,
                center = c
            )
            dashedRing(c, 415f * s, 0.16f, outerRing(), -spin * 0.6f, 12f * s, 108f * s, 109f * s)
            dashedRing(c, 345f * s, 0.34f, innerRing(), spin, 12f * s, 138f * s, 79f * s)
        }

        // Layer 2: the woven X (offscreen layer so BlendMode.Clear only cuts the X)
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val s = size.width / 1024f
            val a0 = Offset(347f * s, 365f * s)
            val a1 = Offset(677f * s, 695f * s)
            val b0 = Offset(347f * s, 695f * s)
            val b1 = Offset(677f * s, 365f * s)
            val w = 96f * s

            val pa = strokeA()
            if (pa > 0f) {
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF8CF6FF), Color(0xFF2F6BFF)), start = a0, end = a1
                    ),
                    start = a0,
                    end = lerp(a0, a1, pa),
                    strokeWidth = w,
                    cap = StrokeCap.Round
                )
            }
            val pb = strokeB()
            if (pb > 0f) {
                val tip = lerp(b0, b1, pb)
                // gap around the "/" stroke -> over/under weave
                drawLine(
                    color = Color.Black,
                    start = b0,
                    end = tip,
                    strokeWidth = 132f * s,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Clear
                )
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF2340C8), Color(0xFF1E9BD8), Color(0xFF12E6E0)),
                        start = b0,
                        end = b1
                    ),
                    start = b0,
                    end = tip,
                    strokeWidth = w,
                    cap = StrokeCap.Round
                )
            }
        }

        // Layer 3: ripple from the badge
        Canvas(Modifier.fillMaxSize()) {
            val t = ripple()
            if (t > 0f && t < 1f) {
                val s = size.width / 1024f
                drawCircle(
                    color = Cyan.copy(alpha = 0.5f * (1f - t)),
                    radius = (96f + 150f * t) * s,
                    center = Offset(690f * s, 352f * s),
                    style = Stroke(width = 5f * s)
                )
            }
        }

        // Layer 4: 5G badge
        val badgeFont = with(LocalDensity.current) { (94f * k).dp.toSp() }
        Box(
            modifier = Modifier
                .offset(x = ((690f - 104f) * k).dp, y = ((352f - 104f) * k).dp)
                .size((208f * k).dp)
                .graphicsLayer {
                    val b = badge()
                    scaleX = b
                    scaleY = b
                    alpha = b.coerceIn(0f, 1f)
                }
                .background(backgroundColor, CircleShape)
                .padding((8f * k).dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF00F5DA), Color(0xFF00B4E6))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "5G",
                color = Color(0xFF03141A),
                fontSize = badgeFont,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun DrawScope.dashedRing(
    center: Offset,
    radius: Float,
    alpha: Float,
    progress: Float,
    rotation: Float,
    stroke: Float,
    on: Float,
    off: Float
) {
    if (progress <= 0f) return
    rotate(rotation, center) {
        drawArc(
            color = RingBlue.copy(alpha = alpha),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off))
            )
        )
    }
}
