package com.vaibhav.relive.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vaibhav.relive.MainActivity
import com.vaibhav.relive.ReliveIntents
import com.vaibhav.relive.data.settings.AndroidAppearanceRepository
import com.vaibhav.relive.presentation.settings.resolveDarkMode
import com.vaibhav.relive.ui.theme.RelivePaletteRoles
import com.vaibhav.relive.ui.theme.paletteFor

/**
 * A single, calm home-screen card that invites the user to capture a moment. It shows no archive
 * content — only a warm prompt — and the whole surface is one tap target that opens the app straight
 * into the Home quick-capture composer via [ReliveIntents.ACTION_ADD_MOMENT].
 *
 * Colors follow whatever palette and light/dark mode the user has chosen in Appearance: the widget
 * reads the saved [com.vaibhav.relive.domain.model.AppearancePreferences] at render time, and
 * `MainActivity` re-renders it (`updateAll`) whenever that choice changes.
 */
class ReliveQuickCaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = AndroidAppearanceRepository(context).preferences.value
        val isDark = resolveDarkMode(preferences.mode, systemDark = context.isSystemDark())
        val roles = paletteFor(preferences.defaultTheme).roles(isDark)
        provideContent { QuickCaptureContent(roles) }
    }
}

@Composable
private fun QuickCaptureContent(roles: RelivePaletteRoles) {
    val context = LocalContext.current
    val ink = ColorProvider(roles.ink)
    val inkSoft = ColorProvider(roles.inkSoft)
    val accent = ColorProvider(roles.primary)
    val onAccent = ColorProvider(contrastOn(roles.primary))
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(roles.canvas))
            .cornerRadius(24.dp)
            .padding(20.dp)
            .clickable(actionStartActivity(addMomentIntent(context))),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier.size(44.dp).cornerRadius(22.dp).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = onAccent, fontSize = 26.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.width(14.dp))
            Column {
                Text(
                    "Capture a moment",
                    style = TextStyle(
                        color = ink,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    "Add today to your archive",
                    style = TextStyle(color = inkSoft, fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
                )
            }
        }
    }
}

private fun Context.isSystemDark(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/** A readable text color for a filled accent badge: white on a dark accent, near-black on a light one. */
private fun contrastOn(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance < 0.55f) Color.White else Color(0xFF1A1A1A)
}

private fun addMomentIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(ReliveIntents.ACTION_ADD_MOMENT)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
