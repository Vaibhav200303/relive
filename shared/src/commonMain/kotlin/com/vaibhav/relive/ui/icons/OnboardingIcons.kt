package com.vaibhav.relive.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small local vectors used only by the reference-matched onboarding flow. */
object OnboardingIcons {
    val Lock = vector(
        name = "Privacy lock",
        pathData = "M17,8H16V6C16,3.79 14.21,2 12,2C9.79,2 8,3.79 8,6V8H7C5.9,8 5,8.9 5,10V20C5,21.1 5.9,22 7,22H17C18.1,22 19,21.1 19,20V10C19,8.9 18.1,8 17,8M10,6C10,4.9 10.9,4 12,4C13.1,4 14,4.9 14,6V8H10V6M13,17.73V19H11V17.73C10.4,17.38 10,16.73 10,16C10,14.9 10.9,14 12,14C13.1,14 14,14.9 14,16C14,16.73 13.6,17.38 13,17.73Z",
    )
    val Note = vector(
        name = "Notes",
        pathData = "M6,2H14L20,8V22H6C4.9,22 4,21.1 4,20V4C4,2.9 4.9,2 6,2M13,4H6V20H18V9H13V4M8,12H16V14H8V12M8,16H16V18H8V16Z",
    )
    val VisibilityOff = vector(
        name = "You control what's visible",
        pathData = "M2.1,3.51L3.51,2.1L21.9,20.49L20.49,21.9L17.38,18.79C15.79,19.57 13.98,20 12,20C7,20 2.73,16.89 1,12C1.74,9.91 2.91,8.1 4.4,6.65L2.1,3.51M12,6C17,6 21.27,9.11 23,12C22.35,13.84 21.35,15.47 20.08,16.82L17.23,13.97C17.41,13.35 17.5,12.69 17.5,12C17.5,8.96 15.04,6.5 12,6.5C11.31,6.5 10.65,6.59 10.03,6.77L8.26,5C9.43,4.35 10.69,4 12,4M12,8.5C13.93,8.5 15.5,10.07 15.5,12C15.5,12.15 15.49,12.3 15.47,12.44L11.56,8.53C11.7,8.51 11.85,8.5 12,8.5M8.53,11.56L12.44,15.47C12.3,15.49 12.15,15.5 12,15.5C10.07,15.5 8.5,13.93 8.5,12C8.5,11.85 8.51,11.7 8.53,11.56M5.82,8.07C4.57,9.11 3.57,10.45 2.97,12C4.5,15.17 7.89,18 12,18C13.42,18 14.75,17.66 15.93,17.07L14.42,15.56C13.69,15.85 12.88,16 12,16C8.96,16 6.5,13.54 6.5,10.5C6.5,9.62 6.65,8.81 6.94,8.08L5.82,8.07Z",
    )

    private fun vector(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
}
