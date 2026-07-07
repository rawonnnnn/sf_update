package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.sofaflix.kmp.Movie
import com.sofaflix.kmp.SofaFlixApi
import com.sofaflix.kmp.LocalLanguage
import com.sofaflix.kmp.Lang
import com.sofaflix.kmp.ui.MovieCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    api: SofaFlixApi,
    initialCategorySlug: String?,
    initialCategoryKind: String?,
    initialCategoryName: String?,
    onMovieClick: (String) -> Unit
) {
    val lang = LocalLanguage.current
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    
    // Filters state
    var selectedType by remember { mutableStateOf("all") } // all, single, series
    var selectedGenre by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }
    
    // Options lists
    var allGenres by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var allCountries by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val yearsList = remember {
        val current = 2026
        (0..11).map { (current - it).toString() }
    }
    
    // UI state
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    
    // Filter collapse/expand state
    var isFiltersExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Handle initial category parameters (when navigated from HomeScreen)
    LaunchedEffect(initialCategorySlug, initialCategoryKind) {
        if (initialCategorySlug != null) {
            query = ""
            debouncedQuery = ""
            isFiltersExpanded = true
            when (initialCategoryKind) {
                "genre" -> {
                    selectedGenre = initialCategorySlug
                    selectedCountry = ""
                    selectedYear = ""
                    selectedType = "all"
                }
                "country" -> {
                    selectedGenre = ""
                    selectedCountry = initialCategorySlug
                    selectedYear = ""
                    selectedType = "all"
                }
                "list" -> {
                    selectedGenre = ""
                    selectedCountry = ""
                    selectedYear = ""
                    selectedType = "all"
                    // If it is list type, slug might be phim-le or phim-bo or phim-hot
                    if (initialCategorySlug == "phim-le") selectedType = "single"
                    else if (initialCategorySlug == "phim-bo") selectedType = "series"
                }
            }
        }
    }
    
    // Fetch genres & countries on mount
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allGenres = api.genres()
                allCountries = api.countries()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Debounce query input
    LaunchedEffect(query) {
        delay(400)
        debouncedQuery = query
    }
    
    // Helper to get genre / country display names
    fun getGenreName(slug: String): String = allGenres.find { it.second == slug }?.first ?: ""
    fun getCountryName(slug: String): String = allCountries.find { it.second == slug }?.first ?: ""
    
    // Search orchestrator function
    fun fetchMovies(pageNum: Int, isRefresh: Boolean) {
        if (loading) return
        loading = true
        scope.launch {
            try {
                var items: List<Movie> = emptyList()
                val limit = 24
                
                if (debouncedQuery.trim().isNotBlank()) {
                    items = api.search(
                        keyword = debouncedQuery.trim(),
                        page = pageNum,
                        limit = limit,
                        genre = selectedGenre,
                        country = selectedCountry,
                        year = selectedYear
                    )
                } else {
                    items = api.discover(
                        type = selectedType,
                        genre = selectedGenre,
                        country = selectedCountry,
                        year = selectedYear,
                        page = pageNum,
                        limit = limit
                    )
                }
                
                // Client-side filtering when searching with filters active
                if (debouncedQuery.trim().isNotBlank()) {
                    var filtered = items
                    if (selectedType == "single") {
                        filtered = items.filter { it.type == "single" || it.type == "movie" || it.type == "phim-le" }
                    } else if (selectedType == "series") {
                        filtered = items.filter { it.type == "series" || it.type == "tv" || it.type == "phim-bo" }
                    }
                    items = filtered
                }
                
                movies = if (isRefresh) items else movies + items
                hasMore = items.isNotEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }
    
    // Trigger list update on filter/query changes
    LaunchedEffect(debouncedQuery, selectedType, selectedGenre, selectedCountry, selectedYear) {
        page = 1
        hasMore = true
        fetchMovies(1, isRefresh = true)
    }
    
    fun loadMore() {
        if (!loading && hasMore) {
            val nextPage = page + 1
            page = nextPage
            fetchMovies(nextPage, isRefresh = false)
        }
    }
    
    // Build path string
    val pathText = remember(lang, selectedType, selectedGenre, selectedCountry, selectedYear, allGenres, allCountries) {
        val parts = mutableListOf("SofaFlix")
        when (selectedType) {
            "single" -> parts.add(Lang.t("movies", lang))
            "series" -> parts.add(Lang.t("series", lang))
            else -> parts.add(if (lang == "vi") "Tất cả" else "All")
        }
        if (selectedGenre.isNotBlank()) parts.add(getGenreName(selectedGenre).ifBlank { selectedGenre })
        if (selectedCountry.isNotBlank()) parts.add(getCountryName(selectedCountry).ifBlank { selectedCountry })
        if (selectedYear.isNotBlank()) parts.add(if (lang == "vi") "Năm $selectedYear" else "Year $selectedYear")
        parts.joinToString("  •  ").uppercase()
    }
    
    val gridState = rememberLazyGridState()
    
    // Infinite scroll detection
    val isAtEnd = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 4
        }
    }
    
    LaunchedEffect(isAtEnd.value) {
        if (isAtEnd.value) {
            loadMore()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191B24))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = Lang.t("search", lang),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Search Input Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF20232D), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.White.copy(alpha = 0.48f),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    fetchMovies(1, true)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.48f)
                                )
                            }
                        }
                    }
                    if (query.isEmpty()) {
                        Text(
                            text = Lang.t("search_placeholder", lang),
                            color = Color.White.copy(alpha = 0.38f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { isFiltersExpanded = !isFiltersExpanded }
                        .padding(top = 22.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = Lang.t("filters", lang),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFiltersExpanded) "▲" else "▼",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                if (isFiltersExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        // 1. Định dạng
                        FilterRow(label = Lang.t("format", lang)) {
                            val types = listOf(
                                "all" to (if (lang == "vi") "Tất cả" else "All"),
                                "single" to Lang.t("movies", lang),
                                "series" to Lang.t("series", lang)
                            )
                            types.forEach { (value, name) ->
                                FilterChip(
                                    label = name,
                                    isActive = selectedType == value,
                                    onClick = { selectedType = value }
                                )
                            }
                        }
                        
                        // 2. Thể loại
                        FilterRow(label = Lang.t("genre_label", lang)) {
                            FilterChip(
                                label = if (lang == "vi") "Tất cả" else "All",
                                isActive = selectedGenre.isEmpty(),
                                onClick = { selectedGenre = "" }
                            )
                            allGenres.forEach { pair ->
                                FilterChip(
                                    label = pair.first,
                                    isActive = selectedGenre == pair.second,
                                    onClick = { selectedGenre = pair.second }
                                )
                            }
                        }

                        // 3. Quốc gia
                        FilterRow(label = Lang.t("country_label", lang)) {
                            FilterChip(
                                label = if (lang == "vi") "Tất cả" else "All",
                                isActive = selectedCountry.isEmpty(),
                                onClick = { selectedCountry = "" }
                            )
                            allCountries.forEach { pair ->
                                FilterChip(
                                    label = pair.first,
                                    isActive = selectedCountry == pair.second,
                                    onClick = { selectedCountry = pair.second }
                                )
                            }
                        }

                        // 4. Năm phát hành
                        FilterRow(label = Lang.t("year_label", lang)) {
                            FilterChip(
                                label = if (lang == "vi") "Tất cả" else "All",
                                isActive = selectedYear.isEmpty(),
                                onClick = { selectedYear = "" }
                            )
                            yearsList.forEach { yr ->
                                FilterChip(
                                    label = yr,
                                    isActive = selectedYear == yr,
                                    onClick = { selectedYear = yr }
                                )
                            }
                        }
                    }
                }
                
                // Active path summary
                Text(
                    text = pathText,
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
            }
            
            // Grid of Movies
            if (movies.isEmpty() && !loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Lang.t("no_movies_found", lang),
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(movies.size) { index ->
                        val movie = movies[index]
                        MovieGridItem(movie = movie, onClick = { onMovieClick(movie.slug) })
                    }
                    
                    if (loading) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(84.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun FilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFF1CC749).copy(alpha = 0.08f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF1CC749).copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable {
                focusManager.clearFocus()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFF1CC749) else Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun MovieGridItem(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            text = movie.year.ifBlank { "2026" },
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 11.sp
        )
    }
}
