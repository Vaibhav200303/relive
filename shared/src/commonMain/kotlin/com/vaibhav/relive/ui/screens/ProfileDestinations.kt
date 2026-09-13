package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhav.relive.domain.entitlement.ReliveLegalLinks
import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.ProfileSettings
import com.vaibhav.relive.platform.notifications.NotificationPermissionState
import com.vaibhav.relive.platform.system.buildSafeDiagnosticMail
import com.vaibhav.relive.platform.system.platformAppInfo
import com.vaibhav.relive.platform.system.platformMailComposer
import com.vaibhav.relive.platform.system.PlatformAppInfo
import com.vaibhav.relive.ui.components.profile.*
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.rememberReliveHandwritingFamily

@Composable
fun LocationScreen(showLocation: Boolean, onShowLocationChange: (Boolean) -> Unit, onBack: () -> Unit) = ProfileScaffold("Location", "Choose whether saved locations appear on moments.", onBack) {
    ProfileSectionHeading("LOCATION")
    ProfileSwitchRow("Show location on moments", checked = showLocation, onCheckedChange = onShowLocationChange)
    ProfileSupportingText("Turning this off hides saved locations from presentation only. Manual location entry and existing Moment data remain unchanged.")
}

@Composable
fun RediscoverNotificationsScreen(settings: ProfileSettings, permission: NotificationPermissionState, onEnabledChange: (Boolean) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) = ProfileScaffold("Reminders", "A gentle daily nudge to capture today, and a look back when a memory resurfaces.", onBack) {
    ProfileSectionHeading("REMINDERS")
    ProfileSwitchRow("Daily reminders", "No memory titles, text, media, or locations appear in notifications.", settings.rediscoverRemindersEnabled, permission != NotificationPermissionState.Unavailable, onEnabledChange)
    if (permission == NotificationPermissionState.Denied) ProfileSettingRow("Notifications are off in system settings", "Open settings to allow reminders", onClick = onOpenSettings)
}

@Composable
fun PrivacySecurityScreen(settings: ProfileSettings, deviceAuthAvailable: Boolean, biometricsAvailable: Boolean, biometricExplanation: String?, onAppLockChange: (Boolean) -> Unit, onBiometricsChange: (Boolean) -> Unit, onLockAfterChange: (LockAfter) -> Unit, onBack: () -> Unit) {
    var selectTimeout by remember { mutableStateOf(false) }
    ProfileScaffold("Privacy & Security", "Protect access to your private archive.", onBack) {
        ProfileSectionHeading("APP LOCK")
        ProfileSwitchRow("App Lock", if (deviceAuthAvailable) "Require device authentication when Relive locks." else "Set a secure device lock to use App Lock.", settings.appLockEnabled, deviceAuthAvailable, onAppLockChange)
        ProfileDivider()
        ProfileSwitchRow("Biometric Unlock", biometricExplanation, settings.biometricUnlockEnabled, settings.appLockEnabled && biometricsAvailable, onBiometricsChange)
        ProfileDivider()
        ProfileSettingRow("Lock after", settings.lockAfter.label, enabled = settings.appLockEnabled, onClick = { selectTimeout = true })
        ProfileSectionHeading("YOUR DATA")
        ProfileSupportingText("Your Relive archive is stored locally on this device.")
        ProfileSupportingText("Location is stored with a Moment only when you add it.")
        ProfileSupportingText("Backup is managed separately through Backup & Restore.")
    }
    if (selectTimeout) ProfileSelectionDialog("Lock Relive", LockAfter.entries.map { it.label }, settings.lockAfter.label, { selectTimeout = false }) { label -> onLockAfterChange(LockAfter.entries.first { it.label == label }); selectTimeout = false }
}

internal enum class HelpTopic(val title: String, val copy: String) {
    CreatingMoments("Create a Moment", "Tap New, add a title or note, and keep it when it feels complete. A Moment can be text-only or include media."),
    AddingMedia("Add photos, video, or voice", "Open Add Media in the composer to attach photos, videos, or a voice recording. You can remove any attachment before keeping the Moment."),
    OrganizingTimelines("Organize timelines", "Custom timelines gather related Moments into chapters. Every Moment still remains safely in your All moments archive."),
    FindingMemories("Find a memory", "Search saved titles and writing, browse a timeline by date, or return to Favourites, On This Day, From Your Past, and All Photos."),
    BackupRestore("Backup & restore", "Backup and restore are separate from your local archive. Connect Google Drive from Backup & Restore when it is available to your plan."),
    PrivacySecurity("Privacy & App Lock", "Your archive stays on this device. Add App Lock in Privacy & Security to require your device authentication before opening Relive."),
    RelivePro("Relive Pro", "Relive Pro unlocks automatic backup, more timelines, and additional appearances. Use Restore purchases after reinstalling or changing devices."),
}

@Composable
fun HelpFeedbackScreen(
    supportEmail: String,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf<HelpTopic?>(HelpTopic.CreatingMoments) }
    var notice by remember { mutableStateOf<String?>(null) }
    val mail = remember { platformMailComposer() }
    val info = remember { platformAppInfo() }
    val supportAvailable = supportEmail.isNotBlank()
    fun send(subject: String) {
        val request = supportMailRequest(subject, info, supportEmail) ?: return
        if (!mail.open(request)) {
            notice = "No mail app is available."
            onMessage(notice!!)
        }
    }
    ProfileScaffold("Help & Feedback", onBack = onBack) {
        DestinationHero(
            eyebrow = "SUPPORT CENTER",
            title = "How can we help?",
            body = "Find a quick answer, learn how a feature works, or send the Relive team a note.",
            icon = ProfileIcons.Help,
        )
        ProfileSectionHeading("QUICK ANSWERS")
        HelpTopic.entries.forEach { topic ->
            ProfileSettingRow(
                topic.title,
                if (expanded == topic) topic.copy else null,
                onClick = { expanded = if (expanded == topic) null else topic },
            )
            if (topic != HelpTopic.entries.last()) ProfileDivider()
        }
        ProfileSectionHeading("CONTACT SUPPORT")
        if (supportAvailable) {
            ProfileSettingRow(
                "Email support",
                supportEmail,
                onClick = { send("Relive support request") },
            )
        } else {
            ProfileSupportingText("Support email is not configured for this build yet.")
        }
        ProfileDivider()
        ProfileSettingRow(
            "Send feedback",
            "Share an idea or tell us what you enjoy.",
            enabled = supportAvailable,
            onClick = if (supportAvailable) ({ send("Relive feedback") }) else null,
        )
        ProfileDivider()
        ProfileSettingRow(
            "Report a problem",
            "Tell us what happened so we can help.",
            enabled = supportAvailable,
            onClick = if (supportAvailable) ({ send("Relive problem report") }) else null,
        )
        ProfileSupportingText("Messages include only the app version, platform, and OS version. No archive data is attached.")
        notice?.let { ProfileSupportingText(it) }
    }
}

internal fun supportMailRequest(subject: String, info: PlatformAppInfo, supportEmail: String) =
    supportEmail.takeIf { it.isNotBlank() }?.let { buildSafeDiagnosticMail(subject, info, it) }

data class AboutGuideSection(val title: String, val body: String, val icon: ImageVector)

val aboutGuideSections = listOf(
    AboutGuideSection(
        "Capture what matters",
        "Keep writing, photos, video, voice, tags, and an optional manual location together in one Moment.",
        ProfileIcons.Media,
    ),
    AboutGuideSection(
        "Make chapters of your life",
        "Create custom timelines for the people, places, and seasons that belong together.",
        ProfileIcons.Person,
    ),
    AboutGuideSection(
        "Rediscover your archive",
        "Return to Favourites, On This Day, From Your Past, and more.",
        ProfileIcons.Calendar,
    ),
    AboutGuideSection(
        "Find your way back",
        "Search saved titles and writing, or use Calendar to jump to any day.",
        ProfileIcons.Search,
    ),
    AboutGuideSection(
        "Notice how life feels",
        "Look back, find patterns, and appreciate your journey.",
        ProfileIcons.Favorite,
    ),
)

val aboutPrivacySection = AboutGuideSection(
    "Your memories stay with you.",
    "Relive is local-first, never a social profile, and never used for advertising.",
    ProfileIcons.Security,
)

@Composable
fun AboutReliveScreen(legalLinks: ReliveLegalLinks, onOpenLicenses: () -> Unit, onBack: () -> Unit) {
    val info = remember { platformAppInfo() }
    val uriHandler = LocalUriHandler.current
    ProfileScaffold("About Relive", onBack = onBack) {
        AboutReliveHero()
        ProfileSectionHeading("WHAT RELIVE HELPS YOU KEEP")
        AboutFeaturePanel()
        Spacer(Modifier.height(ReliveTheme.dimensions.spacing.md))
        AboutPrivacyCard(aboutPrivacySection)
        ProfileSectionHeading("APP INFORMATION")
        AboutSettingsPanel {
            ProfileSettingRow("Version", info.versionAndBuild, icon = ProfileIcons.Info)
        }
        ProfileSectionHeading("LEGAL")
        AboutSettingsPanel {
            ProfileSettingRow(
                "Privacy Policy",
                if (legalLinks.privacyPolicyUrl.isBlank()) "Not configured for this build" else null,
                enabled = legalLinks.privacyPolicyUrl.isNotBlank(),
                icon = ProfileIcons.Export,
                onClick = if (legalLinks.privacyPolicyUrl.isNotBlank()) ({ uriHandler.openUri(legalLinks.privacyPolicyUrl) }) else null,
            )
            ProfileDivider()
            ProfileSettingRow(
                "Terms of Service",
                if (legalLinks.termsOfServiceUrl.isBlank()) "Not configured for this build" else null,
                enabled = legalLinks.termsOfServiceUrl.isNotBlank(),
                icon = ProfileIcons.Export,
                onClick = if (legalLinks.termsOfServiceUrl.isNotBlank()) ({ uriHandler.openUri(legalLinks.termsOfServiceUrl) }) else null,
            )
            ProfileDivider()
            ProfileSettingRow("Open-source licenses", icon = ProfileIcons.Archive, onClick = onOpenLicenses)
        }
        AboutClosingNote()
    }
}

@Composable
private fun AboutReliveHero() {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val handwriting = rememberReliveHandwritingFamily()
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Relive. Capture moments. Relive them later. A private, local-first archive for the thoughts, places, people, and small details you want to hold on to."
            },
    ) {
        AboutLandscape(Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = d.spacing.xl, vertical = d.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(d.spacing.lg),
            verticalAlignment = Alignment.Top,
        ) {
            AboutJournalMark()
            Column(Modifier.weight(1f)) {
                Text("Relive", color = colors.textPrimary, style = ReliveTheme.typography.display)
                Text(
                    "CAPTURE MOMENTS. RELIVE THEM LATER.",
                    modifier = Modifier.padding(top = d.spacing.xs),
                    color = colors.textSecondary,
                    style = ReliveTheme.typography.eyebrow.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                )
                Text(
                    "A private, local-first archive for the thoughts, places, people, and small details you want to hold on to.",
                    modifier = Modifier.padding(top = d.spacing.sm),
                    color = colors.textSecondary,
                    style = ReliveTheme.typography.caption,
                )
                Text(
                    "A kinder you, for later  ♡",
                    modifier = Modifier.padding(top = d.spacing.md),
                    color = colors.accentMuted,
                    style = TextStyle(fontFamily = handwriting, fontSize = 18.sp, lineHeight = 24.sp),
                )
            }
        }
    }
}

@Composable
private fun AboutJournalMark() {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Box(
        Modifier
            .size(84.dp)
            .shadow(
                10.dp,
                shape,
                ambientColor = colors.shadow.copy(alpha = 0.22f),
                spotColor = colors.shadow.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accentMuted, colors.accent))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(56.dp)) {
            val paper = colors.textOnAccent
            val ink = colors.accent
            drawRoundRect(
                paper,
                topLeft = Offset(size.width * .25f, size.height * .12f),
                size = androidx.compose.ui.geometry.Size(size.width * .56f, size.height * .72f),
                cornerRadius = CornerRadius(size.width * .05f),
            )
            drawLine(ink, Offset(size.width * .40f, size.height * .12f), Offset(size.width * .40f, size.height * .84f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(size.width * .43f, size.height * .34f), Offset(size.width * .72f, size.height * .34f), 2.dp.toPx(), StrokeCap.Round)
            drawCircle(paper, size.width * .055f, Offset(size.width * .18f, size.height * .42f))
            drawLine(paper, Offset(size.width * .18f, size.height * .42f), Offset(size.width * .40f, size.height * .42f), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun AboutLandscape(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    Canvas(modifier) {
        drawCircle(
            colors.tint.copy(alpha = .72f),
            radius = size.width * .055f,
            center = Offset(size.width * .83f, size.height * .14f),
        )
        val far = Path().apply {
            moveTo(size.width * .50f, size.height)
            lineTo(size.width * .72f, size.height * .67f)
            lineTo(size.width * .78f, size.height * .76f)
            lineTo(size.width * .92f, size.height * .43f)
            lineTo(size.width, size.height * .53f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(far, colors.tint.copy(alpha = .68f))
        val near = Path().apply {
            moveTo(size.width * .58f, size.height)
            lineTo(size.width * .78f, size.height * .78f)
            lineTo(size.width * .84f, size.height * .86f)
            lineTo(size.width, size.height * .65f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(near, colors.accentMuted.copy(alpha = .24f))
        drawPath(
            Path().apply {
                moveTo(size.width * .76f, size.height)
                quadraticTo(size.width * .73f, size.height * .86f, size.width * .86f, size.height * .82f)
                quadraticTo(size.width * .97f, size.height * .79f, size.width * .88f, size.height * .70f)
            },
            color = colors.surfaceCard.copy(alpha = .82f),
            style = Stroke(width = size.width * .035f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AboutFeaturePanel() {
    val d = ReliveTheme.dimensions
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Column(
        Modifier
            .padding(horizontal = d.spacing.xl)
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(d.stroke.hairline, ReliveTheme.colors.borderMuted, shape),
    ) {
        aboutGuideSections.forEachIndexed { index, section ->
            FeatureGuideRow(section, index)
            if (index != aboutGuideSections.lastIndex) {
                HorizontalDivider(
                    Modifier.padding(start = 72.dp, end = d.spacing.lg),
                    thickness = d.stroke.hairline,
                    color = ReliveTheme.colors.borderMuted,
                )
            }
        }
    }
}

@Composable
private fun FeatureGuideRow(section: AboutGuideSection, index: Int) {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val chip = when (index % 3) {
        0 -> colors.accent.copy(alpha = .10f)
        1 -> colors.spark.copy(alpha = .10f)
        else -> colors.tint.copy(alpha = .82f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = d.spacing.lg, vertical = d.spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = "${section.title}. ${section.body}" },
        horizontalArrangement = Arrangement.spacedBy(d.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(d.radii.medium)).background(chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(section.icon, contentDescription = null, tint = colors.accentMuted, modifier = Modifier.size(d.icon.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                section.title,
                color = colors.textPrimary,
                style = ReliveTheme.typography.action.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(section.body, color = colors.textSecondary, style = ReliveTheme.typography.caption)
        }
    }
}

@Composable
private fun AboutPrivacyCard(section: AboutGuideSection) {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Row(
        Modifier
            .padding(horizontal = d.spacing.xl)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.tint.copy(alpha = .68f))
            .border(d.stroke.hairline, colors.borderMuted, shape)
            .padding(d.spacing.lg)
            .semantics(mergeDescendants = true) {
                contentDescription = "Private by design. ${section.title} ${section.body}"
            },
        horizontalArrangement = Arrangement.spacedBy(d.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(d.radii.medium)).background(colors.accent.copy(alpha = .10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(section.icon, null, tint = colors.accentMuted, modifier = Modifier.size(d.icon.md))
        }
        Column(Modifier.weight(1f)) {
            Text("PRIVATE BY DESIGN", color = colors.textSecondary, style = ReliveTheme.typography.eyebrow.copy(fontSize = 9.sp))
            Text(
                section.title,
                color = colors.textPrimary,
                style = ReliveTheme.typography.title.copy(fontSize = 19.sp, lineHeight = 24.sp),
            )
            Text(section.body, color = colors.textSecondary, style = ReliveTheme.typography.caption)
        }
    }
}

@Composable
private fun AboutSettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    val d = ReliveTheme.dimensions
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Column(
        Modifier
            .padding(horizontal = d.spacing.xl)
            .fillMaxWidth()
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(d.stroke.hairline, ReliveTheme.colors.borderMuted, shape),
        content = content,
    )
}

@Composable
private fun AboutClosingNote() {
    val d = ReliveTheme.dimensions
    Text(
        "Thanks for being here  ♡",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = d.spacing.xl)
            .semantics { contentDescription = "Thanks for being here" },
        color = ReliveTheme.colors.accentMuted,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = rememberReliveHandwritingFamily(),
            fontSize = 17.sp,
            lineHeight = 24.sp,
        ),
    )
}

@Composable
private fun DestinationHero(eyebrow: String, title: String, body: String, icon: ImageVector) {
    val d = ReliveTheme.dimensions
    val shape = RoundedCornerShape(d.radii.largeIncreased)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.spacing.xl, vertical = d.spacing.xl)
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(d.stroke.hairline, ReliveTheme.colors.borderMuted, shape)
            .padding(d.spacing.xl)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $body" },
        horizontalArrangement = Arrangement.spacedBy(d.spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ReliveTheme.colors.accent,
            modifier = Modifier.size(d.icon.lg),
        )
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.eyebrow)
            Text(
                title,
                modifier = Modifier.padding(top = d.spacing.xs).semantics { heading() },
                color = ReliveTheme.colors.textPrimary,
                style = ReliveTheme.typography.title,
            )
            Text(body, modifier = Modifier.padding(top = d.spacing.sm), color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.body)
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) = ProfileScaffold("Open-source licenses", onBack = onBack) {
    ProfileSectionHeading("RELIVE DEPENDENCIES")
    listOf("Kotlin", "Compose Multiplatform", "Material 3", "SQLDelight", "kotlinx.coroutines", "AndroidX Activity, Core, Lifecycle, Credentials, CameraX, Media3, and WorkManager", "Google Identity and Play services").forEach { ProfileSettingRow(it, "Apache License 2.0") }
}
