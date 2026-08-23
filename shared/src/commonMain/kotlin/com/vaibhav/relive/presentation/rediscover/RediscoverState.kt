package com.vaibhav.relive.presentation.rediscover

import com.vaibhav.relive.domain.model.RediscoverOverview
import com.vaibhav.relive.presentation.viewer.MediaViewerState

sealed interface RediscoverContent {
    data object Loading : RediscoverContent
    data object EmptyArchive : RediscoverContent
    data class Loaded(val overview: RediscoverOverview) : RediscoverContent
    data object Failed : RediscoverContent
}

data class RediscoverState(
    val content: RediscoverContent = RediscoverContent.Loading,
    val mediaViewer: MediaViewerState? = null,
)
