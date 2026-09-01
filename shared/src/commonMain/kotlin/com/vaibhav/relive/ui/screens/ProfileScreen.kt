package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.vaibhav.relive.platform.system.ReliveBackHandler
import com.vaibhav.relive.domain.model.AppearanceMode
import com.vaibhav.relive.domain.model.ThemeReference
import com.vaibhav.relive.presentation.date.ProfileSinceFormatter
import com.vaibhav.relive.presentation.profile.ProfileViewModel
import com.vaibhav.relive.presentation.settings.AppearanceViewModel
import com.vaibhav.relive.ui.components.settings.AppearanceModeControl
import com.vaibhav.relive.ui.components.settings.RelivePalettePicker
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.components.timeline.ForwardGlyph
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.platform.media.MediaProcessor
import com.vaibhav.relive.platform.media.RelivedImageTile
import com.vaibhav.relive.platform.media.rememberMediaPickerHandle
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import kotlinx.coroutines.launch
import com.vaibhav.relive.presentation.profile.ExternalActivityGuard
import com.vaibhav.relive.ui.components.profile.ProfilePageHeader
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
    onOpenUpgrade: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    mediaStore: MediaStore,
    mediaProcessor: MediaProcessor,
    entitlementProvider: EntitlementProvider,
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
            .background(ReliveTheme.colors.bgCanvas)
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
                ProfileHeader {
                    finishNameEdit()
                    onBack()
                }
            }
            item(key = "profile-identity") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dims.spacing.xl, bottom = dims.spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dims.spacing.sm),
                ) {
                    Box(Modifier.clickable { finishNameEdit(); photoActions = true }) { ProfileAvatar(state.profilePhoto, mediaStore) }
                    if (editingName) {
                        BasicTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it.take(ProfileViewModel.MAX_NAME_LENGTH) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dims.spacing.xl)
                                .focusRequester(nameFocusRequester)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) nameWasFocused = true
                                    else if (nameWasFocused) finishNameEdit()
                                }
                                .semantics { contentDescription = "Display name" },
                            singleLine = true,
                            textStyle = ReliveTheme.typography.title.copy(
                                color = ReliveTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
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
                        Text(
                            "Since ${ProfileSinceFormatter.format(createdAt)}",
                            style = ReliveTheme.typography.subtitle,
                            color = ReliveTheme.colors.textMuted,
                        )
                    }
                }
            }
            item(key = "profile-statistics") {
                ProfileStatistics(state.momentCount, state.customTimelineCount, state.placeCount)
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
                    labels = listOf("Media & storage", "Backup"),
                    onMediaStorage = { finishNameEdit(); onOpenMediaStorage() },
                    onBackup = { finishNameEdit(); onOpenBackupRestore() },
                )
            }
            item(key = "preferences") {
                ProfileSection(
                    title = "PREFERENCES",
                    labels = listOf("Preferences", "Location", "Rediscover notifications", "Privacy & security"),
                    onPreferences = { finishNameEdit(); onOpenPreferences() },
                    onLocation = { finishNameEdit(); onOpenLocation() },
                    onNotifications = { finishNameEdit(); onOpenNotifications() },
                    onPrivacy = { finishNameEdit(); onOpenPrivacy() },
                )
            }
            item(key = "relive") {
                ProfileSection("RELIVE", listOf("Relive Pro", "Help & feedback", "About Relive"), last = true, onUpgrade = { finishNameEdit(); onOpenUpgrade() }, onHelp = { finishNameEdit(); onOpenHelp() }, onAbout = { finishNameEdit(); onOpenAbout() })
            }
        }
        SnackbarHost(
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
                .clip(RoundedCornerShape(dims.radii.lg))
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
private fun ProfileHeader(onBack: () -> Unit) {
    ProfilePageHeader("Profile", onBack, "Back to Timeline Home")
}

@Composable
private fun ProfileAvatar(photo: com.vaibhav.relive.domain.model.MediaStorageRef?, mediaStore: MediaStore) {
    val dims = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val ringShape = RoundedCornerShape(dims.radii.pill)
    // Accent ring (matching the primary CTA) with a gap between ring and avatar.
    Box(
        modifier = Modifier
            .border(dims.stroke.cardOuter, colors.accent, ringShape)
            .padding(dims.spacing.sm),
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
}

@Composable
private fun ProfileStatistics(momentCount: Long, timelineCount: Long, placeCount: Long) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacing.xl, vertical = dims.spacing.md)
            .semantics { contentDescription = listOf(pluralizedStat(momentCount, "moment"), pluralizedStat(timelineCount, "timeline"), pluralizedStat(placeCount, "place")).joinToString(", ") },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatistic(momentCount, "Moments")
        ProfileStatistic(timelineCount, "Timelines")
        ProfileStatistic(placeCount, "Places")
    }
}

@Composable
private fun ProfileStatistic(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ReliveTheme.dimensions.spacing.xs)) {
        Text(value.toString(), style = ReliveTheme.typography.title, color = ReliveTheme.colors.textPrimary)
        Text(label, style = ReliveTheme.typography.tag, color = ReliveTheme.colors.textMuted)
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
    onUpgrade: (() -> Unit)? = null,
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
                top = dims.spacing.xxl,
                bottom = if (last) dims.spacing.huge else dims.spacing.none,
            ),
    ) {
        Text(title, style = ReliveTheme.typography.eyebrow, color = ReliveTheme.colors.textSecondary)
        labels.forEach { label ->
            ProfileSettingRow(
                label = label,
                icon = profileIconFor(label),
                onClick = when (label.trim()) {
                    "Preferences" -> onPreferences
                    "Media & storage" -> onMediaStorage
                    "Backup" -> onBackup
                    "Relive Pro" -> onUpgrade
                    "Location" -> onLocation
                    "Rediscover notifications" -> onNotifications
                    "Privacy & security" -> onPrivacy
                    "Help & feedback" -> onHelp
                    "About Relive" -> onAbout
                    else -> null
                },
            )
        }
    }
}

private fun profileIconFor(label: String): ImageVector = when (label.trim()) {
    "Media & storage" -> ProfileIcons.Media
    "Backup" -> ProfileIcons.Backup
    "Relive Pro" -> ProfileIcons.Info
    "Preferences" -> ProfileIcons.Preferences
    "Location" -> ProfileIcons.Location
    "Rediscover notifications" -> ProfileIcons.Notifications
    "Privacy & security" -> ProfileIcons.Security
    "Help & feedback" -> ProfileIcons.Help
    "About Relive" -> ProfileIcons.Info
    else -> ProfileIcons.Person
}

@Composable
private fun ProfilePhotoDialog(hasPhoto: Boolean, onDismiss: () -> Unit, onPick: () -> Unit, onRemove: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Profile photo") },
    text = { Text(if (hasPhoto) "Replace or remove your local profile photo." else "Choose a photo from your device.") },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    confirmButton = { Row { if (hasPhoto) TextButton(onClick = onRemove) { Text("Remove") }; TextButton(onClick = onPick) { Text(if (hasPhoto) "Replace" else "Choose") } } },
)

@Composable
private fun ProfileSettingRow(label: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    val dims = ReliveTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dims.minTouchTarget)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(dims.icon.md), tint = ReliveTheme.colors.accentMuted)
        Text(
            label,
            style = ReliveTheme.typography.body,
            color = ReliveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f).padding(start = dims.spacing.md),
        )
        ForwardGlyph(dims.icon.sm, ReliveTheme.colors.textMuted, dims.stroke.icon)
    }
}
