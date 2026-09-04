package com.vaibhav.relive

/** Explicit intent actions Relive components use to talk to [MainActivity]. */
object ReliveIntents {
    /**
     * A notification tap or the home-screen Quick Capture widget uses this action to open the app
     * straight into the Home quick-capture composer. Handled in `MainActivity` before the launch
     * intent is wiped; everything else is treated as a share intent.
     */
    const val ACTION_ADD_MOMENT = "com.vaibhav.relive.ADD_MOMENT"
}
