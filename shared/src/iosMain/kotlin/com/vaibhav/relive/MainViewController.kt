package com.vaibhav.relive

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import com.vaibhav.relive.di.createDefaultReliveAppContainer
import com.vaibhav.relive.ui.theme.IosStatusBarAppearance
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController

@Suppress("DEPRECATION")
private class ReliveComposeControllerDelegate : ComposeUIViewControllerDelegate {
    var darkIcons: Boolean = true

    override val preferredStatusBarStyle: UIStatusBarStyle
        get() = if (darkIcons) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
}

@Suppress("DEPRECATION")
fun MainViewController(): UIViewController {
    val statusDelegate = ReliveComposeControllerDelegate()
    val controller = ComposeUIViewController(
        configure = { delegate = statusDelegate },
    ) {
        val container = createDefaultReliveAppContainer()
        App(container)
    }
    IosStatusBarAppearance.update = { darkIcons ->
        if (statusDelegate.darkIcons != darkIcons) {
            statusDelegate.darkIcons = darkIcons
            controller.setNeedsStatusBarAppearanceUpdate()
        }
    }
    return controller
}
