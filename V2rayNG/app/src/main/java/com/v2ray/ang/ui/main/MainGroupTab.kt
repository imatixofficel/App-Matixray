package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.v2ray.ang.ui.compose.PrimaryGradient
import com.v2ray.ang.ui.compose.liquidGlass
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.GlassCardShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ServersCache
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex)
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        edgePadding = 16.dp,
        indicator = {},
        divider = {}
    ) {
        groups.forEachIndexed { index, group ->
            val serverFlow = remember(group.id, mainViewModel) {
                mainViewModel.serversForGroup(group.id)
            }
            GroupTabItem(
                group = group,
                selected = index == selectedIndex,
                serverFlow = serverFlow,
                onClick = { onTabClick(index) }
            )
        }
    }
}

@Composable
private fun GroupTabItem(
    group: GroupMapItem,
    selected: Boolean,
    serverFlow: StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val servers by serverFlow.collectAsStateWithLifecycle()
    val text = if (group.id.isEmpty()) {
        group.remarks
    } else {
        "${group.remarks} (${servers.size})"
    }

    val pill = RoundedCornerShape(50)
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .widthIn(min = 56.dp)
            .heightIn(min = 40.dp)
            .clip(pill)
            .then(if (selected) Modifier.background(PrimaryGradient) else Modifier.liquidGlass(pill)),
        text = {
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

/**
 * The card above the list, like the group card of the design: the name of the shown group with the number of its
 * configs, the config that is selected, and the speed button that tests the real ping of the group.
 */
@Composable
fun GroupInfoCard(
    group: GroupMapItem,
    serverFlow: StateFlow<List<ServersCache>>,
    selectedName: String?,
    onPing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val servers by serverFlow.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .liquidGlass(GlassCardShape)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${group.remarks} (${servers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedName?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.mx_group_selected, it) }
                    ?: stringResource(R.string.mx_selected_server_none),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onPing) {
            Icon(
                painter = painterResource(R.drawable.ic_speed_24dp),
                contentDescription = stringResource(R.string.mx_ping_all),
                tint = scheme.primary,
            )
        }
    }
}
