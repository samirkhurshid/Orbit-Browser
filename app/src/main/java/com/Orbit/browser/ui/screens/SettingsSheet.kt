package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun SettingsSheet(
    viewModel: BrowserViewModel,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val ui     by viewModel.ui.collectAsState()
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1

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
                    onClick           = { viewModel.closeSettings() },
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
                            text       = "⚙️ Settings",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Black,
                            color      = g.txColor,
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { viewModel.closeSettings() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 14.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier            = Modifier.weight(1f),
                    ) {
                        // Search Engine Selector
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f))
                                    .border(1.dp, g.glassBorder2, RoundedCornerShape(18.dp))
                                    .padding(16.dp),
                            ) {
                                Text("Search Engine", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                                Spacer(Modifier.height(10.dp))
                                val engines = listOf("Google", "DuckDuckGo", "Bing", "Brave")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    engines.forEach { engine ->
                                        val selected = ui.defaultSearchEngine == engine
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) a1 else (if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)))
                                                .clickable { viewModel.setSearchEngine(engine) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text       = engine,
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = if (selected) Color.White else g.txColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Clear Browsing Data Item
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f))
                                    .border(1.dp, g.glassBorder2, RoundedCornerShape(18.dp))
                                    .clickable { viewModel.clearBrowsingData() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Clear Browsing Data", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D6D))
                                    Text("Clear history, cache & cookies", fontSize = 11.sp, color = g.tx2Color)
                                }
                                Text("🗑️", fontSize = 18.sp)
                            }
                        }

                        // App Version & Credits
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Orbit Browser v1.0 • Engine 120.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = g.tx2Color)
                                Text("Built with Ultra Glassmorphism & Fluid Motion", fontSize = 10.sp, color = g.tx2Color.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
