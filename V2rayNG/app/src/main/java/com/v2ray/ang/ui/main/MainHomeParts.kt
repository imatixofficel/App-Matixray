package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.clip
import com.v2ray.ang.ui.compose.GlassPillShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.SlidingPillButton

/** The bar under the live numbers that names the selected server; a tap opens the server list. */
@Composable
fun SelectedServerCard(name: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SlidingPillButton(
        text = name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.mx_selected_server_none),
        subtitle = stringResource(R.string.mx_selected_server_hint),
        onClick = onClick,
        modifier = modifier,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
    )
}

/** The switch under the live numbers: when the server is cut, the app connects to the best server by itself. */
@Composable
fun AutoSwitchRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(GlassPillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 22.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.mx_auto_switch_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.mx_auto_switch_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
