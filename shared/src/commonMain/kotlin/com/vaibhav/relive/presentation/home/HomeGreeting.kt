package com.vaibhav.relive.presentation.home

/**
 * The Home surface's greeting (ADR-0061).
 *
 * `Welcome back, {name}` only when a real profile display name exists; otherwise exactly
 * `Welcome back`, with no trailing punctuation and no placeholder. In particular the
 * `ProfileState` fallback label `Your Relive` is a Profile-screen affordance and must never
 * reach the greeting — "Welcome back, Your Relive" is not something a person would write.
 */
fun homeGreeting(displayName: String?): String {
    val name = displayName?.trim().orEmpty()
    return if (name.isEmpty() || name.equals(PROFILE_PLACEHOLDER_NAME, ignoreCase = true)) {
        "Welcome back"
    } else {
        "Welcome back, $name"
    }
}

/** Always shown beneath the greeting. */
const val HOME_GREETING_SUBTITLE: String = "Your memories are waiting for you."

/**
 * The Profile screen's stand-in label for an unset name. It is not a real name, so the greeting
 * treats it as absent.
 */
internal const val PROFILE_PLACEHOLDER_NAME: String = "Your Relive"
