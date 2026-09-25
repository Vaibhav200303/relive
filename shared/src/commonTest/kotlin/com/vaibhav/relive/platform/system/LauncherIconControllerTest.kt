package com.vaibhav.relive.platform.system

import com.vaibhav.relive.domain.model.ThemeReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherIconControllerTest {
    @Test
    fun every_palette_maps_to_its_stable_launcher_icon() {
        assertEquals(LauncherIcon.WarmJournal, ThemeReference.WarmJournal.toLauncherIcon())
        assertEquals(LauncherIcon.Original, ThemeReference.InkLilac.toLauncherIcon())
        assertEquals(LauncherIcon.IvoryGold, ThemeReference.IvoryGold.toLauncherIcon())
        assertEquals(LauncherIcon.VelvetRose, ThemeReference.VelvetRose.toLauncherIcon())
        assertEquals(LauncherIcon.Evergreen, ThemeReference.TealSaffron.toLauncherIcon())
        assertEquals(LauncherIcon.EmberAqua, ThemeReference.EmberAqua.toLauncherIcon())
        assertEquals(LauncherIcon.PlumGold, ThemeReference.PlumGold.toLauncherIcon())
        assertEquals(LauncherIcon.RoseSage, ThemeReference.RoseSage.toLauncherIcon())
    }

    @Test
    fun unavailable_controller_supports_safe_lifecycle_flush() {
        // Android defers alias mutation until Activity.onStop; the common no-op implementation
        // keeps that lifecycle hook available on platforms without alternate icon support.
        assertTrue(runCatching { UnavailableLauncherIconController.applyPending() }.isSuccess)
    }
}
