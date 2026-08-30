package com.vaibhav.relive.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/**
 * Read `UIAccessibility.isReduceMotionEnabled` at composition. The value is captured once per
 * composition; a mid-session Settings toggle will flip on the next recomposition (e.g. app
 * returning to foreground). A live notification observer can be layered on later without
 * changing this signature.
 */
@Composable
actual fun rememberReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
