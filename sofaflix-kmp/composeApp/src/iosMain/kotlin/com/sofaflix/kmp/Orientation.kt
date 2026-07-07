package com.sofaflix.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

var iosOrientationHandler: ((Boolean) -> Unit)? = null

fun registerOrientationHandler(handler: (Boolean) -> Unit) {
    iosOrientationHandler = handler
}

@Composable
actual fun SetScreenOrientation(isLandscape: Boolean) {
    LaunchedEffect(isLandscape) {
        iosOrientationHandler?.invoke(isLandscape)
    }

    DisposableEffect(Unit) {
        onDispose {
            iosOrientationHandler?.invoke(false)
        }
    }
}
