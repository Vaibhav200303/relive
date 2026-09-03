package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Instant

/**
 * The optional feeling a person attaches to a Moment through the post-save prompt
 * (PRODUCT_SPEC §10A, ADR-0064). Absent is a first-class state forever — a Moment
 * with no feeling is a normal Moment, never an incomplete one.
 *
 * [score] is the ordinal weight Mood insights average over: verdicts and curve
 * points are means of these scores, so the three values must stay evenly spaced.
 */
enum class MomentFeeling(val score: Int) {
    Great(3),
    Good(2),
    Low(1),
}

/**
 * One Moment reduced to the two scalars Mood insights aggregate over. Read through
 * a bounded projection so the insights surface never hydrates the archive.
 */
data class MomentFeelingSample(
    val createdAt: Instant,
    val feeling: MomentFeeling?,
)
