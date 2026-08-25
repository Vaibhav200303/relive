package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.domain.model.StartDestination

fun resolveStartupDestination(
    preferred: StartDestination,
    authoritativeOverride: StartDestination? = null,
): StartDestination = authoritativeOverride ?: preferred
