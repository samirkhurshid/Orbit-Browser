package com.orbit.browser.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbit.browser.ui.WeatherKind
import com.orbit.browser.ui.theme.TimeSlot

@Composable
fun WeatherConditionIcon(
    kind: WeatherKind,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val (icon, tint) = when (kind) {
        WeatherKind.Clear -> Icons.Default.WbSunny to Color(0xFFF59E0B) // Amber Gold
        WeatherKind.Cloudy -> Icons.Default.Cloud to (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
        WeatherKind.Fog -> Icons.Default.Grain to Color(0xFF94A3B8)
        WeatherKind.Drizzle -> Icons.Default.WaterDrop to Color(0xFF38BDF8) // Light Blue
        WeatherKind.Rain -> Icons.Default.Thunderstorm to Color(0xFF0284C7) // Sky Blue
        WeatherKind.Thunderstorm -> Icons.Default.FlashOn to Color(0xFFEAB308) // Electric Yellow
        WeatherKind.Snow -> Icons.Default.AcUnit to Color(0xFF7DD3FC) // Snow Cyan
    }

    Icon(
        imageVector = icon,
        contentDescription = kind.label,
        tint = tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun TimeOfDayIcon(
    slot: TimeSlot,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val (icon, tint) = when (slot) {
        TimeSlot.Dawn -> Icons.Default.WbTwilight to Color(0xFFF59E0B)
        TimeSlot.Morning, TimeSlot.Noon -> Icons.Default.WbSunny to Color(0xFFF59E0B)
        TimeSlot.Afternoon -> Icons.Default.WbSunny to Color(0xFFF97316)
        TimeSlot.Evening -> Icons.Default.WbTwilight to Color(0xFFA855F7)
        TimeSlot.Night -> Icons.Default.NightsStay to Color(0xFF818CF8)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}
