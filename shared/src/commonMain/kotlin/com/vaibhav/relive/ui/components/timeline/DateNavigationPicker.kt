package com.vaibhav.relive.ui.components.timeline

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics

/** Material date picker for navigation; its selected date is never persisted or used as a filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateNavigationPicker(
    initialDate: LocalCalendarDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalCalendarDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = RediscoverCalendar.pickerMillis(initialDate))
    val colors = ReliveTheme.colors
    val haptics = rememberReliveHaptics()
    val pickerColors = DatePickerDefaults.colors(
        containerColor = colors.surfaceOverlay,
        titleContentColor = colors.textSecondary,
        headlineContentColor = colors.textPrimary,
        weekdayContentColor = colors.textMuted,
        subheadContentColor = colors.textPrimary,
        navigationContentColor = colors.textPrimary,
        yearContentColor = colors.textPrimary,
        currentYearContentColor = colors.accent,
        selectedYearContentColor = colors.textOnAccent,
        selectedYearContainerColor = colors.accent,
        dayContentColor = colors.textPrimary,
        selectedDayContentColor = colors.textOnAccent,
        selectedDayContainerColor = colors.accent,
        todayContentColor = colors.accent,
        todayDateBorderColor = colors.accent,
        dividerColor = colors.borderMuted,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ReliveTheme.dimensions.radii.dialog),
        colors = pickerColors,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        haptics.perform(ReliveHapticCue.Selection)
                        onDateSelected(RediscoverCalendar.dateFromPickerMillis(it))
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text("Jump", color = ReliveTheme.colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ReliveTheme.colors.accent) }
        },
    ) {
        DatePicker(state = state, colors = pickerColors)
    }
}
