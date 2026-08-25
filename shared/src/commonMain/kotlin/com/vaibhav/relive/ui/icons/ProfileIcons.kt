package com.vaibhav.relive.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material-style filled vectors kept local to avoid adding the icon-extended dependency. */
object ProfileIcons {
    val Person = vector("Person", "M12,12C14.21,12 16,10.21 16,8C16,5.79 14.21,4 12,4C9.79,4 8,5.79 8,8C8,10.21 9.79,12 12,12M12,14C9.33,14 4,15.34 4,18V20H20V18C20,15.34 14.67,14 12,14Z")
    val Media = vector("Media", "M21,19V5C21,3.9 20.1,3 19,3H5C3.9,3 3,3.9 3,5V19C3,20.1 3.9,21 5,21H19C20.1,21 21,20.1 21,19M8.5,13.5L11,16.5L14.5,12L19,18H5L8.5,13.5Z")
    val Backup = vector("Backup", "M19,9H15V3H9V9H5L12,16L19,9M5,18V20H19V18H5Z")
    val Preferences = vector("Preferences", "M3,17V19H9V17H3M3,5V7H13V5H3M13,21V19H21V17H13V15H11V21H13M7,9V11H3V13H7V15H9V9H7M21,13V11H11V13H21M15,9H17V7H21V5H17V3H15V9Z")
    val Location = vector("Location", "M12,2C8.13,2 5,5.13 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9C19,5.13 15.87,2 12,2M12,11.5C10.62,11.5 9.5,10.38 9.5,9C9.5,7.62 10.62,6.5 12,6.5C13.38,6.5 14.5,7.62 14.5,9C14.5,10.38 13.38,11.5 12,11.5Z")
    val Notifications = vector("Notifications", "M12,22C13.1,22 14,21.1 14,20H10C10,21.1 10.9,22 12,22M18,16V11C18,7.93 16.36,5.36 13.5,4.68V4C13.5,3.17 12.83,2.5 12,2.5C11.17,2.5 10.5,3.17 10.5,4V4.68C7.64,5.36 6,7.93 6,11V16L4,18V19H20V18L18,16Z")
    val Security = vector("Security", "M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,5C13.66,5 15,6.34 15,8C15,9.66 13.66,11 12,11C10.34,11 9,9.66 9,8C9,6.34 10.34,5 12,5M17.13,17C15.92,18.85 14.09,20.28 12,20.88C9.91,20.28 8.08,18.85 6.87,17C6.95,15 10.25,13.9 12,13.9C13.75,13.9 17.05,15 17.13,17Z")
    val Help = vector("Help", "M10,19H13V22H10V19M12,2C6.48,2 2,6.48 2,12C2,17.52 6.48,22 12,22V19C8.13,19 5,15.87 5,12C5,8.13 8.13,5 12,5C15.87,5 19,8.13 19,12C19,13.57 18.5,15 17.63,16.15L19.99,18.51C21.24,16.66 22,14.41 22,12C22,6.48 17.52,2 12,2M12,7C10.34,7 9,8.34 9,10H12C12,9.45 12.45,9 13,9C13.55,9 14,9.45 14,10C14,12 11,11.75 11,16H14C14,13.25 17,13 17,10C17,8.34 15.66,7 14,7H12Z")
    val Info = vector("Info", "M11,17H13V11H11V17M12,2C6.48,2 2,6.48 2,12C2,17.52 6.48,22 12,22C17.52,22 22,17.52 22,12C22,6.48 17.52,2 12,2M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M11,9H13V7H11V9Z")

    private fun vector(name: String, pathData: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.Black))
    }.build()
}
