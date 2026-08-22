package com.vaibhav.relive

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vaibhav.relive.di.ReliveAppContainer
import com.vaibhav.relive.ui.screens.TimelineScreen
import com.vaibhav.relive.ui.theme.ReliveTheme
import com.vaibhav.relive.ui.theme.ReliveThemeId

@Composable
@Preview
fun App(container: ReliveAppContainer) {
    ReliveTheme(themeId = ReliveThemeId.WarmJournal) {
        TimelineScreen(
            momentRepository = container.momentRepository,
            timelineRepository = container.timelineRepository,
            clock = container.clock,
            idGenerator = container.idGenerator,
            mediaStore = container.mediaStore,
            mediaProcessor = container.mediaProcessor,
        )
    }
}
