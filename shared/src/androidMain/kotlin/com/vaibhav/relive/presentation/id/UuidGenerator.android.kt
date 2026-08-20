package com.vaibhav.relive.presentation.id

import java.util.UUID

actual object UuidGenerator : com.vaibhav.relive.domain.id.IdGenerator {
    actual override fun newId(): String = UUID.randomUUID().toString()
}
