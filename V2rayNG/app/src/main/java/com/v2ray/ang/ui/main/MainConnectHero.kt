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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.v2ray.ang.ui.compose.XfBlue
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
 * blue for the main section, orange for the Psiphon section.
 */
@Composable
fun MainConnectHero(
    isRunning: Boolean,
    elapsedSec: () -> Long,
    statusText: String,
    speed: TrafficSpeed,
    pingMillis: Long?,
    modifier: Modifier = Modifier,
    accent: Color = XfBlue,
    @Suppress("UNUSED_PARAMETER") accentLight: Color = XfPurpleLight,
    onStatusClick: (() -> Unit)? = null,
    autoSwitch: Boolean? = null,
    onAutoSwitchChange: ((Boolean) -> Unit)? = null,
    onRouting: (() -> Unit)? = null,
    onTelegram: (() -> Unit)? = null,
    bottomExtra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    // Matixray: the page is a plain list in one white card, as in V2Box (duration, upload, download, smart connect,
    // routing, community), the status line under it and, at the end, whatever the caller adds.
    val idle = stringResource(R.string.mx_stat_none)
    val link = Color(0xFF1BA1C9)
    val ink = scheme.onSurface
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface),
        ) {
            HomeListRow(
                iconRes = R.drawable.ic_mx_clock_24dp,
                label = stringResource(R.string.mx_stat_duration),
                value = if (isRunning) formatClock(elapsedSec()) else "-",
                tint = ink,
            )
            HomeListRow(
                iconRes = R.drawable.ic_arrow_up_24dp,
                label = stringResource(R.string.mx_stat_upload),
                value = if (isRunning) speed.upBytesPerSec.toSpeedString() else idle,
                tint = ink,
            )
            HomeListRow(
                iconRes = R.drawable.ic_arrow_down_24dp,
                label = stringResource(R.string.mx_stat_download),
                value = if (isRunning) speed.downBytesPerSec.toSpeedString() else idle,
                tint = ink,
            )
            HomeListRow(
                iconRes = R.drawable.ic_flash_on_24dp,
                label = stringResource(R.string.mx_stat_ping),
                value = if (isRunning && pingMillis != null) stringResource(R.string.mx_ping_value, pingMillis) else idle,
                tint = ink,
                onClick = if (isRunning) onStatusClick else null,
            )
            if (autoSwitch != null && onAutoSwitchChange != null) {
                HomeListRow(
                    iconRes = R.drawable.ic_flash_on_24dp,
                    label = stringResource(R.string.mx_auto_switch_title),
                    tint = ink,
                    onClick = { onAutoSwitchChange(!autoSwitch) },
                    trailing = { androidx.compose.material3.Switch(checked = autoSwitch, onCheckedChange = null) },
                )
            }
            if (onRouting != null) {
                HomeListRow(
                    iconRes = R.drawable.ic_routing_24dp,
                    label = stringResource(R.string.routing_settings_title),
                    tint = ink,
                    labelColor = link,
                    onClick = onRouting,
                    chevron = true,
                )
            }
            if (onTelegram != null) {
                HomeListRow(
                    iconRes = R.drawable.ic_telegram_24dp,
                    label = stringResource(R.string.mx_telegram_button),
                    tint = ink,
                    labelColor = link,
                    onClick = onTelegram,
                    chevron = true,
                    last = true,
                )
            }
        }
        // The status as a small tag under the card: a dot (green while connected, yellow while not) and the words.
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .then(
                    if (onStatusClick != null && isRunning) Modifier.clickable(role = Role.Button, onClick = onStatusClick)
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                maxLines = 2,
            )
        }
        if (bottomExtra != null) {
            Spacer(Modifier.height(6.dp))
            bottomExtra()
        }
    }
}

private fun formatClock(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val sec = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}

/** One row of the home card: icon, label, the value (or a switch / an arrow) at the end, a thin divider under it. */
@Composable
private fun HomeListRow(
    iconRes: Int,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    value: String? = null,
    labelColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = false,
    last: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                maxLines = 1,
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                )
            }
            if (trailing != null) trailing()
            if (chevron) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (!last) {
            Box(
                modifier = Modifier
                    .padding(start = 52.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant)
            )
        }
    }
}

/** The small square behind a stat icon: a faint fill of the accent and the icon in it. */
@Composable
private fun StatIcon(iconRes: Int, tint: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
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
