package com.vaibhav.relive.domain.id

/**
 * Produces stable, non-blank identifiers for newly created domain aggregates. Kept
 * in the domain layer so shared logic (composer state holders, use cases) can
 * receive it as a dependency without importing platform APIs. Concrete
 * implementations live in the presentation/platform layer.
 */
fun interface IdGenerator {
    fun newId(): String
}
