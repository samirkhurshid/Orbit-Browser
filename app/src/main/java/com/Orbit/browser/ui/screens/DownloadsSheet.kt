package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun DownloadsSheet(
    viewModel: BrowserViewModel,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1

    val dbDownloads by viewModel.downloadsList.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter   = com.orbit.browser.ui.animations.OBMotion.sheetEnterFromBottom,
        exit    = com.orbit.browser.ui.animations.OBMotion.sheetExitToBottom,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = { viewModel.closeDownloads() },
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { /* consume */ },
                color = if (isDark) Color(0xFF0D0F1D) else Color(0xFFF2F6FE),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    // Grab handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)),
                    )

                    Spacer(Modifier.height(16.dp))

                    // Title Bar
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text       = "Downloads",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Black,
                            color      = g.txColor,
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { viewModel.closeDownloads() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 14.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (dbDownloads.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No downloads yet", fontSize = 14.sp, color = g.tx2Color)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier            = Modifier.weight(1f),
                        ) {
                            items(dbDownloads) { dl ->
                                com.orbit.browser.ui.screens.RedesignedDownloadTile(
                                    download = dl,
                                    viewModel = viewModel,
                                    theme = theme
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    return when {
        gb >= 1f -> String.format("%.1f GB", gb)
        mb >= 1f -> String.format("%.1f MB", mb)
        kb >= 1f -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
