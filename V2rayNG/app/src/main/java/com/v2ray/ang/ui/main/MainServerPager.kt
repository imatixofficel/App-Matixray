package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.v2ray.ang.ui.compose.XfMint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.ReorderableGridItem
import com.v2ray.ang.ui.compose.ReorderableListItem
import com.v2ray.ang.ui.compose.colorConfigType
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.liquidGlass
import com.v2ray.ang.ui.compose.verticalScrollbar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs

@Composable
fun GroupPagerPage(
    groupId: String,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    doubleColumnDisplay: Boolean,
    searchQuery: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String, String) -> Unit,
    contentPadding: PaddingValues
) {
    val groupStateFlow = remember(groupId) {
        mainViewModel.serverGroupState(groupId)
    }
    val groupState by groupStateFlow.collectAsStateWithLifecycle()
    val canReorder = groupId.isNotEmpty() && searchQuery.isEmpty()
    val actions = remember(
        onSelectServer,
        onEditServer,
        onShareServer,
        onMoreServer,
        onRemoveServer,
    ) {
        ServerRowActions(
            select = onSelectServer,
            edit = onEditServer,
            share = onShareServer,
            more = onMoreServer,
            remove = onRemoveServer,
        )
    }
    ServerListPage(
        rows = groupState.rows,
        selectedGuid = selectedGuid,
        locateTarget = locateTarget?.takeIf { it.groupId == groupId },
        canReorder = canReorder,
        doubleColumnDisplay = doubleColumnDisplay,
        groupId = groupId,
        lazyListStates = lazyListStates,
        lazyGridStates = lazyGridStates,
        actions = actions,
        onLocateHandled = { mainViewModel.onAction(MainAction.LocateHandled) },
        onMoveServer = { fromGuid, toGuid ->
            mainViewModel.onAction(MainAction.MoveServer(groupId, fromGuid, toGuid))
        },
        contentPadding = contentPadding
    )
}

private class ServerRowActions(
    val select: (String) -> Unit,
    val edit: (String, ProfileItem) -> Unit,
    val share: (String, ProfileItem) -> Unit,
    val more: (String, ProfileItem) -> Unit,
    val remove: (String, String) -> Unit,
)

@Composable
private fun ServerListPage(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    canReorder: Boolean,
    doubleColumnDisplay: Boolean,
    groupId: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    actions: ServerRowActions,
    onLocateHandled: () -> Unit,
    onMoveServer: (fromGuid: String, toGuid: String) -> Unit,
    contentPadding: PaddingValues
) {
    if (doubleColumnDisplay) {
        val gridState = remember(groupId) {
            lazyGridStates.getOrPut(groupId) { LazyGridState() }
        }
        val reorderableGridState = if (canReorder) {
            rememberReorderableLazyGridState(gridState) { from, to ->
                // Matixray: by the keys of the rows, their guids, not by their positions.
                onMoveServer(from.key as? String ?: return@rememberReorderableLazyGridState, to.key as? String ?: return@rememberReorderableLazyGridState)
            }
        } else null

        LocateTargetEffect(locateTarget, rows, gridState, onLocateHandled)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(gridState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = rows, key = { _, item -> item.guid }) { _, row ->
                val content: @Composable () -> Unit = {
                    ServerItemColumn(
                        row = row,
                        isSelected = row.guid == selectedGuid,
                        doubleColumnDisplay = true,
                        actions = actions
                    )
                }
                if (canReorder && reorderableGridState != null) {
                    ReorderableItem(
                        reorderableGridState,
                        key = row.guid
                    ) { isDragging ->
                        ReorderableGridItem(
                            scope = this,
                            isDragging = isDragging
                        ) { content() }
                    }
                } else {
                    content()
                }
            }
        }
    } else {
        val listState = remember(groupId) {
            lazyListStates.getOrPut(groupId) { LazyListState() }
        }
        val reorderableState = if (canReorder) {
            rememberReorderableLazyListState(listState) { from, to ->
                // Matixray: by the keys of the rows, their guids, not by their positions.
                onMoveServer(from.key as? String ?: return@rememberReorderableLazyListState, to.key as? String ?: return@rememberReorderableLazyListState)
            }
        } else null

        LocateTargetEffect(locateTarget, rows, listState, onLocateHandled)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = rows, key = { _, item -> item.guid }) { _, row ->
                if (canReorder && reorderableState != null) {
                    ReorderableItem(
                        reorderableState,
                        key = row.guid
                    ) { isDragging ->
                        ReorderableListItem(
                            scope = this,
                            isDragging = isDragging
                        ) {
                            ServerItemRow(
                                row = row,
                                isSelected = row.guid == selectedGuid,
                                actions = actions
                            )
                        }
                    }
                } else {
                    ServerItemRow(
                        row = row,
                        isSelected = row.guid == selectedGuid,
                        actions = actions
                    )
                }
            }
        }
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyListState,
    onHandled: () -> Unit,
) {
    if (target == null) return
    LaunchedEffect(target, rows) {
        val index = rows.indexOfFirst { it.guid == target.serverGuid }
        if (index < 0) return@LaunchedEffect
        state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
        onHandled()
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyGridState,
    onHandled: () -> Unit,
) {
    if (target == null) return
    LaunchedEffect(target, rows) {
        val index = rows.indexOfFirst { it.guid == target.serverGuid }
        if (index < 0) return@LaunchedEffect
        state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
        onHandled()
    }
}

@Composable
private fun ServerItemRow(
    row: ServerRowUiModel,
    isSelected: Boolean,
    actions: ServerRowActions
) {
    ServerListItem(
        row = row,
        isSelected = isSelected,
        doubleColumnDisplay = false,
        actions = actions
    )
}

@Composable
private fun ServerItemColumn(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions
) {
    Column {
        ServerListItem(
            row = row,
            isSelected = isSelected,
            doubleColumnDisplay = doubleColumnDisplay,
            actions = actions
        )
    }
}

/** Text turned a quarter circle up, for the protocol strip at the edge of a config card; it takes the room of the turned text. */
private fun Modifier.verticalText(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }
    .rotate(-90f)

private val pingGoodText = Color(0xFF0E9F6E)
private val pingMidText = Color(0xFFB7791F)

/** The round-cornered ping mark of a config card: green with the delay, red "Timeout" when the test failed. */
@Composable
private fun PingPill(delayMillis: Long) {
    if (delayMillis == 0L) return
    val failed = delayMillis < 0L
    val color = when {
        failed -> colorPingRed
        delayMillis > 800L -> pingMidText
        else -> pingGoodText
    }
    Text(
        text = if (failed) stringResource(R.string.mx_timeout) else stringResource(R.string.server_test_delay_value, delayMillis),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * One config of the list, small and flat like the design: a strip with the protocol written upward on the edge, the
 * name, the address with the ping mark, and a rail with the edit and share buttons. The selected config gets a violet
 * frame and a mint strip. The card keeps the left-to-right order of the design in every language.
 */
@Composable
private fun ServerListItem(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions
) {
    val scheme = MaterialTheme.colorScheme
    val selectedStateDescription = if (isSelected) {
        stringResource(R.string.acc_selected_server)
    } else {
        null
    }
    val shape = RoundedCornerShape(16.dp)
    val protocol = row.profile.configType.name
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(72.dp)
                .liquidGlass(
                    shape,
                    tint = if (isSelected) scheme.primary.copy(alpha = 0.07f) else Color.Unspecified
                )
                .then(
                    if (isSelected) Modifier.border(1.5.dp, scheme.primary, shape) else Modifier
                )
                .semantics {
                    if (selectedStateDescription != null) {
                        stateDescription = selectedStateDescription
                    }
                }
                .clickable { actions.select(row.guid) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Protocol strip
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .background(if (isSelected) XfMint.copy(alpha = 0.55f) else scheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = protocol,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (isSelected) scheme.onSecondaryContainer else scheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.verticalText(),
                )
            }
            // Name, address and ping
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = row.remarks,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = row.statistics,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PingPill(row.testDelayMillis)
                }
            }
            // Rail with the buttons
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!doubleColumnDisplay) {
                    RailButton(R.drawable.ic_edit_24dp, stringResource(R.string.acc_edit)) {
                        actions.edit(row.guid, row.profile)
                    }
                }
                // Share, with the rest of the server actions (delete) in the same list.
                RailButton(
                    if (doubleColumnDisplay) R.drawable.ic_more_vert_24dp else R.drawable.ic_share_24dp,
                    stringResource(if (doubleColumnDisplay) R.string.acc_more else R.string.title_configuration_share),
                ) { actions.more(row.guid, row.profile) }
            }
        }
    }
}

@Composable
private fun RailButton(@androidx.annotation.DrawableRes iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal suspend fun PagerState.navigateToPageOptimized(
    targetPage: Int,
    animateAdjacentPage: Boolean = true
) {
    if (pageCount <= 0) return
    val target = targetPage.coerceIn(0, pageCount - 1)
    val current = settledPage.coerceIn(0, pageCount - 1)
    if (target == current) return

    if (abs(target - current) == 1 && animateAdjacentPage) {
        animateScrollToPage(target)
    } else {
        scrollToPage(target)
    }
}
