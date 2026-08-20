package com.vaibhav.relive.presentation.id

import platform.Foundation.NSUUID

actual object UuidGenerator : com.vaibhav.relive.domain.id.IdGenerator {
    actual override fun newId(): String = NSUUID().UUIDString()
}
