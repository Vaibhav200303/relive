package com.vaibhav.relive.presentation.composer

import com.vaibhav.relive.domain.id.IdGenerator
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentValidation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.repository.MomentRepository
import com.vaibhav.relive.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Inline composer state holder.
 *
 * Coordinates user input (title, content, tags), validates on demand via
 * [MomentValidation], stamps a new [Moment] with an injected [IdGenerator] +
 * [Clock] (so the domain remains platform-free and tests are deterministic), and
 * persists it through [MomentRepository.insert] with no timeline memberships —
 * `All` is logical and never a stored membership row (ADR-0004).
 *
 * On successful save the composer state resets fully so the timeline can
 * immediately host the next entry. On validation or persistence failure the
 * user's input is preserved so nothing is lost.
 */
class MomentComposerViewModel(
    private val momentRepository: MomentRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(MomentComposerState())
    val state: StateFlow<MomentComposerState> = _state.asStateFlow()

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value, saveState = it.saveState.clearedOnEdit()) }
    }

    fun updateContent(value: String) {
        _state.update { it.copy(content = value, saveState = it.saveState.clearedOnEdit()) }
    }

    fun updatePendingTagInput(value: String) {
        _state.update { it.copy(pendingTagInput = value) }
    }

    /**
     * Attempts to add the pending tag input as a real tag. Uses [Tag.ofOrNull]
     * so blank/oversize input is ignored silently. Canonicalization &
     * deduplication both live in the domain — the composer never re-implements
     * them.
     */
    fun commitPendingTag() {
        _state.update { current ->
            val candidate = Tag.ofOrNull(current.pendingTagInput) ?: return@update current.copy(
                pendingTagInput = "",
            )
            val tags = if (current.tags.contains(candidate)) current.tags else current.tags + candidate
            current.copy(tags = tags, pendingTagInput = "")
        }
    }

    /** Adds [raw] as a tag directly, e.g. from a suggestion tap. */
    fun addTag(raw: String) {
        val candidate = Tag.ofOrNull(raw) ?: return
        _state.update { current ->
            if (current.tags.contains(candidate)) current else current.copy(tags = current.tags + candidate)
        }
    }

    fun removeTag(tag: Tag) {
        _state.update { it.copy(tags = it.tags.filterNot { existing -> existing == tag }) }
    }

    fun toggleAddMedia() {
        _state.update { it.copy(addMediaExpanded = !it.addMediaExpanded) }
    }

    /**
     * Resets composer state to fresh. Safe to call on an already-empty composer.
     * Does not touch persisted moments.
     */
    fun reset() {
        _state.value = MomentComposerState()
    }

    /**
     * Explicit save. Refuses to submit while a previous save is in flight so a
     * double tap cannot create duplicate moments.
     */
    fun keepMoment() {
        val snapshot = _state.value
        if (snapshot.isSaving) return

        val now = clock.now()
        val moment = Moment(
            id = MomentId(idGenerator.newId()),
            createdAt = now,
            title = snapshot.title,
            content = snapshot.content,
            location = snapshot.location,
            tags = snapshot.tags,
        )

        when (val result = MomentValidation.validate(moment)) {
            is MomentValidation.Result.Invalid -> {
                _state.update { it.copy(saveState = SaveState.Invalid(result.reasons)) }
                return
            }
            MomentValidation.Result.Ok -> Unit
        }

        _state.update { it.copy(saveState = SaveState.Saving) }

        scope.launch {
            try {
                momentRepository.insert(moment, emptySet())
                _state.value = MomentComposerState()
            } catch (t: Throwable) {
                _state.update { it.copy(saveState = SaveState.Failure(t)) }
            }
        }
    }
}

/**
 * Editing after a failed save clears the failure indicator so the composer
 * stops showing a stale error while the user is correcting things. A live save
 * (Saving) is never cleared this way.
 */
private fun SaveState.clearedOnEdit(): SaveState = when (this) {
    is SaveState.Invalid, is SaveState.Failure -> SaveState.Idle
    SaveState.Idle, SaveState.Saving -> this
}
