package com.vaibhav.relive.platform.system

/** Coalesces launcher-icon requests while the foreground activity remains visible. */
internal class PendingLauncherIconUpdate {
    private val lock = Any()
    private var pending: LauncherIcon? = null

    fun request(icon: LauncherIcon) {
        synchronized(lock) {
            pending = icon
        }
    }

    fun take(): LauncherIcon? = synchronized(lock) {
        pending.also { pending = null }
    }

    fun retryUnlessSuperseded(icon: LauncherIcon) {
        synchronized(lock) {
            if (pending == null) pending = icon
        }
    }
}
