package com.vaibhav.relive.domain.model

enum class LockAfter(val label: String, val timeoutMillis: Long) {
    Immediately("Immediately", 0),
    OneMinute("After 1 minute", 60_000),
    FiveMinutes("After 5 minutes", 300_000),
}

data class ProfileSettings(
    val displayName: String? = null,
    val profilePhoto: MediaStorageRef? = null,
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val lockAfter: LockAfter = LockAfter.Immediately,
    val rediscoverRemindersEnabled: Boolean = false,
)
