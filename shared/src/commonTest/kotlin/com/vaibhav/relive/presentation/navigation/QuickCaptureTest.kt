package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.presentation.timeline.CurrentTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickCaptureTest {

    @Test
    fun newIsVisibleOnApprovedRootSurfacesAndTargetsAllComposer() {
        listOf(
            QuickCaptureSurface.TimelineHome,
            QuickCaptureSurface.Rediscover,
            QuickCaptureSurface.Search,
        ).forEach { surface ->
            val command = quickCaptureCommand(surface)
            assertEquals(CurrentTimeline.All, command?.timeline)
            assertTrue(command?.openComposer == true)
        }
    }

    @Test
    fun newIsHiddenOnExcludedSurfaces() {
        listOf(
            QuickCaptureSurface.Profile,
            QuickCaptureSurface.TimelineDetail,
            QuickCaptureSurface.MediaViewer,
            QuickCaptureSurface.Camera,
            QuickCaptureSurface.Recorder,
            QuickCaptureSurface.ModalDetail,
        ).forEach { surface -> assertNull(quickCaptureCommand(surface)) }
    }

    @Test
    fun allComposerWaitsForDestinationThenOpensOnceWithoutDuplication() {
        assertFalse(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, false))
        assertTrue(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, true))
        assertFalse(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, true, true))
        assertFalse(
            shouldExpandQuickCaptureComposer(
                requested = true,
                currentTimeline = CurrentTimeline.Custom(TimelineId("custom")),
                isAlreadyExpanded = false,
                isDestinationSettled = true,
            ),
        )
    }
}
