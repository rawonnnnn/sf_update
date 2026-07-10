package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    api: SofaFlixApi,
    token: String,
    movie: Movie,
    initialEpisode: Episode,
    movieDetail: MovieDetail,
    onBackClick: () -> Unit
) {
    val lang = LocalLanguage.current
    var activeEpisode by remember { mutableStateOf(initialEpisode) }
    var activeServerIndex by remember {
        val savedServerName = StorageHelpers.getLastServerName(movie.slug)
        val savedIdx = movieDetail.servers.indexOfFirst { it.name == savedServerName }
        mutableStateOf(if (savedIdx >= 0) savedIdx else 0)
    }
    var showServerPicker by remember { mutableStateOf(false) }

    val currentServer = remember(movieDetail, activeServerIndex) {
        movieDetail.servers.getOrNull(activeServerIndex)
    }

    val nextEpisode = remember(activeEpisode, currentServer) {
        if (currentServer == null) null
        else {
            val idx = currentServer.episodes.indexOfFirst { it.name == activeEpisode.name }
            if (idx != -1 && idx + 1 < currentServer.episodes.size) {
                currentServer.episodes[idx + 1]
            } else {
                null
            }
        }
    }

    val hasNext = nextEpisode != null

    // Episode watched checking
    val watchedSet = remember { mutableStateListOf<String>() }

    LaunchedEffect(movie.slug, activeEpisode) {
        StorageHelpers.addToHistory(movie, activeEpisode.name)
        StorageHelpers.markEpisodeWatched(movie.slug, activeEpisode.name)
        currentServer?.let { StorageHelpers.saveLastServerName(movie.slug, it.name) }
        if (token.isNotBlank()) {
            try {
                api.addToHistory(
                    slug = movie.slug,
                    name = movie.name,
                    thumbUrl = movie.thumbUrl.ifBlank { movie.posterUrl },
                    episode = activeEpisode.name
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        // Refresh watched set
        watchedSet.clear()
        watchedSet.addAll(StorageHelpers.getWatchedEpisodes(movie.slug))
    }

    // Comments states
    var commentsList by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loadingComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSpoilerComment by remember { mutableStateOf(false) }
    var submittingComment by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(movie.slug) {
        loadingComments = true
        runCatching {
            commentsList = api.comments(movie.slug)
        }
        loadingComments = false
    }

    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        SetScreenOrientation(isLandscape = true)
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Scaffold(
        containerColor = Color(0xFF191B24)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.padding(paddingValues))
        ) {
            // 1. Top Bar
            if (!isFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = movie.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Video Player
            val playerModifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }

            Box(
                modifier = playerModifier
            ) {
                WatchPlayer(
                    movie = movie,
                    episode = activeEpisode,
                    hasNext = hasNext,
                    onPlayNextEpisode = {
                        if (nextEpisode != null) {
                            activeEpisode = nextEpisode
                        }
                    },
                    onUpdateProgress = { currentTime, duration ->
                        if (currentTime > 0 && duration > 0) {
                            StorageHelpers.saveEpisodeProgress(movie.slug, activeEpisode.name, currentTime.toFloat())
                            StorageHelpers.updateHistoryProgress(movie.slug, currentTime / duration)
                        }
                    },
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = { isFS ->
                        isFullscreen = isFS
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Native floating fullscreen button overlay for Embed Server (third-party players)
                if (activeEpisode.streamUrl.isBlank() && activeEpisode.embedUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable {
                                isFullscreen = !isFullscreen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FullscreenIcon(isFullscreen = isFullscreen)
                    }
                }
            }

            // 3. Bottom scrollable content
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                // Now playing metadata
                Text(
                    text = Lang.t("now_playing", lang),
                    color = Color(0xFFA7ADBA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${movie.name} - ${activeEpisode.name}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Description
                if (movieDetail.description.isNotBlank()) {
                    var descExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = movieDetail.description,
                        color = Color.White.copy(alpha = 0.69f),
                        fontSize = 13.sp,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { descExpanded = !descExpanded }
                            .padding(bottom = 14.dp)
                    )
                }

                // Servers and episodes
                if (movieDetail.servers.isNotEmpty() && currentServer != null) {
                    val episodes = currentServer.episodes
                    val chunkSize = 30
                    val hasChunks = episodes.size > chunkSize
                    var selectedChunkIndex by remember(currentServer) {
                        val activeIdx = episodes.indexOfFirst { it.name == activeEpisode.name }
                        mutableStateOf(if (activeIdx >= 0) activeIdx / chunkSize else 0)
                    }
                    val visibleEpisodes = if (hasChunks) {
                        val start = selectedChunkIndex * chunkSize
                        episodes.subList(start, minOf(start + chunkSize, episodes.size))
                    } else {
                        episodes
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Lang.t("select_episode", lang),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                        Text(
                                            text = currentServer.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "  ˅",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                            val isActive = ep.name == activeEpisode.name
                            val watched = watchedSet.contains(ep.name)
                            val globalIndex = episodes.indexOf(ep)
                            val displayName = ep.name.ifBlank {
                                if (lang == "vi") "Tập ${globalIndex + 1}" else "Ep ${globalIndex + 1}"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isActive -> Color(0xFF1CC749)
                                            watched -> Color.White.copy(alpha = 0.12f)
                                            else -> Color.White.copy(alpha = 0.06f)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isActive -> Color(0xFF1CC749)
                                            watched -> Color.White.copy(alpha = 0.18f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (!isActive) activeEpisode = ep
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (watched && !isActive) {
                                        Text(
                                            text = "✓",
                                            color = Color(0xFF1CC749),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = displayName,
                                        color = when {
                                            isActive -> Color.Black
                                            else -> Color.White.copy(alpha = 0.85f)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Comments Section
                Spacer(modifier = Modifier.height(24.dp))
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
                                runCatching {
                                    api.createComment(movie.slug, text, null, isSpoilerComment)
                                    newCommentText = ""
                                    isSpoilerComment = false
                                    commentsList = api.comments(movie.slug)
                                }
                                submittingComment = false
                            }
                        }
                    }
                )
                
                // Add a final bottom padding so the floating bottom bar or navigation isn't overlapping
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}

    if (showServerPicker) {
        AlertDialog(
            onDismissRequest = { showServerPicker = false },
            title = { Text(Lang.t("select_server", lang), color = Color.White) },
            containerColor = Color(0xFF191B24),
            text = {
                Column {
                    movieDetail.servers.forEachIndexed { index, server ->
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
}

@Composable
private fun FullscreenIcon(isFullscreen: Boolean) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val len = 5.dp.toPx()
        
        if (isFullscreen) {
            // Exit fullscreen: corners pointing inward
            // Top-left
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, len), end = androidx.compose.ui.geometry.Offset(len, len), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(len, 0f), end = androidx.compose.ui.geometry.Offset(len, len), strokeWidth = strokeWidth)
            
            // Top-right
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, len), end = androidx.compose.ui.geometry.Offset(w, len), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, 0f), end = androidx.compose.ui.geometry.Offset(w - len, len), strokeWidth = strokeWidth)
            
            // Bottom-left
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, h - len), end = androidx.compose.ui.geometry.Offset(len, h - len), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(len, h), end = androidx.compose.ui.geometry.Offset(len, h - len), strokeWidth = strokeWidth)
            
            // Bottom-right
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, h - len), end = androidx.compose.ui.geometry.Offset(w, h - len), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, h), end = androidx.compose.ui.geometry.Offset(w - len, h - len), strokeWidth = strokeWidth)
        } else {
            // Enter fullscreen: corners pointing outward
            // Top-left
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(len, 0f), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, len), strokeWidth = strokeWidth)
            
            // Top-right
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, 0f), end = androidx.compose.ui.geometry.Offset(w, 0f), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w, 0f), end = androidx.compose.ui.geometry.Offset(w, len), strokeWidth = strokeWidth)
            
            // Bottom-left
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, h), end = androidx.compose.ui.geometry.Offset(len, h), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, h - len), end = androidx.compose.ui.geometry.Offset(0f, h), strokeWidth = strokeWidth)
            
            // Bottom-right
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - len, h), end = androidx.compose.ui.geometry.Offset(w, h), strokeWidth = strokeWidth)
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w, h - len), end = androidx.compose.ui.geometry.Offset(w, h), strokeWidth = strokeWidth)
        }
    }
}
