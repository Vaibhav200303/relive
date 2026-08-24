package com.vaibhav.relive.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposerDiscardConfirmationTest {
    @Test
    fun emptyComposerCloseResetsWithoutShowingDiscardConfirmation() {
        val transition = ComposerDiscardConfirmationState().onCloseRequested(hasUserDraft = false)

        assertEquals(ComposerCloseAction.ResetAndCollapse, transition.action)
        assertFalse(transition.state.isVisible)
    }

    @Test
    fun dirtyComposerCloseShowsDiscardConfirmation() {
        val transition = ComposerDiscardConfirmationState().onCloseRequested(hasUserDraft = true)

        assertEquals(ComposerCloseAction.ShowDiscardConfirmation, transition.action)
        assertTrue(transition.state.isVisible)
    }

    @Test
    fun cancellingDiscardClosesConfirmationWithoutRequestingReset() {
        val shown = ComposerDiscardConfirmationState().onCloseRequested(hasUserDraft = true).state

        val afterCancel = shown.onCancelled()

        assertFalse(afterCancel.isVisible)
    }

    @Test
    fun discardingClosesConfirmationAndRequestsResetAndCollapse() {
        val shown = ComposerDiscardConfirmationState().onCloseRequested(hasUserDraft = true).state

        val transition = shown.onDiscarded()

        assertEquals(ComposerCloseAction.ResetAndCollapse, transition.action)
        assertFalse(transition.state.isVisible)
    }

    @Test
    fun consumedConfirmationDoesNotReappearForTheNowEmptyComposer() {
        val shown = ComposerDiscardConfirmationState().onCloseRequested(hasUserDraft = true).state
        val consumed = shown.onDiscarded().state

        val nextClose = consumed.onCloseRequested(hasUserDraft = false)

        assertEquals(ComposerCloseAction.ResetAndCollapse, nextClose.action)
        assertFalse(nextClose.state.isVisible)
    }
}
