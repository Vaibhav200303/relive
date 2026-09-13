package com.vaibhav.relive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.vaibhav.relive.domain.exporting.PdfImageQuality
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.platform.exporting.rememberExportFileHandle
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.presentation.exporting.ExportFlowStage
import com.vaibhav.relive.presentation.exporting.ExportUiState
import com.vaibhav.relive.presentation.exporting.ExportViewModel
import com.vaibhav.relive.presentation.exporting.flowStage
import com.vaibhav.relive.ui.components.ReliveBottomSheet
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
import com.vaibhav.relive.ui.components.timeline.CalendarGlyph
import com.vaibhav.relive.ui.components.timeline.DateNavigationPicker
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveOpacity
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.reliveForwardBackward
import com.vaibhav.relive.ui.theme.timelineWallpaperPalette
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
    var choosingTimeline by remember { mutableStateOf(false) }
    var pendingScope by remember { mutableStateOf<ExportScope?>(null) }

    DisposableEffect(viewModel) {
        viewModel.setScreenVisible(true)
        onDispose { viewModel.setScreenVisible(false) }
    }
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
                onBack = {
                    viewModel.exitSetup()
                    onBack()
                },
                onSelectFormat = viewModel::selectFormat,
                onChooseTimeline = {
                    pendingScope = state.scope
                    choosingTimeline = true
                },
                onPickStart = { pickingStart = true },
                onPickEnd = { pickingEnd = true },
                onUseAllTime = { viewModel.setDateRange(null, null) },
                onSetTitle = viewModel::setTitle,
                onSetSubtitle = viewModel::setSubtitle,
                onSelectPaper = viewModel::setPaper,
                onSelectImageQuality = viewModel::setImageQuality,
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
                onBack = onBack,
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
    TimelineExportPicker(
        visible = choosingTimeline,
        state = state,
        selectedScope = pendingScope ?: state.scope,
        onSelect = { pendingScope = it },
        onDismiss = {
            choosingTimeline = false
            pendingScope = null
        },
        onApply = {
            viewModel.selectScope(pendingScope ?: state.scope)
            choosingTimeline = false
            pendingScope = null
        },
    )
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
    onChooseTimeline: () -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onUseAllTime: () -> Unit,
    onSetTitle: (String) -> Unit,
    onSetSubtitle: (String) -> Unit,
    onSelectPaper: (DiaryPaper) -> Unit,
    onSelectImageQuality: (PdfImageQuality) -> Unit,
    onChooseCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onCreate: () -> Unit,
) {
    ReliveBackHandler(enabled = true, onBack = onBack)
    val dims = ReliveTheme.dimensions

    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Export", onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.xl),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = dims.spacing.xl, vertical = dims.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                    ) {
                        Text(
                            "Take your memories with you",
                            style = ReliveTheme.typography.display,
                            color = ReliveTheme.colors.textPrimary,
                        )
                        Text(
                            "Choose what to include and we’ll prepare the perfect file.",
                            style = ReliveTheme.typography.body,
                            color = ReliveTheme.colors.textSecondary,
                        )
                    }
                    ExportKeepsakeIllustration()
                }
            }

            item { ExportSectionTitle(number = 1, text = "CHOOSE A FORMAT") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = dims.spacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    ExportFormat.entries.forEach { format ->
                        val isPdf = format == ExportFormat.KeepsakePdf
                        ExportFormatCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            selected = state.format == format,
                            icon = if (isPdf) ProfileIcons.Pdf else ProfileIcons.Archive,
                            title = if (isPdf) "Keepsake PDF" else "Relive archive",
                            supporting = if (isPdf) {
                                "A printable A4 diary with your writing and photos."
                            } else {
                                "A view-only copy with photos, video, audio, and timeline style."
                            },
                            detail = if (isPdf) "Best for printing" else "Best for sharing",
                            onSelect = { onSelectFormat(format) },
                        )
                    }
                }
            }

            item { ExportSectionTitle(number = 2, text = "CHOOSE MEMORIES") }
            item {
                Column(
                    Modifier.padding(horizontal = dims.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
                ) {
                    Box {
                        OutlinedButton(
                            onClick = onChooseTimeline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = dims.minTouchTarget + dims.spacing.lg),
                            shape = RoundedCornerShape(dims.radii.largeIncreased),
                        ) {
                            Icon(
                                ProfileIcons.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(dims.icon.lg),
                            )
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = dims.spacing.md),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text("Timeline", style = ReliveTheme.typography.tag)
                                Text(
                                    state.scope.displayName(),
                                    style = ReliveTheme.typography.prominentAction,
                                )
                            }
                            Text("Change  ›", style = ReliveTheme.typography.action)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExportDateButton(
                            modifier = Modifier.weight(1f),
                            label = "From",
                            value = state.startDate.label("Any date"),
                            onClick = onPickStart,
                        )
                        Text("–", style = ReliveTheme.typography.body, color = ReliveTheme.colors.textSecondary)
                        ExportDateButton(
                            modifier = Modifier.weight(1f),
                            label = "Until",
                            value = state.endDate.label("Any date"),
                            onClick = onPickEnd,
                        )
                    }
                    if (state.startDate != null || state.endDate != null) {
                        TextButton(onClick = onUseAllTime) { Text("Clear dates · use all time") }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                ProfileIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(dims.icon.md),
                                tint = ReliveTheme.colors.textSecondary,
                            )
                            Text(
                                "All dates are included.",
                                style = ReliveTheme.typography.tag,
                                color = ReliveTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = state.format == ExportFormat.KeepsakePdf) {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.lg)) {
                        ExportSectionTitle(number = 3, text = "CUSTOMIZE")
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = dims.spacing.xl),
                            shape = RoundedCornerShape(dims.radii.largeIncreased),
                            color = ReliveTheme.colors.surfaceCardTranslucent,
                            border = BorderStroke(dims.stroke.hairline, ReliveTheme.colors.borderMuted),
                        ) {
                            Column(
                                Modifier.padding(dims.spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.lg),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    CoverPhotoPicker(
                                        modifier = Modifier.weight(0.38f),
                                        hasPhoto = state.coverPhotoPath != null,
                                        onChoose = onChooseCover,
                                        onRemove = onRemoveCover,
                                    )
                                    Column(
                                        modifier = Modifier.weight(0.62f),
                                        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                                    ) {
                                        OutlinedTextField(
                                            value = state.title,
                                            onValueChange = onSetTitle,
                                            label = { Text("Cover title") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(dims.radii.largeIncreased),
                                        )
                                        OutlinedTextField(
                                            value = state.subtitle,
                                            onValueChange = onSetSubtitle,
                                            label = { Text("Cover subtitle (optional)") },
                                            placeholder = { Text("A collection of moments") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(dims.radii.largeIncreased),
                                        )
                                    }
                                }

                                Text(
                                    "Image quality",
                                    style = ReliveTheme.typography.prominentAction,
                                    color = ReliveTheme.colors.textPrimary,
                                )
                                PdfImageQualityPicker(
                                    selected = state.imageQuality,
                                    onSelect = onSelectImageQuality,
                                )

                                Text(
                                    "Diary page color",
                                    style = ReliveTheme.typography.prominentAction,
                                    color = ReliveTheme.colors.textPrimary,
                                )
                                DiaryPaperPicker(
                                    selected = state.paper,
                                    onSelect = onSelectPaper,
                                )
                                Text(
                                    "Ink and doodle colors adjust automatically for comfortable reading.",
                                    style = ReliveTheme.typography.tag,
                                    color = ReliveTheme.colors.textSecondary,
                                )
                            }
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
private fun TimelineExportPicker(
    visible: Boolean,
    state: ExportUiState,
    selectedScope: ExportScope,
    onSelect: (ExportScope) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val haptics = rememberReliveHaptics()
    ReliveBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.72f),
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = dims.spacing.xl),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = dims.spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(dims.minTouchTarget, dims.spacing.xs)
                        .background(colors.textMuted, RoundedCornerShape(dims.radii.full)),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose a timeline",
                        style = ReliveTheme.typography.title,
                        color = colors.textPrimary,
                    )
                    Text(
                        "Select which moments to include in your export.",
                        style = ReliveTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(dims.minTouchTarget)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close timeline chooser" },
                    shape = CircleShape,
                    color = colors.tint,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("×", style = ReliveTheme.typography.title, color = colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(dims.spacing.lg))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
            ) {
                item(key = "all-moments") {
                    TimelineExportChoice(
                        title = "All moments",
                        momentCount = state.allMomentCount,
                        icon = ProfileIcons.Archive,
                        iconBackground = colors.accentMuted,
                        iconTint = colors.accent,
                        selected = selectedScope == ExportScope.All,
                        onSelect = {
                            haptics.perform(ReliveHapticCue.Selection)
                            onSelect(ExportScope.All)
                        },
                    )
                }
                state.timelines.forEach { timeline ->
                    item(key = timeline.id.value) {
                        val palette = timelineWallpaperPalette(
                            timeline.appearance.wallpaper,
                            ReliveTheme.isDark,
                        )
                        val scope = ExportScope.Custom(timeline.id, timeline.name)
                        TimelineExportChoice(
                            title = timeline.name,
                            momentCount = state.timelineMomentCounts[timeline.id] ?: 0,
                            icon = ProfileIcons.Media,
                            iconBackground = palette.backgroundColor,
                            iconTint = palette.doodleColor,
                            selected = selectedScope.matches(scope),
                            onSelect = {
                                haptics.perform(ReliveHapticCue.Selection)
                                onSelect(scope)
                            },
                        )
                    }
                }
            }
            Button(
                onClick = {
                    haptics.perform(ReliveHapticCue.Action)
                    onApply()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.minTouchTarget + dims.spacing.md),
                shape = RoundedCornerShape(dims.radii.full),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                ),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "Apply",
                        modifier = Modifier.align(Alignment.Center),
                        style = ReliveTheme.typography.prominentAction,
                    )
                    Text("→", modifier = Modifier.align(Alignment.CenterEnd), style = ReliveTheme.typography.title)
                }
            }
            Spacer(Modifier.height(dims.spacing.lg))
        }
    }
}

private fun ExportScope.matches(other: ExportScope): Boolean = when {
    this is ExportScope.All && other is ExportScope.All -> true
    this is ExportScope.Custom && other is ExportScope.Custom -> timelineId == other.timelineId
    else -> false
}

@Composable
private fun TimelineExportChoice(
    title: String,
    momentCount: Int,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(dims.radii.large),
        color = if (selected) colors.accentMuted else Color.Transparent,
        border = BorderStroke(
            dims.stroke.cardOuter,
            if (selected) colors.accent else colors.borderMuted,
        ),
    ) {
        Row(
            modifier = Modifier.padding(dims.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Surface(
                modifier = Modifier.size(dims.minTouchTarget),
                shape = RoundedCornerShape(dims.radii.medium),
                color = iconBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(dims.icon.lg))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = ReliveTheme.typography.prominentAction, color = colors.textPrimary)
                Text(
                    "$momentCount ${if (momentCount == 1) "moment" else "moments"}",
                    style = ReliveTheme.typography.tag,
                    color = colors.textSecondary,
                )
            }
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.accent,
                    unselectedColor = colors.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun ExportKeepsakeIllustration() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Box(Modifier.size((dims.minTouchTarget * 2) + dims.spacing.lg)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(start = dims.spacing.xl, top = dims.spacing.lg),
            shape = RoundedCornerShape(dims.radii.medium),
            color = colors.tint,
            border = BorderStroke(dims.stroke.hairline, colors.borderMuted),
        ) {}
        Surface(
            modifier = Modifier.fillMaxSize().padding(start = dims.spacing.md, end = dims.spacing.md),
            shape = RoundedCornerShape(dims.radii.medium),
            color = colors.surfaceCardTranslucent,
            border = BorderStroke(dims.stroke.hairline, colors.borderMuted),
        ) {}
        Surface(
            modifier = Modifier.fillMaxSize().padding(end = dims.spacing.xl, bottom = dims.spacing.lg),
            shape = RoundedCornerShape(dims.radii.medium),
            color = colors.surfaceCard,
            border = BorderStroke(dims.stroke.hairline, colors.border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    ProfileIcons.Pdf,
                    contentDescription = null,
                    modifier = Modifier.size(dims.icon.lg),
                    tint = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun PdfImageQualityPicker(
    selected: PdfImageQuality,
    onSelect: (PdfImageQuality) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm)) {
        PdfImageQuality.entries.forEach { quality ->
            val description = quality.description()
            val isSelected = selected == quality
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.perform(ReliveHapticCue.Selection)
                            onSelect(quality)
                        },
                    )
                    .semantics { contentDescription = "${quality.label()}, $description" },
                shape = RoundedCornerShape(dims.radii.medium),
                color = if (isSelected) ReliveTheme.colors.tint else ReliveTheme.colors.surfaceCard,
                border = BorderStroke(
                    if (isSelected) dims.stroke.icon else dims.stroke.hairline,
                    if (isSelected) ReliveTheme.colors.accent else ReliveTheme.colors.borderMuted,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(dims.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Column(Modifier.padding(start = dims.spacing.xs)) {
                        Text(quality.label(), style = ReliveTheme.typography.action, color = ReliveTheme.colors.textPrimary)
                        Text(description, style = ReliveTheme.typography.tag, color = ReliveTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

private fun PdfImageQuality.label(): String = when (this) {
    PdfImageQuality.Standard -> "Standard"
    PdfImageQuality.HD -> "HD"
}

private fun PdfImageQuality.description(): String = when (this) {
    PdfImageQuality.Standard -> "Smaller for sharing"
    PdfImageQuality.HD -> "Sharper for printing"
}

@Composable
private fun DiaryPaperPicker(
    selected: DiaryPaper,
    onSelect: (DiaryPaper) -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val haptics = rememberReliveHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                DiaryPaper.entries.forEach { paper ->
                    val isSelected = paper == selected
                    Box(
                        modifier = Modifier
                            .size(dims.minTouchTarget)
                            .semantics { contentDescription = paper.displayName }
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = {
                                    haptics.perform(ReliveHapticCue.Selection)
                                    onSelect(paper)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(dims.minTouchTarget - dims.spacing.sm),
                            shape = CircleShape,
                            color = paper.previewColor(),
                            border = BorderStroke(
                                if (isSelected) dims.stroke.iconBold else dims.stroke.hairline,
                                if (isSelected) ReliveTheme.colors.accent else ReliveTheme.colors.borderMuted,
                            ),
                        ) {}
                    }
                }
            }
            Text(
                selected.displayName,
                style = ReliveTheme.typography.tag,
                color = ReliveTheme.colors.textSecondary,
            )
        }
        DiaryPaperPreview(selected)
    }
}

@Composable
private fun DiaryPaperPreview(paper: DiaryPaper) {
    val dims = ReliveTheme.dimensions
    val ink = paper.previewInk()
    Surface(
        modifier = Modifier.size(dims.minTouchTarget * 2),
        shape = RoundedCornerShape(dims.radii.medium),
        color = paper.previewColor(),
        border = BorderStroke(dims.stroke.hairline, ReliveTheme.colors.borderMuted),
    ) {
        Column(
            modifier = Modifier.padding(dims.spacing.md),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Text("Aa", style = ReliveTheme.typography.title, color = ink)
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
                repeat(2) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(dims.stroke.iconBold)
                            .background(
                                color = ink.copy(alpha = ReliveOpacity.Low),
                                shape = RoundedCornerShape(dims.radii.full),
                            ),
                    )
                }
            }
        }
    }
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
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(dims.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Button(
            onClick = onCreate,
            enabled = state.selectedMomentCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dims.minTouchTarget + dims.spacing.lg),
            shape = RoundedCornerShape(dims.radii.full),
        ) {
            Text(state.createLabel(), style = ReliveTheme.typography.prominentAction)
            Spacer(Modifier.weight(1f))
            Text("→", style = ReliveTheme.typography.title)
        }
        Text(
            state.selectionSummary(),
            style = ReliveTheme.typography.tag,
            color = if (state.selectedMomentCount > 0) {
                ReliveTheme.colors.textSecondary
            } else {
                ReliveTheme.colors.actionDestructive
            },
        )
    }
}

@Composable
private fun ExportDateButton(
    modifier: Modifier,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = dims.minTouchTarget + dims.spacing.lg),
        shape = RoundedCornerShape(dims.radii.largeIncreased),
        contentPadding = PaddingValues(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        CalendarGlyph(dims.icon.lg, ReliveTheme.colors.textPrimary, dims.stroke.icon)
        Column(
            modifier = Modifier.padding(start = dims.spacing.sm),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                label,
                style = ReliveTheme.typography.tag,
                color = ReliveTheme.colors.textSecondary,
            )
            Text(
                value,
                style = ReliveTheme.typography.action,
                color = ReliveTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun CoverPhotoPicker(
    modifier: Modifier,
    hasPhoto: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
        Text("Cover photo", style = ReliveTheme.typography.action, color = ReliveTheme.colors.textPrimary)
        OutlinedButton(
            onClick = onChoose,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (dims.minTouchTarget * 2) + dims.spacing.lg + dims.spacing.xs),
            shape = RoundedCornerShape(dims.radii.medium),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (hasPhoto) ProfileIcons.Check else ProfileIcons.Media,
                    contentDescription = null,
                    modifier = Modifier.size(dims.icon.lg),
                )
                Spacer(Modifier.height(dims.spacing.sm))
                Text(if (hasPhoto) "Replace photo" else "Add photo", style = ReliveTheme.typography.tag)
            }
        }
        if (hasPhoto) {
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Remove", style = ReliveTheme.typography.tag)
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    modifier: Modifier,
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
        modifier = modifier
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
        tonalElevation = if (selected) dims.spacing.xs else dims.spacing.none,
    ) {
        Column(
            Modifier.fillMaxHeight().padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(dims.radii.medium),
                    color = if (selected) colors.accent else colors.tint,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(dims.spacing.sm).size(dims.icon.lg),
                        tint = if (selected) colors.textOnAccent else colors.accent,
                    )
                }
                Spacer(Modifier.weight(1f))
                RadioButton(selected = selected, onClick = null)
            }
            Text(title, style = ReliveTheme.typography.prominentAction, color = colors.textPrimary)
            Text(supporting, style = ReliveTheme.typography.caption, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(dims.radii.full), color = colors.tint) {
                Text(
                    detail,
                    modifier = Modifier.padding(horizontal = dims.spacing.md, vertical = dims.spacing.xs),
                    style = ReliveTheme.typography.tag,
                    color = colors.accentMuted,
                )
            }
        }
    }
}

@Composable
private fun ExportSectionTitle(number: Int, text: String) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier.padding(horizontal = dims.spacing.xl).semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = ReliveTheme.colors.accent) {
            Box(Modifier.size(dims.minTouchTarget), contentAlignment = Alignment.Center) {
                Text(
                    number.toString(),
                    style = ReliveTheme.typography.prominentAction,
                    color = ReliveTheme.colors.textOnAccent,
                )
            }
        }
        Text(text, style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.textSecondary)
    }
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
private fun ExportProcessingScreen(operation: ExportOperationState, onBack: () -> Unit, onCancel: () -> Unit) {
    ReliveBackHandler(enabled = true, onBack = onBack)
    val dims = ReliveTheme.dimensions
    val format = when (operation) {
        is ExportOperationState.Preparing -> operation.format
        is ExportOperationState.Working -> operation.format
        else -> ExportFormat.KeepsakePdf
    }
    val progress = (operation as? ExportOperationState.Working)?.progress

    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        ProfilePageHeader("Export", onBack, backDescription = "Back to Profile")
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(dims.spacing.xxl),
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
                    "You can keep using Relive while this finishes.",
                    style = ReliveTheme.typography.tag,
                    color = ReliveTheme.colors.textMuted,
                )
                TextButton(onClick = onCancel) { Text("Cancel export") }
            }
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
