package com.v2ray.ang.ui.xfinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.XfPurple

@Composable
fun XfinderConfigsScreen(onBack: () -> Unit, viewModel: XfinderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    XfinderPage(
        title = stringResource(R.string.xf_nav_configs),
        onBack = onBack,
        isLoading = state.isLoading,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XfinderBadge(stringResource(R.string.xf_badge))
                Text(
                    text = stringResource(R.string.xf_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SourceButton(
                    label = stringResource(R.string.xf_tab_top),
                    selected = state.source == XfinderSource.TOP20,
                    onClick = { viewModel.selectSource(XfinderSource.TOP20) },
                    modifier = Modifier.weight(1f),
                )
                SourceButton(
                    label = stringResource(R.string.xf_tab_all),
                    selected = state.source == XfinderSource.ALL,
                    onClick = { viewModel.selectSource(XfinderSource.ALL) },
                    modifier = Modifier.weight(1f),
                )
                val refreshLabel = stringResource(R.string.xf_refresh)
                IconButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.semantics { contentDescription = refreshLabel },
                ) { RefreshIcon() }
            }
            Button(
                onClick = viewModel::addSubscription,
                enabled = !state.isAddingSubscription,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (state.isAddingSubscription) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(stringResource(R.string.mx_xfinder_add_sub), fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(R.string.mx_xfinder_sub_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.items.isNotEmpty() -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    ) {
                        items(state.items, key = { it.raw }) { config ->
                            ConfigRow(config = config)
                        }
                    }

                    state.isLoading -> StatusMessage(stringResource(R.string.xf_loading))
                    state.hasError -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.xf_error_load),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.xf_retry))
                        }
                    }

                    else -> StatusMessage(stringResource(R.string.xf_empty))
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.semantics { this.selected = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(label, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.semantics { this.selected = false },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { Text(label, color = MaterialTheme.colorScheme.onSurface) }
    }
}

/** A config of the list, shown with its ping; the list only previews what the subscription brings, nothing is added from a row. */
@Composable
private fun ConfigRow(config: XfinderConfig) {
    val level = pingLevelOf(config.pingMs)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = XfinderCardBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XfinderBadge(config.protocol)
                    Text(
                        text = config.name,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MonoLtrText(
                    text = if (config.port.isEmpty()) config.server else "${config.server}:${config.port}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            config.pingMs?.let { ms ->
                val color = pingColor(level, MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = stringResource(R.string.xf_ping_ms, ms),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
