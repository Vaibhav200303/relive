package com.vaibhav.relive.presentation.viewer

data class MediaSelectionState(val selectedIndices: Set<Int> = emptySet()) {
    val isActive: Boolean get() = selectedIndices.isNotEmpty()
    val count: Int get() = selectedIndices.size

    fun toggle(index: Int): MediaSelectionState = copy(
        selectedIndices = if (index in selectedIndices) selectedIndices - index else selectedIndices + index,
    )

    fun selectAll(size: Int): MediaSelectionState = copy(selectedIndices = (0 until size).toSet())
    fun clear(): MediaSelectionState = MediaSelectionState()
}
