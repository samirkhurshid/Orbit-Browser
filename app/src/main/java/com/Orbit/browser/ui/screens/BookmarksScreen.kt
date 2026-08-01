package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.data.db.Bookmark
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.components.FrostedBackButton
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun BookmarksScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val theme        = LocalOBTheme.current
    val g            = theme.glass
    val isDark       = theme.isDark
    val a1           = theme.effectiveA1
    val a2           = theme.effectiveA2
    val dbBookmarks  by viewModel.bookmarkList.collectAsState()

    val localView = androidx.compose.ui.platform.LocalView.current
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    LaunchedEffect(activeTabId, dbBookmarks.size) {
        if (activeTabId.isNotBlank() && localView.width > 0 && localView.height > 0) {
            val provider = com.orbit.browser.browser.preview.ComposePreviewProvider(localView, "Bookmarks")
            viewModel.previewManager.requestPreview(
                tabId = activeTabId,
                provider = provider,
                policy = com.orbit.browser.browser.preview.SchedulePolicy.Debounced(com.orbit.browser.browser.preview.PreviewTimingDefaults.COMPOSE_SETTLE_DELAY_MS)
            )
        }
    }

    var selectedFolder by remember { mutableStateOf("All") }

    val folders = remember(dbBookmarks) {
        val extraFolders = dbBookmarks.map { it.folder }.distinct().filter { it.isNotBlank() && it != "All" }
        listOf("All") + extraFolders
    }

    val filteredList = remember(dbBookmarks, selectedFolder) {
        if (selectedFolder == "All") dbBookmarks
        else dbBookmarks.filter { it.folder == selectedFolder }
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
                    onClick = { viewModel.closeBookmarks() },
                    isDark  = isDark,
                )

                Text(
                    text = "Bookmarks",
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

                    // Add Current Page Bookmark Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp)
                            .background(a1.copy(alpha = 0.18f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = LocalIndication.current,
                                onClick           = { viewModel.addCurrentTabBookmark(if (selectedFolder == "All") "Default" else selectedFolder) }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bookmark",
                            tint = a1,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Folder Filter Pills
            if (folders.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                ) {
                    items(folders) { folder ->
                        val isActive = selectedFolder == folder
                        val count = if (folder == "All") dbBookmarks.size else dbBookmarks.count { it.folder == folder }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isActive) Brush.linearGradient(listOf(a1, a2))
                                    else androidx.compose.ui.graphics.SolidColor(g.glassBg2)
                                )
                                .border(
                                    1.dp,
                                    if (isActive) Color.Transparent else g.glassBorder,
                                    RoundedCornerShape(50)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = { selectedFolder = folder }
                                )
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = folder,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else g.tx2Color
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActive) Color.White.copy(alpha = 0.8f) else g.tx3Color
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Bookmarks List or Empty State
            if (filteredList.isEmpty()) {
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
                        Text("🔖", fontSize = 54.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No Bookmarks Saved",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = g.txColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Bookmark webpages to access them instantly from here.",
                            fontSize = 12.sp,
                            color = g.tx2Color,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.linearGradient(listOf(a1, a2)))
                                .clickable { viewModel.addCurrentTabBookmark() }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "＋ Bookmark Active Webpage",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredList, key = { it.id }) { bm ->
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
                                        viewModel.closeBookmarks()
                                        viewModel.navigate(bm.url)
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Domain initial badge
                                val initial = bm.title.trim().take(1).uppercase().ifBlank { "🌐" }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                                        .border(1.dp, g.glassBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = a1)
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bm.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = g.txColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bm.url,
                                        fontSize = 11.sp,
                                        color = g.tx2Color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                // Folder badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(a1.copy(alpha = 0.12f))
                                        .border(1.dp, a1.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = bm.folder,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = a1
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                // Delete button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Red.copy(alpha = 0.1f))
                                        .clickable { viewModel.deleteBookmark(bm) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("✕", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
