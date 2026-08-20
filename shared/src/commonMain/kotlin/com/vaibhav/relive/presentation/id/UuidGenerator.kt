package com.vaibhav.relive.presentation.id

import com.vaibhav.relive.domain.id.IdGenerator

/**
 * Platform-backed [IdGenerator] producing a canonical string UUID. Actuals use the
 * host platform's UUID facility (java.util.UUID on Android/JVM, NSUUID on iOS) so
 * no shared-module dependency is added just for identifier generation.
 */
expect object UuidGenerator : IdGenerator {
    override fun newId(): String
}
