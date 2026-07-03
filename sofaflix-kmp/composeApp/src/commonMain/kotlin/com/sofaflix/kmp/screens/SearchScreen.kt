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
    
    // Dialog / Picker state
    var activePickerType by remember { mutableStateOf<String?>(null) } // type, genre, country, year
    
    val scope = rememberCoroutineScope()
    
    // Handle initial category parameters (when navigated from HomeScreen)
    LaunchedEffect(initialCategorySlug, initialCategoryKind) {
        if (initialCategorySlug != null) {
            query = ""
            debouncedQuery = ""
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
                    items = api.search(debouncedQuery.trim(), pageNum, limit)
                } else {
                    if (selectedType == "single") {
                        items = api.listByType("phim-le", pageNum, limit)
                    } else if (selectedType == "series") {
                        items = api.listByType("phim-bo", pageNum, limit)
                    } else {
                        if (selectedGenre.isNotBlank()) {
                            items = api.genre(selectedGenre, pageNum, limit)
                        } else if (selectedCountry.isNotBlank()) {
                            items = api.country(selectedCountry, pageNum, limit)
                        } else if (selectedYear.isNotBlank()) {
                            items = api.year(selectedYear, pageNum, limit)
                        } else {
                            items = api.latestV3(pageNum, limit, mapOf("sort_field" to "view", "sort_type" to "desc"))
                        }
                    }
                }
                
                // Client-side filtering when searching with filters active
                if (debouncedQuery.trim().isNotBlank()) {
                    var filtered = items
                    if (selectedType == "single") {
                        // In v3, single/series type check can be inferred or checked client side
                    } else if (selectedType == "series") {
                        // In v3, series check
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
    val pathText = remember(selectedType, selectedGenre, selectedCountry, selectedYear, allGenres, allCountries) {
        val parts = mutableListOf("SofaFlix")
        when (selectedType) {
            "single" -> parts.add("Phim lẻ")
            "series" -> parts.add("Phim bộ")
            else -> parts.add("Tất cả")
        }
        if (selectedGenre.isNotBlank()) parts.add(getGenreName(selectedGenre).ifBlank { selectedGenre })
        if (selectedCountry.isNotBlank()) parts.add(getCountryName(selectedCountry).ifBlank { selectedCountry })
        if (selectedYear.isNotBlank()) parts.add("Năm $selectedYear")
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
                    text = "Tìm kiếm",
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
                                onSearch = { fetchMovies(1, true) }
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
                            text = "Tìm tên phim, đạo diễn, diễn viên...",
                            color = Color.White.copy(alpha = 0.38f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
                
                Text(
                    text = "Khám phá",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                )
                
                // Filter pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = when (selectedType) {
                            "single" -> "Phim lẻ"
                            "series" -> "Phim bộ"
                            else -> "Định dạng"
                        },
                        onClick = { activePickerType = "type" }
                    )
                    FilterPill(
                        label = if (selectedGenre.isNotBlank()) getGenreName(selectedGenre) else "Thể loại",
                        onClick = { activePickerType = "genre" }
                    )
                    FilterPill(
                        label = if (selectedCountry.isNotBlank()) getCountryName(selectedCountry) else "Quốc gia",
                        onClick = { activePickerType = "country" }
                    )
                    FilterPill(
                        label = if (selectedYear.isNotBlank()) "Năm $selectedYear" else "Năm",
                        onClick = { activePickerType = "year" }
                    )
                }
                
                // Active path summary
                Text(
                    text = pathText,
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
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
                            text = "Không tìm thấy phim phù hợp.",
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
        
        // Custom Dialog Modal Pickers
        activePickerType?.let { pickerType ->
            val title = when (pickerType) {
                "type" -> "Định dạng phim"
                "genre" -> "Chọn thể loại"
                "country" -> "Chọn quốc gia"
                else -> "Chọn năm phát hành"
            }
            
            val options = when (pickerType) {
                "type" -> listOf("all" to "Tất cả định dạng", "single" to "Phim lẻ (Movies)", "series" to "Phim bộ (Series)")
                "genre" -> listOf("" to "Tất cả thể loại") + allGenres.map { it.second to it.first }
                "country" -> listOf("" to "Tất cả quốc gia") + allCountries.map { it.second to it.first }
                else -> listOf("" to "Tất cả các năm") + yearsList.map { it to "Năm $it" }
            }
            
            val selectedValue = when (pickerType) {
                "type" -> selectedType
                "genre" -> selectedGenre
                "country" -> selectedCountry
                else -> selectedYear
            }
            
            val onSelect: (String) -> Unit = { valSelected ->
                when (pickerType) {
                    "type" -> selectedType = valSelected
                    "genre" -> selectedGenre = valSelected
                    "country" -> selectedCountry = valSelected
                    else -> selectedYear = valSelected
                }
                activePickerType = null
            }
            
            PickerDialog(
                title = title,
                options = options,
                selectedValue = selectedValue,
                onDismiss = { activePickerType = null },
                onSelect = onSelect
            )
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF20232D))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "▾",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
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

@Composable
fun PickerDialog(
    title: String,
    options: List<Pair<String, String>>, // value to label
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191B24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.08f))
                
                // Options list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    options.forEach { (value, label) ->
                        val isSelected = selectedValue == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFF1CC749).copy(alpha = 0.06f) else Color.Transparent)
                                .clickable { onSelect(value) }
                                .padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.68f),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(0xFF1CC749), RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
