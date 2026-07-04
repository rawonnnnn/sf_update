package com.sofaflix.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sofaflix.kmp.screens.*
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.request.ImageRequest

enum class Screen {
    Home, Search, Library, Profile
}

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedMovieSlug by remember { mutableStateOf<String?>(null) }
    val api = remember { SofaFlixApi() }

    var searchCategorySlug by remember { mutableStateOf<String?>(null) }
    var searchCategoryKind by remember { mutableStateOf<String?>(null) }
    var searchCategoryName by remember { mutableStateOf<String?>(null) }

    var token by remember { mutableStateOf("") }
    var userProfileName by remember { mutableStateOf("") }

    var activeWatchMovie by remember { mutableStateOf<Movie?>(null) }
    var activeWatchEpisode by remember { mutableStateOf<Episode?>(null) }
    var activeWatchDetail by remember { mutableStateOf<MovieDetail?>(null) }

    var currentLanguage by remember { mutableStateOf("vi") }

    LaunchedEffect(Unit) {
        token = AppPreferences.getString("sf:token", "")
        userProfileName = AppPreferences.getString("sf:user_name", "")
        api.token = token
        currentLanguage = AppPreferences.getString("sf:language", "vi")
    }

    fun handleLoginSuccess(newToken: String, newName: String) {
        token = newToken
        userProfileName = newName
        api.token = newToken
        AppPreferences.putString("sf:token", newToken)
        AppPreferences.putString("sf:user_name", newName)
        currentScreen = Screen.Profile
    }

    fun handleLogout() {
        token = ""
        userProfileName = ""
        api.token = ""
        AppPreferences.putString("sf:token", "")
        AppPreferences.putString("sf:user_name", "")
        currentScreen = Screen.Profile
    }

    val appColors = remember { AppColors() }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalLanguage provides currentLanguage
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = appColors.primary,
                background = appColors.background,
                surface = appColors.surface,
                onBackground = appColors.textPrimary,
                onSurface = appColors.textPrimary
            )
        ) {
        val uriHandler = LocalUriHandler.current

        if (activeWatchMovie != null && activeWatchEpisode != null && activeWatchDetail != null) {
            BackHandler {
                activeWatchMovie = null
                activeWatchEpisode = null
                activeWatchDetail = null
            }
        } else if (selectedMovieSlug != null) {
            BackHandler {
                selectedMovieSlug = null
            }
        } else if (currentScreen != Screen.Home) {
            BackHandler {
                currentScreen = Screen.Home
                searchCategorySlug = null
                searchCategoryKind = null
                searchCategoryName = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (activeWatchMovie != null && activeWatchEpisode != null && activeWatchDetail != null) {
                    WatchScreen(
                        api = api,
                        token = token,
                        movie = activeWatchMovie!!,
                        initialEpisode = activeWatchEpisode!!,
                        movieDetail = activeWatchDetail!!,
                        onBackClick = {
                            activeWatchMovie = null
                            activeWatchEpisode = null
                            activeWatchDetail = null
                        }
                    )
                } else if (selectedMovieSlug != null) {
                    DetailScreen(
                        api = api,
                        token = token,
                        movieSlug = selectedMovieSlug!!,
                        onBackClick = { selectedMovieSlug = null },
                        onPlayClick = { movie, ep, detailData ->
                            activeWatchMovie = movie
                            activeWatchEpisode = ep
                            activeWatchDetail = detailData
                        }
                    )
                } else {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            api = api,
                            onMovieClick = { selectedMovieSlug = it },
                            onCategoryClick = { name, kind, slug ->
                                searchCategoryName = name
                                searchCategoryKind = kind
                                searchCategorySlug = slug
                                currentScreen = Screen.Search
                            }
                        )
                        Screen.Search -> SearchScreen(
                            api = api,
                            initialCategorySlug = searchCategorySlug,
                            initialCategoryKind = searchCategoryKind,
                            initialCategoryName = searchCategoryName,
                            onMovieClick = { selectedMovieSlug = it }
                        )
                        Screen.Library -> LibraryScreen(
                            api = api,
                            token = token,
                            onMovieClick = { selectedMovieSlug = it }
                        )
                        Screen.Profile -> ProfileScreen(
                            api = api,
                            token = token,
                            userProfileName = userProfileName,
                            currentLanguage = currentLanguage,
                            onLanguageChange = { lang ->
                                currentLanguage = lang
                                AppPreferences.putString("sf:language", lang)
                            },
                            onLoginSuccess = { t, n -> handleLoginSuccess(t, n) },
                            onLogout = { handleLogout() }
                        )
                    }
                }
            }

            // Floating Bottom Navigation
            if (selectedMovieSlug == null && activeWatchMovie == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    FloatingBottomNavigation(
                        currentScreen = currentScreen,
                        onNavigate = { screen ->
                            currentScreen = screen
                            selectedMovieSlug = null
                            searchCategorySlug = null
                            searchCategoryKind = null
                            searchCategoryName = null
                        }
                    )
                }
            }

            // Glass Header (only on Home screen)
            if (currentScreen == Screen.Home && selectedMovieSlug == null && activeWatchMovie == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                ) {
                    com.sofaflix.kmp.ui.GlassHeader(
                        onLogoClick = {
                            selectedMovieSlug = null
                        },
                        onSearchClick = {
                            currentScreen = Screen.Search
                        },
                        onProfileClick = {
                            currentScreen = Screen.Profile
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    color = Color(0xEC0A0C12),
                    shape = RoundedCornerShape(36.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x1FFFFFFF),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val lang = LocalLanguage.current
            val items = listOf(
                Triple(Lang.t("home", lang), Screen.Home, Icons.Default.Home),
                Triple(Lang.t("search", lang), Screen.Search, Icons.Default.Search),
                Triple(Lang.t("library", lang), Screen.Library, Icons.Default.List),
                Triple(Lang.t("profile", lang), Screen.Profile, Icons.Default.Person)
            )

            items.forEach { (label, screen, icon) ->
                val selected = currentScreen == screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(27.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color(0xFF111318) else Color(0x94FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = if (selected) Color(0xFF111318) else Color(0x94FFFFFF),
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
