package com.vaibhav.relive.platform.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class AndroidLauncherIconController(context: Context) : LauncherIconController {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    override fun synchronize(icon: LauncherIcon) {
        val desired = component(icon)
        val aliasesAlreadyMatch = LauncherIcon.entries.all { candidate ->
            isEffectivelyEnabled(component(candidate), candidate == LauncherIcon.WarmJournal) ==
                (candidate == icon)
        }
        if (aliasesAlreadyMatch) return

        packageManager.setComponentEnabledSetting(
            desired,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        LauncherIcon.entries
            .filterNot { it == icon }
            .forEach { candidate ->
                packageManager.setComponentEnabledSetting(
                    component(candidate),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
    }

    private fun isEffectivelyEnabled(component: ComponentName, enabledByDefault: Boolean): Boolean =
        when (packageManager.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> enabledByDefault
            else -> false
        }

    private fun component(icon: LauncherIcon): ComponentName = ComponentName(
        appContext,
        "${appContext.packageName}.${icon.aliasClassName}",
    )
}

private val LauncherIcon.aliasClassName: String
    get() = when (this) {
        LauncherIcon.WarmJournal -> "LauncherWarmJournal"
        LauncherIcon.Original -> "LauncherOriginal"
        LauncherIcon.Sunrise -> "LauncherSunrise"
        LauncherIcon.Sunset -> "LauncherSunset"
        LauncherIcon.Evergreen -> "LauncherEvergreen"
        LauncherIcon.EmberAqua -> "LauncherEmberAqua"
        LauncherIcon.PlumGold -> "LauncherPlumGold"
        LauncherIcon.RoseSage -> "LauncherRoseSage"
    }
