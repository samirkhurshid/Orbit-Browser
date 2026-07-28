package com.orbit.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun QrCodeModal(
    viewModel: BrowserViewModel,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val ui        by viewModel.ui.collectAsState()
    val activeTab by viewModel.tabManager.activeTab.collectAsState()
    val theme     = LocalOBTheme.current
    val g         = theme.glass
    val isDark    = theme.isDark

    val pageUrl   = (activeTab?.url?.ifEmpty { "https://google.com" }) ?: "https://google.com"

    AnimatedVisibility(
        visible  = visible,
        enter    = scaleIn(initialScale = 0.88f, animationSpec = tween(380, easing = com.orbit.browser.ui.animations.OBEasing.FigmaSpring)) + fadeIn(tween(280, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase)),
        exit     = scaleOut(targetScale = 0.88f, animationSpec = tween(220, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase)) + fadeOut(tween(180, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = { viewModel.closeQrModal() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, g.glassBorder2, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { /* consume */ },
                color = if (isDark) Color(0xFF0F111E) else Color(0xFFFFFFFF),
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📱 Scan QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { viewModel.closeQrModal() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 12.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // QR Matrix representation
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rows = 12
                            val cols = 12
                            val cellW = size.width / cols
                            val cellH = size.height / rows
                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    val isCorner = (r < 3 && c < 3) || (r < 3 && c > 8) || (r > 8 && c < 3)
                                    val isPixel  = isCorner || ((r * 7 + c * 13 + pageUrl.hashCode()) % 3 == 0)
                                    if (isPixel) {
                                        drawRect(
                                            color   = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(c * cellW, r * cellH),
                                            size    = androidx.compose.ui.geometry.Size(cellW * 0.9f, cellH * 0.9f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text       = pageUrl,
                        fontSize   = 11.sp,
                        color      = g.tx2Color,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
