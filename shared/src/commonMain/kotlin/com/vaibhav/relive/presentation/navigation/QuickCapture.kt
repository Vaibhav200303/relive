package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.presentation.timeline.CurrentTimeline

/** App surfaces where the global quick-capture action may or may not be shown. */
enum class QuickCaptureSurface {
    TimelineHome,
    Rediscover,
    Search,
    Profile,
    TimelineDetail,
    MediaViewer,
    Camera,
    Recorder,
    ModalDetail,
}
data class QuickCaptureCommand(
    val timeline: CurrentTimeline,
    val openComposer: Boolean,
)

/** The approved global action always targets the existing editable All composer. */
fun quickCaptureCommand(surface: QuickCaptureSurface): QuickCaptureCommand? = when (surface) {
    QuickCaptureSurface.TimelineHome,
    QuickCaptureSurface.Rediscover,
    QuickCaptureSurface.Search,
    -> QuickCaptureCommand(
        timeline = CurrentTimeline.All,
        openComposer = true,
    )
    QuickCaptureSurface.Profile,
    QuickCaptureSurface.TimelineDetail,
    QuickCaptureSurface.MediaViewer,
    QuickCaptureSurface.Camera,
    QuickCaptureSurface.Recorder,
    QuickCaptureSurface.ModalDetail,
    -> null
}
