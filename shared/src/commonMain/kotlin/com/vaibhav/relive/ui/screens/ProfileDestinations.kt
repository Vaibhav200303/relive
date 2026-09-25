package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.vaibhav.relive.ui.components.timeline.BackGlyph
import com.vaibhav.relive.ui.icons.ProfileIcons
import com.vaibhav.relive.ui.theme.ReliveColors
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush
import com.vaibhav.relive.ui.theme.rememberReliveHandwritingFamily
import com.vaibhav.relive.platform.system.ReliveBackHandler

@Composable
fun LocationScreen(showLocation: Boolean, onShowLocationChange: (Boolean) -> Unit, onBack: () -> Unit) = ProfileScaffold("Location", onBack = onBack) {
    SettingsReferenceIllustration(SettingsIllustration.Location)
    SettingsReferenceTitle("Show where it happened", "Add places to your moments and relive your journey on the map.")
    SettingsReferencePanel {
        SettingsReferenceSwitchRow(
            "Show location on moments",
            "Saved locations appear on moments.",
            showLocation,
            onShowLocationChange,
        )
    }
    SettingsReferenceNotice(
        ProfileIcons.Location,
        "You're in control",
        "Turning this off hides saved locations from presentation only. Manual location entry and existing Moment data remain unchanged.",
    )
}

@Composable
fun RediscoverNotificationsScreen(settings: ProfileSettings, permission: NotificationPermissionState, onEnabledChange: (Boolean) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) = ProfileScaffold("Reminders", onBack = onBack) {
    SettingsReferenceIllustration(SettingsIllustration.Reminder)
    SettingsReferenceTitle("Don't let moments slip away", "A gentle daily nudge to capture today, and a look back when a memory resurfaces.")
    SettingsReferencePanel {
        SettingsReferenceSwitchRow(
            "Daily reminders",
            "Get a friendly reminder to capture your day.",
            settings.rediscoverRemindersEnabled,
            onEnabledChange,
            permission != NotificationPermissionState.Unavailable,
        )
        if (permission == NotificationPermissionState.Denied) {
            SettingsReferenceDivider()
            SettingsReferenceActionRow("Notifications are off", "Open system settings to allow reminders", onOpenSettings)
        }
    }
    SettingsReferenceNotice(
        ProfileIcons.Notifications,
        "Your privacy matters",
        "No memory titles, text, media, or locations appear in notifications.",
    )
}

@Composable
fun PrivacySecurityScreen(settings: ProfileSettings, deviceAuthAvailable: Boolean, biometricsAvailable: Boolean, biometricExplanation: String?, onAppLockChange: (Boolean) -> Unit, onBiometricsChange: (Boolean) -> Unit, onLockAfterChange: (LockAfter) -> Unit, onBack: () -> Unit) {
    var selectTimeout by remember { mutableStateOf(false) }
    ProfileScaffold("Privacy & Security", onBack = onBack) {
        SettingsReferenceIllustration(SettingsIllustration.Privacy)
        SettingsReferenceTitle("Your memories, your space", "Keep your archive private and secure.")
        SettingsReferenceSectionLabel("APP LOCK")
        SettingsReferencePanel {
            SettingsReferenceSwitchRow("App Lock", if (deviceAuthAvailable) "Require device authentication when Relive locks." else "Set a secure device lock to use App Lock.", settings.appLockEnabled, onAppLockChange, deviceAuthAvailable, SettingsOptionIcon.Lock)
            SettingsReferenceDivider()
            SettingsReferenceSwitchRow("Biometric Unlock", biometricExplanation ?: "Use your fingerprint or face ID to open the app.", settings.biometricUnlockEnabled, onBiometricsChange, settings.appLockEnabled && biometricsAvailable, SettingsOptionIcon.Fingerprint)
            SettingsReferenceDivider()
            SettingsReferenceActionRow("Lock after", settings.lockAfter.label, { selectTimeout = true }, settings.appLockEnabled, SettingsOptionIcon.Clock)
        }
        SettingsReferenceSectionLabel("YOUR DATA")
        SettingsReferencePanel {
            SettingsReferenceStaticRow("Stored on this device", "Your Relive archive is stored locally on this device.", SettingsOptionIcon.Phone)
            SettingsReferenceDivider()
            SettingsReferenceStaticRow("Location privacy", "Location is stored with a Moment only when you add it.", SettingsOptionIcon.Location)
            SettingsReferenceDivider()
            SettingsReferenceStaticRow("Backups", "Backup is managed separately through Backup & Restore.", SettingsOptionIcon.Cloud)
        }
        SettingsReferenceNotice(ProfileIcons.Security, "You're in control", "Relive is local-first, never a social profile, and is never used for advertising.")
    }
    if (selectTimeout) ProfileSelectionDialog("Lock Relive", LockAfter.entries.map { it.label }, settings.lockAfter.label, { selectTimeout = false }) { label -> onLockAfterChange(LockAfter.entries.first { it.label == label }); selectTimeout = false }
}

private enum class SettingsIllustration { Reminder, Location, Privacy }

private enum class SettingsOptionIcon { Lock, Fingerprint, Clock, Phone, Location, Cloud }

@Composable
private fun SettingsReferenceIllustration(kind: SettingsIllustration) {
    val colors = ReliveTheme.colors
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(142.dp)
            .padding(horizontal = ReliveTheme.dimensions.spacing.xxl),
    ) {
        when (kind) {
            SettingsIllustration.Reminder -> drawReminderHero(colors)
            SettingsIllustration.Location -> drawLocationHero(colors)
            SettingsIllustration.Privacy -> drawPrivacyHero(colors)
        }
    }
}

private fun DrawScope.drawReminderHero(colors: ReliveColors) {
    val centerX = size.width / 2f
    val bellWidth = 112.dp.toPx().coerceAtMost(size.width * .52f)
    val bellHeight = 108.dp.toPx().coerceAtMost(size.height * .68f)
    val left = centerX - bellWidth / 2f
    val top = size.height * .08f
    val accent = colors.accent

    drawOval(
        color = colors.shadow.copy(alpha = .12f),
        topLeft = Offset(centerX - bellWidth * .24f, top + bellHeight * 1.05f),
        size = Size(bellWidth * .48f, bellHeight * .08f),
    )

    val body = Path().apply {
        moveTo(centerX - bellWidth * .10f, top + bellHeight * .09f)
        quadraticTo(centerX - bellWidth * .10f, top, centerX, top)
        quadraticTo(centerX + bellWidth * .10f, top, centerX + bellWidth * .10f, top + bellHeight * .09f)
        cubicTo(
            centerX + bellWidth * .36f,
            top + bellHeight * .13f,
            centerX + bellWidth * .38f,
            top + bellHeight * .42f,
            centerX + bellWidth * .38f,
            top + bellHeight * .58f,
        )
        quadraticTo(
            centerX + bellWidth * .38f,
            top + bellHeight * .71f,
            centerX + bellWidth * .43f,
            top + bellHeight * .78f,
        )
        quadraticTo(
            centerX + bellWidth * .51f,
            top + bellHeight * .78f,
            centerX + bellWidth * .51f,
            top + bellHeight * .88f,
        )
        quadraticTo(
            centerX + bellWidth * .51f,
            top + bellHeight * .98f,
            centerX + bellWidth * .42f,
            top + bellHeight * .98f,
        )
        lineTo(centerX - bellWidth * .42f, top + bellHeight * .98f)
        quadraticTo(
            centerX - bellWidth * .51f,
            top + bellHeight * .98f,
            centerX - bellWidth * .51f,
            top + bellHeight * .88f,
        )
        quadraticTo(
            centerX - bellWidth * .51f,
            top + bellHeight * .78f,
            centerX - bellWidth * .43f,
            top + bellHeight * .78f,
        )
        quadraticTo(
            centerX - bellWidth * .38f,
            top + bellHeight * .71f,
            centerX - bellWidth * .38f,
            top + bellHeight * .58f,
        )
        cubicTo(
            centerX - bellWidth * .38f,
            top + bellHeight * .42f,
            centerX - bellWidth * .36f,
            top + bellHeight * .13f,
            centerX - bellWidth * .10f,
            top + bellHeight * .09f,
        )
        close()
    }
    drawPath(body, accent)

    drawArc(
        color = accent,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(centerX - bellWidth * .17f, top + bellHeight * 1.04f),
        size = Size(bellWidth * .34f, bellHeight * .28f),
    )

    val ringStroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    drawArc(
        color = accent.copy(alpha = .92f),
        startAngle = 196f,
        sweepAngle = 44f,
        useCenter = false,
        topLeft = Offset(left - bellWidth * .10f, top + bellHeight * .04f),
        size = Size(bellWidth * .34f, bellHeight * .48f),
        style = ringStroke,
    )
    drawArc(
        color = accent.copy(alpha = .92f),
        startAngle = 300f,
        sweepAngle = 44f,
        useCenter = false,
        topLeft = Offset(centerX + bellWidth * .27f, top + bellHeight * .04f),
        size = Size(bellWidth * .34f, bellHeight * .48f),
        style = ringStroke,
    )
}

private fun DrawScope.drawLocationHero(colors: ReliveColors) {
    val ink = colors.accent
    val haze = Path().apply {
        moveTo(size.width * .16f, size.height * .64f)
        quadraticTo(size.width * .14f, size.height * .34f, size.width * .31f, size.height * .31f)
        quadraticTo(size.width * .43f, size.height * .30f, size.width * .48f, size.height * .20f)
        quadraticTo(size.width * .65f, size.height * .08f, size.width * .77f, size.height * .31f)
        quadraticTo(size.width * .87f, size.height * .55f, size.width * .76f, size.height * .72f)
        close()
    }
    drawPath(haze, colors.tint.copy(alpha = .74f))
    val map = Path().apply {
        moveTo(size.width * .13f, size.height * .62f)
        lineTo(size.width * .33f, size.height * .40f)
        lineTo(size.width * .50f, size.height * .53f)
        lineTo(size.width * .70f, size.height * .36f)
        lineTo(size.width * .87f, size.height * .61f)
        lineTo(size.width * .68f, size.height * .87f)
        lineTo(size.width * .49f, size.height * .73f)
        lineTo(size.width * .29f, size.height * .88f)
        close()
    }
    drawPath(map, ink.copy(alpha = .35f))
    drawPath(
        Path().apply {
            moveTo(size.width * .33f, size.height * .40f)
            lineTo(size.width * .49f, size.height * .73f)
            lineTo(size.width * .50f, size.height * .53f)
            close()
        },
        colors.surfaceCard.copy(alpha = .68f),
    )
    drawPath(
        Path().apply {
            moveTo(size.width * .50f, size.height * .53f)
            lineTo(size.width * .68f, size.height * .87f)
            lineTo(size.width * .87f, size.height * .61f)
            lineTo(size.width * .70f, size.height * .36f)
            close()
        },
        colors.tint.copy(alpha = .88f),
    )
    val road = colors.surfaceCard.copy(alpha = .94f)
    drawPath(
        Path().apply {
            moveTo(size.width * .18f, size.height * .68f)
            lineTo(size.width * .38f, size.height * .54f)
            lineTo(size.width * .51f, size.height * .58f)
            lineTo(size.width * .68f, size.height * .44f)
        },
        road,
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        Path().apply {
            moveTo(size.width * .30f, size.height * .82f)
            lineTo(size.width * .44f, size.height * .69f)
            lineTo(size.width * .57f, size.height * .73f)
            lineTo(size.width * .76f, size.height * .57f)
        },
        road,
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawLine(road, Offset(size.width * .42f, size.height * .47f), Offset(size.width * .46f, size.height * .70f), 2.6.dp.toPx(), StrokeCap.Round)
    drawLine(road, Offset(size.width * .60f, size.height * .49f), Offset(size.width * .70f, size.height * .79f), 2.6.dp.toPx(), StrokeCap.Round)
    drawMapTree(Offset(size.width * .24f, size.height * .57f), ink.copy(alpha = .88f), .75f)
    drawMapTree(Offset(size.width * .77f, size.height * .47f), ink.copy(alpha = .92f), .84f)
    val pinCenter = Offset(size.width * .51f, size.height * .23f)
    val pin = Path().apply {
        moveTo(pinCenter.x, size.height * .49f)
        cubicTo(size.width * .46f, size.height * .36f, size.width * .455f, size.height * .13f, pinCenter.x, size.height * .13f)
        cubicTo(size.width * .565f, size.height * .13f, size.width * .56f, size.height * .36f, pinCenter.x, size.height * .49f)
        close()
    }
    drawPath(pin, ink)
    drawCircle(colors.surfaceCard, 5.5.dp.toPx(), pinCenter)
}

private fun DrawScope.drawMapTree(center: Offset, color: androidx.compose.ui.graphics.Color, scale: Float = 1f) {
    drawLine(color, center, Offset(center.x, center.y + 25.dp.toPx() * scale), 2.dp.toPx(), StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(center.x, center.y - 18.dp.toPx() * scale)
            lineTo(center.x - 9.dp.toPx() * scale, center.y + 11.dp.toPx() * scale)
            lineTo(center.x + 9.dp.toPx() * scale, center.y + 11.dp.toPx() * scale)
            close()
        },
        color,
    )
    drawPath(
        Path().apply {
            moveTo(center.x, center.y - 10.dp.toPx() * scale)
            lineTo(center.x - 12.dp.toPx() * scale, center.y + 17.dp.toPx() * scale)
            lineTo(center.x + 12.dp.toPx() * scale, center.y + 17.dp.toPx() * scale)
            close()
        },
        color.copy(alpha = .50f),
    )
}

private fun DrawScope.drawPrivacyHero(colors: ReliveColors) {
    val ink = colors.accentMuted
    drawOval(colors.tint.copy(alpha = .82f), Offset(size.width * .20f, size.height * .13f), Size(size.width * .47f, size.height * .63f))
    drawOval(colors.accent.copy(alpha = .08f), Offset(size.width * .29f, size.height * .57f), Size(size.width * .49f, size.height * .21f))
    drawLeaf(Offset(size.width * .66f, size.height * .43f), Size(size.width * .18f, size.height * .28f), -17f, ink.copy(alpha = .28f))
    drawLeaf(Offset(size.width * .65f, size.height * .58f), Size(size.width * .21f, size.height * .23f), 15f, ink.copy(alpha = .43f))
    drawLeaf(Offset(size.width * .57f, size.height * .64f), Size(size.width * .16f, size.height * .18f), 43f, ink.copy(alpha = .22f))
    val lockCenter = Offset(size.width * .49f, size.height * .46f)
    rotate(-5f, lockCenter) {
        val centerX = lockCenter.x
        val bodyTop = size.height * .36f
        drawArc(
            ink,
            180f,
            180f,
            false,
            Offset(centerX - 20.dp.toPx(), size.height * .12f),
            Size(40.dp.toPx(), 50.dp.toPx()),
            style = Stroke(7.dp.toPx(), cap = StrokeCap.Round),
        )
        drawRoundRect(
            ink.copy(alpha = .27f),
            Offset(centerX - 31.dp.toPx(), bodyTop - 4.dp.toPx()),
            Size(62.dp.toPx(), 60.dp.toPx()),
            CornerRadius(9.dp.toPx()),
        )
        drawRoundRect(
            ink.copy(alpha = .66f),
            Offset(centerX - 25.dp.toPx(), bodyTop),
            Size(50.dp.toPx(), 52.dp.toPx()),
            CornerRadius(8.dp.toPx()),
        )
        val innerShield = Path().apply {
            moveTo(centerX - 18.dp.toPx(), bodyTop + 8.dp.toPx())
            quadraticTo(centerX, bodyTop + 3.dp.toPx(), centerX + 18.dp.toPx(), bodyTop + 8.dp.toPx())
            lineTo(centerX + 15.dp.toPx(), bodyTop + 29.dp.toPx())
            quadraticTo(centerX, bodyTop + 45.dp.toPx(), centerX - 15.dp.toPx(), bodyTop + 29.dp.toPx())
            close()
        }
        drawPath(innerShield, colors.surfaceCard.copy(alpha = .22f))
        drawPath(innerShield, ink.copy(alpha = .55f), style = Stroke(1.2.dp.toPx()))
        drawCircle(colors.textPrimary.copy(alpha = .55f), 7.dp.toPx(), Offset(centerX, bodyTop + 22.dp.toPx()))
        drawPath(
            Path().apply {
                moveTo(centerX - 3.dp.toPx(), bodyTop + 26.dp.toPx())
                lineTo(centerX - 5.dp.toPx(), bodyTop + 37.dp.toPx())
                lineTo(centerX + 5.dp.toPx(), bodyTop + 37.dp.toPx())
                lineTo(centerX + 3.dp.toPx(), bodyTop + 26.dp.toPx())
                close()
            },
            colors.textPrimary.copy(alpha = .55f),
        )
    }
}

private fun DrawScope.drawLeaf(center: Offset, leafSize: Size, rotation: Float, color: androidx.compose.ui.graphics.Color) {
    rotate(rotation, center) {
        val leaf = Path().apply {
            moveTo(center.x - leafSize.width / 2f, center.y)
            quadraticTo(center.x, center.y - leafSize.height / 2f, center.x + leafSize.width / 2f, center.y)
            quadraticTo(center.x, center.y + leafSize.height / 2f, center.x - leafSize.width / 2f, center.y)
            close()
        }
        drawPath(leaf, color)
        drawLine(
            color = color.copy(alpha = .8f),
            start = Offset(center.x - leafSize.width * .35f, center.y),
            end = Offset(center.x + leafSize.width * .35f, center.y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun SettingsReferenceTitle(title: String, body: String) {
    val d = ReliveTheme.dimensions
    Column(Modifier.fillMaxWidth().padding(horizontal = d.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.title, textAlign = TextAlign.Center)
        Text(body, modifier = Modifier.padding(top = d.spacing.xs), color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.caption, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsReferenceSectionLabel(label: String) = Text(label, Modifier.fillMaxWidth().padding(start = ReliveTheme.dimensions.spacing.xl, top = ReliveTheme.dimensions.spacing.xl, bottom = ReliveTheme.dimensions.spacing.sm), color = ReliveTheme.colors.accentMuted, style = ReliveTheme.typography.eyebrow)

@Composable
private fun SettingsReferencePanel(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxWidth().padding(horizontal = ReliveTheme.dimensions.spacing.lg, vertical = ReliveTheme.dimensions.spacing.xl).clip(RoundedCornerShape(ReliveTheme.dimensions.radii.largeIncreased)).background(ReliveTheme.colors.surfaceCard).border(ReliveTheme.dimensions.stroke.hairline, ReliveTheme.colors.borderMuted, RoundedCornerShape(ReliveTheme.dimensions.radii.largeIncreased)), content = content)

@Composable
private fun SettingsReferenceSwitchRow(label: String, supporting: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true, icon: SettingsOptionIcon? = null) {
    val d = ReliveTheme.dimensions
    Row(Modifier.fillMaxWidth().heightIn(min = d.minTouchTarget).toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange).padding(horizontal = d.spacing.md, vertical = d.spacing.sm).semantics(mergeDescendants = true) { contentDescription = "$label, ${if (checked) "on" else "off"}" }, verticalAlignment = Alignment.CenterVertically) {
        icon?.let { SettingsOptionIcon(it, enabled); Spacer(Modifier.width(d.spacing.sm)) }
        Column(Modifier.weight(1f)) { Text(label, color = if (enabled) ReliveTheme.colors.textPrimary else ReliveTheme.colors.textMuted, style = ReliveTheme.typography.action); supporting?.let { Text(it, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.tag) } }
        androidx.compose.material3.Switch(checked, null, enabled = enabled)
    }
}

@Composable
private fun SettingsReferenceActionRow(label: String, supporting: String?, onClick: () -> Unit, enabled: Boolean = true, icon: SettingsOptionIcon? = null) = SettingsReferenceStaticRow(label, supporting, icon, enabled, onClick)

@Composable
private fun SettingsReferenceStaticRow(label: String, supporting: String?, icon: SettingsOptionIcon? = null, enabled: Boolean = true, onClick: (() -> Unit)? = null, showChevron: Boolean = onClick != null) {
    val d = ReliveTheme.dimensions
    Row(Modifier.fillMaxWidth().heightIn(min = d.minTouchTarget).then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = d.spacing.md, vertical = d.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { SettingsOptionIcon(it, enabled); Spacer(Modifier.width(d.spacing.sm)) }
        Column(Modifier.weight(1f)) { Text(label, color = if (enabled) ReliveTheme.colors.textPrimary else ReliveTheme.colors.textMuted, style = ReliveTheme.typography.action); supporting?.let { Text(it, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.tag) } }
        if (showChevron && enabled) SettingsChevron()
    }
}

@Composable
private fun SettingsOptionIcon(icon: SettingsOptionIcon, enabled: Boolean) {
    val d = ReliveTheme.dimensions
    val colors = ReliveTheme.colors
    val glyph = if (enabled) colors.accentMuted else colors.textMuted
    val tile = when (icon) {
        SettingsOptionIcon.Cloud -> colors.spark.copy(alpha = .10f)
        SettingsOptionIcon.Fingerprint -> colors.accent.copy(alpha = .08f)
        else -> colors.tint.copy(alpha = .70f)
    }
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(d.radii.medium)).background(tile),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(21.dp)) {
            val stroke = 1.5.dp.toPx()
            val outline = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (icon) {
                SettingsOptionIcon.Lock -> {
                    drawArc(glyph, 180f, 180f, false, Offset(size.width * .25f, size.height * .04f), Size(size.width * .50f, size.height * .58f), style = outline)
                    drawRoundRect(glyph, Offset(size.width * .18f, size.height * .39f), Size(size.width * .64f, size.height * .54f), CornerRadius(2.5.dp.toPx()), style = outline)
                    drawCircle(glyph, 1.5.dp.toPx(), Offset(size.width * .50f, size.height * .65f))
                }
                SettingsOptionIcon.Fingerprint -> {
                    drawArc(glyph, 205f, 225f, false, Offset(size.width * .16f, size.height * .13f), Size(size.width * .68f, size.height * .76f), style = outline)
                    drawArc(glyph, 205f, 225f, false, Offset(size.width * .27f, size.height * .24f), Size(size.width * .46f, size.height * .58f), style = outline)
                    drawArc(glyph, 205f, 190f, false, Offset(size.width * .38f, size.height * .35f), Size(size.width * .24f, size.height * .37f), style = outline)
                    drawArc(glyph, 135f, 90f, false, Offset(size.width * .08f, size.height * .28f), Size(size.width * .84f, size.height * .70f), style = outline)
                }
                SettingsOptionIcon.Clock -> {
                    drawCircle(glyph, size.minDimension * .38f, center, style = outline)
                    drawLine(glyph, center, Offset(center.x, size.height * .27f), stroke, StrokeCap.Round)
                    drawLine(glyph, center, Offset(size.width * .68f, size.height * .57f), stroke, StrokeCap.Round)
                }
                SettingsOptionIcon.Phone -> {
                    drawRoundRect(glyph, Offset(size.width * .25f, size.height * .05f), Size(size.width * .50f, size.height * .90f), CornerRadius(2.5.dp.toPx()), style = outline)
                    drawLine(glyph, Offset(size.width * .34f, size.height * .18f), Offset(size.width * .66f, size.height * .18f), stroke * .75f, StrokeCap.Round)
                    drawCircle(glyph, 1.2.dp.toPx(), Offset(center.x, size.height * .82f))
                }
                SettingsOptionIcon.Location -> {
                    val pin = Path().apply {
                        moveTo(center.x, size.height * .95f)
                        cubicTo(size.width * .33f, size.height * .72f, size.width * .21f, size.height * .46f, size.width * .21f, size.height * .34f)
                        cubicTo(size.width * .21f, size.height * .02f, size.width * .79f, size.height * .02f, size.width * .79f, size.height * .34f)
                        cubicTo(size.width * .79f, size.height * .48f, size.width * .67f, size.height * .72f, center.x, size.height * .95f)
                    }
                    drawPath(pin, glyph, style = outline)
                    drawCircle(glyph, size.width * .10f, Offset(center.x, size.height * .34f), style = Stroke(stroke))
                }
                SettingsOptionIcon.Cloud -> {
                    val cloud = Path().apply {
                        moveTo(size.width * .24f, size.height * .77f)
                        cubicTo(size.width * .04f, size.height * .77f, size.width * .04f, size.height * .44f, size.width * .27f, size.height * .42f)
                        cubicTo(size.width * .34f, size.height * .10f, size.width * .72f, size.height * .12f, size.width * .77f, size.height * .43f)
                        cubicTo(size.width * .98f, size.height * .46f, size.width * .96f, size.height * .77f, size.width * .76f, size.height * .77f)
                        close()
                    }
                    drawPath(cloud, glyph, style = outline)
                }
            }
        }
    }
}

@Composable
private fun SettingsChevron() {
    val d = ReliveTheme.dimensions
    val color = ReliveTheme.colors.textSecondary
    Canvas(Modifier.size(d.icon.sm)) {
        val stroke = d.stroke.icon.toPx()
        drawLine(color, Offset(size.width * .34f, size.height * .18f), Offset(size.width * .70f, size.height * .50f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .70f, size.height * .50f), Offset(size.width * .34f, size.height * .82f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun SettingsReferenceDivider() = HorizontalDivider(Modifier.padding(start = ReliveTheme.dimensions.spacing.md), ReliveTheme.dimensions.stroke.hairline, ReliveTheme.colors.borderMuted)

@Composable
private fun SettingsReferenceNotice(icon: ImageVector, title: String, body: String) {
    val d = ReliveTheme.dimensions
    Row(Modifier.fillMaxWidth().padding(horizontal = d.spacing.lg).clip(RoundedCornerShape(d.radii.large)).background(ReliveTheme.colors.tint.copy(alpha = .65f)).padding(d.spacing.lg), horizontalArrangement = Arrangement.spacedBy(d.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(d.icon.lg), ReliveTheme.colors.accentMuted)
        Column(Modifier.weight(1f)) { Text(title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.action); Text(body, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.tag) }
    }
}

internal enum class HelpTopic(
    val title: String,
    val summary: String,
    val copy: String,
    val icon: ImageVector,
) {
    CreatingMoments(
        "Create a Moment",
        "Capture your thoughts, photos, and more",
        "Tap New, add a title or note, and keep it when it feels complete. A Moment can be text-only or include media.",
        ProfileIcons.Plus,
    ),
    AddingMedia(
        "Add Media",
        "Photos, videos or voice notes",
        "Open Add Media in the composer to attach photos, videos, or a voice recording. You can remove any attachment before keeping the Moment.",
        ProfileIcons.Media,
    ),
    OrganizingTimelines(
        "Organize Timelines",
        "Create and manage your timelines",
        "Custom timelines gather related Moments into chapters. Every Moment still remains safely in your All moments archive.",
        ProfileIcons.Folder,
    ),
    FindingMemories(
        "Find a Memory",
        "Search, filter and rediscover",
        "Search saved titles and writing, browse a timeline by date, or return to Favourites, On This Day, From Your Past, and Media.",
        ProfileIcons.Search,
    ),
    BackupRestore(
        "Backup & Restore",
        "Keep your memories safe",
        "Backup and restore are separate from your local archive. Connect Google Drive from Backup & Restore when it is available to your plan.",
        ProfileIcons.CloudOutline,
    ),
    PrivacySecurity(
        "Privacy & App Lock",
        "Secure your archive",
        "Your archive stays on this device. Add App Lock in Privacy & Security to require your device authentication before opening Relive.",
        ProfileIcons.ShieldOutline,
    ),
    RelivePro(
        "Relive Pro",
        "Learn about premium features and benefits",
        "Relive Pro unlocks automatic backup, more timelines, and additional appearances. Use Restore purchases after reinstalling or changing devices.",
        ProfileIcons.Crown,
    ),
}

@Composable
fun HelpFeedbackScreen(
    supportEmail: String,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<HelpTopic?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val mail = remember { platformMailComposer() }
    val info = remember { platformAppInfo() }
    val supportAvailable = supportEmail.isNotBlank()
    val visibleTopics = filterHelpTopics(query)
    fun send(subject: String) {
        val request = supportMailRequest(subject, info, supportEmail) ?: return
        if (!mail.open(request)) {
            notice = "No mail app is available."
            onMessage(notice!!)
        }
    }
    val d = ReliveTheme.dimensions
    ReliveBackHandler(true, onBack)
    Column(Modifier.fillMaxSize().background(ReliveTheme.colors.canvasBrush())) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = d.spacing.md, top = d.spacing.xs),
        ) {
            androidx.compose.material3.IconButton(
                onClick = onBack,
                modifier = Modifier.size(d.minTouchTarget).semantics { contentDescription = "Back to Profile" },
            ) {
                BackGlyph(d.icon.lg, ReliveTheme.colors.textSecondary, d.stroke.icon)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = d.spacing.lg)
                .padding(bottom = d.spacing.huge),
            verticalArrangement = Arrangement.spacedBy(d.spacing.lg),
        ) {
            HelpHeader()
            HelpSearchField(query, onQueryChange = { query = it })
            HelpSectionHeader("Popular topics", "See all", onAction = {
                query = ""
                expanded = null
            })
            if (visibleTopics.isEmpty()) {
                Text(
                    "No help topics match “$query”.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = d.spacing.xl),
                    color = ReliveTheme.colors.textSecondary,
                    style = ReliveTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(d.spacing.sm)) {
                    visibleTopics.chunked(2).forEach { rowTopics ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spacing.sm)) {
                            rowTopics.forEach { topic ->
                                HelpTopicCard(
                                    topic = topic,
                                    expanded = expanded == topic,
                                    onClick = { expanded = if (expanded == topic) null else topic },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowTopics.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (query.isBlank() || HelpTopic.RelivePro.title.contains(query, ignoreCase = true)) {
                HelpProCard(
                    expanded = expanded == HelpTopic.RelivePro,
                    onClick = {
                        expanded = if (expanded == HelpTopic.RelivePro) null else HelpTopic.RelivePro
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(d.spacing.xs)) {
                Text(
                    "Contact us",
                    color = ReliveTheme.colors.textPrimary,
                    style = ReliveTheme.typography.title.copy(fontSize = 18.sp, lineHeight = 24.sp),
                )
                Text(
                    "Have a question, found a bug, or just want to say hi?",
                    color = ReliveTheme.colors.textSecondary,
                    style = ReliveTheme.typography.caption,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spacing.sm)) {
                HelpContactCard(
                    title = "Send feedback",
                    body = "Share an idea or tell us what you enjoy",
                    icon = ProfileIcons.Send,
                    enabled = supportAvailable,
                    onClick = { send("Relive feedback") },
                    modifier = Modifier.weight(1f),
                )
                HelpContactCard(
                    title = "Report a problem",
                    body = "Tell us what happened so we can help",
                    icon = ProfileIcons.Bug,
                    enabled = supportAvailable,
                    onClick = { send("Relive problem report") },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!supportAvailable) {
                Text(
                    "Support email is not configured for this build yet.",
                    color = ReliveTheme.colors.textMuted,
                    style = ReliveTheme.typography.tag,
                )
            }
            HelpPrivacyNotice()
            notice?.let {
                Text(it, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.caption)
            }
        }
    }
}

internal fun filterHelpTopics(query: String): List<HelpTopic> = HelpTopic.entries
    .filter { it != HelpTopic.RelivePro }
    .filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) ||
            it.summary.contains(query, ignoreCase = true) || it.copy.contains(query, ignoreCase = true)
    }

@Composable
private fun HelpHeader() {
    val d = ReliveTheme.dimensions
    Row(Modifier.fillMaxWidth().heightIn(min = 112.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.35f)) {
            Text(
                "Help & Feedback",
                modifier = Modifier.semantics { heading() },
                color = ReliveTheme.colors.textPrimary,
                style = ReliveTheme.typography.title.copy(fontSize = 26.sp, lineHeight = 32.sp),
            )
            Text(
                "We’re here for you.",
                modifier = Modifier.padding(top = d.spacing.xs),
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.body,
            )
            Text(
                "Find answers, learn how things work, or tell us what you think.",
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.caption,
            )
        }
        HelpEnvelopeArt(Modifier.weight(.85f).height(112.dp))
    }
}

@Composable
private fun HelpEnvelopeArt(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val envelopeLeft = size.width * .10f
            val envelopeTop = size.height * .27f
            val envelopeWidth = size.width * .66f
            val envelopeHeight = size.height * .46f
            val center = Offset(envelopeLeft + envelopeWidth / 2f, envelopeTop + envelopeHeight / 2f)
            val blushLight = colors.tint.copy(alpha = .94f)
            val blush = colors.accentMuted.copy(alpha = .48f)
            val blushDeep = colors.accentMuted.copy(alpha = .72f)
            rotate(-6f, center) {
                drawRoundRect(
                    color = colors.shadow.copy(alpha = .10f),
                    topLeft = Offset(envelopeLeft + 2.dp.toPx(), envelopeTop + 4.dp.toPx()),
                    size = Size(envelopeWidth, envelopeHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                drawRoundRect(
                    color = blushLight,
                    topLeft = Offset(envelopeLeft, envelopeTop),
                    size = Size(envelopeWidth, envelopeHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                val backFlap = Path().apply {
                    moveTo(envelopeLeft, envelopeTop)
                    lineTo(envelopeLeft + envelopeWidth / 2f, envelopeTop + envelopeHeight * .53f)
                    lineTo(envelopeLeft + envelopeWidth, envelopeTop)
                    close()
                }
                drawPath(backFlap, blushDeep)
                drawRoundRect(
                    color = colors.surfaceCard,
                    topLeft = Offset(envelopeLeft + envelopeWidth * .20f, envelopeTop - envelopeHeight * .38f),
                    size = Size(envelopeWidth * .64f, envelopeHeight * .86f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawLine(
                    colors.accentMuted.copy(alpha = .22f),
                    Offset(envelopeLeft + envelopeWidth * .28f, envelopeTop - envelopeHeight * .18f),
                    Offset(envelopeLeft + envelopeWidth * .73f, envelopeTop - envelopeHeight * .18f),
                    1.2.dp.toPx(),
                    StrokeCap.Round,
                )
                repeat(3) { index ->
                    val y = envelopeTop - envelopeHeight * .02f + index * 7.dp.toPx()
                    drawLine(
                        colors.accentMuted.copy(alpha = .18f),
                        Offset(envelopeLeft + envelopeWidth * .28f, y),
                        Offset(envelopeLeft + envelopeWidth * (.73f - index * .05f), y),
                        1.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
                val leftFold = Path().apply {
                    moveTo(envelopeLeft, envelopeTop)
                    lineTo(envelopeLeft + envelopeWidth * .48f, envelopeTop + envelopeHeight * .55f)
                    lineTo(envelopeLeft, envelopeTop + envelopeHeight)
                    close()
                }
                drawPath(leftFold, blush)
                val rightFold = Path().apply {
                    moveTo(envelopeLeft + envelopeWidth, envelopeTop)
                    lineTo(envelopeLeft + envelopeWidth * .52f, envelopeTop + envelopeHeight * .55f)
                    lineTo(envelopeLeft + envelopeWidth, envelopeTop + envelopeHeight)
                    close()
                }
                drawPath(rightFold, blushDeep.copy(alpha = .54f))
                val frontFold = Path().apply {
                    moveTo(envelopeLeft, envelopeTop + envelopeHeight)
                    lineTo(envelopeLeft + envelopeWidth * .40f, envelopeTop + envelopeHeight * .54f)
                    quadraticTo(
                        envelopeLeft + envelopeWidth / 2f,
                        envelopeTop + envelopeHeight * .47f,
                        envelopeLeft + envelopeWidth * .60f,
                        envelopeTop + envelopeHeight * .54f,
                    )
                    lineTo(envelopeLeft + envelopeWidth, envelopeTop + envelopeHeight)
                    close()
                }
                drawPath(frontFold, blushLight)
            }
            val rayColor = colors.accentMuted.copy(alpha = .85f)
            drawLine(rayColor, Offset(size.width * .78f, size.height * .17f), Offset(size.width * .80f, size.height * .05f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(rayColor, Offset(size.width * .85f, size.height * .21f), Offset(size.width * .94f, size.height * .13f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(rayColor, Offset(size.width * .88f, size.height * .30f), Offset(size.width * .99f, size.height * .31f), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun HelpSearchField(query: String, onQueryChange: (String) -> Unit) {
    val d = ReliveTheme.dimensions
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(d.radii.large))
            .background(ReliveTheme.colors.surfaceCardTranslucent)
            .padding(horizontal = d.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ProfileIcons.Search, null, Modifier.size(d.icon.md), ReliveTheme.colors.textSecondary)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).padding(horizontal = d.spacing.md),
            singleLine = true,
            textStyle = ReliveTheme.typography.caption.copy(color = ReliveTheme.colors.textPrimary),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) {
                        Text("Search for help...", color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.caption)
                    }
                    field()
                }
            },
        )
        Box(
            Modifier.clip(RoundedCornerShape(d.radii.small)).background(ReliveTheme.colors.tint.copy(alpha = .7f)).padding(horizontal = d.spacing.sm, vertical = 3.dp),
        ) {
            Text("Ctrl K", color = ReliveTheme.colors.textMuted, style = ReliveTheme.typography.tag)
        }
    }
}

@Composable
private fun HelpSectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            color = ReliveTheme.colors.textPrimary,
            style = ReliveTheme.typography.title.copy(fontSize = 17.sp, lineHeight = 22.sp),
        )
        Text(
            "$action  >",
            modifier = Modifier.clickable(onClick = onAction).padding(8.dp),
            color = ReliveTheme.colors.accentMuted,
            style = ReliveTheme.typography.tag,
        )
    }
}

@Composable
private fun HelpTopicCard(topic: HelpTopic, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val d = ReliveTheme.dimensions
    val accent = when (topic) {
        HelpTopic.CreatingMoments -> ReliveTheme.colors.accentMuted
        HelpTopic.AddingMedia -> ReliveTheme.colors.spark
        HelpTopic.OrganizingTimelines -> ReliveTheme.colors.accent
        HelpTopic.FindingMemories -> ReliveTheme.colors.spark
        HelpTopic.BackupRestore -> ReliveTheme.colors.accentMuted
        HelpTopic.PrivacySecurity -> ReliveTheme.colors.accent
        HelpTopic.RelivePro -> ReliveTheme.colors.spark
    }
    Row(
        modifier
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(d.radii.large))
            .background(ReliveTheme.colors.surfaceCardTranslucent)
            .clickable(onClick = onClick)
            .padding(d.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelpIconTile(topic.icon, accent)
        Column(Modifier.weight(1f).padding(start = d.spacing.sm)) {
            Text(topic.title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.action, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (expanded) topic.copy else topic.summary,
                modifier = Modifier.padding(top = 2.dp),
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.tag,
                maxLines = if (expanded) 8 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HelpArrow()
    }
}

@Composable
private fun HelpIconTile(icon: ImageVector, accent: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.size(38.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(23.dp), accent)
    }
}

@Composable
private fun HelpArrow() {
    Text(
        ">",
        modifier = Modifier.padding(start = 3.dp),
        color = ReliveTheme.colors.accentMuted,
        style = ReliveTheme.typography.action,
    )
}

@Composable
private fun HelpProCard(expanded: Boolean, onClick: () -> Unit) {
    val d = ReliveTheme.dimensions
    val topic = HelpTopic.RelivePro
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(d.radii.large))
            .background(ReliveTheme.colors.surfaceCardTranslucent)
            .clickable(onClick = onClick),
    ) {
        HelpProLandscape(Modifier.matchParentSize().align(Alignment.CenterEnd))
        Row(Modifier.fillMaxWidth().padding(d.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            HelpIconTile(topic.icon, ReliveTheme.colors.accentMuted)
            Column(Modifier.weight(1f).padding(horizontal = d.spacing.sm)) {
                Text(topic.title, color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.title.copy(fontSize = 17.sp, lineHeight = 22.sp))
                Text(
                    if (expanded) topic.copy else topic.summary,
                    color = ReliveTheme.colors.textSecondary,
                    style = ReliveTheme.typography.tag,
                    maxLines = if (expanded) 5 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HelpArrow()
        }
    }
}

@Composable
private fun HelpProLandscape(modifier: Modifier = Modifier) {
    val colors = ReliveTheme.colors
    Canvas(modifier) {
        val ground = size.height
        val mountain = Path().apply {
            moveTo(size.width * .57f, ground)
            quadraticTo(size.width * .70f, ground * .55f, size.width * .79f, ground * .82f)
            quadraticTo(size.width * .88f, ground * .33f, size.width, ground * .78f)
            lineTo(size.width, ground)
            close()
        }
        drawPath(mountain, colors.accentMuted.copy(alpha = .15f))
        drawCircle(colors.spark.copy(alpha = .24f), 10.dp.toPx(), Offset(size.width * .76f, size.height * .28f))
        repeat(3) { index ->
            val x = size.width * (.76f + index * .09f)
            drawLine(colors.accentMuted.copy(alpha = .62f), Offset(x, ground), Offset(x, ground * (.72f - index * .08f)), 2.dp.toPx(), StrokeCap.Round)
            val tree = Path().apply {
                moveTo(x, ground * (.53f - index * .08f))
                lineTo(x - 7.dp.toPx(), ground * (.82f - index * .05f))
                lineTo(x + 7.dp.toPx(), ground * (.82f - index * .05f))
                close()
            }
            drawPath(tree, colors.accentMuted.copy(alpha = .62f))
        }
    }
}

@Composable
private fun HelpContactCard(
    title: String,
    body: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = ReliveTheme.dimensions
    val accent = if (enabled) ReliveTheme.colors.accentMuted else ReliveTheme.colors.textMuted
    Row(
        modifier
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(d.radii.large))
            .background(ReliveTheme.colors.surfaceCardTranslucent)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(d.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelpIconTile(icon, accent)
        Column(Modifier.weight(1f).padding(start = d.spacing.sm)) {
            Text(title, color = if (enabled) ReliveTheme.colors.textPrimary else ReliveTheme.colors.textMuted, style = ReliveTheme.typography.action)
            Text(body, color = ReliveTheme.colors.textSecondary, style = ReliveTheme.typography.tag, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (enabled) HelpArrow()
    }
}

@Composable
private fun HelpPrivacyNotice() {
    val d = ReliveTheme.dimensions
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.radii.large))
            .background(ReliveTheme.colors.tint.copy(alpha = .56f))
            .padding(d.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(d.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ProfileIcons.Info, null, Modifier.size(20.dp), ReliveTheme.colors.textSecondary)
        Column(Modifier.weight(1f)) {
            Text("Your privacy matters", color = ReliveTheme.colors.textPrimary, style = ReliveTheme.typography.action)
            Text(
                "Messages include only the app version, platform, and OS version. No archive data is attached.",
                color = ReliveTheme.colors.textSecondary,
                style = ReliveTheme.typography.tag,
            )
        }
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
    AboutGuideSection(
        "Make Relive yours",
        "Choose an appearance, tune preferences, and give your archive its own visual character.",
        ProfileIcons.Preferences,
    ),
    AboutGuideSection(
        "Backup on your terms",
        "Keep your archive safe with manual backup and restore, separate from your local Moments.",
        ProfileIcons.Backup,
    ),
    AboutGuideSection(
        "Export your keepsakes",
        "Create a paper-diary PDF or a portable Relive archive from the memories you choose.",
        ProfileIcons.Export,
    ),
    AboutGuideSection(
        "Grow with Relive Pro",
        "Unlock automatic backup, more custom timelines, and every appearance while your archive stays yours.",
        ProfileIcons.Info,
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
                trailingChevron = true,
            )
            ProfileDivider()
            ProfileSettingRow(
                "Terms of Service",
                if (legalLinks.termsOfServiceUrl.isBlank()) "Not configured for this build" else null,
                enabled = legalLinks.termsOfServiceUrl.isNotBlank(),
                icon = ProfileIcons.Export,
                onClick = if (legalLinks.termsOfServiceUrl.isNotBlank()) ({ uriHandler.openUri(legalLinks.termsOfServiceUrl) }) else null,
                trailingChevron = true,
            )
            ProfileDivider()
            ProfileSettingRow("Open-source licenses", icon = ProfileIcons.Archive, onClick = onOpenLicenses, trailingChevron = true)
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
