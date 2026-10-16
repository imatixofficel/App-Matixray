package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min

/*
 * The loading animation: three balls that bounce, flatten when they land and cast a small shadow. It is the Uiverse.io
 * "bouncing circles" loader by deax_9186, kept as its CSS has it: a 200 x 60 box, balls of 20 + 2 x 5 px at 15 %,
 * 45 % and 15 % from the right, 0.5 s alternating "ease" bounces, the 2nd ball 0.2 s and the 3rd 0.3 s late. One CSS
 * pixel is one dp. The drawing is one canvas redrawn each frame from a clock; nothing is recomposed.
 */

private const val HALF_MS = 500L
private const val BALL_BOX = 30f      // 20 px of content + the 5 px border on each side
private const val BORDER = 5f
private val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val BallLeft = floatArrayOf(30f, 90f, 140f)
private val BallDelayMs = longArrayOf(0L, 200L, 300L)

private val GrayStart = Color(0xFF808080)
private val GrayMid = Color(44, 44, 44)

/** The animation's directed progress 0..1..0 at [timeMs], or null while the animation is still waiting its delay. */
private fun directedProgress(timeMs: Long, delayMs: Long): Float? {
    val t = timeMs - delayMs
    if (t < 0L) return null
    val cycle = t % (2 * HALF_MS)
    return if (cycle < HALF_MS) cycle / HALF_MS.toFloat() else 1f - (cycle - HALF_MS) / HALF_MS.toFloat()
}

@Composable
fun BouncingBallsLoader(modifier: Modifier = Modifier) {
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.longValue = (it - start) / 1_000_000L }
    }
    // 200 dp wide, 60 dp of wrapper plus the room the balls and shadows take below it at the bottom of a bounce.
    Spacer(
        modifier
            .size(200.dp, 100.dp)
            .drawBehind {
                val now = clock.longValue
                for (i in 0..2) drawShadow(BallLeft[i], directedProgress(now, BallDelayMs[i]))
                for (i in 0..2) drawBall(BallLeft[i], directedProgress(now, BallDelayMs[i]))
            }
    )
}

private fun DrawScope.drawShadow(left: Float, d: Float?) {
    var scaleX = 1f
    var opacity = 1f
    if (d != null) {
        if (d <= 0.4f) {
            val u = Ease.transform(d / 0.4f)
            scaleX = lerp(1.5f, 1f, u)
            opacity = lerp(1f, 0.7f, u)
        } else {
            val u = Ease.transform((d - 0.4f) / 0.6f)
            scaleX = lerp(1f, 0.2f, u)
            opacity = lerp(0.7f, 0.4f, u)
        }
    }
    val w = 20.dp.toPx()
    val h = 4.dp.toPx()
    val x = left.dp.toPx()
    val y = 62.dp.toPx()
    scale(scaleX, 1f, pivot = Offset(x + w / 2f, y + h / 2f)) {
        // The CSS blurs the shadow by 1 px: a fainter, slightly larger oval under the solid one.
        val soft = 1.dp.toPx()
        drawOval(Color(0x80000000).copy(alpha = 0.5f * 0.5f * opacity), Offset(x - soft, y - soft), Size(w + 2 * soft, h + 2 * soft))
        drawOval(Color(0x80000000).copy(alpha = 0.5f * opacity), Offset(x, y), Size(w, h))
    }
}

private fun DrawScope.drawBall(left: Float, d: Float?) {
    // Before its delay a ball rests at the top of the box as a plain white circle (the CSS base style).
    var top = 0f
    var inner = 20f
    var radiusX = 15f
    var radiusYFactorOfBox = 0.5f
    var morph = 1f
    var scaleX = 1f
    var fill = Color.White
    if (d != null) {
        top = lerp(60f, 0f, Ease.transform(d))
        if (d <= 0.4f) {
            val u = Ease.transform(d / 0.4f)
            inner = lerp(5f, 20f, u)
            scaleX = lerp(2.7f, 1f, u)
            fill = androidx.compose.ui.graphics.lerp(GrayStart, GrayMid, u)
            morph = u
        } else {
            fill = androidx.compose.ui.graphics.lerp(GrayMid, Color.White, Ease.transform((d - 0.4f) / 0.6f))
        }
    }
    val boxW = BALL_BOX
    val boxH = inner + 2 * BORDER
    // border-radius: 50px 50px 25px 25px at the start, 50 % at 40 % of the bounce; the CSS then shrinks every radius by
    // one factor when two neighbours would overlap.
    val rxTop = lerp(50f, radiusX, morph)
    val rxBottom = lerp(25f, radiusX, morph)
    val ryTop = lerp(50f, boxH * radiusYFactorOfBox, morph)
    val ryBottom = lerp(25f, boxH * radiusYFactorOfBox, morph)
    val factor = min(
        1f,
        min(boxW / (2 * rxTop), min(boxW / (2 * rxBottom), boxH / (ryTop + ryBottom)))
    )
    val tl = CornerRadius(rxTop * factor, ryTop * factor)
    val bl = CornerRadius(rxBottom * factor, ryBottom * factor)

    val x = left.dp.toPx()
    val y = top.dp.toPx()
    val b = BORDER.dp.toPx()
    val w = boxW.dp.toPx()
    val h = boxH.dp.toPx()
    scale(scaleX, 1f, pivot = Offset(x + w / 2f, y + h / 2f)) {
        val outer = Path().apply {
            addRoundRect(RoundRect(Rect(x, y, x + w, y + h), tl, tl, bl, bl))
        }
        drawPath(outer, Color.Black)
        val innerRadius = { c: CornerRadius -> CornerRadius(max(0f, c.x - b), max(0f, c.y - b)) }
        val inside = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(x + b, y + b, x + w - b, y + h - b),
                    innerRadius(tl), innerRadius(tl), innerRadius(bl), innerRadius(bl),
                )
            )
        }
        drawPath(inside, fill)
    }
}
