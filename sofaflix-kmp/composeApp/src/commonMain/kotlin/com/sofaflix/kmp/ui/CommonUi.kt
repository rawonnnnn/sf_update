package com.sofaflix.kmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.Movie

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.painterResource
import sofaflix.composeapp.generated.resources.Res
import sofaflix.composeapp.generated.resources.sofaflix_header_logo

@Composable
fun MovieCard(
    movie: Movie,
    horizontal: Boolean = true,
    onClick: () -> Unit
) {
    val posterHeight = if (horizontal) 198.dp else 180.dp
    val subtitleText = if (movie.episode.isNotBlank()) movie.episode else movie.year.ifBlank { "2026" }

    Column(
        modifier = (if (horizontal) Modifier.width(135.dp).padding(end = 14.dp) else Modifier.padding(bottom = 12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(posterHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF20232D))
        ) {
            AsyncImage(
                model = movie.posterUrl.ifBlank { movie.thumbUrl },
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            if (movie.progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(androidx.compose.ui.Alignment.BottomStart)
                        .background(Color.Gray.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(movie.progress.toFloat())
                            .background(Color(0xFF1CC749))
                    )
                }
            }
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
            text = subtitleText,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF1CC749))
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GlassHeader(
    onLogoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD9191B24),
                        Color(0x4C191B24),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.sofaflix_header_logo),
                contentDescription = "SofaFlix Logo",
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onLogoClick() }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x0AFFFFFF), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0x15FFFFFF), shape = RoundedCornerShape(20.dp))
                        .clickable { onSearchClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x0AFFFFFF), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0x15FFFFFF), shape = RoundedCornerShape(20.dp))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
