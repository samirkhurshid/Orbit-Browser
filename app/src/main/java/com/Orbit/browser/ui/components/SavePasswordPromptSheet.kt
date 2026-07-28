package com.orbit.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.LocalOBTheme

data class PendingSaveCredential(
    val site: String,
    val username: String,
    val password: String,
)

@Composable
fun SavePasswordPromptSheet(
    pendingCredential: PendingSaveCredential?,
    onSave: (PendingSaveCredential) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val a1    = theme.effectiveA1

    AnimatedVisibility(
        visible = pendingCredential != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        if (pendingCredential != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .frostedGlass(isDark = theme.isDark, shape = RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(a1.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = a1, modifier = Modifier.size(24.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Save Password for ${pendingCredential.site}?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = g.txColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Username: ${pendingCredential.username}\nEncrypted (AES-256) & biometric locked",
                        fontSize = 12.sp,
                        color = g.tx2Color,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Not Now", color = g.txColor)
                        }

                        Button(
                            onClick = { onSave(pendingCredential) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = a1),
                        ) {
                            Text("Save", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
