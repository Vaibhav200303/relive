package com.vaibhav.relive.platform.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingLauncherIconUpdateTest {
    @Test
    fun requestsCoalesceToTheLatestIcon() {
        val update = PendingLauncherIconUpdate()

        update.request(LauncherIcon.WarmJournal)
        update.request(LauncherIcon.Sunrise)
        update.request(LauncherIcon.Sunset)

        assertEquals(LauncherIcon.Sunset, update.take())
        assertNull(update.take())
    }

    @Test
    fun retryDoesNotOverwriteANewerRequest() {
        val update = PendingLauncherIconUpdate()
        update.request(LauncherIcon.WarmJournal)
        val failed = update.take()
        update.request(LauncherIcon.Evergreen)

        update.retryUnlessSuperseded(failed!!)

        assertEquals(LauncherIcon.Evergreen, update.take())
    }

    @Test
    fun retryRestoresARequestWhenNothingSupersededIt() {
        val update = PendingLauncherIconUpdate()
        update.request(LauncherIcon.PlumGold)
        val failed = update.take()

        update.retryUnlessSuperseded(failed!!)

        assertEquals(LauncherIcon.PlumGold, update.take())
    }
}
