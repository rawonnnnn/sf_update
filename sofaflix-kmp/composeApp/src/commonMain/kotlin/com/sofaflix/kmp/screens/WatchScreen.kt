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
    var activeEpisode by remember { mutableStateOf(initialEpisode) }
    var activeServerIndex by remember { mutableStateOf(0) }
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

    Scaffold(
        containerColor = Color(0xFF191B24)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top Bar
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

            // 2. Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Bottom scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Now playing metadata
                Text(
                    text = "ĐANG PHÁT",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chọn tập phim",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
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

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentServer.episodes.size) { epIndex ->
                            val ep = currentServer.episodes[epIndex]
                            val isActive = ep.name == activeEpisode.name
                            val watched = watchedSet.contains(ep.name)
                            
                            Column(
                                modifier = Modifier
                                    .width(152.dp)
                                    .clickable {
                                        if (!isActive) {
                                            activeEpisode = ep
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(152.dp, 86.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isActive) Color.Transparent else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = if (isActive) 2.dp else 1.dp,
                                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = movie.posterUrl.ifBlank { movie.thumbUrl },
                                        contentDescription = ep.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.1f),
                                                        Color.Black.copy(alpha = 0.5f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = if (isActive) Color(0xFF1CC749) else Color.White.copy(alpha = 0.86f),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (watched && !isActive) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .background(
                                                    color = Color.Black.copy(alpha = 0.76f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✓ ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text("Đã xem", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ep.name.ifBlank { "Tập ${epIndex + 1}" },
                                    color = if (isActive) Color.White else Color(0xFFA7ADBA),
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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

    if (showServerPicker) {
        AlertDialog(
            onDismissRequest = { showServerPicker = false },
            title = { Text("Chọn nguồn phát", color = Color.White) },
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
