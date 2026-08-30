package com.vaibhav.relive.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.PathEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Path

@Immutable
data class ReliveDurations(
    val short1: Int = 50,
    val short2: Int = 100,
    val short3: Int = 150,
    val short4: Int = 200,
    val medium1: Int = 250,
    val medium2: Int = 300,
    val medium3: Int = 350,
    val medium4: Int = 400,
    val long1: Int = 450,
    val long2: Int = 500,
    val long3: Int = 550,
    val long4: Int = 600,
    val extraLong1: Int = 700,
) {
    @Deprecated("use the M3 duration scale", ReplaceWith("short3"))
    val fastMillis: Int get() = short3

    @Deprecated("use the M3 duration scale", ReplaceWith("short2"))
    val timelineReturnMillis: Int get() = short2

    @Deprecated("use the M3 duration scale", ReplaceWith("medium2"))
    val standardMillis: Int get() = medium2

    @Deprecated("use the M3 duration scale", ReplaceWith("long2"))
    val slowMillis: Int get() = long2
}

// PathEasing needs `androidx.compose.ui.graphics.Path`, whose Android actual delegates to
// `android.graphics.Path` and is not present on the plain-JVM host-test classpath. Defer
// construction until first use so token-shape assertions can run without an Android runtime.
private val EmphasizedEasing: Easing by lazy {
    val path = Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
    }
    PathEasing(path)
}

@Immutable
class ReliveEasings {
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val emphasized: Easing get() = EmphasizedEasing
}

@Immutable
data class ReliveMotion(
    val durations: ReliveDurations = ReliveDurations(),
    val easings: ReliveEasings = ReliveEasings(),
)

val DefaultReliveMotion: ReliveMotion = ReliveMotion()
