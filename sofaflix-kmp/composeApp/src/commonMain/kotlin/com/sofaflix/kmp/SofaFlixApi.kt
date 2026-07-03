package com.sofaflix.kmp

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class SofaFlixApi(
    private val baseUrl: String = "https://sofaflix.top"
) {
    var token: String = ""
    private val imageBase = "https://phimimg.com"
    private val imageProxy = "https://wsrv.nl/?url="

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                prettyPrint = true
            })
        }
    }

    suspend fun home(): HomePayload {
        val hot = latestV3(1, 12, mapOf("sort_field" to "view", "sort_type" to "desc"))
        return HomePayload(
            hot = hot.mapIndexed { index, movie ->
                if (index < 7 && movie.tmdbLogo.isBlank()) {
                    movie.copy(tmdbLogo = runCatching { tmdbLogo(movie.slug) }.getOrDefault(""))
                } else {
                    movie
                }
            },
            latest = latestV3(1, 18),
            singles = listByType("phim-le", 1, 12),
            series = listByType("phim-bo", 1, 12),
            animation = genre("hoat-hinh", 1, 12)
        )
    }

    suspend fun latestV3(page: Int, limit: Int, extra: Map<String, String> = emptyMap()): List<Movie> {
        val params = mutableMapOf("page" to "$page", "limit" to "$limit")
        params.putAll(extra)
        return movieList("/api/danh-sach/phim-moi-cap-nhat-v3", params)
    }

    suspend fun listByType(type: String, page: Int, limit: Int): List<Movie> {
        return movieList("/api/v1/api/danh-sach/$type", mapOf("page" to "$page", "limit" to "$limit"))
    }

    suspend fun genre(slug: String, page: Int, limit: Int): List<Movie> {
        return movieList("/api/v1/api/the-loai/$slug", mapOf("page" to "$page", "limit" to "$limit"))
    }

    suspend fun country(slug: String, page: Int, limit: Int): List<Movie> {
        return movieList("/api/v1/api/quoc-gia/$slug", mapOf("page" to "$page", "limit" to "$limit"))
    }

    suspend fun year(year: String, page: Int, limit: Int): List<Movie> {
        return movieList("/api/v1/api/nam/$year", mapOf("page" to "$page", "limit" to "$limit"))
    }

    suspend fun search(keyword: String, page: Int = 1, limit: Int = 30): List<Movie> {
        return movieList("/api/v1/api/tim-kiem", mapOf("keyword" to keyword, "page" to "$page", "limit" to "$limit"))
    }

    suspend fun genres(): List<Pair<String, String>> {
        val arr = getJson("/api/the-loai").jsonArrayOrNull ?: return emptyList()
        val list = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.size) {
            val obj = arr[i].jsonObjectOrNull ?: continue
            val name = obj.firstText("name", "title")
            val slug = obj.firstText("slug")
            if (name.isNotBlank() && slug.isNotBlank()) {
                list.add(name to slug)
            }
        }
        return list
    }

    suspend fun countries(): List<Pair<String, String>> {
        val arr = getJson("/api/quoc-gia").jsonArrayOrNull ?: return emptyList()
        val list = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.size) {
            val obj = arr[i].jsonObjectOrNull ?: continue
            val name = obj.firstText("name", "title")
            val slug = obj.firstText("slug")
            if (name.isNotBlank() && slug.isNotBlank()) {
                list.add(name to slug)
            }
        }
        return list
    }

    suspend fun comments(slug: String): List<JsonElement> {
        val responseText = try {
            val response = client.get("$baseUrl/v1/comments/$slug") {
                header("Accept", "application/json")
                header("x-data-source", "local")
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                    header("x-access-token", token)
                }
            }
            response.bodyAsText()
        } catch (e: Exception) {
            return emptyList()
        }
        val list = mutableListOf<JsonElement>()
        try {
            val jsonElement = Json.parseToJsonElement(responseText)
            if (jsonElement is JsonArray) {
                for (i in 0 until jsonElement.size) {
                    list.add(jsonElement[i])
                }
            } else if (jsonElement is JsonObject) {
                val arr = jsonElement["data"]?.jsonArrayOrNull
                    ?: jsonElement["items"]?.jsonArrayOrNull
                    ?: buildJsonArray {}
                for (i in 0 until arr.size) {
                    list.add(arr[i])
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    suspend fun detail(slug: String): MovieDetail {
        val json = getJson("/api/phim/$slug")
        val data = json.jsonObjectOrNull?.get("data")
        val rawMovie = json.jsonObjectOrNull?.get("movie")
            ?: json.jsonObjectOrNull?.get("item")
            ?: data?.jsonObjectOrNull?.get("movie")
            ?: data?.jsonObjectOrNull?.get("item")
            ?: data
            ?: buildJsonObject {}

        val rawEpisodes = json.jsonObjectOrNull?.get("episodes")?.jsonArrayOrNull
            ?: data?.jsonObjectOrNull?.get("episodes")?.jsonArrayOrNull
            ?: rawMovie.jsonObjectOrNull?.get("episodes")?.jsonArrayOrNull
            ?: buildJsonArray {}

        val servers = mutableListOf<EpisodeServer>()
        for (i in 0 until rawEpisodes.size) {
            val serverJson = rawEpisodes[i].jsonObjectOrNull ?: continue
            val rawList = serverJson.get("server_data")?.jsonArrayOrNull
                ?: serverJson.get("items")?.jsonArrayOrNull
                ?: serverJson.get("episodes")?.jsonArrayOrNull
                ?: buildJsonArray {}

            val episodes = mutableListOf<Episode>()
            for (j in 0 until rawList.size) {
                val ep = rawList[j].jsonObjectOrNull ?: continue
                val subs = mutableListOf<SubtitleTrack>()
                val rawSubs = ep.get("subtitles")?.jsonArrayOrNull
                if (rawSubs != null) {
                    for (k in 0 until rawSubs.size) {
                        val subObj = rawSubs[k].jsonObjectOrNull ?: continue
                        val label = subObj.firstText("label", "name", "lang").ifBlank { "Phụ đề ${k + 1}" }
                        val url = subObj.firstText("url", "link", "src", "file")
                        if (url.isNotBlank()) {
                            subs.add(SubtitleTrack(label, absoluteUrl(url)))
                        }
                    }
                }
                val linkSubVal = ep.firstText("link_sub", "subtitle", "sub")
                val linkSub = if (linkSubVal.isNotBlank()) absoluteUrl(linkSubVal) else ""

                episodes.add(
                    Episode(
                        name = ep.firstText("name", "title", "slug").ifBlank { "Tap ${j + 1}" },
                        embedUrl = ep.firstText("link_embed", "embed", "embed_url"),
                        streamUrl = ep.firstText("link_m3u8", "m3u8", "video_url", "url"),
                        subtitles = subs,
                        linkSub = linkSub
                    )
                )
            }

            servers.add(
                EpisodeServer(
                    name = serverJson.firstText("server_name", "name").ifBlank { "Server ${i + 1}" },
                    episodes = episodes
                )
            )
        }

        return MovieDetail(
            movie = normalizeMovie(rawMovie),
            description = stripHtml(rawMovie.firstText("content", "description")),
            servers = servers,
            categories = asList(rawMovie.jsonObjectOrNull?.get("category") ?: rawMovie.jsonObjectOrNull?.get("genres")),
            actors = asList(rawMovie.jsonObjectOrNull?.get("actor") ?: rawMovie.jsonObjectOrNull?.get("actors")),
            directors = asList(rawMovie.jsonObjectOrNull?.get("director") ?: rawMovie.jsonObjectOrNull?.get("directors")),
            writers = asList(rawMovie.jsonObjectOrNull?.get("writer") ?: rawMovie.jsonObjectOrNull?.get("writers")),
            time = rawMovie.firstText("time", "duration")
        )
    }

    suspend fun tmdbLogo(slug: String): String {
        if (slug.isBlank()) return ""
        val data = getJson("/api/tmdb-logo/$slug")
        val logo = data.firstText("logo", "url")
            .ifBlank { data.jsonObjectOrNull?.get("data")?.firstText("logo", "url") ?: "" }
        return tmdbLogoUrl(logo)
    }

    fun absoluteUrl(value: String): String {
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
        return "$baseUrl/${value.trimStart('/')}"
    }

    private suspend fun movieList(path: String, params: Map<String, String>): List<Movie> {
        return extractItems(getJson(path, params)).map { normalizeMovie(it) }
    }

    private suspend fun getJson(path: String, params: Map<String, String> = emptyMap()): JsonElement {
        val response = client.get("$baseUrl$path") {
            header("Accept", "application/json")
            header("x-data-source", "local")
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
                header("x-access-token", token)
            }
            params.forEach { (k, v) ->
                parameter(k, v)
            }
        }
        return Json.parseToJsonElement(response.bodyAsText())
    }

    private suspend fun postJson(path: String, body: JsonElement): JsonElement {
        val response = client.post("$baseUrl$path") {
            header("Content-Type", "application/json")
            header("Accept", "application/json")
            header("x-data-source", "local")
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
                header("x-access-token", token)
            }
            setBody(body)
        }
        return Json.parseToJsonElement(response.bodyAsText())
    }

    suspend fun login(email: String, pass: String): JsonElement {
        val payload = buildJsonObject {
            put("email", email)
            put("password", pass)
        }
        val res = postJson("/v1/auth/login", payload)
        val resToken = res.firstText("token")
        if (resToken.isNotBlank()) {
            token = resToken
        }
        return res
    }

    suspend fun register(username: String, pass: String, email: String, name: String): JsonElement {
        val payload = buildJsonObject {
            put("username", username)
            put("password", pass)
            put("email", email)
            put("name", name)
        }
        return postJson("/v1/auth/register", payload)
    }

    suspend fun profile(): JsonElement {
        return getJson("/v1/auth/profile")
    }

    suspend fun createComment(slug: String, content: String, parentId: String?, isSpoiler: Boolean): JsonElement {
        val body = buildJsonObject {
            put("movieSlug", slug)
            put("content", content)
            if (parentId != null) put("parentId", parentId)
            put("isSpoiler", isSpoiler)
        }
        return postJson("/v1/comments", body)
    }

    private suspend fun deleteJson(path: String): JsonElement {
        val response = client.delete("$baseUrl$path") {
            header("Accept", "application/json")
            header("x-data-source", "local")
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
                header("x-access-token", token)
            }
        }
        return Json.parseToJsonElement(response.bodyAsText())
    }

    suspend fun getHistory(): List<JsonElement> {
        return try {
            val responseText = client.get("$baseUrl/v1/user/history") {
                header("Accept", "application/json")
                header("x-data-source", "local")
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                    header("x-access-token", token)
                }
            }.bodyAsText()
            val list = mutableListOf<JsonElement>()
            val jsonElement = Json.parseToJsonElement(responseText)
            if (jsonElement is JsonArray) {
                for (i in 0 until jsonElement.size) {
                    list.add(jsonElement[i])
                }
            } else if (jsonElement is JsonObject) {
                val arr = jsonElement["data"]?.jsonArrayOrNull ?: jsonElement["items"]?.jsonArrayOrNull ?: buildJsonArray {}
                for (i in 0 until arr.size) {
                    list.add(arr[i])
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToHistory(slug: String, name: String, thumbUrl: String, episode: String) {
        try {
            val body = buildJsonObject {
                put("movieSlug", slug)
                put("name", name)
                put("thumb_url", thumbUrl)
                put("episode", episode)
            }
            postJson("/v1/user/history", body)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeHistory(slug: String) {
        try {
            deleteJson("/v1/user/history/$slug")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getFavorites(): List<JsonElement> {
        return try {
            val responseText = client.get("$baseUrl/v1/user/favorites") {
                header("Accept", "application/json")
                header("x-data-source", "local")
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                    header("x-access-token", token)
                }
            }.bodyAsText()
            val list = mutableListOf<JsonElement>()
            val jsonElement = Json.parseToJsonElement(responseText)
            if (jsonElement is JsonArray) {
                for (i in 0 until jsonElement.size) {
                    list.add(jsonElement[i])
                }
            } else if (jsonElement is JsonObject) {
                val arr = jsonElement["data"]?.jsonArrayOrNull ?: jsonElement["items"]?.jsonArrayOrNull ?: buildJsonArray {}
                for (i in 0 until arr.size) {
                    list.add(arr[i])
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(slug: String, name: String, thumbUrl: String, type: String = "phim-le") {
        try {
            val body = buildJsonObject {
                put("movieSlug", slug)
                put("name", name)
                put("thumb_url", thumbUrl)
                put("type", type)
            }
            postJson("/v1/user/favorites", body)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractItems(payload: JsonElement): List<JsonElement> {
        val raw = payload.jsonObjectOrNull?.get("items")?.jsonArrayOrNull
            ?: payload.jsonObjectOrNull?.get("data")?.jsonObjectOrNull?.get("items")?.jsonArrayOrNull
            ?: payload.jsonObjectOrNull?.get("data")?.jsonObjectOrNull?.get("data")?.jsonObjectOrNull?.get("items")?.jsonArrayOrNull
            ?: buildJsonArray {}

        val items = mutableListOf<JsonElement>()
        for (i in 0 until raw.size) {
            val item = raw[i].jsonObjectOrNull ?: continue
            if (item.firstText("slug").isNotBlank()) {
                items.add(item)
            } else {
                val nested = item["items"]?.jsonArrayOrNull
                    ?: item["movies"]?.jsonArrayOrNull
                    ?: item["data"]?.jsonObjectOrNull?.get("items")?.jsonArrayOrNull
                if (nested != null) {
                    for (j in 0 until nested.size) nested[j].jsonObjectOrNull?.let { items.add(it) }
                }
            }
        }
        return items
    }

    private fun normalizeMovie(json: JsonElement): Movie {
        val thumb = json.firstText("thumb_url", "thumbnail", "image", "poster_url", "poster")
        val poster = json.firstText("poster_url", "poster", "thumb_url", "thumbnail", "image")
        return Movie(
            slug = json.firstText("slug", "id", "_id"),
            name = json.firstText("name", "title").ifBlank { "Chua ro ten" },
            originName = json.firstText("origin_name", "originName", "original_name"),
            thumbUrl = imageUrl(thumb, 70, 260),
            posterUrl = imageUrl(poster, 82, 720),
            episode = json.firstText("episode_current", "episodeCurrent"),
            quality = json.firstText("quality"),
            lang = json.firstText("lang"),
            year = json.firstText("year"),
            tmdbLogo = tmdbLogoUrl(json.firstText("tmdb_logo", "tmdbLogo", "logo_url", "logoUrl", "logo_path"))
        )
    }

    private fun tmdbLogoUrl(value: String): String {
        if (value.isBlank()) return ""
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
        if (value.startsWith("/")) return "https://image.tmdb.org/t/p/w500$value"
        return imageUrl(value, 82, 500)
    }

    private fun imageUrl(value: String, q: Int, width: Int): String {
        if (value.isBlank()) return ""
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
        return "$imageBase/${value.trimStart('/')}"
    }

    private fun stripHtml(value: String): String {
        return value.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun asList(value: JsonElement?): List<String> {
        if (value == null || value is JsonNull) return emptyList()
        if (value is JsonArray) {
            val list = mutableListOf<String>()
            for (i in 0 until value.size) {
                val item = value[i]
                if (item is JsonObject) {
                    val name = item.firstText("name", "title")
                    if (name.isNotBlank()) list.add(name)
                } else {
                    val name = item.jsonPrimitiveOrNull?.content?.trim() ?: ""
                    if (name.isNotBlank()) list.add(name)
                }
            }
            return list
        }
        if (value is JsonPrimitive) {
            return value.content.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        return emptyList()
    }
}

fun JsonElement.firstText(vararg keys: String): String {
    val obj = this.jsonObjectOrNull ?: return ""
    for (key in keys) {
        val element = obj[key]
        if (element != null && element !is JsonNull) {
            val content = element.jsonPrimitiveOrNull?.content ?: ""
            if (content.isNotBlank()) return content
        }
    }
    return ""
}

val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive
