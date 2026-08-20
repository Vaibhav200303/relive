package com.vaibhav.relive.domain.model

import kotlin.jvm.JvmInline

/**
 * User-visible label attached to a moment. Stored verbatim as entered.
 *
 * Normalization: a canonical form (lowercased, whitespace collapsed) exists only for
 * equality/deduplication in queries. The original [label] is preserved for display.
 */
class Tag private constructor(val label: String) {

    val canonical: String = canonicalize(label)

    override fun equals(other: Any?): Boolean = other is Tag && other.canonical == canonical
    override fun hashCode(): Int = canonical.hashCode()
    override fun toString(): String = label

    companion object {
        const val MAX_LENGTH: Int = 64

        fun of(raw: String): Tag {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "Tag must not be blank" }
            require(trimmed.length <= MAX_LENGTH) { "Tag exceeds $MAX_LENGTH chars" }
            return Tag(trimmed)
        }

        fun ofOrNull(raw: String): Tag? = runCatching { of(raw) }.getOrNull()

        private fun canonicalize(label: String): String =
            label.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
