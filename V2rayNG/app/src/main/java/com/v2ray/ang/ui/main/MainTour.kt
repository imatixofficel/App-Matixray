package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

/** The parts of the main screen the first-run tour can circle. */
enum class TourTarget {
    Connect,
    Sections,
    TabHome,
    TabConfigs,
    TabSettings,
    AddConfig,
    AutoBest,
    PingAll,
    ConnectionBar,
    SettingsTools,
}

/** Where each circled part currently is on the screen; the parts report it while they are on screen. */
class TourState {
    val bounds = mutableStateMapOf<TourTarget, Rect>()
}

/** Marks a part of the screen as a stop of the tour: it reports its place in the window. */
fun Modifier.tourTarget(state: TourState, target: TourTarget): Modifier =
    this.onGloballyPositioned { coordinates -> state.bounds[target] = coordinates.boundsInWindow() }

private class TourStep(
    val target: TourTarget?,
    /** The bottom tab (0 home, 1 configs, 2 settings) that has to be open for this step. */
    val tab: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

private val tourSteps = listOf(
    TourStep(null, 0, R.string.mx_tour_welcome_title, R.string.mx_tour_welcome_body),
    TourStep(TourTarget.ConnectionBar, 0, R.string.mx_tour_connect_title, R.string.mx_tour_connect_body),
    TourStep(TourTarget.Sections, 0, R.string.mx_tour_sections_title, R.string.mx_tour_sections_body),
    TourStep(TourTarget.TabConfigs, 0, R.string.mx_tour_tab_configs_title, R.string.mx_tour_tab_configs_body),
    TourStep(TourTarget.AddConfig, 1, R.string.mx_tour_add_title, R.string.mx_tour_add_body),
    TourStep(TourTarget.PingAll, 1, R.string.mx_tour_ping_title, R.string.mx_tour_ping_body),
    TourStep(TourTarget.AutoBest, 1, R.string.mx_tour_best_title, R.string.mx_tour_best_body),
    TourStep(TourTarget.TabSettings, 1, R.string.mx_tour_tab_settings_title, R.string.mx_tour_tab_settings_body),
    TourStep(TourTarget.SettingsTools, 2, R.string.mx_tour_tools_title, R.string.mx_tour_tools_body),
)

/**
 * The first-run tour: the screen is dimmed, the part being explained is circled, and a card says what it is, with
 * previous and next buttons and a way out. [onTabChange] opens the tab a step needs; [onFinish] is called when the
 * tour ends by its last step or by skipping, and the caller remembers that it was shown.
 */
@Composable
fun MainTourOverlay(
    state: TourState,
    onTabChange: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = tourSteps[index.coerceIn(0, tourSteps.lastIndex)]
    val isLast = index >= tourSteps.lastIndex
    LaunchedEffect(index) { onTabChange(step.tab) }
    BackHandler { onFinish() }

    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val circled = step.target?.let { state.bounds[it] }?.translate(Offset(-origin.x, -origin.y))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // The tour takes every touch, so nothing behind it is pressed by mistake.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val screenHeight = with(density) { maxHeight.toPx() }
        val primary = scheme.primary

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color(0xD11A1033))
            if (circled != null) {
                val pad = 6.dp.toPx()
                val topLeft = Offset(circled.left - pad, circled.top - pad)
                val size = Size(circled.width + pad * 2, circled.height + pad * 2)
                val corner = CornerRadius(16.dp.toPx())
                drawRoundRect(Color.Black, topLeft, size, corner, blendMode = BlendMode.Clear)
                drawRoundRect(primary, topLeft, size, corner, style = Stroke(width = 2.dp.toPx()))
            }
        }

        // The card goes under the circled part when that part is in the upper half, above it otherwise.
        val gap = 18.dp
        val cardModifier = when {
            circled == null -> Modifier.align(Alignment.Center)
            circled.center.y < screenHeight / 2f -> Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(density) { circled.bottom.toDp() } + gap)
            else -> Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(density) { (screenHeight - circled.top).toDp() } + gap)
        }

        Surface(
            modifier = cardModifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = scheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mx_tour_counter, index + 1, tourSteps.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(scheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    if (!isLast) {
                        TextButton(onClick = onFinish) {
                            Text(stringResource(R.string.mx_tour_skip), color = scheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(step.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(step.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (index > 0) {
                        OutlinedButton(
                            onClick = { index = (index - 1).coerceAtLeast(0) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.mx_tour_prev)) }
                    }
                    Button(
                        onClick = { if (isLast) onFinish() else index += 1 },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(if (isLast) R.string.mx_tour_done else R.string.mx_tour_next),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
