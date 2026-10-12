package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** CSS `ease-in-out`, so the bounce feels the same as the web version of this loader. */
private val EaseInOutCss = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * The loading animation: a row of round dots that swell and fade in turn, one after another, without end. It is the
 * Compose version of the web `BouncingDots`: each dot goes from scale 0.8 and half opacity up to scale 1.2 and full
 * opacity and back, [durationMillis] for the whole cycle, and every next dot starts [delayMillis] later.
 * `BouncingDots(dotSize = 14.dp)` is the default look. The progress is read only while drawing, so a frame costs a few
 * circles and no recomposition.
 *
 * @param dotSize width and height of one dot (the gap between dots follows it, as the 12 % gap of the web one).
 * @param contentDescription what a screen reader says, or null when the text next to the loader already says it.
 */
@Composable
fun BouncingDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dots: Int = 3,
    dotSize: Dp = 14.dp,
    durationMillis: Int = 1400,
    delayMillis: Int = 200,
    contentDescription: String? = null,
) {
    val transition = rememberInfiniteTransition(label = "bouncingDots")
    Row(
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        horizontalArrangement = Arrangement.spacedBy(dotSize * 0.47f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(dots) { index ->
            // Up for half the cycle and back for the other half; the start offset is what makes the dots take turns.
            val progress = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis / 2, easing = EaseInOutCss),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delayMillis * index),
                ),
                label = "bouncingDot$index",
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        val p = progress.value
                        val s = 0.8f + 0.4f * p
                        scaleX = s
                        scaleY = s
                        alpha = 0.5f + 0.5f * p
                    }
                    .background(color, CircleShape),
            )
        }
    }
}
