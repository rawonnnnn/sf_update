package com.sofaflix.kmp

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

object StorageHelpers {
    fun getLocalFavorites(): List<Movie> {
        val raw = AppPreferences.getString("sf:favorites", "")
        if (raw.isBlank()) return emptyList()
        return try {
            Json.decodeFromString(ListSerializer(Movie.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalFavorites(list: List<Movie>) {
        val jsonStr = Json.encodeToString(ListSerializer(Movie.serializer()), list)
        AppPreferences.putString("sf:favorites", jsonStr)
    }

    fun isMovieFavorite(slug: String): Boolean {
        return getLocalFavorites().any { it.slug == slug }
    }

    fun toggleFavorite(movie: Movie): Boolean {
        val list = getLocalFavorites().toMutableList()
        val index = list.indexOfFirst { it.slug == movie.slug }
        val isAdded = if (index >= 0) {
            list.removeAt(index)
            false
        } else {
            list.add(0, movie)
            true
        }
        saveLocalFavorites(list)
        return isAdded
    }

    fun getWatchHistory(): List<Movie> {
        val raw = AppPreferences.getString("sf:watch_history", "")
        if (raw.isBlank()) return emptyList()
        return try {
            Json.decodeFromString(ListSerializer(Movie.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWatchHistory(list: List<Movie>) {
        val jsonStr = Json.encodeToString(ListSerializer(Movie.serializer()), list)
        AppPreferences.putString("sf:watch_history", jsonStr)
    }

    fun addToHistory(movie: Movie, episode: String) {
        val list = getWatchHistory().toMutableList()
        list.removeAll { it.slug == movie.slug }
        val updatedMovie = movie.copy(episode = episode)
        list.add(0, updatedMovie)
        if (list.size > 50) {
            list.removeAt(list.lastIndex)
        }
        saveWatchHistory(list)
    }

    fun removeHistory(slug: String) {
        val list = getWatchHistory().toMutableList()
        list.removeAll { it.slug == slug }
        saveWatchHistory(list)
    }

    fun getEpisodeProgress(slug: String, epName: String): Float {
        return AppPreferences.getString("sf:progress:${slug}:${epName}", "0.0").toFloatOrNull() ?: 0f
    }

    fun saveEpisodeProgress(slug: String, epName: String, progress: Float) {
        AppPreferences.putString("sf:progress:${slug}:${epName}", progress.toString())
    }

    fun updateHistoryProgress(slug: String, progress: Double) {
        val list = getWatchHistory().toMutableList()
        val index = list.indexOfFirst { it.slug == slug }
        if (index >= 0) {
            val updated = list[index].copy(progress = progress)
            list[index] = updated
            saveWatchHistory(list)
        }
    }

    fun getWatchedEpisodes(slug: String): Set<String> {
        val raw = AppPreferences.getString("sf:watched_episodes:$slug", "[]")
        if (raw.isBlank()) return emptySet()
        return try {
            Json.decodeFromString(ListSerializer(String.serializer()), raw).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun markEpisodeWatched(slug: String, episodeName: String) {
        val set = getWatchedEpisodes(slug).toMutableSet()
        set.add(episodeName)
        val jsonStr = Json.encodeToString(ListSerializer(String.serializer()), set.toList())
        AppPreferences.putString("sf:watched_episodes:$slug", jsonStr)
    }
}
