package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.ProfileSettings
import com.vaibhav.relive.platform.notifications.NotificationPermissionState
import com.vaibhav.relive.platform.system.buildSafeDiagnosticMail
import com.vaibhav.relive.platform.system.platformAppInfo
import com.vaibhav.relive.platform.system.platformMailComposer
import com.vaibhav.relive.ui.components.profile.*
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

private enum class HelpTopic(val title: String, val copy: String) {
    GettingStarted("Getting started", "Create a Moment with the New button, add what you want to remember, then save it to your private archive."),
    Timelines("Timelines", "Use custom timelines to gather related moments into chapters without moving them out of chronological order."),
    FindingMemory("Finding a memory", "Search text and tags, browse Calendar, or revisit Favorites and On This Day in Rediscover."),
    BackupRestore("Backup & restore", "Connect Google Drive from Backup & Restore. Relive keeps backup controls separate from your local archive."),
}

@Composable
fun HelpFeedbackScreen(onBack: () -> Unit, onMessage: (String) -> Unit) {
    var expanded by remember { mutableStateOf<HelpTopic?>(HelpTopic.GettingStarted) }
    var notice by remember { mutableStateOf<String?>(null) }
    val mail = remember { platformMailComposer() }
    val info = remember { platformAppInfo() }
    fun send(subject: String) { if (!mail.open(buildSafeDiagnosticMail(subject, info, SUPPORT_EMAIL))) { notice = "No mail app is available."; onMessage(notice!!) } }
    ProfileScaffold("Help & Feedback", "A few quick answers, or send us a note.", onBack) {
        ProfileSectionHeading("QUICK HELP")
        HelpTopic.entries.forEach { topic ->
            ProfileSettingRow(topic.title, if (expanded == topic) topic.copy else null, onClick = { expanded = topic })
            if (topic != HelpTopic.entries.last()) ProfileDivider()
        }
        ProfileSectionHeading("FEEDBACK")
        ProfileSettingRow("Send feedback", onClick = { send("Relive feedback") })
        ProfileDivider()
        ProfileSettingRow("Report a problem", onClick = { send("Relive problem report") })
        ProfileSupportingText("Messages include only app version, platform, and OS version. No archive data is attached.")
        notice?.let { ProfileSupportingText(it) }
    }
}

data class AboutScreenshotPlaceholder(val id: String, val caption: String)
data class AboutArticleSection(val title: String, val body: String, val screenshot: AboutScreenshotPlaceholder? = null)

val aboutGuideSections = listOf(
    AboutArticleSection("Capture", "Save writing, photos, video, audio, tags, and an optional location in a Moment.", AboutScreenshotPlaceholder("about_capture", "The Moment composer")),
    AboutArticleSection("Timelines", "Shape custom chapters while keeping every Moment in your chronological archive.", AboutScreenshotPlaceholder("about_timelines", "Timeline Home")),
    AboutArticleSection("Your timeline", "Scroll chronologically through your memories and open rich media in place.", AboutScreenshotPlaceholder("about_chronology", "A chronological timeline")),
    AboutArticleSection("Search", "Find memories by their saved words and tags.", AboutScreenshotPlaceholder("about_search", "Search your archive")),
    AboutArticleSection("Rediscover", "Return to Favorites and eligible On This Day memories.", AboutScreenshotPlaceholder("about_rediscover", "Rediscover")),
    AboutArticleSection("Calendar", "Move directly to a date in your archive.", AboutScreenshotPlaceholder("about_calendar", "Calendar navigation")),
    AboutArticleSection("Backup", "Manage Google Drive backup and restore separately from the archive.", AboutScreenshotPlaceholder("about_backup", "Backup & Restore")),
    AboutArticleSection("Make it yours", "Choose Appearance and Preferences without changing the memories themselves."),
    AboutArticleSection("Private by design", "Relive is local-first. Your archive is not a social profile and is never used for advertising."),
)

@Composable
fun AboutReliveScreen(onOpenLicenses: () -> Unit, onBack: () -> Unit) {
    val info = remember { platformAppInfo() }
    ProfileScaffold("About Relive", onBack = onBack) {
        Column(Modifier.fillMaxWidth().padding(horizontal = ReliveTheme.dimensions.spacing.xl, vertical = ReliveTheme.dimensions.spacing.xxl)) {
            Text("Relive", color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.title)
            Text("Remember. Relive.", color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.subtitle)
            Text("A private, local-first home for the moments that make up your life.", modifier = Modifier.padding(top = ReliveTheme.dimensions.spacing.md), color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.body)
        }
        ProfileSectionHeading("GUIDE")
        aboutGuideSections.forEach { section ->
            Text(section.title, modifier = Modifier.padding(horizontal = ReliveTheme.dimensions.spacing.xl, vertical = ReliveTheme.dimensions.spacing.sm), color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.subtitle)
            ProfileSupportingText(section.body)
            section.screenshot?.let { shot -> Box(Modifier.fillMaxWidth().height(132.dp).padding(horizontal = ReliveTheme.dimensions.spacing.xl, vertical = ReliveTheme.dimensions.spacing.sm).semantics { contentDescription = "${shot.caption}, screenshot placeholder ${shot.id}" }) { Text(shot.caption, color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.tag) } }
        }
        ProfileSectionHeading("APP")
        ProfileSettingRow("Version", info.versionAndBuild)
        ProfileSectionHeading("LEGAL")
        ProfileSettingRow("Privacy Policy", "Coming before release", enabled = false)
        ProfileSettingRow("Terms of Service", "Coming before release", enabled = false)
        ProfileSettingRow("Open-source licenses", onClick = onOpenLicenses)
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) = ProfileScaffold("Open-source licenses", onBack = onBack) {
    ProfileSectionHeading("RELIVE DEPENDENCIES")
    listOf("Kotlin", "Compose Multiplatform", "Material 3", "SQLDelight", "kotlinx.coroutines", "AndroidX Activity, Core, Lifecycle, Credentials, CameraX, Media3, and WorkManager", "Google Identity and Play services").forEach { ProfileSettingRow(it, "Apache License 2.0") }
}

const val SUPPORT_EMAIL = "support@relive.invalid" // RELEASE_CONFIG: replace with a deliverable support address.
