@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.sofaflix.kmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight

class MainComposeViewController(
    private val composeController: UIViewController
) : UIViewController(nibName = null, bundle = null) {

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(composeController)
        view.addSubview(composeController.view)
        composeController.view.setFrame(view.bounds)
        composeController.didMoveToParentViewController(this)
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        composeController.view.setFrame(view.bounds)
    }

    override fun prefersStatusBarHidden(): Boolean {
        val app = UIApplication.sharedApplication
        val orientation = app.statusBarOrientation
        return orientation == UIInterfaceOrientationLandscapeLeft || orientation == UIInterfaceOrientationLandscapeRight
    }

    override val prefersHomeIndicatorAutoHidden: Boolean
        get() {
            val app = UIApplication.sharedApplication
            val orientation = app.statusBarOrientation
            return orientation == UIInterfaceOrientationLandscapeLeft || orientation == UIInterfaceOrientationLandscapeRight
        }
}

fun MainViewController(): UIViewController {
    val composeVC = ComposeUIViewController {
        App()
    }
    return MainComposeViewController(composeVC)
}
