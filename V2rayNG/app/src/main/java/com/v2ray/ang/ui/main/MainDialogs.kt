package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.DeleteConfirmDialog

data class ServerDeleteTarget(
    val guid: String,
    val profileName: String,
) {
    companion object {
        val Saver = listSaver<ServerDeleteTarget?, String>(
            save = { target -> target?.let { listOf(it.guid, it.profileName) }.orEmpty() },
            restore = { ServerDeleteTarget(it[0], it[1]) },
        )
    }
}

@Composable
fun MainDialogs(
    showDelAllConfirm: Boolean,
    onDismissDelAll: () -> Unit,
    onConfirmDelAll: () -> Unit,
    showDelDuplicateConfirm: Boolean,
    onDismissDelDuplicate: () -> Unit,
    onConfirmDelDuplicate: () -> Unit,
    showDelInvalidConfirm: Boolean,
    onDismissDelInvalid: () -> Unit,
    onConfirmDelInvalid: () -> Unit,
    showRemoveConfirm: ServerDeleteTarget?,
    onDismissRemove: () -> Unit,
    onConfirmRemove: (String) -> Unit,
) {
    if (showDelAllConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_visible_profiles),
            onConfirm = onConfirmDelAll,
            onDismiss = onDismissDelAll
        )
    }
    if (showDelDuplicateConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_duplicate_profiles),
            onConfirm = onConfirmDelDuplicate,
            onDismiss = onDismissDelDuplicate
        )
    }
    if (showDelInvalidConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_invalid_profiles),
            onConfirm = onConfirmDelInvalid,
            onDismiss = onDismissDelInvalid
        )
    }
    showRemoveConfirm?.let { target ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_profile),
            itemName = target.profileName,
            onConfirm = { onConfirmRemove(target.guid) },
            onDismiss = onDismissRemove
        )
    }
}

/** Matixray: asks where configs the user just added go: a new group (named here) or a group they already have. */
@Composable
fun ImportNameDialog(
    groups: List<com.v2ray.ang.dto.GroupMapItem>,
    onConfirm: (name: String, targetGroupId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(stringResource(R.string.mx_import_name_title)) },
        text = {
            ImportTargetPicker(
                groups = groups,
                targetId = target,
                onTargetChange = { target = it },
                name = name,
                onNameChange = { name = it },
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(name, target) }) {
                androidx.compose.material3.Text(stringResource(R.string.mx_import_name_ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(stringResource(android.R.string.cancel))
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    )
}

/**
 * Matixray: the update dialog. "Update now" downloads the new APK inside the app, shows the progress and then opens the
 * system installer; a link that is not a direct APK link (a release page) is opened in the browser instead.
 * The card has a colored band around it, a round badge with the version on top and a solid update button.
 */
@Composable
fun InAppUpdateDialog(version: String, url: String?, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by com.v2ray.ang.handler.ApkUpdater.state.collectAsStateWithLifecycle()
    val downloading = state is com.v2ray.ang.handler.ApkUpdater.State.Downloading
    androidx.compose.runtime.LaunchedEffect(state) {
        val ready = state as? com.v2ray.ang.handler.ApkUpdater.State.Ready ?: return@LaunchedEffect
        com.v2ray.ang.handler.ApkUpdater.install(context, ready.file)
    }
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val outerShape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val innerShape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
    val band = androidx.compose.ui.graphics.Brush.sweepGradient(
        listOf(
            androidx.compose.ui.graphics.Color(0xFF0B63F6),
            androidx.compose.ui.graphics.Color(0xFF8E54E9),
            androidx.compose.ui.graphics.Color(0xFFFF4E8A),
            androidx.compose.ui.graphics.Color(0xFFFFB02E),
            androidx.compose.ui.graphics.Color(0xFF2EBD6B),
            androidx.compose.ui.graphics.Color(0xFF0B63F6),
        )
    )
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !downloading,
            dismissOnClickOutside = !downloading,
        ),
    ) {
        // The colored band: a gradient box with the card inside it, leaving 3 dp of the gradient showing all around.
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .clip(outerShape)
                .background(band)
                .padding(3.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .clip(innerShape)
                    .background(scheme.surface)
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(com.v2ray.ang.ui.compose.XfBlue),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_check_update_24dp),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = androidx.compose.ui.Modifier.size(30.dp),
                    )
                }
                androidx.compose.material3.Text(
                    text = stringResource(R.string.mx_update_notice_title),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = scheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                androidx.compose.material3.Text(
                    text = stringResource(R.string.mx_update_notice_body, version),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                when (val s = state) {
                    is com.v2ray.ang.handler.ApkUpdater.State.Downloading -> {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { s.percent / 100f },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.CircleShape),
                        )
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.mx_update_downloading, s.percent),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    is com.v2ray.ang.handler.ApkUpdater.State.Ready ->
                        androidx.compose.material3.Text(stringResource(R.string.mx_update_ready), color = scheme.onSurface)
                    com.v2ray.ang.handler.ApkUpdater.State.Failed ->
                        androidx.compose.material3.Text(stringResource(R.string.mx_update_failed), color = scheme.error)
                    com.v2ray.ang.handler.ApkUpdater.State.Idle -> {}
                }
                androidx.compose.material3.Button(
                    enabled = !downloading,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    onClick = {
                        val ready = state as? com.v2ray.ang.handler.ApkUpdater.State.Ready
                        when {
                            ready != null -> com.v2ray.ang.handler.ApkUpdater.install(context, ready.file)
                            com.v2ray.ang.handler.ApkUpdater.isApkUrl(url) -> com.v2ray.ang.handler.ApkUpdater.start(context, url!!)
                            else -> {
                                url?.let { com.v2ray.ang.util.Utils.openUri(context, it) }
                                onDismiss()
                            }
                        }
                    },
                ) {
                    androidx.compose.material3.Text(
                        stringResource(if (state is com.v2ray.ang.handler.ApkUpdater.State.Ready) R.string.mx_update_install else R.string.update_now),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
                androidx.compose.material3.TextButton(
                    enabled = !downloading,
                    onClick = {
                        com.v2ray.ang.handler.ApkUpdater.reset()
                        onDismiss()
                    },
                ) { androidx.compose.material3.Text(stringResource(R.string.mx_update_later)) }
            }
        }
    }
}
