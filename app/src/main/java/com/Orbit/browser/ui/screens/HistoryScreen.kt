package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.orbit.browser.ui.glass.frostedGlass
import coil.compose.SubcomposeAsyncImage
import com.orbit.browser.data.db.HistoryEntry
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.components.FrostedBackButton
import com.orbit.browser.ui.theme.LocalOBTheme
import java.text.SimpleDateFormat
import java.util.*

private data class RealHistoryGroup(
    val label: String,
    val items: List<HistoryEntry>,
)

@Composable
fun HistoryScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val theme        = LocalOBTheme.current
    val g            = theme.glass
    val isDark       = theme.isDark
    val a1           = theme.effectiveA1
    val dbHistory    by viewModel.recentHistory.collectAsState()

    val localView = androidx.compose.ui.platform.LocalView.current
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    LaunchedEffect(activeTabId, dbHistory.size) {
        if (activeTabId.isNotBlank() && localView.width > 0 && localView.height > 0) {
            val provider = com.orbit.browser.browser.preview.ComposePreviewProvider(localView, "History")
            viewModel.previewManager.requestPreview(
                tabId = activeTabId,
                provider = provider,
                policy = com.orbit.browser.browser.preview.SchedulePolicy.Debounced(300L)
            )
        }
    }



    val historyGroups = remember(dbHistory) {
        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86400000L
        val weekStart = todayStart - (6 * 86400000L)

        val todayList = dbHistory.filter { it.visitedAt >= todayStart }
        val yesterdayList = dbHistory.filter { it.visitedAt >= yesterdayStart && it.visitedAt < todayStart }
        val thisWeekList = dbHistory.filter { it.visitedAt >= weekStart && it.visitedAt < yesterdayStart }
        val olderList = dbHistory.filter { it.visitedAt < weekStart }

        buildList {
            if (todayList.isNotEmpty()) add(RealHistoryGroup("Today", todayList))
            if (yesterdayList.isNotEmpty()) add(RealHistoryGroup("Yesterday", yesterdayList))
            if (thisWeekList.isNotEmpty()) add(RealHistoryGroup("This Week", thisWeekList))
            if (olderList.isNotEmpty()) add(RealHistoryGroup("Older", olderList))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FrostedBackButton(
                    onClick = { viewModel.closeHistory() },
                    isDark  = isDark,
                )

                Text(
                    text = "History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = g.txColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Right-side Circular FrostedGlass Search Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp)
                            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = LocalIndication.current,
                                onClick           = { viewModel.openSearch() }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clear All Button
                    if (dbHistory.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFF4D6D).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFF4D6D).copy(alpha = 0.25f), RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = LocalIndication.current,
                                    onClick           = { viewModel.clearHistory() }
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF4D6D)
                            )
                        }
                    }
                }
            }

            // History Content or Empty State
            if (dbHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("🕒", fontSize = 54.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No Browsing History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = g.txColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Webpages you visit will automatically appear here.",
                            fontSize = 12.sp,
                            color = g.tx2Color
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    historyGroups.forEach { group ->
                        item(key = group.label) {
                            Text(
                                text = group.label.uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = a1.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp)
                            )
                        }

                        items(group.items, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.6f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = LocalIndication.current,
                                        onClick           = {
                                            viewModel.closeHistory()
                                            viewModel.navigate(item.url)
                                        }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cleanDomain = remember(item.url) {
                                        try { android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: "" } catch (e: Exception) { "" }
                                    }
                                    val faviconUrl = remember(cleanDomain) {
                                        if (cleanDomain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128" else null
                                    }
                                    val initial = item.title.trim().take(1).uppercase().ifBlank { "🌐" }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 18.dp)
                                            .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (faviconUrl != null) {
                                            SubcomposeAsyncImage(
                                                model              = faviconUrl,
                                                contentDescription = null,
                                                modifier           = Modifier.size(24.dp).clip(CircleShape),
                                                error = { Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = a1) },
                                                loading = { Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = a1) },
                                            )
                                        } else {
                                            Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = a1)
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = g.txColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.url,
                                            fontSize = 10.5.sp,
                                            color = g.tx2Color,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = formatTime(item.visitedAt),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = g.tx3Color
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Red.copy(alpha = 0.08f))
                                            .clickable { viewModel.deleteHistoryEntry(item) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✕", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}
