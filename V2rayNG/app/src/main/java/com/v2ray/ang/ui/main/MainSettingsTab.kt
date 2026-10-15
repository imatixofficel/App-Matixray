package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.ui.compose.SuccessBadge
import com.v2ray.ang.util.Utils
import java.util.Locale

private val toolsItems = listOf(
    MainDestination.XfinderConfigs,
    MainDestination.IpScanner,
    MainDestination.XfinderLogs,
)

private val generalItems = listOf(
    MainDestination.Settings,
    MainDestination.Language,
    MainDestination.Subscriptions,
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.UserAssets,
)

private val systemItems = listOf(
    MainDestination.Logcat,
    MainDestination.CheckUpdate,
    MainDestination.BackupRestore,
    MainDestination.About,
)

/**
 * The Settings tab: every section the old hamburger menu had, in three rounded cards (Matix tools, general, system),
 * then a card with the numbers of the running connection, the auto-switch and the Telegram community.
 */
@Composable
fun MainSettingsTab(
    isRunning: Boolean,
    elapsedSec: () -> Long,
    speed: TrafficSpeed,
    autoSwitch: Boolean,
    onAutoSwitchChange: (Boolean) -> Unit,
    updateAvailable: Boolean,
    onNavigate: (MainDestination) -> Unit,
    tour: TourState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize()) {
        MainPageHeader(subtitle = stringResource(R.string.mx_nav_settings))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionLabel(stringResource(R.string.mx_settings_tools))
            SettingsCard(modifier = Modifier.tourTarget(tour, TourTarget.SettingsTools)) {
                toolsItems.forEachIndexed { i, item ->
                    if (i > 0) RowDivider()
                    NavRow(item, showNew = false, onClick = { onNavigate(item) })
                }
            }

            SectionLabel(stringResource(R.string.mx_settings_general))
            SettingsCard {
                generalItems.forEachIndexed { i, item ->
                    if (i > 0) RowDivider()
                    NavRow(item, showNew = false, onClick = { onNavigate(item) })
                }
            }

            SectionLabel(stringResource(R.string.mx_settings_system))
            SettingsCard {
                systemItems.forEachIndexed { i, item ->
                    if (i > 0) RowDivider()
                    NavRow(item, showNew = updateAvailable && item == MainDestination.CheckUpdate, onClick = { onNavigate(item) })
                }
            }

            SectionLabel(stringResource(R.string.mx_settings_status))
            SettingsCard {
                val idle = stringResource(R.string.mx_stat_none)
                InfoRow(
                    iconRes = R.drawable.ic_speed_24dp,
                    label = stringResource(R.string.mx_settings_duration),
                    value = if (isRunning) formatDuration(elapsedSec()) else idle,
                )
                RowDivider()
                InfoRow(
                    iconRes = R.drawable.ic_arrow_up_24dp,
                    label = stringResource(R.string.mx_stat_upload),
                    value = if (isRunning) speed.upBytesPerSec.toSpeedString() else idle,
                )
                RowDivider()
                InfoRow(
                    iconRes = R.drawable.ic_arrow_down_24dp,
                    label = stringResource(R.string.mx_stat_download),
                    value = if (isRunning) speed.downBytesPerSec.toSpeedString() else idle,
                )
                RowDivider()
                SwitchRow(
                    iconRes = R.drawable.ic_flash_on_24dp,
                    label = stringResource(R.string.mx_auto_switch_title),
                    checked = autoSwitch,
                    onCheckedChange = onAutoSwitchChange,
                )
                RowDivider()
                LinkRow(
                    iconRes = R.drawable.ic_telegram_24dp,
                    label = stringResource(R.string.mx_settings_telegram),
                    onClick = { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) { content() }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 62.dp, end = 16.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** The little violet square behind the icon of a row. */
@Composable
private fun RowIcon(@DrawableRes iconRes: Int) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun Chevron() {
    // The arrow points to the end of the line: right in English, left in Persian.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Icon(
        painter = painterResource(R.drawable.ic_arrow_forward_24dp),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { scaleX = if (rtl) -1f else 1f },
    )
}

@Composable
private fun NavRow(item: MainDestination, showNew: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RowIcon(item.iconRes)
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showNew) SuccessBadge(text = stringResource(R.string.mx_badge_new))
        Spacer(Modifier.weight(1f))
        Chevron()
    }
}

@Composable
private fun InfoRow(@DrawableRes iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RowIcon(iconRes)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SwitchRow(@DrawableRes iconRes: Int, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RowIcon(iconRes)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LinkRow(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RowIcon(iconRes)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Chevron()
    }
}
