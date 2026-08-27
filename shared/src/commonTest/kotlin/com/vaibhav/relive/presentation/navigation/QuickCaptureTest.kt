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
    fun newFromEveryRootOpensAllOnlyAfterTheDestinationSettlesAndOnlyOnce() {
        listOf(
            QuickCaptureSurface.TimelineHome,
            QuickCaptureSurface.Rediscover,
            QuickCaptureSurface.Search,
        ).forEach { surface ->
            assertTrue(quickCaptureCommand(surface)?.openComposer == true)
        }
        assertFalse(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, false))
        assertTrue(shouldExpandQuickCaptureComposer(true, CurrentTimeline.All, false, true))
        // After TimelineScreen consumes the one-shot intent, recomposition remains collapsed-to-open.
        assertFalse(shouldExpandQuickCaptureComposer(false, CurrentTimeline.All, true, true))
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

    @Test
    fun emptyCustomTimelineComposerOpensOnEverySettledEntry() {
        val custom = CurrentTimeline.Custom(TimelineId("custom"))

        assertTrue(
            shouldExpandComposerOnEnter(
                requested = true,
                currentTimeline = custom,
                isAlreadyExpanded = false,
                isDestinationSettled = true,
                isTimelineEmpty = true,
            ),
        )
        // A later ordinary entry is still auto-expanded while the scope is empty.
        assertTrue(
            shouldExpandComposerOnEnter(
                requested = false,
                currentTimeline = custom,
                isAlreadyExpanded = false,
                isDestinationSettled = true,
                isTimelineEmpty = true,
            ),
        )
        assertFalse(
            shouldExpandComposerOnEnter(
                requested = true,
                currentTimeline = custom,
                isAlreadyExpanded = true,
                isDestinationSettled = true,
                isTimelineEmpty = true,
            ),
        )
        // An explicit share/quick-capture request must also open an existing
        // custom timeline so its payload can enter the normal composer.
        assertTrue(
            shouldExpandComposerOnEnter(
                requested = true,
                currentTimeline = custom,
                isAlreadyExpanded = false,
                isDestinationSettled = true,
                isTimelineEmpty = false,
            ),
        )
    }

    @Test
    fun allTimelineKeepsExplicitQuickCaptureBehavior() {
        assertFalse(
            shouldExpandComposerOnEnter(
                requested = false,
                currentTimeline = CurrentTimeline.All,
                isAlreadyExpanded = false,
                isDestinationSettled = true,
                isTimelineEmpty = true,
            ),
        )
        assertTrue(
            shouldExpandComposerOnEnter(
                requested = true,
                currentTimeline = CurrentTimeline.All,
                isAlreadyExpanded = false,
                isDestinationSettled = true,
                isTimelineEmpty = false,
            ),
        )
    }
}
