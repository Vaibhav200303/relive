package com.vaibhav.relive.presentation.navigation

import com.vaibhav.relive.presentation.timeline.CurrentTimeline

/** True only after the requested All destination has produced its normal layout. */
fun shouldExpandQuickCaptureComposer(
    requested: Boolean,
    currentTimeline: CurrentTimeline,
    isAlreadyExpanded: Boolean,
    isDestinationSettled: Boolean,
): Boolean = requested &&
    currentTimeline == CurrentTimeline.All &&
    !isAlreadyExpanded &&
    isDestinationSettled
