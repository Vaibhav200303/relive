package com.vaibhav.relive.presentation.home

/**
 * The Home surface's greeting (ADR-0061).
 *
 * `Hey, {first name}!!!` only when a real profile display name exists; otherwise exactly
 * `Hey!!!`, with no placeholder. Only the first word of the
 * display name is used — a greeting is addressed to a person, not to their full legal name.
 * The `ProfileState` fallback label `Your Relive` is a Profile-screen affordance and must never
 * reach the greeting — "Hey, Your Relive!!!" is not something a person would write.
 */
fun homeGreeting(displayName: String?): String {
    val name = displayName?.trim().orEmpty()
    if (name.isEmpty() || name.equals(PROFILE_PLACEHOLDER_NAME, ignoreCase = true)) {
        return "Hey!!!"
    }
    val firstName = name.split(' ', '\t', '\n').first { it.isNotBlank() }
    return "Hey, $firstName!!!"
}

/** Always shown beneath the greeting. */
const val HOME_GREETING_SUBTITLE: String = "Your memories are waiting for you."

/**
 * The Profile screen's stand-in label for an unset name. It is not a real name, so the greeting
 * treats it as absent.
 */
internal const val PROFILE_PLACEHOLDER_NAME: String = "Your Relive"
