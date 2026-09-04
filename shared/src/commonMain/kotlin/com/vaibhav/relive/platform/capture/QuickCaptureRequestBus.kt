package com.vaibhav.relive.platform.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A process-lifetime bridge that lets a platform entry point outside the UI — an Android
 * notification tap or a home-screen widget — ask the running app to open the Home quick-capture
 * composer.
 *
 * It carries no payload, only a monotonically increasing request count: a second tap must never be
 * a silent no-op the way a latched boolean would be. This mirrors how `IncomingShareGateway`
 * bridges share intents into the shared UI, and the observing side reacts by running the exact same
 * inline-composer path the global `+ New` action uses.
 */
class QuickCaptureRequestBus {
    private val _requests = MutableStateFlow(0)
    val requests: StateFlow<Int> = _requests.asStateFlow()

    /** Signal that something outside the UI asked to add a new moment. */
    fun request() {
        _requests.value += 1
    }
}
