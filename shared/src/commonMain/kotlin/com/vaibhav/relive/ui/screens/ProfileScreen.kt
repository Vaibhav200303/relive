package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.vaibhav.relive.ui.components.ReliveAlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.presentation.date.ProfileSinceFormatter
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.presentation.exporting.ExportUiState
import com.vaibhav.relive.presentation.exporting.profileExportStatus
import com.vaibhav.relive.presentation.settings.AppearanceViewModel
import com.vaibhav.relive.ui.components.settings.AppearanceModeControl
import com.vaibhav.relive.ui.components.settings.RelivePalettePicker
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.profile.ProfileChevronGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import kotlinx.coroutines.launch
import com.vaibhav.relive.presentation.profile.ExternalActivityGuard
import com.vaibhav.relive.ui.components.ReliveSnackbarHost
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.presentation.profile.pluralizedStat
import com.vaibhav.relive.domain.entitlement.EntitlementProvider
import com.vaibhav.relive.domain.entitlement.EntitlementPolicy

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    appearanceViewModel: AppearanceViewModel,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenMediaStorage: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenReliveArchive: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
    entitlementProvider: EntitlementProvider,
    exportState: ExportUiState = ExportUiState(),
) {
    val state by viewModel.state.collectAsState()
    val appearance by appearanceViewModel.state.collectAsState()
    val entitlement by entitlementProvider.state.collectAsState()
    val dims = ReliveTheme.dimensions
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val picker = rememberMediaPickerHandle(mediaStore)
    val haptics = rememberReliveHaptics()
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }
    val backgroundTapInteraction = remember { MutableInteractionSource() }
    var editingName by remember { mutableStateOf(false) }
    var nameWasFocused by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var photoActions by remember { mutableStateOf(false) }
    fun finishNameEdit() {
        if (!editingName) return
        editingName = false
        val value = nameDraft.trim()
        if (value.isEmpty()) {
            nameDraft = state.displayName.takeIf { it != "Your Relive" }.orEmpty()
            haptics.perform(ReliveHapticCue.Reject)
        } else {
            viewModel.saveDisplayName(value) { ok -> haptics.perform(if (ok) ReliveHapticCue.Confirm else ReliveHapticCue.Reject) }
        }
    }
    LaunchedEffect(state.displayName, editingName) {
        if (!editingName) nameDraft = state.displayName.takeIf { it != "Your Relive" }.orEmpty()
    }
    LaunchedEffect(editingName) {
        if (editingName) nameFocusRequester.requestFocus()
    }
    ReliveBackHandler(enabled = true, onBack = onBack)
    LaunchedEffect(appearance.errorMessage) {
        appearance.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            appearanceViewModel.clearError()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ReliveTheme.colors.canvasBrush())
            .clickable(
                interactionSource = backgroundTapInteraction,
                indication = null,
            ) { focusManager.clearFocus() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "profile-header") {
                ProfileHeader(
                    onBack = {
                        finishNameEdit()
                        onBack()
                    },
                    onEdit = {
                        nameDraft = state.displayName.takeIf { it != "Your Relive" }.orEmpty()
                        nameWasFocused = false
                        editingName = true
                    },
                )
            }
            item(key = "profile-identity") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing.lg),
                ) {
                    Box(Modifier.clickable { finishNameEdit(); photoActions = true }) { ProfileAvatar(state.profilePhoto, mediaStore) }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
                    ) {
                        if (editingName) {
                            BasicTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it.take(ProfileViewModel.MAX_NAME_LENGTH) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(nameFocusRequester)
                                    .onFocusChanged { focus ->
                                        if (focus.isFocused) nameWasFocused = true
                                        else if (nameWasFocused) finishNameEdit()
                                    }
                                    .semantics { contentDescription = "Display name" },
                                singleLine = true,
                                textStyle = ReliveTheme.typography.title.copy(
                                    color = ReliveTheme.colors.textPrimary,
                                    textAlign = TextAlign.Start,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            )
                        } else {
                            Text(
                                state.displayName,
                                style = ReliveTheme.typography.title,
                                color = ReliveTheme.colors.textPrimary,
                                modifier = Modifier.clickable {
                                    nameDraft = state.displayName.takeIf { it != "Your Relive" }.orEmpty()
                                    nameWasFocused = false
                                    editingName = true
                                }.semantics { contentDescription = "Edit display name, ${state.displayName}" },
                            )
                        }
                        Text(
                            "Your private memory space",
                            style = ReliveTheme.typography.subtitle,
                            color = ReliveTheme.colors.textSecondary,
                        )
                        state.joiningDate?.let { createdAt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
                            ) {
                                Icon(
                                    imageVector = ProfileIcons.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(dims.icon.sm),
                                    tint = ReliveTheme.colors.accentMuted,
                                )
                                Text(
                                    "Since ${ProfileSinceFormatter.format(createdAt)}",
                                    style = ReliveTheme.typography.tag,
                                    color = ReliveTheme.colors.textMuted,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "profile-statistics") {
                ProfileStatistics(state.momentCount, state.customTimelineCount, state.placeCount)
            }
            item(key = "relive-pro") {
                ProfileProCard(
                    isPro = entitlement.isPro,
                    onClick = {
                        finishNameEdit()
                        onOpenUpgrade()
                    },
                )
            }
            item(key = "appearance") {
                ProfileAppearanceSection(
                    mode = appearance.preferences.mode,
                    theme = appearance.preferences.defaultTheme,
                    onModeChange = appearanceViewModel::setMode,
                    onThemeChange = appearanceViewModel::setDefaultTheme,
                    isPro = entitlement.isPro,
                    onRestrictedSelection = onOpenUpgrade,
                    onInteraction = ::finishNameEdit,
                )
            }
            item(key = "your-memories") {
                ProfileSection(
                    title = "YOUR MEMORIES",
                    labels = listOf("Media & storage", "Backup", "Export", "Open Relive archive"),
                    exportSupporting = exportState.profileExportStatus(),
                    onMediaStorage = { finishNameEdit(); onOpenMediaStorage() },
                    onBackup = {
                        finishNameEdit()
                        onOpenBackupRestore()
                    },
                    onExport = { finishNameEdit(); onOpenExport() },
                    onOpenReliveArchive = { finishNameEdit(); onOpenReliveArchive() },
                )
            }
            item(key = "preferences") {
                ProfileSection(
                    title = "PREFERENCES",
                    labels = listOf("Preferences", "Location", "Reminders", "Privacy & security"),
                    onPreferences = { finishNameEdit(); onOpenPreferences() },
                    onLocation = { finishNameEdit(); onOpenLocation() },
                    onNotifications = { finishNameEdit(); onOpenNotifications() },
                    onPrivacy = { finishNameEdit(); onOpenPrivacy() },
                )
            }
            item(key = "relive") {
                ProfileSection("RELIVE", listOf("Help & feedback", "About Relive"), last = true, onHelp = { finishNameEdit(); onOpenHelp() }, onAbout = { finishNameEdit(); onOpenAbout() })
            }
            item(key = "profile-footer") {
                ProfileFooter()
            }
        }
        ReliveSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(dims.spacing.lg),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = ReliveTheme.colors.accent,
                contentColor = ReliveTheme.colors.textOnAccent,
            )
        }
        if (photoActions) ProfilePhotoDialog(
            hasPhoto = state.profilePhoto != null,
            onDismiss = { photoActions = false },
            onPick = {
                photoActions = false
                scope.launch {
                    ExternalActivityGuard.active = true
                    val raw = try { picker.pickImage().firstOrNull() } finally { ExternalActivityGuard.active = false } ?: return@launch
                    runCatching { mediaProcessor.process(raw) }.onSuccess { processed ->
                        if (processed.type == MediaType.Image) viewModel.setProfilePhoto(processed.storageRef) { ok -> haptics.perform(if (ok) ReliveHapticCue.Confirm else ReliveHapticCue.Reject) }
                    }.onFailure { snackbarHostState.showSnackbar("Could not save profile photo.") }
                }
            },
            onRemove = { photoActions = false; viewModel.setProfilePhoto(null) { ok -> haptics.perform(if (ok) ReliveHapticCue.Confirm else ReliveHapticCue.Reject) } },
        )
    }
}

@Composable
private fun ProfileAppearanceSection(
    mode: AppearanceMode,
    theme: ThemeReference,
    onModeChange: (AppearanceMode) -> Unit,
    onThemeChange: (ThemeReference) -> Unit,
    isPro: Boolean,
    onRestrictedSelection: () -> Unit,
    onInteraction: () -> Unit,
) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.md),
    ) {
        Text("APPEARANCE", style = ReliveTheme.typography.eyebrow, color = colors.textSecondary)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dims.radii.largeIncreased))
                .background(colors.surfaceCard)
                .padding(dims.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(dims.spacing.lg),
        ) {
            AppearanceModeControl(
                selected = mode,
                onSelect = { selected ->
                    onInteraction()
                    onModeChange(selected)
                },
            )
            HorizontalDivider(color = colors.borderMuted, thickness = dims.stroke.hairline)
            RelivePalettePicker(
                selectedTheme = theme,
                globalTheme = theme,
                includeUseAppTheme = false,
                onSelect = { selected ->
                    onInteraction()
                    selected?.let(onThemeChange)
                },
                isSelectionAllowed = { selected -> selected == null || EntitlementPolicy(com.vaibhav.relive.domain.entitlement.EntitlementState(isPro = isPro)).maySelectPalette(selected) },
                onRestrictedSelection = onRestrictedSelection,
            )
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit, onEdit: () -> Unit) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(dims.minTouchTarget).semantics { contentDescription = "Back to Home" },
        ) {
            BackGlyph(dims.icon.lg, colors.textSecondary, dims.stroke.icon)
        }
        Text(
            "Profile",
            modifier = Modifier.padding(start = dims.spacing.sm).semantics { heading() },
            style = ReliveTheme.typography.title,
            color = colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .heightIn(min = dims.minTouchTarget)
                .clip(ReliveTheme.shapes.pill)
                .background(colors.tint)
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm)
                .semantics { contentDescription = "Edit profile name" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.xs),
        ) {
            Icon(ProfileIcons.Edit, contentDescription = null, modifier = Modifier.size(dims.icon.sm), tint = colors.accentMuted)
            Text("Edit", style = ReliveTheme.typography.action, color = colors.accentMuted)
        }
    }
}

@Composable
private fun ProfileAvatar(photo: com.vaibhav.relive.domain.model.MediaStorageRef?, mediaStore: MediaStore) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    // The avatar intentionally stays circular: the decorative-shape default is the
    // restrained option, while the semantic shape token keeps it consistent with the theme.
    val ringShape = ReliveTheme.shapes.pill
    // Accent ring (matching the primary CTA) with a gap between ring and avatar.
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .border(dims.stroke.cardOuter, colors.surfaceCard, ringShape)
                .padding(dims.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                RelivedImageTile(photo, mediaStore, Modifier.size(dims.profile.avatarSize).clip(ringShape).semantics { contentDescription = "Change profile photo" })
            } else Box(
                modifier = Modifier
                    .size(dims.profile.avatarSize)
                    .clip(ringShape)
                    .background(colors.surfaceCard)
                    .semantics { contentDescription = "Profile avatar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ProfileIcons.Person, contentDescription = null, modifier = Modifier.size(dims.profile.avatarSize * 0.42f), tint = colors.textSecondary)
            }
        }
        Box(
            modifier = Modifier
                .size(dims.spacing.xxl)
                .clip(ringShape)
                .background(colors.accent)
                .border(dims.stroke.cardOuter, colors.surfaceCard, ringShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ProfileIcons.Camera, contentDescription = null, modifier = Modifier.size(dims.icon.sm), tint = colors.textOnAccent)
        }
    }
}

@Composable
private fun ProfileStatistics(momentCount: Long, timelineCount: Long, placeCount: Long) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.sm)
            .shadow(dims.stroke.cardOuter, RoundedCornerShape(dims.radii.largeIncreased))
            .clip(RoundedCornerShape(dims.radii.largeIncreased))
            .background(colors.surfaceCard)
            .padding(vertical = dims.spacing.md)
            .semantics { contentDescription = listOf(pluralizedStat(momentCount, "moment"), pluralizedStat(timelineCount, "timeline"), pluralizedStat(placeCount, "place")).joinToString(", ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileStatistic(momentCount, "Moments", ProfileIcons.Archive, Modifier.weight(1f))
        ProfileStatDivider()
        ProfileStatistic(timelineCount, "Timelines", ProfileIcons.Layers, Modifier.weight(1f))
        ProfileStatDivider()
        ProfileStatistic(placeCount, "Places", ProfileIcons.Location, Modifier.weight(1f))
    }
}

@Composable
private fun ProfileStatistic(value: Long, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = modifier.padding(horizontal = dims.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Box(
            modifier = Modifier.size(dims.spacing.xxl).clip(RoundedCornerShape(dims.radii.medium)).background(colors.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(dims.icon.md), tint = colors.accentMuted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
            Text(value.toString(), style = ReliveTheme.typography.action, color = colors.textPrimary)
            Text(label, style = ReliveTheme.typography.tag, color = colors.textMuted)
        }
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        Modifier
            .width(ReliveTheme.dimensions.stroke.hairline)
            .heightIn(min = ReliveTheme.dimensions.minTouchTarget)
            .background(ReliveTheme.colors.borderMuted),
    )
}

@Composable
private fun ProfileProCard(isPro: Boolean, onClick: () -> Unit) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(dims.radii.largeIncreased)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.lg)
            .shadow(dims.spacing.xs, shape)
            .clip(shape)
            .background(colors.surfaceCard)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (isPro) {
                    "Relive Pro active. View membership"
                } else {
                    "Upgrade to Relive Pro. Automatic backup, unlimited timelines, and every appearance"
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = colors.tint,
                radius = size.minDimension * 0.72f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 1.16f),
            )
            drawCircle(
                color = colors.accent.copy(alpha = 0.10f),
                radius = size.minDimension * 0.42f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 1.08f),
            )
        }
        Row(
            modifier = Modifier.padding(dims.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(dims.minTouchTarget)
                    .clip(RoundedCornerShape(dims.radii.medium))
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ProfileIcons.Crown, contentDescription = null, modifier = Modifier.size(dims.icon.lg), tint = colors.textOnAccent)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dims.spacing.xs),
            ) {
                Text(
                    text = if (isPro) "Relive Pro active" else "Upgrade to Relive Pro",
                    style = ReliveTheme.typography.action,
                    color = colors.textPrimary,
                )
                Text(
                    text = if (isPro) {
                        "Your complete archive experience is unlocked."
                    } else {
                        "Automatic backup, unlimited timelines,\nand every appearance."
                    },
                    style = ReliveTheme.typography.tag,
                    color = colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .clip(ReliveTheme.shapes.pill)
                    .background(colors.accent)
                    .padding(horizontal = dims.spacing.lg, vertical = dims.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isPro) "View" else "Upgrade", style = ReliveTheme.typography.action, color = colors.textOnAccent)
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    labels: List<String>,
    last: Boolean = false,
    onPreferences: (() -> Unit)? = null,
    onMediaStorage: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    exportSupporting: String? = null,
    onOpenReliveArchive: (() -> Unit)? = null,
    onLocation: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    onPrivacy: (() -> Unit)? = null,
    onHelp: (() -> Unit)? = null,
    onAbout: (() -> Unit)? = null,
) {
    val dims = ReliveTheme.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dims.spacing.xl,
                end = dims.spacing.xl,
                top = dims.spacing.lg,
                bottom = if (last) dims.spacing.lg else dims.spacing.none,
            ),
        verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        Text(
            title.lowercase().replaceFirstChar { it.uppercase() },
            style = ReliveTheme.typography.title.copy(
                fontSize = ReliveTheme.typography.body.fontSize,
                lineHeight = ReliveTheme.typography.body.lineHeight,
            ),
            color = ReliveTheme.colors.textSecondary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(dims.stroke.cardOuter, RoundedCornerShape(dims.radii.largeIncreased))
                .clip(RoundedCornerShape(dims.radii.largeIncreased))
                .background(ReliveTheme.colors.surfaceCard),
        ) {
            labels.forEachIndexed { index, label ->
                ProfileSettingRow(
                    label = label,
                    supporting = if (label.trim() == "Export" && exportSupporting != null) exportSupporting else profileSupportingFor(label),
                    icon = profileIconFor(label),
                    onClick = when (label.trim()) {
                        "Preferences" -> onPreferences
                        "Media & storage" -> onMediaStorage
                        "Backup" -> onBackup
                        "Export" -> onExport
                        "Open Relive archive" -> onOpenReliveArchive
                        "Location" -> onLocation
                        "Reminders" -> onNotifications
                        "Privacy & security" -> onPrivacy
                        "Help & feedback" -> onHelp
                        "About Relive" -> onAbout
                        else -> null
                    },
                )
                if (index < labels.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = dims.minTouchTarget + dims.spacing.lg, end = dims.spacing.md),
                        color = ReliveTheme.colors.borderMuted,
                        thickness = dims.stroke.hairline,
                    )
                }
            }
        }
    }
}

private fun profileSupportingFor(label: String): String? = when (label.trim()) {
    "Media & storage" -> "See how your archive uses space."
    "Backup" -> "Keep your memories safe."
    "Export" -> "Make a copy of your memories."
    "Open Relive archive" -> "Browse your archive files."
    "Preferences" -> "Choose how Relive behaves."
    "Location" -> "Show or hide locations on moments."
    "Reminders" -> "Set a daily nudge to capture today."
    "Privacy & security" -> "Keep your archive private and secure."
    "Help & feedback" -> "Get support or share an idea."
    "About Relive" -> "Learn more about the app."
    else -> null
}

private fun profileIconFor(label: String): ImageVector = when (label.trim()) {
    "Media & storage" -> ProfileIcons.Media
    "Backup" -> ProfileIcons.Backup
    "Export" -> ProfileIcons.Export
    "Open Relive archive" -> ProfileIcons.Archive
    "Preferences" -> ProfileIcons.Preferences
    "Location" -> ProfileIcons.Location
    "Reminders" -> ProfileIcons.Notifications
    "Privacy & security" -> ProfileIcons.Security
    "Help & feedback" -> ProfileIcons.Help
    "About Relive" -> ProfileIcons.Info
    else -> ProfileIcons.Person
}

@Composable
private fun ProfilePhotoDialog(hasPhoto: Boolean, onDismiss: () -> Unit, onPick: () -> Unit, onRemove: () -> Unit) = ReliveAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Profile photo") },
    text = { Text(if (hasPhoto) "Replace or remove your local profile photo." else "Choose a photo from your device.") },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    confirmButton = { Row { if (hasPhoto) TextButton(onClick = onRemove) { Text("Remove") }; TextButton(onClick = onPick) { Text(if (hasPhoto) "Replace" else "Choose") } } },
)

@Composable
private fun ProfileSettingRow(
    label: String,
    supporting: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm)
            .semantics { contentDescription = listOfNotNull(label, supporting).joinToString(", ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dims.spacing.xxl)
                .clip(RoundedCornerShape(dims.radii.medium))
                .background(ReliveTheme.colors.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(dims.icon.md), tint = ReliveTheme.colors.accentMuted)
        }
        Column(Modifier.weight(1f).padding(start = dims.spacing.md), verticalArrangement = Arrangement.spacedBy(dims.spacing.xs)) {
            Text(
                label,
                style = ReliveTheme.typography.action.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = ReliveTheme.colors.textPrimary,
            )
            supporting?.let {
                Text(
                    it,
                    style = ReliveTheme.typography.tag.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = ReliveTheme.colors.textMuted,
                )
            }
        }
        ProfileChevronGlyph()
    }
}

@Composable
private fun ProfileFooter() {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing.sm),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = colors.borderMuted, thickness = dims.stroke.hairline)
        Icon(ProfileIcons.Favorite, contentDescription = null, modifier = Modifier.size(dims.icon.sm), tint = colors.accentMuted)
        Text("Small moments. A fuller you.", style = ReliveTheme.typography.subtitle, color = colors.textMuted)
        HorizontalDivider(Modifier.weight(1f), color = colors.borderMuted, thickness = dims.stroke.hairline)
    }
}
