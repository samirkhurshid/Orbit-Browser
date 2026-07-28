package com.orbit.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun FindInPageBar(
    viewModel: BrowserViewModel,
    visible: Boolean,
    onFindNext: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui     by viewModel.ui.collectAsState()
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    AnimatedVisibility(
        visible  = visible,
        enter    = com.orbit.browser.ui.animations.OBMotion.panelEnterFromTop,
        exit     = com.orbit.browser.ui.animations.OBMotion.panelExitToTop,
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, g.glassBorder2, RoundedCornerShape(20.dp)),
            color = if (isDark) Color(0xFF0F111E).copy(alpha = 0.95f) else Color(0xFFF0F4FF).copy(alpha = 0.95f),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = g.txColor,
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value         = ui.findInPageQuery,
                    onValueChange = { viewModel.onFindQueryChanged(it) },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = g.txColor),
                    modifier      = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (ui.findInPageQuery.isEmpty()) {
                                Text("Find in page…", fontSize = 14.sp, color = g.tx2Color)
                            }
                            innerTextField()
                        }
                    },
                )

                if (ui.findInPageQuery.isNotEmpty()) {
                    val countText = if (ui.findMatchTotal > 0) "${ui.findMatchCurrent}/${ui.findMatchTotal}" else "0"
                    Text(
                        text = countText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.effectiveA1,
                    )
                }

                // Up Arrow
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { onFindNext(false) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous",
                        tint = g.txColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Down Arrow
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { onFindNext(true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next",
                        tint = g.txColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { viewModel.closeFindInPage() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 13.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
