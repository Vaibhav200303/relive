package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag

/**
 * Immutable snapshot of the inline composer. Preserves user input verbatim; the
 * only trimming ever performed on title/content is inside the composer's own
 * validation check — the persisted values are the exact text the user entered.
 *
 * [location] is retained through Phase 3 as composer state only. Phase 3 does
 * not acquire GPS or expose a manual location field; the property is here so a
 * later phase can populate it without changing the composer's shape.
 */
data class MomentComposerState(
    val title: String = "",
    val content: String = "",
    val tags: List<Tag> = emptyList(),
    val pendingTagInput: String = "",
    val location: ReliveLocation? = null,
    val addMediaExpanded: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
) {
    val isSaving: Boolean get() = saveState is SaveState.Saving
}

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState

    /** Domain rejected the moment (see [MomentValidation]). */
    data class Invalid(val reasons: List<MomentValidation.Reason>) : SaveState

    /** Persistence threw while attempting to write the moment. */
    data class Failure(val cause: Throwable) : SaveState
}
