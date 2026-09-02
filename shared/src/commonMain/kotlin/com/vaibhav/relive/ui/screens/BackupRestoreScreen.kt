package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import com.vaibhav.relive.ui.components.ReliveBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.backup.*
import com.vaibhav.relive.presentation.profile.BackupRestoreViewModel
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.platform.backup.backupAuthLog
import com.vaibhav.relive.presentation.date.BackupTimestampFormatter
import com.vaibhav.relive.presentation.profile.formatByteSize
import com.vaibhav.relive.presentation.time.SystemClock
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupRestoreScreen(viewModel: BackupRestoreViewModel, onBack: () -> Unit, onUpgrade: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var sheet by remember { mutableStateOf<String?>(null) }
    var disconnectDialog by remember { mutableStateOf(false) }
    val summary = state.remoteSummary
    Scaffold(containerColor = ReliveTheme.colors.bgCanvas, topBar = {
        ProfilePageHeader("Backup & Restore", onBack)
    }) { padding ->
        Column(Modifier.padding(padding).padding(start = 32.dp, top = 24.dp, end = 32.dp, bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("Keep your memories safe in Google Drive.", style = ReliveTheme.typography.subtitle)
            BackupStatusCard(summary = summary, operation = state.operation, onBackUpNow = viewModel::backUpNow)
            BackupSection("GOOGLE ACCOUNT") {
                BackupRow(state.account?.email ?: "Connect Google account", onClick = { backupAuthLog("account row clicked"); if (state.account == null) viewModel.connectAccount() else sheet = "account" })
            }
            BackupSection("BACKUP SETTINGS") {
                BackupRow("Automatic backup", state.cadence.name) { sheet = "cadence" }
                BackupRow("Network for backup", if (state.networkPolicy == BackupNetworkPolicy.WifiOnly) "Wi-Fi only" else "Wi-Fi or cellular") { sheet = "network" }
            }
            BackupSection("RESTORE") {
                BackupRow("Restore from Google Drive", onClick = viewModel::restore)
                RestoreProgressCard(operation = state.operation)
            }
            HorizontalDivider(color = ReliveTheme.colors.borderMuted)
            Text("Your memories stay protected\n\nBackups use Relive's private\nGoogle Drive app storage.", color = ReliveTheme.colors.textMuted)
        }
    }
    ReliveBottomSheet(
        visible = sheet != null,
        onDismissRequest = { sheet = null },
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        when (sheet) {
            "cadence" -> BackupCadence.values().forEach { value -> BackupChoice(value.name, state.cadence == value) { viewModel.setCadence(value); sheet = null } }
            "network" -> BackupNetworkPolicy.values().forEach { value -> BackupChoice(if (value == BackupNetworkPolicy.WifiOnly) "Wi-Fi only" else "Wi-Fi or cellular", state.networkPolicy == value) { viewModel.setNetworkPolicy(value); sheet = null } }
            "account" -> { BackupChoice("Change account", false) { viewModel.connectAccount(); sheet = null }; BackupChoice("Disconnect", false) { disconnectDialog = true; sheet = null } }
        }
    }
    if (disconnectDialog) ReliveAlertDialog(onDismissRequest = { disconnectDialog = false }, title = { Text("Disconnect Google account?") }, text = { Text("Scheduled backups will stop. Existing remote backups will remain available.") }, confirmButton = { TextButton(onClick = { disconnectDialog = false; viewModel.disconnectAccount() }) { Text("Disconnect") } }, dismissButton = { TextButton(onClick = { disconnectDialog = false }) { Text("Cancel") } })
    if (state.operation is BackupOperationState.AuthorizationRequired) ReliveAlertDialog(onDismissRequest = viewModel::clearOperation, title = { Text("Google Drive authorization required") }, text = { Text("Reconnect and authorize Google Drive before using backup or restore.") }, confirmButton = { TextButton(onClick = { viewModel.clearOperation(); viewModel.connectAccount() }) { Text("Reconnect") } }, dismissButton = { TextButton(onClick = viewModel::clearOperation) { Text("Cancel") } })
    state.restorePreview?.let { preview ->
        ReliveAlertDialog(
            onDismissRequest = viewModel::clearOperation,
            title = { Text("Replace current archive?") },
            text = { Text("Restore replaces all current Moments, custom timelines, memberships, tags, and attachments with this backup. This cannot be merged or undone.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Replace & restore") } },
            dismissButton = { TextButton(onClick = viewModel::clearOperation) { Text("Cancel") } },
        )
    }
    (state.operation as? BackupOperationState.Failed)?.let { failure ->
        ReliveAlertDialog(onDismissRequest = viewModel::clearOperation, title = { Text("Google Drive unavailable") }, text = { Text(failure.message) }, confirmButton = { TextButton(onClick = viewModel::clearOperation) { Text("OK") } })
    }
    if (state.upgradeRequired) ReliveAlertDialog(
        onDismissRequest = viewModel::clearUpgradeRequired,
        title = { Text("Scheduled backup is a Relive Pro feature") },
        text = { Text("Manual backup and every restore option remain free.") },
        confirmButton = { TextButton(onClick = { viewModel.clearUpgradeRequired(); onUpgrade() }) { Text("View Relive Pro") } },
        dismissButton = { TextButton(onClick = viewModel::clearUpgradeRequired) { Text("Not now") } },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable private fun BackupStatusCard(summary: BackupSummary?, operation: BackupOperationState, onBackUpNow: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Column(
        Modifier.fillMaxWidth().padding(dims.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        Text("BACKUP STATUS", style = ReliveTheme.typography.eyebrow)
        BackupOperationPresentation(operation)?.let { progress ->
            Text(progress.label, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
            if (progress.fraction == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(color = ReliveTheme.colors.accent)
                }
            } else {
                Text("${(progress.fraction * 100).toInt()}%", style = ReliveTheme.typography.title)
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (summary == null) {
            Text("No backup yet", style = ReliveTheme.typography.title)
            Text("Your Relive archive hasn't been\nbacked up yet.", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textMuted)
            BackupActionButton(onClick = onBackUpNow, enabled = BackupOperationPresentation(operation) == null)
        } else {
            Text("Last backup", style = ReliveTheme.typography.body)
            Text(BackupTimestampFormatter.format(summary.manifest.createdAt, SystemClock.now().epochMilliseconds), style = ReliveTheme.typography.body)
            val count = summary.manifest.momentCount
            Text("${formatByteSize(summary.manifest.logicalBytes)}  •  $count ${if (count == 1L) "moment" else "moments"}", style = ReliveTheme.typography.body)
            BackupActionButton(onClick = onBackUpNow, enabled = BackupOperationPresentation(operation) == null)
        }
    }
}

@Composable
private fun BackupActionButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ReliveTheme.colors.borderMuted,
            contentColor = ReliveTheme.colors.textPrimary,
            disabledContainerColor = ReliveTheme.colors.borderMuted,
            disabledContentColor = ReliveTheme.colors.textMuted,
        ),
        border = androidx.compose.foundation.BorderStroke(
            ReliveTheme.dimensions.stroke.hairline,
            ReliveTheme.colors.borderMuted,
        ),
    ) { Text("Back up now") }
}

private data class BackupOperationPresentation(val label: String, val fraction: Float?)

private fun BackupOperationPresentation(operation: BackupOperationState): BackupOperationPresentation? = when (operation) {
    BackupOperationState.Preparing -> BackupOperationPresentation("Preparing your backup…", null)
    is BackupOperationState.Uploading -> progressPresentation(operation.progress)
    BackupOperationState.WaitingForWifi -> BackupOperationPresentation("Waiting for Wi-Fi…", null)
    else -> null
}

private fun progressPresentation(progress: BackupProgress): BackupOperationPresentation {
    val fraction = if (progress.totalBytes > 0) (progress.completedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f) else null
    return BackupOperationPresentation("${progress.phase} your archive…", fraction)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestoreProgressCard(operation: BackupOperationState) {
    val presentation = when (operation) {
        BackupOperationState.PreparingRestore -> BackupOperationPresentation("Checking your Google Drive backup…", null)
        is BackupOperationState.Downloading -> progressPresentation(operation.progress)
        else -> null
    } ?: return
    val dims = ReliveTheme.dimensions
    androidx.compose.animation.AnimatedContent(
        targetState = presentation,
        label = "restore-progress",
    ) { progress ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(progress.label, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
            if (progress.fraction == null) {
                LoadingIndicator(color = ReliveTheme.colors.accent)
            } else {
                LoadingIndicator(progress = { progress.fraction }, color = ReliveTheme.colors.accent)
                Text("${(progress.fraction * 100).toInt()}%", style = ReliveTheme.typography.title)
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable private fun BackupChoice(label: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); RadioButton(selected = selected, onClick = onClick) } }

@Composable private fun BackupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = ReliveTheme.typography.eyebrow)
        content()
    }
}

@Composable private fun BackupRow(label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column { Text(label, style = ReliveTheme.typography.body); value?.let { Text(it, style = ReliveTheme.typography.body) } }
        Text("›", style = ReliveTheme.typography.title)
    }
}
