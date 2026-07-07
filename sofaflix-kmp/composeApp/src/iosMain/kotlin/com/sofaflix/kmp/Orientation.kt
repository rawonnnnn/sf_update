package com.sofaflix.kmp

import androidx.compose.runtime.Composable

@Composable
actual fun SetScreenOrientation(isLandscape: Boolean) {
    // On iOS, orientation is managed natively by the Swift side
    // (ContentView.swift uses GeometryReader + FullscreenViewController)
    // so this is intentionally a no-op.
}
