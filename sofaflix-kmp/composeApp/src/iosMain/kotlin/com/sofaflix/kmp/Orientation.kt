package com.sofaflix.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIDevice
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.Foundation.NSNumber
import platform.Foundation.numberWithInteger
import platform.Foundation.setValue

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun SetScreenOrientation(isLandscape: Boolean) {
    LaunchedEffect(isLandscape) {
        val app = UIApplication.sharedApplication
        val windowScene = app.connectedScenes.firstOrNull() as? UIWindowScene
        if (windowScene != null) {
            val mask = if (isLandscape) {
                UIInterfaceOrientationMaskLandscapeRight
            } else {
                UIInterfaceOrientationMaskPortrait
            }
            val preferences = UIWindowSceneGeometryPreferencesIOS(mask)
            windowScene.requestGeometryUpdateWithPreferences(preferences) { error ->
                // ignore
            }
        } else {
            val value = if (isLandscape) {
                UIInterfaceOrientationLandscapeRight
            } else {
                UIInterfaceOrientationPortrait
            }
            UIDevice.currentDevice.setValue(
                value = NSNumber.numberWithInteger(value.toLong()),
                forKey = "orientation"
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val app = UIApplication.sharedApplication
            val windowScene = app.connectedScenes.firstOrNull() as? UIWindowScene
            if (windowScene != null) {
                val preferences = UIWindowSceneGeometryPreferencesIOS(UIInterfaceOrientationMaskPortrait)
                windowScene.requestGeometryUpdateWithPreferences(preferences) { error ->
                    // ignore
                }
            } else {
                UIDevice.currentDevice.setValue(
                    value = NSNumber.numberWithInteger(UIInterfaceOrientationPortrait.toLong()),
                    forKey = "orientation"
                )
            }
        }
    }
}
