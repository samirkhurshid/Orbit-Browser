package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.WeatherKind
import com.orbit.browser.ui.WeatherState
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.GREET_EMOJI
import com.orbit.browser.ui.theme.GREET_TEXT
import com.orbit.browser.ui.theme.WEATHER_ICON
import com.orbit.browser.ui.theme.WEATHER_LABEL
import com.orbit.browser.ui.theme.OBThemeConfig
import com.orbit.browser.ui.theme.getTimeSlot
import com.orbit.browser.ui.glass.frostedGlass
import java.util.Calendar

@Composable
fun IncognitoHomeScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val ui            by viewModel.ui.collectAsState()
    val theme         = LocalOBTheme.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entranceAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "incog_entrance",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(entranceAlpha)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 14.dp, bottom = 110.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            IncognitoGreetingSection(
                weather = ui.weatherState,
                theme = theme
            )
            Spacer(Modifier.height(22.dp))

            IncognitoSearchBar(theme = theme, onClick = { viewModel.openSearch() })
            Spacer(Modifier.height(18.dp))

            IncognitoDetailsCard()
        }
    }
}

@Composable
private fun IncognitoGreetingSection(
    weather: WeatherState,
    theme: OBThemeConfig,
) {
    val hour     = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val slot     = remember(hour) { getTimeSlot(hour) }
    val greetText  = GREET_TEXT[slot] ?: "Good night"
    val greetEmoji = GREET_EMOJI[slot] ?: "🌙"

    val weatherIcon  = WEATHER_ICON[weather.kind] ?: "☀️"
    val weatherLabel = WEATHER_LABEL[weather.kind] ?: "Clear"

    val a1 = theme.effectiveA1
    val a2 = theme.effectiveA2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Spacer(Modifier.height(10.dp))

            // Greeting pill
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .frostedGlass(isDark = true, shape = RoundedCornerShape(50.dp), blurRadius = 20.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = if (slot == com.orbit.browser.ui.theme.TimeSlot.Night || slot == com.orbit.browser.ui.theme.TimeSlot.Evening) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text       = greetText,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    color      = Color.White.copy(alpha = 0.65f),
                )
                if (weather.kind != WeatherKind.Clear && !weather.loading) {
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .frostedGlass(isDark = true, shape = RoundedCornerShape(20.dp), blurRadius = 12.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text       = weatherLabel,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Incognito Title
            Text(
                text  = "Incognito",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(a2, a1, Color.White.copy(alpha = 0.92f))
                    )
                ),
                fontSize      = 32.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight    = (32 * 1.1).sp,
            )
        }
    }
}

@Composable
private fun IncognitoSearchBar(theme: OBThemeConfig, onClick: () -> Unit) {
    val a1 = theme.effectiveA1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(54.dp)
            .frostedGlass(isDark = true, shape = RoundedCornerShape(50.dp), blurRadius = 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Incognito",
                tint = a1,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text       = "Search or enter URL…",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White.copy(alpha = 0.55f),
                modifier   = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(theme.effectiveA1, theme.effectiveA2)))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text          = "PRIVATE",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color         = Color.White,
                )
            }
        }
    }
}

@Composable
private fun IncognitoDetailsCard() {
    val violet = Color(0xFFC084FC)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(isDark = true, shape = RoundedCornerShape(24.dp), blurRadius = 24.dp)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "You’ve gone incognito",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Now you can browse privately. Other people who use this device won’t see your activity. However, downloads and bookmarks will still be saved.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Orbit Browser won’t save:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Your browsing history",
                "Cookies and site data",
                "Information entered in forms"
            ).forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", color = violet, fontWeight = FontWeight.Bold)
                    Text(bullet, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your activity might still be visible to:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Websites you visit",
                "Your employer or school",
                "Your internet service provider"
            ).forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", color = violet, fontWeight = FontWeight.Bold)
                    Text(bullet, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}
