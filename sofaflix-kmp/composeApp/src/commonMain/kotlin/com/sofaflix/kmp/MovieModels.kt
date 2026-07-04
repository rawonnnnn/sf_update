package com.sofaflix.kmp

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val slug: String,
    val name: String,
    val originName: String,
    val thumbUrl: String,
    val posterUrl: String,
    val episode: String,
    val quality: String,
    val lang: String,
    val year: String,
    val tmdbLogo: String = "",
    val progress: Double = 0.0,
    val type: String = ""
)

@Serializable
data class MovieDetail(
    val movie: Movie,
    val description: String,
    val servers: List<EpisodeServer>,
    val categories: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val time: String = ""
) {
    val firstEpisode: Episode?
        get() = servers.firstOrNull { it.episodes.isNotEmpty() }?.episodes?.firstOrNull()
}

@Serializable
data class EpisodeServer(
    val name: String,
    val episodes: List<Episode>
)

@Serializable
data class SubtitleTrack(
    val label: String,
    val url: String
)

@Serializable
data class Episode(
    val name: String,
    val embedUrl: String,
    val streamUrl: String,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val linkSub: String = ""
)

@Serializable
data class HomePayload(
    val hot: List<Movie>,
    val latest: List<Movie>,
    val singles: List<Movie>,
    val series: List<Movie>,
    val animation: List<Movie>
)
