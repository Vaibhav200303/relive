package com.vaibhav.relive.ui.components.timeline

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.presentation.date.RediscoverCalendar
import com.vaibhav.relive.ui.theme.ReliveTheme

/** Material date picker for navigation; its selected date is never persisted or used as a filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateNavigationPicker(
    initialDate: LocalCalendarDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalCalendarDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = RediscoverCalendar.pickerMillis(initialDate))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
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
        DatePicker(state = state)
    }
}
