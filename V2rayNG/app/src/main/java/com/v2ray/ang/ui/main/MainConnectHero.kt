package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.v2ray.ang.R
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.ui.compose.XfConnected
import com.v2ray.ang.ui.compose.XfGreen
import com.v2ray.ang.ui.compose.XfPurpleLight
import com.v2ray.ang.ui.compose.XfRed
import com.v2ray.ang.ui.compose.XfYellow
import com.v2ray.ang.ui.compose.liquidGlass
import java.util.Locale

/**
 * Connection hero of the main screen: the timer and the status line at the top, the logo as a faint mark behind the
 * page, and at the bottom the live numbers in framed boxes (download and upload side by side, the ping under them), as
 * in V2Box. The round connect button is gone from every page; the button of the bar at the bottom connects (the
 * Psiphon page's bar button connects Psiphon). The time is passed in, so it does not restart when the section changes,
 * and it is read through a lambda so only the timer is redrawn each second. [accent] colors the mark and the icons:
 * the theme's ink for the main section, orange for the Psiphon section.
 */
@Composable
fun MainConnectHero(
    isRunning: Boolean,
    elapsedSec: () -> Long,
    statusText: String,
    speed: TrafficSpeed,
    pingMillis: Long?,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") accentLight: Color = XfPurpleLight,
    onStatusClick: (() -> Unit)? = null,
    bottomExtra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    // The page is one column: the timer and status at the top, the live numbers (and whatever the caller adds) at the
    // bottom, and between them the free space with the faint logo, centered a little above its middle.
    Column(modifier = modifier.fillMaxSize().clipToBounds()) {
        Column(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatElapsed(if (isRunning) elapsedSec() else 0L, scheme.onBackground, isRunning),
                style = MaterialTheme.typography.displaySmall,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            // The status as a small framed tag: a dot (green while connected, yellow while not) and the words.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(scheme.onSurface.copy(alpha = 0.06f))
                    .then(
                        // A tap on the status line of a running tunnel tests the connection again.
                        if (onStatusClick != null && isRunning) Modifier.clickable(role = Role.Button, onClick = onStatusClick)
                        else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) XfGreen else XfYellow)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // The logo is a picture drawn once and tinted, so it costs one bitmap draw per frame and never animates.
            Image(
                painter = painterResource(R.drawable.xf_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = if (isRunning) 0.16f else 0.09f,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier
                    .align(BiasAlignment(0f, -0.05f))
                    .requiredSize(minOf(maxWidth * 0.9f, maxHeight * 1.05f)),
            )
        }

        Column(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            val idle = stringResource(R.string.mx_stat_none)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatBox(
                    iconRes = R.drawable.ic_arrow_down_24dp,
                    label = stringResource(R.string.mx_stat_download),
                    value = if (isRunning) speed.downBytesPerSec.toSpeedString() else idle,
                    tint = accent,
                )
                StatBox(
                    iconRes = R.drawable.ic_arrow_up_24dp,
                    label = stringResource(R.string.mx_stat_upload),
                    value = if (isRunning) speed.upBytesPerSec.toSpeedString() else idle,
                    tint = accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            PingBox(
                label = stringResource(R.string.mx_stat_ping),
                value = if (isRunning && pingMillis != null) stringResource(R.string.mx_ping_value, pingMillis) else idle,
                pingMillis = if (isRunning) pingMillis else null,
                tint = accent,
                onClick = if (isRunning) onStatusClick else null,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (bottomExtra != null) {
                Spacer(Modifier.height(10.dp))
                bottomExtra()
            }
        }
    }
}

/** The small square behind a stat icon: a faint fill of the accent and the icon in it. */
@Composable
private fun StatIcon(iconRes: Int, tint: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One live number in its own frame: the icon and the name on top, the value below; one focusable node. */
@Composable
private fun RowScope.StatBox(iconRes: Int, label: String, value: String, tint: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .liquidGlass(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatIcon(iconRes, tint)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** The ping in a wide frame: the value and four signal bars that fill with the quality; a tap tests again. */
@Composable
private fun PingBox(
    label: String,
    value: String,
    pingMillis: Long?,
    tint: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatIcon(R.drawable.ic_speed_24dp, tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        SignalBars(pingMillis)
    }
}

/** Ping quality in four steps: 4 bars under 150 ms, 3 under 300, 2 under 600, 1 above; none while there is no number. */
fun pingLevel(ms: Long?): Int = when {
    ms == null -> 0
    ms < 150 -> 4
    ms < 300 -> 3
    ms < 600 -> 2
    else -> 1
}

/** The color of a ping value: green when healthy, yellow when slow, red when bad. */
fun pingColor(ms: Long?): Color = when (pingLevel(ms)) {
    4, 3 -> XfConnected
    2 -> XfYellow
    1 -> XfRed
    else -> Color.Unspecified
}

@Composable
private fun SignalBars(pingMillis: Long?) {
    val level = pingLevel(pingMillis)
    val on = pingColor(pingMillis).let { if (it == Color.Unspecified) Color.Transparent else it }
    val off = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Canvas(modifier = Modifier.size(width = 22.dp, height = 16.dp)) {
        val gap = 2.dp.toPx()
        val bar = (size.width - gap * 3) / 4f
        for (i in 0 until 4) {
            val h = size.height * (0.38f + 0.62f * i / 3f)
            drawRoundRect(
                color = if (i < level) on else off,
                topLeft = Offset(i * (bar + gap), size.height - h),
                size = Size(bar, h),
                cornerRadius = CornerRadius(1.2.dp.toPx()),
            )
        }
    }
}

/** "hh:mm:ss" with the seconds dimmed, like the timer of the design; dimmed as a whole while idle. */
private fun formatElapsed(totalSeconds: Long, color: Color, running: Boolean): AnnotatedString {
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    val main = color.copy(alpha = if (running) 1f else 0.5f)
    val dim = color.copy(alpha = if (running) 0.45f else 0.3f)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = main)) { append(String.format(Locale.US, "%02d:%02d:", h, m)) }
        withStyle(SpanStyle(color = dim)) { append(String.format(Locale.US, "%02d", s)) }
    }
}
