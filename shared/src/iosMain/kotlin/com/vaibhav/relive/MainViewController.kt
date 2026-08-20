package com.vaibhav.relive

import androidx.compose.ui.window.ComposeUIViewController
import com.vaibhav.relive.di.createDefaultReliveAppContainer

fun MainViewController() = ComposeUIViewController {
    val container = createDefaultReliveAppContainer()
    App(container)
}
