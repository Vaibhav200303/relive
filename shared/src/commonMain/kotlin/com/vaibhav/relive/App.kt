package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.ui.screens.DesignSystemShowcase
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId

@Composable
@Preview
fun App() {
    ReliveTheme(themeId = ReliveThemeId.WarmJournal) {
        DesignSystemShowcase()
    }
}
