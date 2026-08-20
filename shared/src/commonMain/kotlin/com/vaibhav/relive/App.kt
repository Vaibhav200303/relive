package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.AllTimelineScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId

@Composable
@Preview
fun App(container: ReliveAppContainer) {
    ReliveTheme(themeId = ReliveThemeId.WarmJournal) {
        AllTimelineScreen(
            momentRepository = container.momentRepository,
            clock = container.clock,
            idGenerator = container.idGenerator,
        )
    }
}
