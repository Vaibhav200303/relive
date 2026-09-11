package com.vaibhav.relive.platform.system

import com.vaibhav.relive.domain.model.ThemeReference

/** The launcher artwork paired with one global app palette. */
enum class LauncherIcon {
    WarmJournal,
    Original,
    Sunrise,
    Sunset,
    Evergreen,
    EmberAqua,
    PlumGold,
    RoseSage,
}

fun ThemeReference.toLauncherIcon(): LauncherIcon = when (this) {
    ThemeReference.WarmJournal -> LauncherIcon.WarmJournal
    ThemeReference.InkLilac -> LauncherIcon.Original
    ThemeReference.Sunrise -> LauncherIcon.Sunrise
    ThemeReference.Sunset -> LauncherIcon.Sunset
    ThemeReference.TealSaffron -> LauncherIcon.Evergreen
    ThemeReference.EmberAqua -> LauncherIcon.EmberAqua
    ThemeReference.PlumGold -> LauncherIcon.PlumGold
    ThemeReference.RoseSage -> LauncherIcon.RoseSage
}

/** Platform edge for keeping the launcher icon paired with the selected global palette. */
fun interface LauncherIconController {
    fun synchronize(icon: LauncherIcon)
}

object UnavailableLauncherIconController : LauncherIconController {
    override fun synchronize(icon: LauncherIcon) = Unit
}
