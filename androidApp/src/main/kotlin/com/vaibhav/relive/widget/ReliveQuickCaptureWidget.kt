package com.vaibhav.relive.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.ContentScale
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
import com.vaibhav.relive.R
import com.vaibhav.relive.ReliveIntents
import com.vaibhav.relive.data.settings.AndroidAppearanceRepository
import com.vaibhav.relive.data.settings.AndroidProfileSettingsRepository
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.platform.media.AndroidMediaStore
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
        val profilePhoto = AndroidProfileSettingsRepository(context).settings.value.profilePhoto
        val isDark = resolveDarkMode(preferences.mode, systemDark = context.isSystemDark())
        val roles = paletteFor(preferences.defaultTheme).roles(isDark)
        val avatar = profilePhoto?.let { loadCircularAvatar(context, it) }
        provideContent { QuickCaptureContent(roles, avatar) }
    }
}

@Composable
private fun QuickCaptureContent(roles: RelivePaletteRoles, avatar: Bitmap?) {
    val context = LocalContext.current
    val ink = ColorProvider(roles.ink)
    val inkSoft = ColorProvider(roles.inkSoft)
    val accent = ColorProvider(roles.primary)
    val onAccent = ColorProvider(contrastOn(roles.primary))
    val glass = ColorProvider(roles.surface.copy(alpha = WIDGET_GLASS_ALPHA))
    val glassEdge = ColorProvider(roles.ink.copy(alpha = WIDGET_EDGE_ALPHA))
    val avatarSurface = ColorProvider(roles.surface)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(glassEdge)
            .cornerRadius(28.dp)
            .padding(1.dp)
            .clickable(actionStartActivity(addMomentIntent(context))),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(glass)
                .cornerRadius(27.dp)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier.size(52.dp).cornerRadius(26.dp).background(accent).padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp).background(avatarSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatar != null) {
                            Image(
                                provider = ImageProvider(avatar),
                                contentDescription = "Profile photo",
                                modifier = GlanceModifier.size(48.dp),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.widget_profile_placeholder),
                                contentDescription = "Profile",
                                modifier = GlanceModifier.size(24.dp),
                                colorFilter = ColorFilter.tint(inkSoft),
                            )
                        }
                    }
                }
                Spacer(GlanceModifier.width(16.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "Capture a moment",
                        style = TextStyle(
                            color = ink,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        "Add today to your archive",
                        style = TextStyle(color = inkSoft, fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.width(12.dp))
                Box(
                    // Match the expanded + New control from the floating navigation toolbar.
                    modifier = GlanceModifier
                        .width(136.dp)
                        .height(64.dp)
                        .cornerRadius(32.dp)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+ New",
                        style = TextStyle(
                            color = onAccent,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

private const val WIDGET_GLASS_ALPHA = 0.78f
private const val WIDGET_EDGE_ALPHA = 0.18f
private const val AVATAR_SIZE_PX = 96

/** Decodes the locally managed profile image at widget scale and crops it to the avatar circle. */
private fun loadCircularAvatar(context: Context, ref: MediaStorageRef): Bitmap? = runCatching {
    val path = AndroidMediaStore(context).resolveAbsolutePath(ref)
    val decoded = BitmapFactory.decodeFile(path) ?: return null
    val output = Bitmap.createBitmap(AVATAR_SIZE_PX, AVATAR_SIZE_PX, Bitmap.Config.ARGB_8888)
    val shader = BitmapShader(decoded, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val scale = maxOf(
        AVATAR_SIZE_PX.toFloat() / decoded.width,
        AVATAR_SIZE_PX.toFloat() / decoded.height,
    )
    val dx = (AVATAR_SIZE_PX - decoded.width * scale) / 2f
    val dy = (AVATAR_SIZE_PX - decoded.height * scale) / 2f
    val matrix = android.graphics.Matrix().apply { setScale(scale, scale); postTranslate(dx, dy) }
    shader.setLocalMatrix(matrix)
    Canvas(output).drawCircle(
        AVATAR_SIZE_PX / 2f,
        AVATAR_SIZE_PX / 2f,
        AVATAR_SIZE_PX / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader },
    )
    decoded.recycle()
    output
}.getOrNull()

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
