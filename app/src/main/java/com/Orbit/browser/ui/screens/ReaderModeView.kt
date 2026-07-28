package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun ReaderModeView(
    viewModel: BrowserViewModel,
    visible: Boolean,
    title: String,
    byline: String,
    contentHtml: String,
    modifier: Modifier = Modifier,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    var fontSize by remember { mutableStateOf(16.sp) }
    var useSerif  by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f))) + fadeIn(tween(220)),
        exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f))) + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = if (isDark) Color(0xFF10121A) else Color(0xFFFAF7F0),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                // Header Bar
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Reader", fontSize = 18.sp, fontWeight = FontWeight.Black, color = g.txColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { useSerif = !useSerif }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(if (useSerif) "Serif" else "Sans", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { viewModel.closeReaderMode() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 14.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Scrollable Article Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text       = title.ifBlank { "Article" },
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.Default,
                        color      = g.txColor,
                    )

                    if (byline.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text       = byline,
                            fontSize   = 12.sp,
                            color      = g.tx2Color,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    val cleanText = remember(contentHtml) {
                        contentHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
                    }

                    Text(
                        text       = if (cleanText.isBlank()) "Clean article content extracted from page." else cleanText,
                        fontSize   = fontSize,
                        lineHeight = (fontSize.value * 1.55f).sp,
                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.Default,
                        color      = g.txColor,
                    )
                }
            }
        }
    }
}
