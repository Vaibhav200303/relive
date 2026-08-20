package com.vaibhav.relive.domain.model

/**
 * Semantic-level validation applied on top of [Moment]'s structural invariants.
 * The constructor of [Moment] already enforces structural rules (unique attachment
 * ids/sort indices, deduped tags, non-negative updatedAt). This layer answers a
 * separate question: "does this content deserve to be saved as a moment at all?"
 *
 * A moment is invalid when it carries no title, no content, and no media — such a
 * moment would render as an empty rail dot with nothing to relive. See ADR-0012.
 */
object MomentValidation {

    sealed interface Result {
        data object Ok : Result
        data class Invalid(val reasons: List<Reason>) : Result
    }

    enum class Reason { Empty }

    fun validate(moment: Moment): Result {
        val reasons = buildList {
            if (moment.title.isBlank() &&
                moment.content.isBlank() &&
                moment.attachments.isEmpty()
            ) {
                add(Reason.Empty)
            }
        }
        return if (reasons.isEmpty()) Result.Ok else Result.Invalid(reasons)
    }
}
