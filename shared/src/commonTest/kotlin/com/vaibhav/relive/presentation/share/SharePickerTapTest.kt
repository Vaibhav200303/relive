package com.vaibhav.relive.presentation.share

import com.vaibhav.relive.platform.share.IncomingSharePayload
import com.vaibhav.relive.platform.share.IncomingShareState
import kotlin.test.Test
import kotlin.test.assertEquals

class SharePickerTapTest {
    @Test
    fun aReadyPayloadCommitsImmediately() {
        val payload = IncomingSharePayload(requestId = "req-1", text = "a note")

        assertEquals(
            SharePickerTapOutcome.Commit(payload),
            resolveSharePickerTap(IncomingShareState.Ready(payload)),
        )
    }

    @Test
    fun aTapWhileStillReadingIsHeldRatherThanLost() {
        assertEquals(SharePickerTapOutcome.Hold, resolveSharePickerTap(IncomingShareState.Reading))
    }

    @Test
    fun aRejectedRequestDropsTheChoice() {
        assertEquals(
            SharePickerTapOutcome.Drop,
            resolveSharePickerTap(IncomingShareState.Error("Couldn't read what you shared")),
        )
    }

    @Test
    fun aCancelledOrClaimedRequestDropsTheChoice() {
        assertEquals(SharePickerTapOutcome.Drop, resolveSharePickerTap(IncomingShareState.Idle))
    }
}
