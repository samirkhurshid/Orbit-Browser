package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.orbit.browser.data.news.RealNewsArticle
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.OBThemeConfig

@Composable
fun NewsHubScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalOBTheme.current
    val isDark = theme.isDark
    val g = theme.glass
    val a1 = theme.effectiveA1
    val a2 = theme.effectiveA2

    val newsArticles by viewModel.newsArticles.collectAsState()
    val isNewsLoadingMore by viewModel.isNewsLoadingMore.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Tech", "World", "Science", "Business", "Entertainment", "Sports", "Health")

    val filteredArticles = remember(newsArticles, selectedCategory, searchQuery) {
        newsArticles.filter { article ->
            val matchCat = selectedCategory == "All" || article.category.contains(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    article.title.contains(searchQuery, ignoreCase = true) ||
                    article.source.contains(searchQuery, ignoreCase = true) ||
                    article.description.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF06070F) else Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .frostedGlass(isDark = isDark, shape = CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.88f))
                            .clickable { viewModel.closeNewsHub() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "News & Media Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            color = g.txColor
                        )
                        Text(
                            text = "Live feeds from global sources",
                            fontSize = 11.5.sp,
                            color = g.tx2Color
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .frostedGlass(isDark = isDark, shape = CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.88f))
                        .clickable { viewModel.loadMoreNews() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = a1,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Search & Filter Bar ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .frostedGlass(isDark = isDark, shape = RoundedCornerShape(20.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.88f))
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = g.tx2Color,
                        modifier = Modifier.size(18.dp)
                    )
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search news by keyword or source...", fontSize = 13.sp, color = g.tx2Color.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = g.txColor,
                            unfocusedTextColor = g.txColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = g.tx2Color,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Category Filter Chips ───────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                itemsIndexed(categories) { _, cat ->
                    val isSelected = selectedCategory == cat
                    val chipBg = if (isSelected) Brush.linearGradient(listOf(a1, a2))
                    else if (isDark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.06f)))
                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color.White.copy(alpha = 0.85f)))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else g.glassBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else g.txColor
                        )
                    }
                }
            }

            // ── Main News Stream ───────────────────────────────────────────
            if (filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📰", fontSize = 42.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No matching articles found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = g.txColor
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(filteredArticles) { _, news ->
                        FullNewsCard(
                            item = news,
                            theme = theme,
                            onClick = { viewModel.onNewsArticleClick(news) }
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Brush.linearGradient(listOf(a1, a2)))
                                    .clickable { viewModel.loadMoreNews() }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (isNewsLoadingMore) "Fetching fresh feeds..." else "⚡ Load More Articles",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullNewsCard(
    item: RealNewsArticle,
    theme: OBThemeConfig,
    onClick: () -> Unit,
) {
    val g = theme.glass
    val isDark = theme.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(24.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.88f))
            .border(1.dp, g.glassBorder, RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
    ) {
        Column {
            // Hero Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.gradientStart.copy(alpha = 0.25f),
                                item.gradientEnd.copy(alpha = 0.18f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(item.emoji, fontSize = 52.sp)
                }

                // Category & Source Badge Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.emoji} ${item.source} • ${item.category.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Article Content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    color = g.txColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = g.tx2Color,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read Full Story ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.effectiveA1
                    )
                }
            }
        }
    }
}
