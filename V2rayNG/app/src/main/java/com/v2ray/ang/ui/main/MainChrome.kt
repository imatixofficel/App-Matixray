package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.SlidingPillButton
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import com.v2ray.ang.ui.compose.XfPurpleDark
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.XfMint

/** The three tabs of the bottom bar. */
object MainTab {
    const val Home = 0
    const val Configs = 1
    const val Settings = 2
}

/**
 * Top of every page, as in the design: an optional row of buttons (the first at the left, the rest at the right),
 * and under it the big title, the name "Matixray" at the left (whatever the language) with the version, and the
 * name of the page in small gray under it.
 */
@Composable
fun MainPageHeader(
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)) {
            if (leading != null || actions != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    leading?.invoke(this)
                    Spacer(Modifier.weight(1f))
                    actions?.invoke(this)
                }
            }
            Column(modifier = Modifier.padding(start = 8.dp, top = if (leading != null || actions != null) 0.dp else 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = scheme.onBackground)) { append("Matix") }
                            withStyle(SpanStyle(color = scheme.primary)) { append("ray") }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME.trimStart('v', 'V'),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(scheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Bottom bar: Home, Configs and Settings, the open one on a soft violet pill. */
@Composable
fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit, tour: TourState) {
    val scheme = MaterialTheme.colorScheme
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.primary,
        selectedTextColor = scheme.primary,
        indicatorColor = Color.Transparent,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
    NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
        BottomItem(MainTab.Home, selected, onSelect, R.drawable.ic_home_24dp, R.string.mx_nav_home, colors,
            Modifier.tourTarget(tour, TourTarget.TabHome))
        BottomItem(MainTab.Configs, selected, onSelect, R.drawable.ic_list_24dp, R.string.mx_nav_configs, colors,
            Modifier.tourTarget(tour, TourTarget.TabConfigs))
        BottomItem(MainTab.Settings, selected, onSelect, R.drawable.ic_settings_24dp, R.string.mx_nav_settings, colors,
            Modifier.tourTarget(tour, TourTarget.TabSettings))
    }
}

@Composable
private fun RowScope.BottomItem(
    tab: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    @DrawableRes icon: Int,
    label: Int,
    colors: androidx.compose.material3.NavigationBarItemColors,
    modifier: Modifier,
) {
    NavigationBarItem(
        selected = selected == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(painterResource(icon), contentDescription = null) },
        label = { Text(stringResource(label), maxLines = 1) },
        colors = colors,
        modifier = modifier,
    )
}

/**
 * The bar above the bottom tabs on the configs page: what the connection is doing in words, a tap on the words tests
 * the connection, and the pill button connects or disconnects. A short mint bar sits on its top edge.
 */
@Composable
fun MainConnectionBar(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (isRunning) R.string.mx_bar_connected else R.string.mx_bar_disconnected),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = isRunning, role = Role.Button, onClick = onTest)
                    .padding(vertical = 6.dp),
            )
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = scheme.primary, spotColor = scheme.primary)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.primary)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(horizontal = 32.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (isRunning) R.string.mx_bar_btn_connected else R.string.mx_bar_btn_connect),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}


/**
 * Matixray: the four quick buttons above the connect bar: get configs (filled, lilac gradient, with a spinner and
 * the count while it runs; a second tap stops it), test all (a tap while testing stops the tests), sort and remove
 * the dead ones. Every tile is one focusable button that dips a little under the finger.
 */
@Composable
fun MainQuickActions(
    fetch: ConfigFetchState,
    testing: Boolean,
    onFetch: () -> Unit,
    onTestAll: () -> Unit,
    onSort: () -> Unit,
    onRemoveDead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fetchCaption = when (fetch.phase) {
        FetchPhase.Downloading -> stringResource(R.string.mx_fetch_progress, fetch.sourcesDone, fetch.sourcesTotal, fetch.added)
        FetchPhase.Testing -> stringResource(R.string.mx_fetch_testing)
        FetchPhase.Idle -> null
    }
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionTile(
                label = stringResource(if (fetch.busy) R.string.mx_qa_stop else R.string.mx_qa_fetch),
                caption = fetchCaption,
                iconRes = R.drawable.ic_cloud_download_24dp,
                busy = fetch.busy,
                filled = true,
                onClick = onFetch,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = stringResource(if (testing) R.string.mx_qa_stop else R.string.mx_qa_test_all),
                caption = null,
                iconRes = R.drawable.ic_speed_24dp,
                busy = testing,
                filled = false,
                onClick = onTestAll,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = stringResource(R.string.mx_qa_sort),
                caption = null,
                iconRes = R.drawable.ic_sort_24dp,
                busy = false,
                filled = false,
                onClick = onSort,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = stringResource(R.string.mx_qa_remove_dead),
                caption = null,
                iconRes = R.drawable.ic_delete_24dp,
                busy = false,
                filled = false,
                onClick = onRemoveDead,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    caption: String?,
    @DrawableRes iconRes: Int,
    busy: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val source = remember { MutableInteractionSource() }
    val contentColor = if (filled) scheme.onPrimary else scheme.primary
    val surface = if (filled) {
        Modifier
            .shadow(6.dp, shape, ambientColor = scheme.primary, spotColor = scheme.primary)
            .clip(shape)
            .background(Brush.linearGradient(listOf(lerp(scheme.primary, Color.White, 0.22f), scheme.primary, XfPurpleDark.copy(alpha = 0.85f))))
    } else {
        Modifier
            .clip(shape)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.60f), scheme.primary.copy(alpha = 0.10f))), shape)
    }
    Column(
        modifier = modifier
            .heightIn(min = 66.dp)
            .pressScale(source)
            .then(surface)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Icon(painterResource(iconRes), contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (filled) scheme.onPrimary else scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
