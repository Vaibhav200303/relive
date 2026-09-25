package com.vaibhav.relive.platform.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class AndroidLauncherIconController(context: Context) : LauncherIconController {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val pendingUpdate = PendingLauncherIconUpdate()
    private val manifestPackage = requireNotNull(appContext.applicationInfo.className)
        .substringBeforeLast('.')

    override fun synchronize(icon: LauncherIcon) {
        // Never mutate launcher components while the activity is visible. Enabling the selected
        // alias first exposes two Relive icons, while disabling the current alias can remove the
        // foreground task despite DONT_KILL_APP. onStop() applies the complete swap atomically on
        // Android 13+, at the first lifecycle point where the launcher can actually be visible.
        pendingUpdate.request(icon)
    }

    override fun applyPending() {
        val icon = pendingUpdate.take() ?: return
        runCatching { apply(icon) }
            .onFailure {
                // A transient PackageManager/launcher failure must never take Relive down. Keep
                // the request so the next lifecycle boundary can retry it, unless a newer palette
                // request arrived while PackageManager was working.
                pendingUpdate.retryUnlessSuperseded(icon)
                Log.w(TAG, "Unable to synchronize launcher icon", it)
            }
    }

    private fun apply(icon: LauncherIcon) {
        val aliasesAlreadyMatch = LauncherIcon.entries.all { candidate ->
            isEffectivelyEnabled(component(candidate), candidate == LauncherIcon.IvoryGold) ==
                (candidate == icon)
        }
        if (aliasesAlreadyMatch) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.setComponentEnabledSettings(
                LauncherIcon.entries.map { candidate ->
                    PackageManager.ComponentEnabledSetting(
                        component(candidate),
                        if (candidate == icon) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        PackageManager.DONT_KILL_APP,
                    )
                },
            )
        } else {
            // Older Android releases have no atomic API. Enabling the desired alias first keeps
            // there from being a moment with no launcher entry while the old aliases are retired.
            packageManager.setComponentEnabledSetting(
                component(icon),
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
    }

    private fun isEffectivelyEnabled(component: ComponentName, enabledByDefault: Boolean): Boolean =
        when (packageManager.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> enabledByDefault
            else -> false
        }

    private fun component(icon: LauncherIcon): ComponentName = ComponentName(
        appContext,
        launcherComponentClassName(manifestPackage, icon),
    )
}

internal fun launcherComponentClassName(manifestPackage: String, icon: LauncherIcon): String =
    "$manifestPackage.${icon.aliasClassName}"

private const val TAG = "ReliveLauncherIcon"

private val LauncherIcon.aliasClassName: String
    get() = when (this) {
        LauncherIcon.WarmJournal -> "LauncherWarmJournal"
        LauncherIcon.Original -> "LauncherOriginal"
        LauncherIcon.IvoryGold -> "LauncherIvoryGold"
        LauncherIcon.VelvetRose -> "LauncherVelvetRose"
        LauncherIcon.Evergreen -> "LauncherEvergreen"
        LauncherIcon.EmberAqua -> "LauncherEmberAqua"
        LauncherIcon.PlumGold -> "LauncherPlumGold"
        LauncherIcon.RoseSage -> "LauncherRoseSage"
    }
