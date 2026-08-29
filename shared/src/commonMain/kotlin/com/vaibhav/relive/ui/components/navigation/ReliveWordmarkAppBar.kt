package com.vaibhav.relive.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.vaibhav.relive.ui.theme.ReliveTheme

/**
 * Shared top-level identity bar for Relive roots.
 *
 * Defaults to the "Relive" wordmark used on Timeline Home. A root that is not the app's
 * primary identity (e.g. Rediscover) passes its own [title] and a smaller brand [style]
 * token so its masthead reads as a section heading beneath the home wordmark, rather than
 * repeating the full-size wordmark. [style] defaults to `type.wordmark`; pass `type.title`
 * for the smaller serif treatment. No raw font sizes are used at the call site.
 */
@Composable
fun ReliveWordmarkAppBar(
    modifier: Modifier = Modifier,
    title: String = "Relive",
    style: TextStyle? = null,
    action: @Composable BoxScope.() -> Unit = {},
) {
    val colors = ReliveTheme.colors
    val dims = ReliveTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = dims.spacing.md, vertical = dims.spacing.sm),
    ) {
        Text(
            text = title,
            style = style ?: ReliveTheme.typography.wordmark,
            color = colors.accent,
            modifier = Modifier.align(Alignment.Center),
        )
        action()
    }
}
