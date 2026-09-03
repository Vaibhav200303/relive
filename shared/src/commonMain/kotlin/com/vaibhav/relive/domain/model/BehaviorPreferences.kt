package com.vaibhav.relive.domain.model

/**
 * Behaviour settings that configure presentation rather than archive data.
 *
 * There is no start-destination preference: with the unified Home surface there is a single landing
 * root, so the app always opens on Home (ADR-0061).
 */
data class BehaviorPreferences(
    val confirmBeforeDiscarding: Boolean = true,
    val showLocations: Boolean = true,
    val showTags: Boolean = true,
    val showOnThisDay: Boolean = true,
    val showFavorites: Boolean = true,
)
