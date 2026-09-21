package com.signalx.app.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.signalx.app.ui.components.BrandTagline
import com.signalx.app.ui.components.SignalXLogo
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor
import kotlinx.coroutines.delay

/**
 * ~1.6s total: logo fades up, badge pops, one signal pulse, then auto-navigates.
 * Deliberately short - this is a utility, not an intro sequence.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(600), label = "fade")
    val scale by animateFloatAsState(if (visible) 1f else 0.92f, spring(Spring.DampingRatioLowBouncy), label = "scale")

    val ring = rememberInfiniteTransition(label = "ring")
    val ringProgress by ring.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ringProgress"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1600)
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(SxColor.Background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(320.dp).alpha(alpha * 0.5f)) {
            listOf(0f, 0.33f, 0.66f).forEach { offset ->
                val p = (ringProgress + offset) % 1f
                drawCircle(
                    color = SxColor.Cyan.copy(alpha = (1f - p) * 0.35f),
                    radius = size.minDimension / 2f * p,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            com.signalx.app.ui.components.SignalXAppIcon(
                modifier = Modifier
                    .alpha(alpha)
                    .scale(scale),
                size = 96.dp
            )
            Spacer(Modifier.height(Sx.s4))
            SignalXLogo(Modifier.alpha(alpha).scale(scale), fontSize = 36)
            Spacer(Modifier.height(Sx.s3))
            BrandTagline(Modifier.alpha(alpha))
        }
    }
}
