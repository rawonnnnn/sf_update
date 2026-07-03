package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.Movie
import com.sofaflix.kmp.SofaFlixApi
import com.sofaflix.kmp.firstText
import com.sofaflix.kmp.jsonObjectOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

// Shared session storage for guest users
object GuestStorage {
    val favorites = mutableStateListOf<Movie>()
    val history = mutableStateListOf<Movie>()
}

@Composable
fun LibraryScreen(
    api: SofaFlixApi,
    token: String,
    onMovieClick: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("saved") } // saved or history
    var favoritesList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var historyList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun mapJsonToMovie(json: JsonElement): Movie {
        val obj = json.jsonObjectOrNull ?: return Movie("", "Chưa rõ", "", "", "", "", "", "", "", "")
        val slug = obj.firstText("movieSlug", "slug", "id", "_id")
        val name = obj.firstText("name", "title")
        val thumb = obj.firstText("thumb_url", "thumbnail", "image", "poster_url", "poster")
        val episode = obj.firstText("episode", "episode_current")
        val quality = obj.firstText("quality")
        val lang = obj.firstText("lang")
        val year = obj.firstText("year")
        return Movie(
            slug = slug,
            name = name,
            originName = "",
            thumbUrl = thumb,
            posterUrl = thumb,
            episode = episode,
            quality = quality,
            lang = lang,
            year = year,
            tmdbLogo = ""
        )
    }
    
    fun loadData() {
        if (token.isBlank()) {
            // For Guest users, pull from GuestStorage memory cache
            favoritesList = GuestStorage.favorites.toList()
            historyList = GuestStorage.history.toList()
            return
        }
        
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val favs = api.getFavorites().map { mapJsonToMovie(it) }
                val hists = api.getHistory().map { mapJsonToMovie(it) }
                favoritesList = favs
                historyList = hists
            } catch (e: Exception) {
                errorMsg = e.message ?: "Lỗi tải thư viện"
            } finally {
                loading = false
            }
        }
    }
    
    LaunchedEffect(token, activeTab) {
        loadData()
    }
    
    fun handleRemoveFavorite(movie: Movie) {
        if (token.isBlank()) {
            GuestStorage.favorites.removeAll { it.slug == movie.slug }
            favoritesList = GuestStorage.favorites.toList()
        } else {
            favoritesList = favoritesList.filter { it.slug != movie.slug }
            scope.launch {
                try {
                    api.toggleFavorite(movie.slug, movie.name, movie.thumbUrl)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    fun handleRemoveHistory(slug: String) {
        if (token.isBlank()) {
            GuestStorage.history.removeAll { it.slug == slug }
            historyList = GuestStorage.history.toList()
        } else {
            historyList = historyList.filter { it.slug != slug }
            scope.launch {
                try {
                    api.removeHistory(slug)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    val activeData = if (activeTab == "saved") favoritesList else historyList
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191B24))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = "Thư viện",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Tabs Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabPill(
                        label = "Đã lưu",
                        isActive = activeTab == "saved",
                        onClick = { activeTab = "saved" }
                    )
                    TabPill(
                        label = "Lịch sử",
                        isActive = activeTab == "history",
                        onClick = { activeTab = "history" }
                    )
                }
            }
            
            // Grid content
            if (loading && activeData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (activeData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (activeTab == "saved") "Thư viện phim đã lưu trống." else "Chưa có lịch sử xem phim.",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeData.size) { index ->
                        val movie = activeData[index]
                        LibraryGridItem(
                            movie = movie,
                            showDelete = true,
                            onDeleteClick = {
                                if (activeTab == "saved") {
                                    handleRemoveFavorite(movie)
                                } else {
                                    handleRemoveHistory(movie.slug)
                                }
                            },
                            onClick = { onMovieClick(movie.slug) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) Color.White else Color(0xFF20232D))
            .border(1.dp, if (isActive) Color.White else Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.Black else Color.White.copy(alpha = 0.62f),
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
fun LibraryGridItem(
    movie: Movie,
    showDelete: Boolean,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(135f / 240f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(135f / 198f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                AsyncImage(
                    model = movie.posterUrl.ifBlank { movie.thumbUrl },
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = movie.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = movie.episode.ifBlank { movie.year }.ifBlank { "2026" },
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (showDelete) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1117).copy(alpha = 0.76f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
