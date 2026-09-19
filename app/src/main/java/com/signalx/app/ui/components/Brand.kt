package com.signalx.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalx.app.ui.theme.SxColor

/**
 * SignalX(5G) wordmark. "Signal" in white, "X" carrying the cyan->blue gradient,
 * and a circular 5G badge pinned to the upper-right of the X - never inline text.
 */
@Composable
fun SignalXLogo(
    modifier: Modifier = Modifier,
    fontSize: Int = 34,
    onDark: Boolean = true,
    glow: Boolean = true
) {
    val wordColor = if (onDark) SxColor.TextPrimary else SxColor.LTextPrimary
    val badge: Dp = (fontSize * 0.62f).dp

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "SignalX 5G" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Signal",
            style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
            color = wordColor,
            modifier = Modifier.clearAndSetSemantics { }
        )
        // The X plus its badge share one box so the badge anchors to the glyph.
        Box(contentAlignment = Alignment.Center) {
            if (glow) {
                Box(
                    Modifier
                        .size((fontSize * 1.5f).dp)
                        .alpha(0.20f)
                        .background(SxColor.Cyan, androidx.compose.foundation.shape.CircleShape)
                )
            }
            Text(
                "X",
                style = TextStyle(fontSize = (fontSize * 1.12f).sp, fontWeight = FontWeight.Black),
                color = Color.Unspecified,
                modifier = Modifier
                    .clearAndSetSemantics { }
                    .brandGradient(),
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = badge * 0.55f, y = -badge * 0.30f)
                    .size(badge)
                    .background(SxColor.AccentGradient, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "5G",
                    style = TextStyle(fontSize = (fontSize * 0.26f).sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                    color = SxColor.Background,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
        Spacer(Modifier.width(badge * 0.6f))   // room for the badge overhang
    }
}

/** Paints text with the brand cyan->blue gradient. */
private fun Modifier.brandGradient(): Modifier = this.drawWithCache {
    onDrawWithContent {
        drawContent()
        drawRect(brush = SxColor.XGradient, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
    }
}

/** Compact mark used by the top app bar and small surfaces. */
@Composable
fun SignalXMark(modifier: Modifier = Modifier, size: Int = 28) {
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Text(
            "X",
            style = TextStyle(fontSize = (size * 0.8f).sp, fontWeight = FontWeight.Black),
            modifier = Modifier.brandGradient()
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size((size * 0.46f).dp)
                .background(SxColor.AccentGradient, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("5G", fontSize = (size * 0.19f).sp, color = SxColor.Background, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BrandTagline(modifier: Modifier = Modifier) = Text(
    "Network Control & Diagnostics",
    style = MaterialTheme.typography.labelSmall,
    color = SxColor.TextSecondary,
    modifier = modifier
)

/** Slow breathing pulse shared by the splash rings and the live-status dot. */
@Composable
fun rememberPulse(durationMs: Int = 1600): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    return t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue"
    ).value
}
