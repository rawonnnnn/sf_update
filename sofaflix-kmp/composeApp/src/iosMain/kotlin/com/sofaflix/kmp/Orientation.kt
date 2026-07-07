package com.sofaflix.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSNotificationCenter

@Composable
actual fun SetScreenOrientation(isLandscape: Boolean) {
    LaunchedEffect(isLandscape) {
        val name = if (isLandscape) "SofaFlixRequestLandscape" else "SofaFlixRequestPortrait"
        NSNotificationCenter.defaultCenter.postNotificationName(name, `object` = null)
    }

    DisposableEffect(Unit) {
        onDispose {
            NSNotificationCenter.defaultCenter.postNotificationName("SofaFlixRequestPortrait", `object` = null)
        }
    }
}
