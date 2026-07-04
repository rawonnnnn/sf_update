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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlin.math.absoluteValue
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
import com.sofaflix.kmp.LocalLanguage
import com.sofaflix.kmp.Lang
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
    val lang = LocalLanguage.current
    
    fun loadHome() {
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                payload = api.home()
            } catch (e: Exception) {
                errorMsg = e.message ?: Lang.t("load_movies_error", lang)
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
                    Text(Lang.t("try_again", lang), color = Color.White)
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
                    
                    // Auto-scroll banner slides every 5 seconds when not interacting
                    LaunchedEffect(pagerState) {
                        while (true) {
                            kotlinx.coroutines.delay(5000)
                            if (!pagerState.isScrollInProgress) {
                                val nextPage = (pagerState.currentPage + 1) % hotMovies.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    }
                    
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
                            
                            // Parallax scale and fade animations
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            val scale = (1f - (pageOffset.absoluteValue * 0.12f)).coerceIn(0.88f, 1f)
                            val alpha = (1f - (pageOffset.absoluteValue * 0.42f)).coerceIn(0.58f, 1f)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                        this.alpha = alpha
                                    }
                            ) {
                                HeroSlide(movie = movie, onClick = { onMovieClick(movie.slug) })
                            }
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
                
                MovieRail(
                    title = Lang.t("latest", lang),
                    movies = homeData.latest,
                    onMovieClick = onMovieClick,
                    onSeeAllClick = { onCategoryClick(Lang.t("phim_moi", lang), "list", "") }
                )
                MovieRail(
                    title = Lang.t("movies", lang),
                    movies = homeData.singles,
                    onMovieClick = onMovieClick,
                    onSeeAllClick = { onCategoryClick(Lang.t("movies", lang), "list", "phim-le") }
                )
                MovieRail(
                    title = Lang.t("series", lang),
                    movies = homeData.series,
                    onMovieClick = onMovieClick,
                    onSeeAllClick = { onCategoryClick(Lang.t("series", lang), "list", "phim-bo") }
                )
                MovieRail(
                    title = Lang.t("animation", lang),
                    movies = homeData.animation,
                    onMovieClick = onMovieClick,
                    onSeeAllClick = { onCategoryClick(Lang.t("animation", lang), "genre", "hoat-hinh") }
                )
                
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
            
            val lang = LocalLanguage.current
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
                    text = Lang.t("view_details", lang),
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
    val imageUrl: String,
    val accentColor: Color
)

@Composable
fun InterestCategories(onCategoryClick: (name: String, kind: String, slug: String) -> Unit) {
    val lang = LocalLanguage.current
    val items = remember(lang) {
        listOf(
            CategoryUi(if (lang == "vi") "Hot Rần Rần" else "Trending", "country", "trung-quoc", "https://sofaflix.top/images/chu-de.webp", Color(0xFFF472B6)),
            CategoryUi(if (lang == "vi") "Hàn Quốc" else "Korean", "country", "han-quoc", "https://sofaflix.top/images/chieu-rap.webp", Color(0xFFFB923C)),
            CategoryUi(if (lang == "vi") "Lồng Tiếng" else "Dubbed", "genre", "long-tieng", "https://sofaflix.top/images/long-tieng.webp", Color(0xFFFACC15)),
            CategoryUi(if (lang == "vi") "Thuyết Minh" else "Subbed", "genre", "thuyet-minh", "https://sofaflix.top/images/thuyet-minh.webp", Color(0xFF60A5FA)),
            CategoryUi(if (lang == "vi") "Cổ Trang" else "Historical", "genre", "co-trang", "https://sofaflix.top/images/co-trang.webp", Color(0xFF34D399)),
            CategoryUi(if (lang == "vi") "Kinh Dị" else "Horror", "genre", "kinh-di", "https://sofaflix.top/images/kinh-di.webp", Color(0xFFA78BFA)),
            CategoryUi(if (lang == "vi") "Âu Mỹ" else "Western", "country", "au-my", "https://sofaflix.top/images/dien-anh-au-my.webp", Color(0xFFBEF264)),
            CategoryUi(if (lang == "vi") "Hoạt Hình" else "Animation", "genre", "hoat-hinh", "https://sofaflix.top/images/hoat-hinh.webp", Color(0xFF2DD4BF))
        )
    }
    
    Column(modifier = Modifier.padding(top = 30.dp)) {
        SectionHeader(
            title = Lang.t("explore_categories", lang),
            onSeeAllClick = { onCategoryClick(Lang.t("explore", lang), "list", "") }
        )
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
            .width(140.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090A0F))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Blurred background image
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(2.5f)
                    .blur(20.dp)
            )
            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Illustration Image (bottom-right aligned)
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomEnd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 0.dp)
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.58f)
        )

        // Contents (left/bottom aligned)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon backdrop box
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(item.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    color = item.accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Category Title
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.68f)
            )
        }
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
            val lang = LocalLanguage.current
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
                text = Lang.t("trending", lang),
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
fun MovieRail(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(top = 30.dp)) {
        SectionHeader(title = title, onSeeAllClick = onSeeAllClick)
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
