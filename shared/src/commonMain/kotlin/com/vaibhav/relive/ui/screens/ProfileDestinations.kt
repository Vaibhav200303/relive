package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
        "Create custom timelines for the people, places, and seasons that belong together. Every Moment also stays in All moments.",
        ProfileIcons.Person,
    ),
    AboutGuideSection(
        "Rediscover your archive",
        "Return to Favourites, On This Day, From Your Past, and All Photos whenever a memory deserves another look.",
        ProfileIcons.Info,
    ),
    AboutGuideSection(
        "Find your way back",
        "Search saved titles and writing, or use Calendar to move directly to a day in your archive.",
        ProfileIcons.Help,
    ),
    AboutGuideSection(
        "Notice how life feels",
        "Optionally mark a new Moment as Great, Good, or Low, then explore gentle mood reflections over time.",
        ProfileIcons.Info,
    ),
    AboutGuideSection(
        "Make Relive yours",
        "Choose an appearance, tune preferences, and give All moments and each custom timeline its own visual character.",
        ProfileIcons.Preferences,
    ),
    AboutGuideSection(
        "Private by design",
        "Relive is local-first: your archive lives on your device, is never a social profile, and is never used for advertising.",
        ProfileIcons.Security,
    ),
    AboutGuideSection(
        "Backup on your terms",
        "Backup and restore are managed separately from the local archive. Relive Pro adds scheduled automatic backup where available.",
        ProfileIcons.Backup,
    ),
    AboutGuideSection(
        "Grow with Relive Pro",
        "Relive Pro unlocks automatic backup, more custom timelines, and every appearance while your existing archive remains yours.",
        ProfileIcons.Info,
    ),
)

@Composable
fun AboutReliveScreen(legalLinks: ReliveLegalLinks, onOpenLicenses: () -> Unit, onBack: () -> Unit) {
    val info = remember { platformAppInfo() }
    val uriHandler = LocalUriHandler.current
    ProfileScaffold("About Relive", onBack = onBack) {
        DestinationHero(
            eyebrow = "CAPTURE MOMENTS. RELIVE THEM LATER.",
            title = "A home for your life.",
            body = "Relive is a private, local-first archive for the thoughts, places, people, and small details you want to hold on to.",
            icon = ProfileIcons.Info,
        )
        ProfileSectionHeading("WHAT RELIVE HELPS YOU KEEP")
        aboutGuideSections.forEach { section ->
            FeatureGuideCard(section)
        }
        ProfileSectionHeading("APP")
        ProfileSettingRow("Version", info.versionAndBuild)
        ProfileSectionHeading("LEGAL")
        ProfileSettingRow(
            "Privacy Policy",
            if (legalLinks.privacyPolicyUrl.isBlank()) "Not configured for this build" else null,
            enabled = legalLinks.privacyPolicyUrl.isNotBlank(),
            onClick = if (legalLinks.privacyPolicyUrl.isNotBlank()) ({ uriHandler.openUri(legalLinks.privacyPolicyUrl) }) else null,
        )
        ProfileDivider()
        ProfileSettingRow(
            "Terms of Service",
            if (legalLinks.termsOfServiceUrl.isBlank()) "Not configured for this build" else null,
            enabled = legalLinks.termsOfServiceUrl.isNotBlank(),
            onClick = if (legalLinks.termsOfServiceUrl.isNotBlank()) ({ uriHandler.openUri(legalLinks.termsOfServiceUrl) }) else null,
        )
        ProfileDivider()
        ProfileSettingRow("Open-source licenses", onClick = onOpenLicenses)
    }
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
private fun FeatureGuideCard(section: AboutGuideSection) {
    val d = ReliveTheme.dimensions
    val shape = RoundedCornerShape(d.radii.large)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.spacing.xl, vertical = d.spacing.xs)
            .clip(shape)
            .background(ReliveTheme.colors.surfaceCard)
            .border(d.stroke.hairline, ReliveTheme.colors.borderMuted, shape)
            .padding(d.spacing.lg)
            .semantics(mergeDescendants = true) { contentDescription = "${section.title}. ${section.body}" },
        horizontalArrangement = Arrangement.spacedBy(d.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            section.icon,
            contentDescription = null,
            tint = ReliveTheme.colors.accentMuted,
            modifier = Modifier.size(d.icon.md),
        )
        Column(Modifier.weight(1f)) {
            Text(section.title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.subtitle)
            Text(
                section.body,
                modifier = Modifier.padding(top = d.spacing.xs),
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.body,
            )
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) = ProfileScaffold("Open-source licenses", onBack = onBack) {
    ProfileSectionHeading("RELIVE DEPENDENCIES")
    listOf("Kotlin", "Compose Multiplatform", "Material 3", "SQLDelight", "kotlinx.coroutines", "AndroidX Activity, Core, Lifecycle, Credentials, CameraX, Media3, and WorkManager", "Google Identity and Play services").forEach { ProfileSettingRow(it, "Apache License 2.0") }
}
