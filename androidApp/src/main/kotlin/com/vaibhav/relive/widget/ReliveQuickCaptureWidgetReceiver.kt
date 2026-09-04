package com.vaibhav.relive.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Registers [ReliveQuickCaptureWidget] with the home-screen host. */
class ReliveQuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReliveQuickCaptureWidget()
}
