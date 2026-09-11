package com.vaibhav.relive.platform.system

class IosLauncherIconController(
    private val updateAlternateIcon: (String?) -> Unit,
) : LauncherIconController {
    override fun synchronize(icon: LauncherIcon) {
        updateAlternateIcon(icon.assetName)
    }
}

private val LauncherIcon.assetName: String?
    get() = when (this) {
        LauncherIcon.WarmJournal -> null
        LauncherIcon.Original -> "AppIconOriginal"
        LauncherIcon.Sunrise -> "AppIconSunrise"
        LauncherIcon.Sunset -> "AppIconSunset"
        LauncherIcon.Evergreen -> "AppIconEvergreen"
        LauncherIcon.EmberAqua -> "AppIconEmberAqua"
        LauncherIcon.PlumGold -> "AppIconPlumGold"
        LauncherIcon.RoseSage -> "AppIconRoseSage"
    }
