package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.verticalScrollbar

/**
 * Header of the configs page: the name at the top left and, at the right, the buttons of the design: scan a QR code,
 * the star (find the best config and connect), add, and the three-dot menu.
 */
@Composable
fun MainConfigsHeader(
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
    tour: TourState,
) {
    var showImportMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val importMenuScrollState = rememberScrollState()
    val moreMenuScrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - navBarHeight - 20.dp

    MainPageHeader(
        subtitle = stringResource(R.string.mx_nav_configs),
        leading = {
            HeaderIcon(R.drawable.ic_qu_scan_24dp, R.string.menu_item_import_config_qrcode) {
                onAction(MainAction.ImportQRcode)
            }
        },
    ) {
        HeaderIcon(
            iconRes = R.drawable.ic_star_24dp,
            descriptionRes = R.string.mx_auto_connect,
            modifier = Modifier.tourTarget(tour, TourTarget.AutoBest),
        ) { onAction(MainAction.AutoConnectBest) }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            HeaderIcon(
                iconRes = R.drawable.ic_add_24dp,
                descriptionRes = R.string.acc_add,
                modifier = Modifier.tourTarget(tour, TourTarget.AddConfig),
            ) { showImportMenu = true }
            DropdownMenu(
                expanded = showImportMenu,
                onDismissRequest = { showImportMenu = false },
                scrollState = importMenuScrollState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .heightIn(max = maxMenuHeight)
                    .verticalScrollbar(importMenuScrollState)
            ) {
                ImportMenuContent(
                    onAction = { action ->
                        showImportMenu = false
                        onAction(action)
                    }
                )
            }
        }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            HeaderIcon(R.drawable.ic_more_vert_24dp, R.string.acc_more) { showMenu = true }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                scrollState = moreMenuScrollState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .heightIn(max = maxMenuHeight)
                    .verticalScrollbar(moreMenuScrollState)
            ) {
                MoreMenuContent { action ->
                    showMenu = false
                    onMoreMenuAction(action)
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(
    @DrawableRes iconRes: Int,
    @StringRes descriptionRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = modifier.size(42.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * The row under the header: the sort chip ("Ping", sorts the list by the test results), the search button, and the
 * "Ping all" button at the right. Like the header it keeps the left-to-right order of the design.
 */
@Composable
fun MainConfigsToolbar(
    onSort: () -> Unit,
    onSearch: () -> Unit,
    onPingAll: () -> Unit,
    tour: TourState,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onSort)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort_24dp),
                    contentDescription = stringResource(R.string.mx_sort_by_ping),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.mx_sort_ping),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(38.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24dp),
                    contentDescription = stringResource(R.string.acc_search),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.mx_ping_all),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                modifier = Modifier
                    .tourTarget(tour, TourTarget.PingAll)
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onPingAll)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/** The search box that takes the place of the toolbar while the user searches the configs. */
@Composable
fun MainConfigsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(44.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(BorderStroke(1.dp, scheme.outlineVariant), shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_24dp),
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.menu_item_search),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.mx_search_close),
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
