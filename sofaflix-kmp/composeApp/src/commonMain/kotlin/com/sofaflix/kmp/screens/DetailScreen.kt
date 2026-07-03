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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    api: SofaFlixApi,
    token: String,
    movieSlug: String,
    onBackClick: () -> Unit,
    onPlayClick: (Movie, Episode, MovieDetail) -> Unit
) {
    var detail by remember { mutableStateOf<MovieDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }
    
    var activeServerIndex by remember { mutableStateOf(0) }
    var showServerPicker by remember { mutableStateOf(false) }

    // Comments states
    var commentsList by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loadingComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSpoilerComment by remember { mutableStateOf(false) }
    var submittingComment by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val watchedSet = remember { mutableStateListOf<String>() }

    fun loadData() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val detailData = api.detail(movieSlug)
                detail = detailData
                isFav = StorageHelpers.isMovieFavorite(movieSlug)
                
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
                errorMsg = e.message ?: "Có lỗi xảy ra khi tải chi tiết."
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
                        Text("Đang tải chi tiết...", color = Color.Gray, fontSize = 14.sp)
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
                            Text("Thử lại", color = Color.White)
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
                                    val resumeText = if (hasWatchedAny) "Xem tiếp" else "Xem ngay"
                                    
                                    Button(
                                        onClick = {
                                            val lastWatchedEpName = StorageHelpers.getWatchHistory()
                                                .find { it.slug == movie.slug }?.episode
                                            val ep = if (!lastWatchedEpName.isNullOrBlank()) {
                                                movieDetail.servers.flatMap { it.episodes }
                                                    .find { it.name.trim().lowercase() == lastWatchedEpName.trim().lowercase() }
                                                    ?: movieDetail.firstEpisode
                                            } else {
                                                movieDetail.firstEpisode
                                            }
                                            
                                            if (ep != null) {
                                                StorageHelpers.addToHistory(movie, ep.name)
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
                                                isFav = StorageHelpers.toggleFavorite(movie)
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
                                CrewLine("Đạo diễn: ", movieDetail.directors.joinToString(", "))
                            }
                            val writersVal = if (movieDetail.writers.isNotEmpty()) movieDetail.writers.joinToString(", ") else "Đang cập nhật"
                            CrewLine("Biên kịch: ", writersVal)
                            if (movieDetail.actors.isNotEmpty()) {
                                CrewLine("Diễn viên: ", movieDetail.actors.take(5).joinToString(", "))
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
                                        text = if (descExpanded) "Thu gọn ▲" else "Xem thêm ▼",
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Danh sách tập", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        
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
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(currentServer.episodes.size) { epIndex ->
                                            val ep = currentServer.episodes[epIndex]
                                            val watched = watchedSet.contains(ep.name)
                                            
                                            Column(
                                                modifier = Modifier
                                                    .width(172.dp)
                                                    .clickable {
                                                        StorageHelpers.addToHistory(movie, ep.name)
                                                        onPlayClick(movie, ep, movieDetail)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(172.dp, 98.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.05f))
                                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = movie.posterUrl.ifBlank { movie.thumbUrl },
                                                        contentDescription = ep.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    
                                                    // Dark overlay & play button
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.3f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("▶", color = Color.White.copy(alpha = 0.86f), fontSize = 22.sp)
                                                    }
                                                    
                                                    if (watched) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Black.copy(alpha = 0.76f))
                                                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("✓ Đã xem", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = ep.name.ifBlank { "Tập ${epIndex + 1}" },
                                                    color = Color.White,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
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
            title = { Text("Chọn nguồn phát", color = Color.White) },
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
        imdb = String.format("%.1f", 7.2 + (abs % 16) / 10.0),
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Text("Bình luận", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text("Viết bình luận...", color = Color.White.copy(alpha = 0.38f)) },
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
                            text = "👁️ Spoiler",
                            color = if (isSpoiler) Color(0xFF1CC749) else Color(0xFFA7ADBA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onSubmitComment,
                        enabled = !submitting && newCommentText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1CC749),
                            disabledContainerColor = Color.Gray
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Gửi", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    text = "💬 Đăng nhập để tham gia bình luận",
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
                    text = "Chưa có bình luận nào. Hãy là người đầu tiên!",
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
                    val username = user?.get("username")?.jsonPrimitive?.content ?: "Ẩn danh"
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
                                    Text("Bình luận chứa spoiler! Nhấn để xem 👁️", color = Color(0xFFFD3F52), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            Text("Xem thêm bình luận", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
