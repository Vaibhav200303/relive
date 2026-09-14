package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.backup.*
import com.vaibhav.relive.platform.backup.backupAuthLog
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.date.BackupTimestampFormatter
import com.vaibhav.relive.presentation.profile.BackupRestoreViewModel
import com.vaibhav.relive.presentation.profile.formatByteSize
import com.vaibhav.relive.presentation.time.SystemClock
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import com.vaibhav.relive.ui.components.ReliveBottomSheet
import com.vaibhav.relive.ui.components.profile.ProfileChevronGlyph
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.components.timeline.HeartGlyph
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberReliveHandwritingFamily
import org.jetbrains.compose.resources.painterResource
import relive.shared.generated.resources.Res
import relive.shared.generated.resources.google_drive_logo
import relive.shared.generated.resources.google_g_logo
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var sheet by remember { mutableStateOf<String?>(null) }
    var disconnectDialog by remember { mutableStateOf(false) }
    val dims = ReliveTheme.dimensions
    ReliveBackHandler(enabled = true, onBack = onBack)

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ProfilePageHeader(
                    title = "Backup & Restore",
                    onBack = onBack,
                    titleStyle = ReliveTheme.typography.title.copy(
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    titleStartPadding = 4.dp,
                    verticalPadding = 4.dp,
                )
            },
        ) { padding ->
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = dims.spacing.xl).padding(bottom = dims.spacing.huge),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
            ) {
                BackupIntro()
                BackupStatusCard(state.remoteSummary, state.operation, viewModel::backUpNow)

                BackupSurface {
                    AccountRow(state.account?.email) {
                        backupAuthLog("account row clicked")
                        if (state.account == null) viewModel.connectAccount() else sheet = "account"
                    }
                }

                BackupSection("BACKUP SETTINGS") {
                    BackupSurface {
                        BackupSettingRow(
                            ProfileIcons.Calendar,
                            BackupIconTone.Peach,
                            "Automatic backup",
                            state.cadence.displayName(),
                        ) { sheet = "cadence" }
                        HorizontalDivider(color = ReliveTheme.colors.borderMuted)
                        BackupSettingRow(
                            ProfileIcons.Wifi,
                            BackupIconTone.Lavender,
                            "Network for backup",
                            state.networkPolicy.displayName(),
                        ) { sheet = "network" }
                    }
                }

                BackupSection("RESTORE") {
                    BackupSurface {
                        RestoreRow(state.operation, viewModel::restore)
                    }
                }

                ProtectionCard()
            }
        }
    }

    ReliveBottomSheet(
        visible = sheet != null,
        onDismissRequest = { sheet = null },
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        sheet?.let { activeSheet ->
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = dims.spacing.lg),
            ) {
                BackupSheetHeader(
                    title = when (activeSheet) {
                        "cadence" -> "Automatic backup"
                        "network" -> "Network for backup"
                        else -> "Google account"
                    },
                    subtitle = when (activeSheet) {
                        "cadence" -> "Choose how often Relive backs up your memories."
                        "network" -> "Choose which connection Relive can use."
                        else -> "Manage the Google account used for backup."
                    },
                )
                when (activeSheet) {
                    "cadence" -> BackupCadence.values().forEachIndexed { index, value ->
                        if (index > 0) BackupChoiceDivider()
                        BackupChoice(value.displayName(), state.cadence == value) {
                            viewModel.setCadence(value)
                            sheet = null
                        }
                    }
                    "network" -> BackupNetworkPolicy.values().forEachIndexed { index, value ->
                        if (index > 0) BackupChoiceDivider()
                        BackupChoice(value.displayName(), state.networkPolicy == value) {
                            viewModel.setNetworkPolicy(value)
                            sheet = null
                        }
                    }
                    "account" -> {
                        BackupChoice("Change account", false) {
                            viewModel.connectAccount()
                            sheet = null
                        }
                        BackupChoiceDivider()
                        BackupChoice("Disconnect", false) {
                            disconnectDialog = true
                            sheet = null
                        }
                    }
                }
            }
        }
    }

    if (disconnectDialog) {
        ReliveAlertDialog(
            onDismissRequest = { disconnectDialog = false },
            title = { Text("Disconnect Google account?") },
            text = { Text("Scheduled backups will stop. Existing remote backups will remain available.") },
            confirmButton = {
                TextButton(onClick = {
                    disconnectDialog = false
                    viewModel.disconnectAccount()
                }) { Text("Disconnect") }
            },
            dismissButton = { TextButton(onClick = { disconnectDialog = false }) { Text("Cancel") } },
        )
    }
    if (state.operation is BackupOperationState.AuthorizationRequired) {
        ReliveAlertDialog(
            onDismissRequest = viewModel::clearOperation,
            title = { Text("Google Drive authorization required") },
            text = { Text("Reconnect and authorize Google Drive before using backup or restore.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearOperation()
                    viewModel.connectAccount()
                }) { Text("Reconnect") }
            },
            dismissButton = { TextButton(onClick = viewModel::clearOperation) { Text("Cancel") } },
        )
    }
    state.restorePreview?.let {
        ReliveAlertDialog(
            onDismissRequest = viewModel::clearOperation,
            title = { Text("Replace current archive?") },
            text = { Text("Restore replaces all current Moments, custom timelines, memberships, tags, and attachments with this backup. This cannot be merged or undone.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Replace & restore") } },
            dismissButton = { TextButton(onClick = viewModel::clearOperation) { Text("Cancel") } },
        )
    }
    (state.operation as? BackupOperationState.Failed)?.let { failure ->
        ReliveAlertDialog(
            onDismissRequest = viewModel::clearOperation,
            title = { Text("Google Drive unavailable") },
            text = { Text(failure.message) },
            confirmButton = { TextButton(onClick = viewModel::clearOperation) { Text("OK") } },
        )
    }
    if (state.upgradeRequired) {
        ReliveAlertDialog(
            onDismissRequest = viewModel::clearUpgradeRequired,
            title = { Text("Scheduled backup is a Relive Pro feature") },
            text = { Text("Manual backup and every restore option remain free.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearUpgradeRequired()
                    onUpgrade()
                }) { Text("View Relive Pro") }
            },
            dismissButton = { TextButton(onClick = viewModel::clearUpgradeRequired) { Text("Not now") } },
        )
    }
}

@Composable
private fun BackupIntro() {
    val handwriting = rememberReliveHandwritingFamily()
    Box(
        Modifier
            .fillMaxWidth()
            // Reserve enough vertical room for the two-line handwritten note. Keeping the
            // note inside its own measured area prevents the rotated glyphs being clipped.
            .height(44.dp)
            .padding(horizontal = ReliveTheme.dimensions.spacing.xs),
    ) {
        Text(
            "Keep your memories safe in Google Drive.",
            modifier = Modifier.align(Alignment.CenterStart),
            color = ReliveTheme.colors.textSecondary,
            style = ReliveTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
        )
        Text(
            "Memories\nsafe, always",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 6.dp)
                .rotate(-4f),
            color = ReliveTheme.colors.accentMuted,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = handwriting,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            ),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 16.dp),
        ) {
            HeartGlyph(
                size = 17.dp,
                color = ReliveTheme.colors.accentMuted,
                strokeWidth = 1.5.dp,
                filled = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupStatusCard(
    summary: BackupSummary?,
    operation: BackupOperationState,
    onBackUpNow: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Surface(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 207.dp),
        shape = RoundedCornerShape(20.dp),
        color = ReliveTheme.colors.surfaceCardTranslucent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        val progress = backupOperationPresentation(operation)
        if (progress != null) {
            // Keep the progress/loading presentation unchanged while the idle state gets the
            // room needed by the editorial artwork.
            Column(
                Modifier.fillMaxWidth().padding(dims.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
            ) {
                Text("BACKUP STATUS", style = ReliveTheme.typography.eyebrow)
                BackupProgressPanel(progress)
                if (summary == null) {
                    Text("No backup yet", style = ReliveTheme.typography.title)
                    Text(
                        "Your Relive archive hasn't been\nbacked up yet.",
                        style = ReliveTheme.typography.body,
                        color = ReliveTheme.colors.textMuted,
                    )
                } else {
                    Text("Last backup", style = ReliveTheme.typography.body)
                    Text(
                        BackupTimestampFormatter.format(
                            summary.manifest.createdAt,
                            SystemClock.now().epochMilliseconds,
                        ),
                        style = ReliveTheme.typography.body,
                    )
                    val count = summary.manifest.momentCount
                    Text(
                        "${formatByteSize(summary.manifest.logicalBytes)}  •  $count " +
                            if (count == 1L) "moment" else "moments",
                        style = ReliveTheme.typography.body,
                    )
                }
                BackupActionButton(onBackUpNow, enabled = false)
            }
        } else {
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(24.dp),
            ) {
                // `maxWidth` is the post-padding width. The reference phone is 360dp wide, so
                // its 312dp inner width still gets the layered composition; genuinely narrow
                // windows get a readable stack.
                val stacked = LocalDensity.current.fontScale >= 1.3f || maxWidth < 300.dp
                if (stacked) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                            verticalAlignment = Alignment.Top,
                        ) {
                            BackupStatusDetails(summary, operation, Modifier.weight(1f))
                            BackupCloudArtwork(Modifier.size(176.dp))
                        }
                        BackupActionButton(onBackUpNow)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(159.dp)) {
                        Column(
                            Modifier.widthIn(max = 208.dp).fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            BackupStatusDetails(summary, operation)
                            BackupActionButton(onBackUpNow)
                        }
                        BackupCloudArtwork(
                            Modifier.size(176.dp).align(Alignment.TopEnd).offset(y = (-2).dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupStatusDetails(
    summary: BackupSummary?,
    operation: BackupOperationState,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm)) {
        Text("LAST BACKUP", color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.eyebrow)
        Text(
            summary?.let {
                BackupTimestampFormatter.format(it.manifest.createdAt, SystemClock.now().epochMilliseconds)
            } ?: "No backup yet",
            color = ReliveTheme.colors.textPrimary,
            style = ReliveTheme.typography.title.copy(
                fontSize = if (summary == null) 22.sp else 24.sp,
                lineHeight = if (summary == null) 28.sp else 30.sp,
            ),
        )
        if (summary != null) {
            val count = summary.manifest.momentCount
            Text(
                "${formatByteSize(summary.manifest.logicalBytes)}  •  $count ${if (count == 1L) "moment" else "moments"}",
                color = ReliveTheme.colors.textMuted,
                style = ReliveTheme.typography.caption,
            )
            BackupHealthLine(operation)
        } else if (operation.needsAttention()) {
            BackupHealthLine(operation)
        } else {
            Text(
                "Ready for your first backup.",
                modifier = Modifier.widthIn(max = 132.dp),
                color = ReliveTheme.colors.textMuted,
                style = ReliveTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun BackupHealthLine(operation: BackupOperationState) {
    val label = when (operation) {
        BackupOperationState.WaitingForWifi -> "Waiting for Wi-Fi"
        BackupOperationState.QuotaUnavailable -> "Drive storage unavailable"
        is BackupOperationState.Corrupt -> "Backup needs attention"
        is BackupOperationState.UnsupportedVersion -> "Backup version unsupported"
        else -> "All backed up"
    }
    val positive = operation !is BackupOperationState.QuotaUnavailable &&
        operation !is BackupOperationState.Corrupt &&
        operation !is BackupOperationState.UnsupportedVersion
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (positive) ProfileIcons.Check else ProfileIcons.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (positive) Color(0xFF55A873) else ReliveTheme.colors.actionDestructive,
        )
        Spacer(Modifier.width(ReliveTheme.dimensions.spacing.xs))
        Text(label, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.caption)
    }
}

private fun BackupOperationState.needsAttention(): Boolean =
    this is BackupOperationState.QuotaUnavailable ||
        this is BackupOperationState.Corrupt ||
        this is BackupOperationState.UnsupportedVersion

@Composable
private fun BackupProgressPanel(progress: BackupOperationPresentation) {
    Text(progress.label, style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
    if (progress.fraction == null) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            LoadingIndicator(color = ReliveTheme.colors.accent)
        }
    } else {
        Text("${(progress.fraction * 100).toInt()}%", style = ReliveTheme.typography.title)
        LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BackupActionButton(onClick: () -> Unit, enabled: Boolean = true) {
    // The visual button is kept close to the reference's 43dp height; the surrounding box keeps
    // a full 48dp semantic target for switch/accessibility users.
    Box(
        Modifier
            .width(170.dp)
            .height(ReliveTheme.dimensions.minTouchTarget)
            .semantics(mergeDescendants = true) {
                contentDescription = "Back up now"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.width(170.dp).height(43.dp),
            shape = ReliveTheme.shapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled) ReliveTheme.colors.accent else ReliveTheme.colors.borderMuted,
                contentColor = if (enabled) ReliveTheme.colors.textOnAccent else ReliveTheme.colors.textMuted,
                disabledContainerColor = ReliveTheme.colors.borderMuted,
                disabledContentColor = ReliveTheme.colors.textMuted,
            ),
        ) {
            Icon(ProfileIcons.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(ReliveTheme.dimensions.spacing.sm))
            Text("Back up now", style = ReliveTheme.typography.action)
        }
    }
}

private data class BackupOperationPresentation(val label: String, val fraction: Float?)

private fun backupOperationPresentation(operation: BackupOperationState): BackupOperationPresentation? = when (operation) {
    BackupOperationState.Preparing -> BackupOperationPresentation("Preparing your backup…", null)
    is BackupOperationState.Uploading -> progressPresentation(operation.progress)
    BackupOperationState.WaitingForWifi -> BackupOperationPresentation("Waiting for Wi-Fi…", null)
    else -> null
}

private fun progressPresentation(progress: BackupProgress): BackupOperationPresentation {
    val fraction = if (progress.totalBytes > 0) {
        (progress.completedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
    } else {
        null
    }
    return BackupOperationPresentation("${progress.phase} your archive…", fraction)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestoreRow(operation: BackupOperationState, onRestore: () -> Unit) {
    val inProgress = when (operation) {
        BackupOperationState.DiscoveringRestore,
        BackupOperationState.PreparingRestore,
        is BackupOperationState.Downloading -> true
        else -> false
    }
    val presentation = when (operation) {
        BackupOperationState.DiscoveringRestore -> BackupOperationPresentation("Checking your Google Drive backup…", null)
        BackupOperationState.PreparingRestore -> BackupOperationPresentation("Preparing to restore your archive…", null)
        is BackupOperationState.Downloading -> progressPresentation(operation.progress)
        else -> null
    }
    androidx.compose.animation.AnimatedContent(targetState = inProgress, label = "restore-row") { showingProgress ->
        if (!showingProgress) {
            BackupSettingRow(
                ProfileIcons.Restore,
                BackupIconTone.Peach,
                "Restore from Google Drive",
                "Replace this device’s data with a backup",
                onRestore,
            )
        } else {
            RestoreProgressRow(presentation ?: BackupOperationPresentation("Restoring your archive…", null))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestoreProgressRow(progress: BackupOperationPresentation) {
    val dims = ReliveTheme.dimensions
    val treatment = backupIconTreatment(BackupIconTone.Peach)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(treatment.background),
            contentAlignment = Alignment.Center,
        ) {
            if (progress.fraction == null) {
                LoadingIndicator(color = ReliveTheme.colors.accent)
            } else {
                LoadingIndicator(progress = { progress.fraction }, color = ReliveTheme.colors.accent)
            }
        }
        Spacer(Modifier.width(dims.spacing.md))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            Text(
                progress.label,
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            progress.fraction?.let { fraction ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(dims.spacing.xs))
                    Text(
                        "${(fraction * 100).toInt()}%",
                        color = ReliveTheme.colors.textMuted,
                        style = ReliveTheme.typography.caption,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.sm)) {
        Text(
            title,
            modifier = Modifier.padding(start = ReliveTheme.dimensions.spacing.xs),
            color = ReliveTheme.colors.textSecondary,
            style = ReliveTheme.typography.eyebrow,
        )
        content()
    }
}

@Composable
private fun BackupSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ReliveTheme.colors.surfaceCardTranslucent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) { Column(content = content) }
}

private enum class BackupIconTone {
    Blush,
    Lavender,
    Peach,
}

private data class BackupIconTreatment(
    val tint: Color,
    val background: Color,
)

/**
 * Backup tiles use semantic theme roles with different opacity treatments rather than fixed
 * colors. This keeps the blush/lavender/peach rhythm visible in Warm Journal while adapting
 * naturally when a person changes palette or switches appearance mode.
 */
@Composable
private fun backupIconTreatment(tone: BackupIconTone): BackupIconTreatment {
    val colors = ReliveTheme.colors
    return when (tone) {
        BackupIconTone.Blush -> BackupIconTreatment(
            tint = colors.accentMuted,
            background = colors.tint.copy(alpha = 0.72f),
        )
        BackupIconTone.Lavender -> BackupIconTreatment(
            tint = colors.accent,
            background = colors.tint.copy(alpha = 0.42f),
        )
        BackupIconTone.Peach -> BackupIconTreatment(
            tint = colors.accentMuted,
            background = colors.accent.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun AccountRow(email: String?, onClick: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = email?.let { "Google account, $it, Connected" } ?: "Connect Google account"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.google_g_logo),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.width(dims.spacing.md))
        Column(Modifier.weight(1f)) {
            Text("GOOGLE ACCOUNT", color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.eyebrow)
            Text(email ?: "Connect Google account", color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.action)
            Text(
                if (email == null) "Required for backup and restore" else "Connected",
                color = ReliveTheme.colors.textMuted,
                style = ReliveTheme.typography.caption,
            )
        }
        ProfileChevronGlyph()
    }
}

@Composable
private fun BackupSettingRow(
    icon: ImageVector,
    tone: BackupIconTone,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val treatment = backupIconTreatment(tone)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $value"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(treatment.background),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(23.dp), tint = treatment.tint) }
        Spacer(Modifier.width(dims.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.action)
            Text(value, color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.caption)
        }
        ProfileChevronGlyph()
    }
}

@Composable
private fun ProtectionCard() {
    val dims = ReliveTheme.dimensions
    val treatment = backupIconTreatment(BackupIconTone.Blush)
    Surface(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
        shape = RoundedCornerShape(20.dp),
        color = ReliveTheme.colors.tint.copy(alpha = 0.58f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(treatment.background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ProfileIcons.ShieldCheck, null, Modifier.size(26.dp), tint = treatment.tint)
            }
            Spacer(Modifier.width(dims.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    "Your memories stay protected",
                    color = ReliveTheme.colors.textPrimary,
                    style = ReliveTheme.typography.title.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    "Backups use Relive’s private Google Drive app storage and are never shared by Relive.",
                    color = ReliveTheme.colors.textSecondary,
                    style = ReliveTheme.typography.caption,
                )
            }
            Spacer(Modifier.width(dims.spacing.sm))
            BotanicalSprig(Modifier.width(52.dp).height(78.dp))
        }
    }
}

@Composable
private fun BackupSheetHeader(title: String, subtitle: String) {
    val dims = ReliveTheme.dimensions
    Column(
        Modifier.fillMaxWidth().padding(horizontal = dims.spacing.xl),
    ) {
        Box(
            Modifier.fillMaxWidth().height(dims.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.width(dims.minTouchTarget).height(dims.spacing.xs)
                    .background(ReliveTheme.colors.textMuted, RoundedCornerShape(dims.radii.full)),
            )
        }
        Text(title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.title)
        Spacer(Modifier.height(dims.spacing.xs))
        Text(subtitle, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.body)
        Spacer(Modifier.height(dims.spacing.md))
    }
}

@Composable
private fun BackupChoiceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl),
        color = ReliveTheme.colors.borderMuted,
    )
}

@Composable
private fun BackupChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = ReliveTheme.dimensions.spacing.xl, vertical = ReliveTheme.dimensions.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.RadioButton
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = ReliveTheme.colors.textPrimary,
            style = ReliveTheme.typography.body,
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun BackupCloudArtwork(modifier: Modifier = Modifier) {
    val accent = ReliveTheme.colors.accent
    val surface = ReliveTheme.colors.surfaceCard
    val handwriting = rememberReliveHandwritingFamily()
    Box(modifier.semantics { contentDescription = "Google Drive backup illustration" }) {
        Canvas(Modifier.matchParentSize()) {
            val stem = accent.copy(alpha = 0.36f)

            // Pointed, translucent foliage follows the reference's curved botanical silhouette.
            val upperStem = Path().apply {
                moveTo(size.width * 0.72f, size.height * 0.35f)
                cubicTo(
                    size.width * 0.80f,
                    size.height * 0.27f,
                    size.width * 0.88f,
                    size.height * 0.17f,
                    size.width * 0.93f,
                    size.height * 0.07f,
                )
            }
            drawPath(upperStem, stem, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            drawBotanicalLeaf(
                Offset(size.width * 0.84f, size.height * 0.22f),
                Offset(size.width * 0.73f, size.height * 0.10f),
                size.width * 0.045f,
                accent,
            )
            drawBotanicalLeaf(
                Offset(size.width * 0.88f, size.height * 0.17f),
                Offset(size.width * 0.98f, size.height * 0.10f),
                size.width * 0.04f,
                accent,
            )
            drawBotanicalLeaf(
                Offset(size.width * 0.91f, size.height * 0.11f),
                Offset(size.width * 0.89f, size.height * 0.01f),
                size.width * 0.035f,
                accent,
            )

            val lowerStem = Path().apply {
                moveTo(size.width * 0.39f, size.height * 0.94f)
                cubicTo(
                    size.width * 0.32f,
                    size.height * 0.84f,
                    size.width * 0.26f,
                    size.height * 0.69f,
                    size.width * 0.20f,
                    size.height * 0.53f,
                )
            }
            drawPath(lowerStem, stem, style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))
            drawLine(stem, Offset(size.width * 0.31f, size.height * 0.82f), Offset(size.width * 0.14f, size.height * 0.72f), 1.2.dp.toPx(), StrokeCap.Round)
            drawLine(stem, Offset(size.width * 0.26f, size.height * 0.70f), Offset(size.width * 0.38f, size.height * 0.61f), 1.2.dp.toPx(), StrokeCap.Round)
            drawBotanicalLeaf(
                Offset(size.width * 0.31f, size.height * 0.82f),
                Offset(size.width * 0.08f, size.height * 0.70f),
                size.width * 0.072f,
                accent,
            )
            drawBotanicalLeaf(
                Offset(size.width * 0.27f, size.height * 0.72f),
                Offset(size.width * 0.14f, size.height * 0.55f),
                size.width * 0.068f,
                accent,
            )
            drawBotanicalLeaf(
                Offset(size.width * 0.26f, size.height * 0.70f),
                Offset(size.width * 0.43f, size.height * 0.58f),
                size.width * 0.065f,
                accent,
            )
            drawBotanicalLeaf(
                Offset(size.width * 0.22f, size.height * 0.59f),
                Offset(size.width * 0.19f, size.height * 0.42f),
                size.width * 0.06f,
                accent,
            )

            // One compact cloud keeps the official Drive mark as the sole visual focus.
            drawCircle(
                accent.copy(alpha = 0.07f),
                size.minDimension * 0.33f,
                Offset(size.width * 0.60f, size.height * 0.43f),
            )
            drawOval(
                accent.copy(alpha = 0.10f),
                Offset(size.width * 0.34f, size.height * 0.68f),
                Size(size.width * 0.55f, size.height * 0.08f),
            )
            drawRoundRect(
                surface,
                Offset(size.width * 0.29f, size.height * 0.40f),
                Size(size.width * 0.62f, size.height * 0.29f),
                androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
            )
            drawCircle(
                surface,
                size.minDimension * 0.13f,
                Offset(size.width * 0.38f, size.height * 0.51f),
            )
            drawCircle(
                surface,
                size.minDimension * 0.20f,
                Offset(size.width * 0.60f, size.height * 0.38f),
            )
            drawCircle(
                surface,
                size.minDimension * 0.13f,
                Offset(size.width * 0.80f, size.height * 0.51f),
            )
        }
        Image(
            painter = painterResource(Res.drawable.google_drive_logo),
            contentDescription = null,
            modifier = Modifier.size(46.dp).align(Alignment.Center).offset(x = 18.dp, y = (-4).dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            "safe in Drive",
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-3).dp, y = (-1).dp).rotate(-4f),
            color = accent.copy(alpha = 0.72f),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = handwriting,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            ),
        )
    }
}

@Composable
private fun BotanicalSprig(modifier: Modifier = Modifier) {
    val accent = ReliveTheme.colors.accent
    Canvas(modifier) {
        val stem = accent.copy(alpha = 0.40f)
        val branch = Path().apply {
            moveTo(size.width * 0.55f, size.height)
            cubicTo(size.width * 0.52f, size.height * 0.70f, size.width * 0.58f, size.height * 0.35f, size.width * 0.68f, 0f)
        }
        drawPath(branch, stem, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawBotanicalLeaf(Offset(size.width * 0.55f, size.height * 0.68f), Offset(size.width * 0.02f, size.height * 0.43f), size.width * 0.16f, accent)
        drawBotanicalLeaf(Offset(size.width * 0.58f, size.height * 0.48f), Offset(size.width * 0.98f, size.height * 0.22f), size.width * 0.15f, accent)
        drawBotanicalLeaf(Offset(size.width * 0.64f, size.height * 0.26f), Offset(size.width * 0.58f, 0f), size.width * 0.14f, accent)
    }
}

private fun DrawScope.drawBotanicalLeaf(base: Offset, tip: Offset, halfWidth: Float, accent: Color) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val normalX = -dy / length * halfWidth
    val normalY = dx / length * halfWidth
    val leafPath = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            base.x + dx * 0.22f + normalX * 0.45f,
            base.y + dy * 0.22f + normalY * 0.45f,
            base.x + dx * 0.68f + normalX,
            base.y + dy * 0.68f + normalY,
            tip.x,
            tip.y,
        )
        cubicTo(
            base.x + dx * 0.68f - normalX,
            base.y + dy * 0.68f - normalY,
            base.x + dx * 0.22f - normalX * 0.45f,
            base.y + dy * 0.22f - normalY * 0.45f,
            base.x,
            base.y,
        )
        close()
    }
    drawPath(
        leafPath,
        Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.12f), accent.copy(alpha = 0.31f)),
            start = base,
            end = tip,
        ),
    )
    drawLine(
        accent.copy(alpha = 0.18f),
        base,
        Offset(base.x + dx * 0.82f, base.y + dy * 0.82f),
        0.7.dp.toPx(),
        StrokeCap.Round,
    )
}

private fun BackupCadence.displayName(): String = when (this) {
    BackupCadence.Off -> "Off"
    BackupCadence.Daily -> "Daily"
    BackupCadence.Weekly -> "Weekly"
    BackupCadence.Monthly -> "Monthly"
}

private fun BackupNetworkPolicy.displayName(): String = when (this) {
    BackupNetworkPolicy.WifiOnly -> "Wi-Fi only"
    BackupNetworkPolicy.WifiOrCellular -> "Wi-Fi or cellular"
}
