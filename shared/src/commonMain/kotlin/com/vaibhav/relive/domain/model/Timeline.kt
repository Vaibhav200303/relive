package com.vaibhav.relive.domain.model

/**
 * A timeline the user browses moments through.
 *
 * [All] is logical: it always contains every saved moment and is never persisted as a
 * regular [Custom] timeline nor stored as per-moment membership rows. [Custom] is a
 * user-created chapter that references moments via [CustomTimelineMembership].
 */
sealed interface Timeline {

    data object All : Timeline

    data class Custom(
        val id: TimelineId,
        val name: String,
        val theme: ThemeReference? = null,
    ) : Timeline {
        init {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Timeline name must not be blank" }
            require(trimmed.length <= MAX_NAME_LENGTH) { "Timeline name exceeds $MAX_NAME_LENGTH chars" }
        }

        companion object {
            const val MAX_NAME_LENGTH: Int = 60
        }
    }
}

/**
 * Many-to-many link between a moment and a custom timeline. Never used to link a
 * moment to [Timeline.All] — All membership is logical.
 */
data class CustomTimelineMembership(
    val timelineId: TimelineId,
    val momentId: MomentId,
)
