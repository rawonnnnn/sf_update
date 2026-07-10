package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    api: SofaFlixApi,
    token: String,
    movieSlug: String,
    onBackClick: () -> Unit,
    onPlayClick: (Movie, Episode, MovieDetail) -> Unit,
    onLoginRequired: (() -> Unit)? = null
) {
    var detail by remember { mutableStateOf<MovieDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }
    
    var activeServerIndex by remember { mutableStateOf(0) }
    var showServerPicker by remember { mutableStateOf(false) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    // Comments states
    var commentsList by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loadingComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSpoilerComment by remember { mutableStateOf(false) }
    var submittingComment by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val watchedSet = remember { mutableStateListOf<String>() }
    val lang = LocalLanguage.current

    fun loadData() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val detailData = api.detail(movieSlug)
                detail = detailData
                var fav = StorageHelpers.isMovieFavorite(movieSlug)
                if (token.isNotBlank()) {
                    try {
                        val serverFavs = api.getFavorites()
                        val isServerFav = serverFavs.any {
                            val obj = it.jsonObjectOrNull
                            val slug = obj?.firstText("movieSlug", "slug", "id", "_id")
                            slug == movieSlug
                        }
                        fav = isServerFav
                    } catch (e: Exception) {
                        // ignore server error, fallback to local
                    }
                }
                isFav = fav
                
                // Get watched history and populate watchedSet
                val localHistory = StorageHelpers.getWatchHistory()
                val historyItem = localHistory.find { it.slug == movieSlug }
                watchedSet.clear()
                if (historyItem != null && historyItem.episode.isNotBlank()) {
                    // Let's split by comma or add the specific episode name
                    watchedSet.add(historyItem.episode)
                }

                // Load comments
                loadingComments = true
                try {
                    commentsList = api.comments(movieSlug)
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    loadingComments = false
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: Lang.t("loading_detail_error", lang)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(movieSlug) {
        loadData()
    }

    Scaffold(
        containerColor = Color(0xFF191B24)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF1CC749))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(Lang.t("loading_detail", lang), color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else if (errorMsg != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(errorMsg!!, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1CC749))
                        ) {
                            Text(Lang.t("try_again", lang), color = Color.White)
                        }
                    }
                }
            } else {
                detail?.let { movieDetail ->
                    val movie = movieDetail.movie
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Immersive Hero Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp)
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = movie.posterUrl.ifBlank { movie.thumbUrl },
                                contentDescription = movie.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.36f),
                                                Color.Black.copy(alpha = 0.1f),
                                                Color(0xFF191B24).copy(alpha = 0.6f),
                                                Color(0xFF191B24)
                                            )
                                        )
                                    )
                            )
                            
                            // Floating Back Button
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(start = 18.dp, top = 20.dp)
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(19.dp))
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                            }
                            
                            // Floating Volume Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(end = 18.dp, top = 20.dp)
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(19.dp))
                                    .clickable { isMuted = !isMuted },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isMuted) "🔇" else "🔊", color = Color.White, fontSize = 15.sp)
                            }
                            
                            // Overlaid Title, Categories & Actions
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = movie.name.uppercase(),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (movieDetail.categories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = movieDetail.categories.take(3).joinToString(" • "),
                                        color = Color.White.copy(alpha = 0.76f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(18.dp))
                                
                                // Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val hasWatchedAny = watchedSet.isNotEmpty()
                                    val resumeText = if (hasWatchedAny) Lang.t("xem_tiep", lang) else Lang.t("xem_ngay", lang)
                                    
                                    Button(
                                        onClick = {
                                            val lastWatchedEpName = StorageHelpers.getWatchHistory()
                                                .find { it.slug == movie.slug }?.episode
                                            val savedServerName = StorageHelpers.getLastServerName(movie.slug)
                                            
                                            // Find the saved server first
                                            val savedServerIdx = if (savedServerName.isNotBlank()) {
                                                movieDetail.servers.indexOfFirst { it.name == savedServerName }
                                            } else -1
                                            val targetServerIdx = if (savedServerIdx >= 0) savedServerIdx else 0
                                            val targetServer = movieDetail.servers.getOrNull(targetServerIdx)
                                            
                                            // Set the active server index so DetailScreen shows the correct server
                                            activeServerIndex = targetServerIdx
                                            
                                            val ep = if (!lastWatchedEpName.isNullOrBlank() && targetServer != null) {
                                                targetServer.episodes
                                                    .find { it.name.trim().lowercase() == lastWatchedEpName.trim().lowercase() }
                                                    ?: targetServer.episodes.firstOrNull()
                                                    ?: movieDetail.firstEpisode
                                            } else {
                                                targetServer?.episodes?.firstOrNull() ?: movieDetail.firstEpisode
                                            }
                                            
                                            if (ep != null) {
                                                StorageHelpers.addToHistory(movie, ep.name)
                                                if (token.isNotBlank()) {
                                                    scope.launch {
                                                        try {
                                                            api.addToHistory(
                                                                slug = movie.slug,
                                                                name = movie.name,
                                                                thumbUrl = movie.thumbUrl.ifBlank { movie.posterUrl },
                                                                episode = ep.name
                                                            )
                                                        } catch (e: Exception) {
                                                            // Ignore
                                                        }
                                                    }
                                                }
                                                onPlayClick(movie, ep, movieDetail)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("▶ ", color = Color.Black, fontSize = 14.sp)
                                            Text(resumeText, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Favorite button
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color.White.copy(alpha = 0.14f))
                                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                                            .clickable {
                                                if (token.isBlank()) {
                                                    showLoginRequiredDialog = true
                                                } else {
                                                    isFav = StorageHelpers.toggleFavorite(movie)
                                                    scope.launch {
                                                        try {
                                                            api.toggleFavorite(
                                                                slug = movie.slug,
                                                                name = movie.name,
                                                                thumbUrl = movie.thumbUrl.ifBlank { movie.posterUrl }
                                                            )
                                                        } catch (e: Exception) {
                                                            // Ignore
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isFav) "★" else "☆",
                                            color = if (isFav) Color(0xFF1CC749) else Color.White,
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 2. Info Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            // Meta Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                if (movie.year.isNotBlank()) {
                                    Text(movie.year, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (movieDetail.time.isNotBlank()) {
                                    Text(movieDetail.time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = movie.quality.ifBlank { "PG-13" },
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Ratings Rail
                            RatingsRail(movie.slug)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Crew Details
                            if (movieDetail.directors.isNotEmpty()) {
                                CrewLine(Lang.t("director", lang), movieDetail.directors.joinToString(", "))
                            }
                            val writersVal = if (movieDetail.writers.isNotEmpty()) movieDetail.writers.joinToString(", ") else Lang.t("updating", lang)
                            CrewLine(Lang.t("writer", lang), writersVal)
                            if (movieDetail.actors.isNotEmpty()) {
                                CrewLine(Lang.t("actor", lang), movieDetail.actors.take(5).joinToString(", "))
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Description
                            if (movieDetail.description.isNotBlank()) {
                                Text(
                                    text = movieDetail.description,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.5.sp,
                                    maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                if (movieDetail.description.length > 120) {
                                    Text(
                                        text = if (descExpanded) Lang.t("show_less", lang) else Lang.t("show_more", lang),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { descExpanded = !descExpanded }
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Episodes List
                            if (movieDetail.servers.isNotEmpty()) {
                                val currentServer = movieDetail.servers.getOrNull(activeServerIndex)
                                if (currentServer != null) {
                                    val episodes = currentServer.episodes
                                    val chunkSize = 30
                                    val hasChunks = episodes.size > chunkSize
                                    
                                    // Use remember with keys so state resets appropriately on movie/server changes
                                    var selectedChunkIndex by remember(movie.slug, activeServerIndex) {
                                        // Calculate the chunk index where the last watched episode resides
                                        val lastWatchedEp = watchedSet.firstOrNull()
                                        val watchedIndex = if (lastWatchedEp != null) {
                                            episodes.indexOfFirst { it.name == lastWatchedEp }
                                        } else -1
                                        val initialChunk = if (watchedIndex >= 0) watchedIndex / chunkSize else 0
                                        mutableStateOf(initialChunk)
                                    }
                                    
                                    val visibleEpisodes = remember(episodes, selectedChunkIndex, hasChunks) {
                                        if (hasChunks) {
                                            val start = selectedChunkIndex * chunkSize
                                            val end = minOf(start + chunkSize, episodes.size)
                                            episodes.subList(start, end)
                                        } else {
                                            episodes
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(Lang.t("episodes_list", lang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        
                                        if (movieDetail.servers.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color.White.copy(alpha = 0.08f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                                    .clickable { showServerPicker = true }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(currentServer.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(" ˅", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Range selector for many episodes
                                    if (hasChunks) {
                                        val totalChunks = (episodes.size + chunkSize - 1) / chunkSize
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 10.dp)
                                        ) {
                                            items(totalChunks) { chunkIdx ->
                                                val start = chunkIdx * chunkSize + 1
                                                val end = minOf((chunkIdx + 1) * chunkSize, episodes.size)
                                                val isSelected = chunkIdx == selectedChunkIndex
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) Color(0xFF1CC749) else Color.White.copy(alpha = 0.07f)
                                                        )
                                                        .clickable { selectedChunkIndex = chunkIdx }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "$start–$end",
                                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Compact episode chip grid
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        visibleEpisodes.forEach { ep ->
                                            val watched = watchedSet.contains(ep.name)
                                            val globalIndex = episodes.indexOf(ep)
                                            val displayName = ep.name.ifBlank {
                                                if (lang == "vi") "Tập ${globalIndex + 1}" else "Ep ${globalIndex + 1}"
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (watched) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (watched) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        StorageHelpers.addToHistory(movie, ep.name)
                                                        if (token.isNotBlank()) {
                                                            scope.launch {
                                                                try {
                                                                    api.addToHistory(
                                                                        slug = movie.slug,
                                                                        name = movie.name,
                                                                        thumbUrl = movie.thumbUrl.ifBlank { movie.posterUrl },
                                                                        episode = ep.name
                                                                    )
                                                                } catch (e: Exception) {
                                                                    // Ignore
                                                                }
                                                            }
                                                        }
                                                        onPlayClick(movie, ep, movieDetail)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (watched) {
                                                        Text(
                                                            text = "✓",
                                                            color = Color(0xFF1CC749),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = displayName,
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            // 3. Comments Section
                            CommentsSection(
                                comments = commentsList,
                                loading = loadingComments,
                                isLoggedIn = token.isNotBlank(),
                                newCommentText = newCommentText,
                                isSpoiler = isSpoilerComment,
                                submitting = submittingComment,
                                onCommentTextChange = { newCommentText = it },
                                onSpoilerChange = { isSpoilerComment = it },
                                onSubmitComment = {
                                    val text = newCommentText.trim()
                                    if (text.isNotBlank()) {
                                        submittingComment = true
                                        scope.launch {
                                            try {
                                                api.createComment(movieSlug, text, null, isSpoilerComment)
                                                newCommentText = ""
                                                isSpoilerComment = false
                                                // Refresh comments
                                                commentsList = api.comments(movieSlug)
                                            } catch (e: Exception) {
                                                // Ignore
                                            } finally {
                                                submittingComment = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Server selector dialog
    if (showServerPicker && detail != null) {
        AlertDialog(
            onDismissRequest = { showServerPicker = false },
            title = { Text(Lang.t("select_server", lang), color = Color.White) },
            containerColor = Color(0xFF191B24),
            text = {
                Column {
                    detail!!.servers.forEachIndexed { index, server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeServerIndex = index
                                    showServerPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(server.name, color = Color.White, fontSize = 16.sp)
                            if (index == activeServerIndex) {
                                Text("✓", color = Color(0xFF1CC749), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showLoginRequiredDialog = false },
            title = {
                Text(
                    text = if (lang == "vi") "Yêu cầu đăng nhập" else "Login Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (lang == "vi") "Vui lòng đăng nhập tài khoản để sử dụng tính năng lưu phim yêu thích."
                           else "Please log in to save your favorite movies.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            containerColor = Color(0xFF1F222B),
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoginRequiredDialog = false
                        onLoginRequired?.invoke()
                    }
                ) {
                    Text(if (lang == "vi") "Đăng nhập ngay" else "Login Now", color = Color(0xFF1CC749), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginRequiredDialog = false }) {
                    Text(if (lang == "vi") "Đóng" else "Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
fun CrewLine(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)) {
                append(label)
            }
            withStyle(style = SpanStyle(color = Color.White)) {
                append(value)
            }
        },
        fontSize = 13.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun RatingBadge(mark: String, value: String, backgroundColor: Color, textColor: Color, textOnly: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        if (!textOnly) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(text = mark, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
        } else {
            Text(text = mark, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RatingsRail(slug: String) {
    val ratings = remember(slug) { mockRatings(slug) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RatingBadge("IMDb", ratings.imdb, Color(0xFFF5C518), Color.Black)
        RatingBadge("TMDB", ratings.tmdb, Color(0xFF01B4E4), Color.White)
        RatingBadge("🍅", ratings.rotten, Color.Transparent, Color.White, textOnly = true)
        RatingBadge("M", ratings.metacritic, Color(0xFF66CC33), Color.Black)
        RatingBadge("T", ratings.trakt, Color(0xFFED2224), Color.White)
        RatingBadge("🍿", ratings.letterboxd, Color.Transparent, Color.White, textOnly = true)
    }
}

data class Ratings(
    val imdb: String,
    val tmdb: String,
    val rotten: String,
    val metacritic: String,
    val trakt: String,
    val letterboxd: String
)

fun mockRatings(slug: String): Ratings {
    var hash = 0
    slug.forEach { ch -> hash = ch.code + ((hash shl 5) - hash) }
    val abs = kotlin.math.abs(hash)
    return Ratings(
        imdb = "${kotlin.math.round((7.2 + (abs % 16) / 10.0) * 10) / 10.0}",
        tmdb = "${74 + kotlin.math.abs((hash shr 2) % 20)}",
        rotten = "${78 + kotlin.math.abs((hash shr 4) % 18)}%",
        metacritic = "${68 + kotlin.math.abs((hash shr 6) % 22)}",
        trakt = "${76 + kotlin.math.abs((hash shr 8) % 18)}",
        letterboxd = "${80 + kotlin.math.abs((hash shr 10) % 16)}%"
    )
}

@Composable
fun CommentsSection(
    comments: List<JsonElement>,
    loading: Boolean,
    isLoggedIn: Boolean,
    newCommentText: String,
    isSpoiler: Boolean,
    submitting: Boolean,
    onCommentTextChange: (String) -> Unit,
    onSpoilerChange: (Boolean) -> Unit,
    onSubmitComment: () -> Unit
) {
    val lang = LocalLanguage.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Text(Lang.t("comments", lang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(comments.size.toString(), color = Color(0xFFA7ADBA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                TextField(
                    value = newCommentText,
                    onValueChange = onCommentTextChange,
                    placeholder = { Text(Lang.t("write_comment", lang), color = Color.White.copy(alpha = 0.38f)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spoiler checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (isSpoiler) Color(0x331CC749) else Color.White.copy(alpha = 0.06f)
                            )
                            .clickable { onSpoilerChange(!isSpoiler) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Lang.t("spoiler", lang),
                            color = if (isSpoiler) Color(0xFF1CC749) else Color(0xFFA7ADBA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSubmitComment()
                        },
                        enabled = !submitting && newCommentText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1CC749),
                            disabledContainerColor = Color.Gray
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(Lang.t("send", lang), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x331CC749))
                    .border(1.dp, Color(0x381CC749), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = Lang.t("login_to_comment", lang),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1CC749), modifier = Modifier.size(24.dp))
            }
        } else if (comments.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("💬", fontSize = 24.sp, color = Color.White.copy(alpha = 0.31f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Lang.t("no_comments", lang),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
        } else {
            var visibleCount by remember { mutableStateOf(5) }
            val itemsToShow = comments.take(visibleCount)
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsToShow.forEach { commentElement ->
                    val commentObj = commentElement.jsonObject
                    val user = commentObj["user"]?.jsonObject
                    val username = user?.get("username")?.jsonPrimitive?.content ?: Lang.t("anonymous", lang)
                    val content = commentObj["content"]?.jsonPrimitive?.content ?: ""
                    val isSpoiler = commentObj["isSpoiler"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    
                    var spoilerRevealed by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Color(0xFF252936)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.firstOrNull()?.uppercase()?.toString() ?: "U",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(username, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            if (isSpoiler && !spoilerRevealed) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Red.copy(alpha = 0.15f))
                                        .clickable { spoilerRevealed = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(Lang.t("spoiler_warning", lang), color = Color(0xFFFD3F52), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(content, color = Color.White.copy(alpha = 0.86f), fontSize = 13.5.sp)
                            }
                        }
                    }
                }
                
                if (comments.size > visibleCount) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { visibleCount += 5 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x121CC749),
                                contentColor = Color(0xFF1CC749)
                            ),
                            shape = RoundedCornerShape(19.dp),
                            border = BorderStroke(1.dp, Color(0x471CC749))
                        ) {
                            Text(Lang.t("load_more_comments", lang), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
