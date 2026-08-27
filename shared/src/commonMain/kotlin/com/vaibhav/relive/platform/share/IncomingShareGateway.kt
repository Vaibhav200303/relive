package com.vaibhav.relive.platform.share

import com.vaibhav.relive.platform.media.RawMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A platform-owned external share request, before it becomes a composer draft. */
data class IncomingSharePayload(
    val requestId: String,
    val subject: String? = null,
    val text: String? = null,
    val media: List<RawMedia> = emptyList(),
)

sealed interface IncomingShareState {
    data object Idle : IncomingShareState
    data object Reading : IncomingShareState
    data class Ready(val payload: IncomingSharePayload) : IncomingShareState
    data class Error(val message: String) : IncomingShareState
}

/**
 * The shared UI only observes an already-normalized request. Platform code owns
 * source handles and temporary files until [claim] transfers them to the composer.
 */
interface IncomingShareGateway {
    val state: StateFlow<IncomingShareState>

    fun retry()
    fun cancel()
    fun claim(requestId: String)
}

object UnavailableIncomingShareGateway : IncomingShareGateway {
    private val idle = MutableStateFlow<IncomingShareState>(IncomingShareState.Idle)
    override val state: StateFlow<IncomingShareState> = idle.asStateFlow()
    override fun retry() = Unit
    override fun cancel() = Unit
    override fun claim(requestId: String) = Unit
}
