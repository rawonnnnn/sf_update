package com.sofaflix.kmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun WatchPlayer(
    movie: Movie,
    episode: Episode,
    hasNext: Boolean,
    onPlayNextEpisode: () -> Unit,
    onUpdateProgress: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
)
