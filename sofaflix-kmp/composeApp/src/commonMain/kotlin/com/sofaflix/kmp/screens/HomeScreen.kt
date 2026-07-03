package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.Movie
import com.sofaflix.kmp.HomePayload
import com.sofaflix.kmp.SofaFlixApi
import com.sofaflix.kmp.ui.MovieCard
import com.sofaflix.kmp.ui.SectionHeader
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import sofaflix.composeapp.generated.resources.Res
import sofaflix.composeapp.generated.resources.sofaflix_header_logo
import sofaflix.composeapp.generated.resources.splashscreen_logo

@Composable
fun HomeScreen(
    api: SofaFlixApi,
    onMovieClick: (String) -> Unit,
    onCategoryClick: (name: String, kind: String, slug: String) -> Unit
) {
    var payload by remember { mutableStateOf<HomePayload?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun loadHome() {
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                payload = api.home()
            } catch (e: Exception) {
                errorMsg = e.message ?: "Đã xảy ra lỗi tải phim"
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadHome()
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.splashscreen_logo),
                contentDescription = "Loading...",
                modifier = Modifier
                    .width(180.dp)
                    .height(180.dp)
            )
        }
    } else if (errorMsg != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMsg!!,
                    color = Color.Red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { loadHome() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1CC749))
                ) {
                    Text("Thử lại", color = Color.White)
                }
            }
        }
    } else {
        payload?.let { homeData ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val hotMovies = homeData.hot.take(7)
                if (hotMovies.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { hotMovies.size })
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(560.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val movie = hotMovies[page]
                            HeroSlide(movie = movie, onClick = { onMovieClick(movie.slug) })
                        }
                        
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(hotMovies.size) { index ->
                                val active = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(if (active) 24.dp else 6.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (active) Color.White else Color.White.copy(alpha = 0.42f))
                                )
                            }
                        }
                    }
                }
                
                InterestCategories(onCategoryClick = onCategoryClick)
                
                TopChart(movies = homeData.hot, onMovieClick = onMovieClick)
                
                MovieRail(title = "Mới cập nhật", movies = homeData.latest, onMovieClick = onMovieClick)
                MovieRail(title = "Phim lẻ", movies = homeData.singles, onMovieClick = onMovieClick)
                MovieRail(title = "Phim bộ", movies = homeData.series, onMovieClick = onMovieClick)
                MovieRail(title = "Hoạt hình", movies = homeData.animation, onMovieClick = onMovieClick)
                
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}

@Composable
fun HeroSlide(
    movie: Movie,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.posterUrl.ifBlank { movie.thumbUrl },
            contentDescription = movie.name,
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
                            Color.Black.copy(alpha = 0.4f),
                            Color(0xFF191B24).copy(alpha = 0.9f),
                            Color(0xFF191B24)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 58.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (movie.tmdbLogo.isNotBlank()) {
                AsyncImage(
                    model = movie.tmdbLogo,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(300.dp)
                        .height(80.dp)
                )
            } else {
                Text(
                    text = movie.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            val metaText = listOf(movie.year, movie.quality, movie.lang, movie.episode)
                .filter { it.isNotBlank() }
                .joinToString("  |  ")
                .ifBlank { "Phim  |  2026" }
                
            Text(
                text = metaText,
                color = Color(0xFFD1D5DB),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .width(146.dp)
                    .height(44.dp)
            ) {
                Text(
                    text = "Xem chi tiết",
                    color = Color(0xFF111318),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class CategoryUi(
    val title: String,
    val kind: String,
    val slug: String,
    val startColor: Color,
    val endColor: Color,
    val mark: String
)

@Composable
fun InterestCategories(onCategoryClick: (name: String, kind: String, slug: String) -> Unit) {
    val items = remember {
        listOf(
            CategoryUi("Kinh Dị", "genre", "kinh-di", Color(0xFFFD3F52), Color(0xFFFD6B35), "☠"),
            CategoryUi("Cổ Trang", "genre", "co-trang", Color(0xFF843CF6), Color(0xFFA367FC), "🛡"),
            CategoryUi("Hoạt Hình", "genre", "hoat-hinh", Color(0xFF1E72F6), Color(0xFF49A3F9), "✦"),
            CategoryUi("Hàn Quốc", "country", "han-quoc", Color(0xFF00C749), Color(0xFF15D886), "♡"),
            CategoryUi("Âu Mỹ", "country", "au-my", Color(0xFFF43F80), Color(0xFFFF6B9D), "◎"),
            CategoryUi("Lồng Tiếng", "genre", "long-tieng", Color(0xFFFD8900), Color(0xFFFDB600), "🎙"),
            CategoryUi("Thuyết Minh", "genre", "thuyet-minh", Color(0xFF00BFA5), Color(0xFF00E5FF), "☵"),
            CategoryUi("Phim Hot", "list", "phim-hot", Color(0xFFE040FB), Color(0xFFEA80FC), "♨")
        )
    }
    
    Column(modifier = Modifier.padding(top = 30.dp)) {
        SectionHeader("Khám phá theo thể loại")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                CategoryCard(item = item, onClick = {
                    onCategoryClick(item.title, item.kind, item.slug)
                })
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(item.startColor, item.endColor)
                )
            )
            .clickable { onClick() }
    ) {
        Text(
            text = item.mark,
            color = Color.White.copy(alpha = 0.16f),
            fontSize = 58.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(18.dp, 20.dp)
        )
        
        Text(
            text = item.mark,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        )
        
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
        )
    }
}

@Composable
fun TopChart(movies: List<Movie>, onMovieClick: (String) -> Unit) {
    val items = movies.take(10)
    if (items.isEmpty()) return
    
    Column(modifier = Modifier.padding(top = 30.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "TOP 10",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Đang hot",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .width(32.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF1CC749))
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items.size) { index ->
                val movie = items[index]
                TopChartCard(movie = movie, rank = index + 1, onClick = { onMovieClick(movie.slug) })
            }
        }
    }
}

@Composable
fun TopChartCard(movie: Movie, rank: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(198.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            AsyncImage(
                model = movie.posterUrl.ifBlank { movie.thumbUrl },
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Text(
                text = rank.toString(),
                color = Color.White.copy(alpha = 0.18f),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(8.dp, 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.name,
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.year.ifBlank { "2026" },
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun MovieRail(title: String, movies: List<Movie>, onMovieClick: (String) -> Unit) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(top = 30.dp)) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(movies.size) { index ->
                MovieCard(movie = movies[index], onClick = { onMovieClick(movies[index].slug) })
            }
        }
    }
}
