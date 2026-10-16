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
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.foundation.layout.fillMaxHeight
import com.v2ray.ang.ui.compose.MatixButtonBrush
import com.v2ray.ang.ui.compose.XfBlue
import com.v2ray.ang.ui.compose.MatixSmallCutShape
import com.v2ray.ang.ui.compose.onMatixButton
import com.v2ray.ang.ui.compose.verticalScrollbar

/**
 * Header of the configs page, one short row so the list gets the room: scan a QR code at the left, the name, and at
 * the right the add button (metal, with the menu that opens the paste box first) and the three-dot menu.
 */
@Composable
fun MainConfigsHeader(
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
    onManualAdd: () -> Unit,
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
        compact = true,
        leading = {
            HeaderIcon(R.drawable.ic_qu_scan_24dp, R.string.menu_item_import_config_qrcode) {
                onAction(MainAction.ImportQRcode)
            }
        },
    ) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            HeaderIcon(
                iconRes = R.drawable.ic_add_24dp,
                descriptionRes = R.string.acc_add,
                filled = true,
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
                // Matixray: typing or pasting configs opens its own box that takes any number of them at once.
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.mx_add_manual_menu),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_mx_paste_24dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        showImportMenu = false
                        onManualAdd()
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
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
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    if (filled) {
        // The main button of the row: the metal tile with the cut corners of the logo.
        Box(
            modifier = modifier
                .padding(horizontal = 4.dp)
                .size(36.dp)
                .clip(MatixSmallCutShape)
                .background(MatixButtonBrush)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(descriptionRes),
                tint = onMatixButton,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier.size(42.dp)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(descriptionRes),
                tint = XfBlue,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The row under the header: the sort chip ("Ping", sorts the list by the test results), the search chip, and the
 * "Ping all" button at the right. Low and framed, so the list starts right under it. Like the header it keeps the
 * left-to-right order of the design.
 */
@Composable
fun MainConfigsToolbar(
    onSort: () -> Unit,
    onSearch: () -> Unit,
    onPingAll: () -> Unit,
    tour: TourState,
) {
    val scheme = MaterialTheme.colorScheme
    val chip = RoundedCornerShape(50)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(chip)
                    .border(1.dp, scheme.outlineVariant, chip)
                    .clickable(role = Role.Button, onClick = onSort)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort_24dp),
                    contentDescription = stringResource(R.string.mx_sort_by_ping),
                    tint = XfBlue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.mx_sort_ping),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(chip)
                    .border(1.dp, scheme.outlineVariant, chip)
                    .clickable(role = Role.Button, onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24dp),
                    contentDescription = stringResource(R.string.acc_search),
                    tint = XfBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .tourTarget(tour, TourTarget.PingAll)
                    .height(34.dp)
                    .clip(chip)
                    .background(XfBlue.copy(alpha = 0.12f))
                    .clickable(role = Role.Button, onClick = onPingAll)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speed_24dp),
                    contentDescription = null,
                    tint = XfBlue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.mx_ping_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
            }
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
