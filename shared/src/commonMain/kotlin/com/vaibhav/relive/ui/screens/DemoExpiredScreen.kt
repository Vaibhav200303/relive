package com.vaibhav.relive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vaibhav.relive.ui.components.ReliveDoodles
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.canvasBrush

/** Final, non-interactive surface for the time-limited public evaluation build. */
@Composable
fun DemoExpiredScreen() {
    val colors = ReliveTheme.colors
    val type = ReliveTheme.typography
    val dimensions = ReliveTheme.dimensions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasBrush())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = dimensions.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Relive",
                style = type.wordmark,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(dimensions.spacing.xl))
            ReliveDoodles.KeptSafe()
            Spacer(Modifier.height(dimensions.spacing.xl))
            Text(
                text = "Demo expired",
                style = type.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensions.spacing.sm))
            Text(
                text = "Thank you for exploring Relive. This time-limited judging build is no longer available.",
                style = type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
