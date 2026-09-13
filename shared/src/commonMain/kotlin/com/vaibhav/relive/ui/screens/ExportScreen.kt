package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.exporting.DiaryPaper
import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportResult
import com.vaibhav.relive.domain.exporting.ExportScope
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.platform.exporting.rememberExportFileHandle
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.exporting.ExportFlowStage
import com.vaibhav.relive.presentation.exporting.ExportUiState
import com.vaibhav.relive.presentation.exporting.ExportViewModel
import com.vaibhav.relive.presentation.exporting.flowStage
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.reliveForwardBackward
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    mediaStore: MediaStore,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val files = rememberExportFileHandle()
    val photos = rememberMediaPickerHandle(mediaStore)
    val stage = state.operation.flowStage()
    val animatedStage = remember(stage) {
        ExportAnimatedStage(
            stage = stage,
            result = (state.operation as? ExportOperationState.Ready)?.result,
            errorMessage = (state.operation as? ExportOperationState.Failed)?.message,
        )
    }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    DisposableEffect(viewModel) { onDispose(viewModel::close) }
    LaunchedEffect(state.upgradeRequired) {
        if (state.upgradeRequired) {
            viewModel.clearUpgradeRequired()
            onUpgrade()
        }
    }

    val motion = ReliveTheme.motion
    val reduceMotion = ReliveTheme.reduceMotion
    AnimatedContent(
        targetState = animatedStage,
        transitionSpec = {
            reliveForwardBackward(
                motion = motion,
                reduceMotion = reduceMotion,
                movingForward = targetState.stage.ordinal > initialState.stage.ordinal,
            )
        },
        label = "export flow",
    ) { activeStage ->
        when (activeStage.stage) {
            ExportFlowStage.Setup -> ExportSetup(
                state = state,
                onBack = onBack,
                onSelectFormat = viewModel::selectFormat,
                onSelectScope = viewModel::selectScope,
                onPickStart = { pickingStart = true },
                onPickEnd = { pickingEnd = true },
                onUseAllTime = { viewModel.setDateRange(null, null) },
                onSetTitle = viewModel::setTitle,
                onSetSubtitle = viewModel::setSubtitle,
                onSelectPaper = viewModel::setPaper,
                onChooseCover = {
                    scope.launch {
                        val picked = photos.pickImage()
                        picked.firstOrNull()?.let { viewModel.setCoverPhoto(it.sourcePath) }
                        picked.drop(1).filter { it.ownedByRelive }
                            .forEach { viewModel.deleteTemporaryCover(it.sourcePath) }
                    }
                },
                onRemoveCover = { viewModel.setCoverPhoto(null) },
                onCreate = viewModel::createSelectedFormat,
            )

            ExportFlowStage.Processing -> ExportProcessingScreen(
                operation = state.operation,
                onCancel = viewModel::cancel,
            )

            ExportFlowStage.Result -> ExportResultScreen(
                result = requireNotNull(activeStage.result),
                onBack = viewModel::clearOperation,
                onSave = files::save,
                onShare = files::share,
            )

            ExportFlowStage.Error -> ExportErrorScreen(
                message = requireNotNull(activeStage.errorMessage),
                onBack = viewModel::clearOperation,
            )
        }
    }

    val fallback = state.startDate ?: state.endDate ?: LocalCalendarDate(2026, 1, 1)
    if (pickingStart) {
        DateNavigationPicker(fallback, { pickingStart = false }) {
            viewModel.setDateRange(it, state.endDate)
            pickingStart = false
        }
    }
    if (pickingEnd) {
        DateNavigationPicker(state.endDate ?: state.startDate ?: fallback, { pickingEnd = false }) {
            viewModel.setDateRange(state.startDate, it)
            pickingEnd = false
        }
    }
}

private data class ExportAnimatedStage(
    val stage: ExportFlowStage,
    val result: ExportResult? = null,
    val errorMessage: String? = null,
)

@Composable
private fun ExportSetup(
    state: ExportUiState,
    onBack: () -> Unit,
    onSelectFormat: (ExportFormat) -> Unit,
    onSelectScope: (ExportScope) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onUseAllTime: () -> Unit,
    onSetTitle: (String) -> Unit,
    onSetSubtitle: (String) -> Unit,
    onSelectPaper: (DiaryPaper) -> Unit,
    onChooseCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onCreate: () -> Unit,
) {
    ReliveBackHandler(enabled = true, onBack = onBack)
    val dims = ReliveTheme.dimensions
    var scopeMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Export", onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
        ) {
            item {
                Column(
                    Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    Text(
                        "Take your memories with you",
                        style = ReliveTheme.typography.title,
                        color = ReliveTheme.colors.textPrimary,
                    )
                    Text(
                        "Choose what to include, then Relive will prepare the right file for saving or sharing.",
                        style = ReliveTheme.typography.body,
                        color = ReliveTheme.colors.textSecondary,
                    )
                }
            }

            item { ExportSectionTitle("1  CHOOSE A FORMAT") }
            items(ExportFormat.entries) { format ->
                val isPdf = format == ExportFormat.KeepsakePdf
                ExportFormatCard(
                    selected = state.format == format,
                    icon = if (isPdf) ProfileIcons.Pdf else ProfileIcons.Archive,
                    title = if (isPdf) "Keepsake PDF" else "Relive archive",
                    supporting = if (isPdf) {
                        "A printable A4 diary with your writing and photos."
                    } else {
                        "A view-only copy with photos, video, audio, and timeline style."
                    },
                    detail = if (isPdf) "Best for printing or reading" else "Best for sharing with Relive users",
                    onSelect = { onSelectFormat(format) },
                )
            }

            item { ExportSectionTitle("2  CHOOSE MEMORIES") }
            item {
                Column(
                    Modifier.padding(horizontal = dims.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                ) {
                    Text("Timeline", style = ReliveTheme.typography.subtitle, color = ReliveTheme.colors.textPrimary)
                    Box {
                        OutlinedButton(
                            onClick = { scopeMenu = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text(state.scope.displayName(), modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            Text("Change")
                        }
                        DropdownMenu(expanded = scopeMenu, onDismissRequest = { scopeMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All moments") },
                                onClick = {
                                    scopeMenu = false
                                    onSelectScope(ExportScope.All)
                                },
                            )
                            state.timelines.forEach { timeline ->
                                DropdownMenuItem(
                                    text = { Text(timeline.name) },
                                    onClick = {
                                        scopeMenu = false
                                        onSelectScope(ExportScope.Custom(timeline.id, timeline.name))
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                    ) {
                        OutlinedButton(onClick = onPickStart, modifier = Modifier.weight(1f)) {
                            Text(state.startDate.label("From"))
                        }
                        OutlinedButton(onClick = onPickEnd, modifier = Modifier.weight(1f)) {
                            Text(state.endDate.label("Until"))
                        }
                    }
                    if (state.startDate != null || state.endDate != null) {
                        TextButton(onClick = onUseAllTime) { Text("Clear dates · use all time") }
                    } else {
                        Text(
                            "All dates are included.",
                            style = ReliveTheme.typography.tag,
                            color = ReliveTheme.colors.textSecondary,
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(visible = state.format == ExportFormat.KeepsakePdf) {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.lg)) {
                        ExportSectionTitle("3  PERSONALIZE THE KEEPSAKE")
                        Column(
                            Modifier.padding(horizontal = dims.spacing.xl),
                            verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                        ) {
                            OutlinedTextField(
                                value = state.title,
                                onValueChange = onSetTitle,
                                label = { Text("Cover title") },
                                supportingText = { Text("Shown on the first page") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = state.subtitle,
                                onValueChange = onSetSubtitle,
                                label = { Text("Cover subtitle (optional)") },
                                supportingText = { Text("Leave blank to use the date span") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(onClick = onChooseCover, modifier = Modifier.fillMaxWidth()) {
                                Text(if (state.coverPhotoPath == null) "Add a cover photo" else "Replace cover photo")
                            }
                            if (state.coverPhotoPath != null) {
                                TextButton(onClick = onRemoveCover) { Text("Remove cover photo") }
                            }
                            Text(
                                "Diary page color",
                                style = ReliveTheme.typography.subtitle,
                                color = ReliveTheme.colors.textPrimary,
                            )
                            Text(
                                "Ink and doodle colors adjust automatically for comfortable reading.",
                                style = ReliveTheme.typography.tag,
                                color = ReliveTheme.colors.textSecondary,
                            )
                            DiaryPaperPicker(
                                selected = state.paper,
                                onSelect = onSelectPaper,
                            )
                        }
                    }
                }
            }

            item { PrivacyNotice(Modifier.padding(horizontal = dims.spacing.xl)) }

            item { Spacer(Modifier.height(dims.spacing.sm)) }
        }

        ExportActionBar(state = state, onCreate = onCreate)
    }
}

@Composable
private fun DiaryPaperPicker(
    selected: DiaryPaper,
    onSelect: (DiaryPaper) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        DiaryPaper.entries.forEach { paper ->
            val isSelected = paper == selected
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .semantics { contentDescription = paper.displayName }
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.perform(ReliveHapticCue.Selection)
                            onSelect(paper)
                        },
                    ),
                shape = CircleShape,
                color = paper.previewColor(),
                border = BorderStroke(
                    if (isSelected) 3.dp else 1.dp,
                    if (isSelected) ReliveTheme.colors.accent else ReliveTheme.colors.borderMuted,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Aa",
                        style = ReliveTheme.typography.subtitle,
                        color = paper.previewInk(),
                    )
                }
            }
        }
    }
    Text(
        selected.displayName,
        style = ReliveTheme.typography.tag,
        color = ReliveTheme.colors.textSecondary,
    )
}

private fun DiaryPaper.previewColor() = Color(
    when (this) {
        DiaryPaper.WarmCream -> 0xFFF3EBDD
        DiaryPaper.BlushPink -> 0xFFF8E8E8
        DiaryPaper.SageGreen -> 0xFFE8EEE2
        DiaryPaper.Lavender -> 0xFFEEE8F7
        DiaryPaper.PowderBlue -> 0xFFE6F0F7
        DiaryPaper.SoftPeach -> 0xFFFAE9DE
    },
)

private fun DiaryPaper.previewInk() = Color(
    when (this) {
        DiaryPaper.WarmCream -> 0xFF2D2722
        DiaryPaper.BlushPink -> 0xFF40262B
        DiaryPaper.SageGreen -> 0xFF263125
        DiaryPaper.Lavender -> 0xFF312943
        DiaryPaper.PowderBlue -> 0xFF22323E
        DiaryPaper.SoftPeach -> 0xFF3B2B24
    },
)

@Composable
private fun ExportActionBar(state: ExportUiState, onCreate: () -> Unit) {
    val dims = ReliveTheme.dimensions
    Surface(color = ReliveTheme.colors.surfaceCard, shadowElevation = 8.dp, tonalElevation = 2.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Text(
                state.selectionSummary(),
                style = ReliveTheme.typography.subtitle,
                color = if (state.selectedMomentCount > 0) {
                    ReliveTheme.colors.textPrimary
                } else {
                    ReliveTheme.colors.actionDestructive
                },
            )
            Button(
                onClick = onCreate,
                enabled = state.selectedMomentCount > 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(state.createLabel(), style = ReliveTheme.typography.prominentAction)
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    supporting: String,
    detail: String,
    onSelect: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val haptics = rememberReliveHaptics()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptics.perform(ReliveHapticCue.Selection)
                    onSelect()
                },
            ),
        shape = RoundedCornerShape(dims.radii.largeIncreased),
        color = if (selected) colors.tint else colors.surfaceCard,
        border = BorderStroke(
            width = if (selected) dims.stroke.icon else dims.stroke.hairline,
            color = if (selected) colors.accent else colors.borderMuted,
        ),
        tonalElevation = if (selected) 3.dp else 0.dp,
    ) {
        Row(
            Modifier.padding(dims.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = colors.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
                Text(title, style = ReliveTheme.typography.subtitle, color = colors.textPrimary)
                Text(supporting, style = ReliveTheme.typography.body, color = colors.textSecondary)
                Text(detail, style = ReliveTheme.typography.tag, color = colors.accentMuted)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ExportSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl).semantics { heading() },
        style = ReliveTheme.typography.eyebrow,
        color = ReliveTheme.colors.textSecondary,
    )
}

@Composable
private fun PrivacyNotice(modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.radii.large),
        color = ReliveTheme.colors.surfaceCardTranslucent,
        border = BorderStroke(dims.stroke.hairline, ReliveTheme.colors.borderMuted),
    ) {
        Column(Modifier.padding(dims.spacing.lg), verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
            Text("Your privacy travels with the file", style = ReliveTheme.typography.subtitle)
            Text(
                "Exports are not encrypted and can include private text, media, and readable locations. Save or share them only where you trust.",
                style = ReliveTheme.typography.tag,
                color = ReliveTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ExportProcessingScreen(operation: ExportOperationState, onCancel: () -> Unit) {
    ReliveBackHandler(enabled = true, onBack = onCancel)
    val dims = ReliveTheme.dimensions
    val format = when (operation) {
        is ExportOperationState.Preparing -> operation.format
        is ExportOperationState.Working -> operation.format
        else -> ExportFormat.KeepsakePdf
    }
    val progress = (operation as? ExportOperationState.Working)?.progress

    Box(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(dims.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (progress?.fraction != null) {
                    CircularProgressIndicator(
                        progress = { progress.fraction!! },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 7.dp,
                    )
                    Text(
                        "${(progress.fraction!! * 100).toInt()}%",
                        style = ReliveTheme.typography.subtitle,
                        color = ReliveTheme.colors.textPrimary,
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(80.dp), strokeWidth = 7.dp)
                }
            }
            Text(
                if (format == ExportFormat.KeepsakePdf) "Creating your keepsake" else "Packing your Relive archive",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                progress?.phase ?: "Preparing your moments…",
                style = ReliveTheme.typography.body,
                color = ReliveTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (progress?.fraction != null) {
                Text(
                    "${progress.completed} of ${progress.total}",
                    style = ReliveTheme.typography.tag,
                    color = ReliveTheme.colors.textSecondary,
                )
            }
            Text(
                "Keep Relive open while this finishes.",
                style = ReliveTheme.typography.tag,
                color = ReliveTheme.colors.textMuted,
            )
            TextButton(onClick = onCancel) { Text("Cancel export") }
        }
    }
}

@Composable
private fun ExportResultScreen(
    result: ExportResult,
    onBack: () -> Unit,
    onSave: suspend (ExportResult) -> Boolean,
    onShare: (ExportResult) -> Boolean,
) {
    ReliveBackHandler(enabled = true, onBack = onBack)
    val dims = ReliveTheme.dimensions
    val scope = rememberCoroutineScope()
    val haptics = rememberReliveHaptics()
    var deliveryMessage by remember(result.path) { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Export ready", onBack, backDescription = "Back to export setup")
        Column(
            modifier = Modifier.fillMaxSize().padding(dims.spacing.xl).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = RoundedCornerShape(dims.radii.xl), color = ReliveTheme.colors.tint) {
                Icon(
                    imageVector = ProfileIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(dims.spacing.xl).size(72.dp),
                    tint = ReliveTheme.colors.accent,
                )
            }
            Spacer(Modifier.height(dims.spacing.xl))
            Text(
                if (result.format == ExportFormat.KeepsakePdf) "Your keepsake is ready" else "Your archive is ready",
                style = ReliveTheme.typography.title,
                color = ReliveTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dims.spacing.sm))
            Text(
                result.filename,
                style = ReliveTheme.typography.body,
                color = ReliveTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dims.spacing.xxl))
            Button(
                onClick = {
                    scope.launch {
                        deliveryMessage = if (onSave(result)) {
                            haptics.perform(ReliveHapticCue.Confirm)
                            "Saved to your chosen location."
                        } else {
                            "Save cancelled. Your export is still ready here."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Save to device", style = ReliveTheme.typography.prominentAction) }
            Spacer(Modifier.height(dims.spacing.sm))
            OutlinedButton(
                onClick = {
                    deliveryMessage = if (onShare(result)) {
                        "Choose where you want to share it."
                    } else {
                        "Relive could not open sharing. Try saving the file instead."
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Share export") }
            deliveryMessage?.let {
                Spacer(Modifier.height(dims.spacing.md))
                Text(
                    it,
                    style = ReliveTheme.typography.tag,
                    color = ReliveTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(dims.spacing.lg))
            TextButton(onClick = onBack) { Text("Create another export") }
        }
    }
}

@Composable
private fun ExportErrorScreen(message: String, onBack: () -> Unit) {
    ReliveBackHandler(enabled = true, onBack = onBack)
    val dims = ReliveTheme.dimensions
    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Export couldn't finish", onBack, backDescription = "Back to export setup")
        Column(
            modifier = Modifier.fillMaxSize().padding(dims.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Something interrupted the export",
                style = ReliveTheme.typography.title,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dims.spacing.md))
            Text(
                message,
                style = ReliveTheme.typography.body,
                color = ReliveTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dims.spacing.xl))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to export setup") }
        }
    }
}

private fun ExportScope.displayName(): String = when (this) {
    ExportScope.All -> "All moments"
    is ExportScope.Custom -> name
}

private fun ExportUiState.selectionSummary(): String = if (selectedMomentCount == 0) {
    "No Moments in this selection"
} else {
    "$selectedMomentCount ${if (selectedMomentCount == 1) "Moment" else "Moments"} · oldest to newest"
}

private fun ExportUiState.createLabel(): String {
    val label = if (format == ExportFormat.KeepsakePdf) "Create Keepsake PDF" else "Create Relive archive"
    return if (isPro) label else "$label · Pro"
}

private fun LocalCalendarDate?.label(empty: String): String = this?.let {
    "${it.year.toString().padStart(4, '0')}-${it.month.toString().padStart(2, '0')}-${it.day.toString().padStart(2, '0')}"
} ?: empty
