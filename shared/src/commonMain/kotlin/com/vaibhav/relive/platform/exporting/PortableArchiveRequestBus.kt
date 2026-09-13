package com.vaibhav.relive.platform.exporting

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PortableArchiveRequest(val id: Long, val path: String)

/** Process bridge for platform document-open requests. App Lock remains the outer UI gate. */
class PortableArchiveRequestBus {
    private val _request = MutableStateFlow<PortableArchiveRequest?>(null)
    val request: StateFlow<PortableArchiveRequest?> = _request.asStateFlow()
    private var nextId = 0L

    fun open(path: String) {
        _request.value = PortableArchiveRequest(++nextId, path)
    }

    fun consume(id: Long) {
        if (_request.value?.id == id) _request.value = null
    }
}
