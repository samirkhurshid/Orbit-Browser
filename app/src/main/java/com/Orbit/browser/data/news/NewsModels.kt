package com.orbit.browser.data.news

import androidx.compose.ui.graphics.Color

data class RealNewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val imageUrl: String? = null,
    val category: String,
    val pubDateMillis: Long = System.currentTimeMillis(),
    val isRecommended: Boolean = false,
    val recommendationReason: String? = null,
    val recommendationScore: Float = 0f,
    val gradientStart: Color = Color(0xFF1A6FFF),
    val gradientEnd: Color = Color(0xFF7C3AED),
    val emoji: String = "📰"
)

enum class NewsCategory(val displayName: String, val emoji: String, val startColor: Color, val endColor: Color) {
    TECHNOLOGY("Technology", "📱", Color(0xFF1A6FFF), Color(0xFF7C3AED)),
    AI_SCIENCE("AI & Science", "🤖", Color(0xFFF97316), Color(0xFFDC2626)),
    SECURITY("Security & Privacy", "🛡️", Color(0xFF00DDA0), Color(0xFF0891B2)),
    BUSINESS("Business & Economy", "📈", Color(0xFF10B981), Color(0xFF059669)),
    WORLD("World News", "🌐", Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    ENTERTAINMENT("Entertainment", "🎬", Color(0xFFA855F7), Color(0xFF7E22CE)),
    GENERAL("General", "📰", Color(0xFF6B7280), Color(0xFF374151));

    companion object {
        fun fromTag(tag: String): NewsCategory {
            val lower = tag.lowercase()
            return when {
                lower.contains("tech") || lower.contains("mobile") || lower.contains("software") -> TECHNOLOGY
                lower.contains("ai") || lower.contains("science") || lower.contains("robot") -> AI_SCIENCE
                lower.contains("security") || lower.contains("privacy") || lower.contains("crypto") || lower.contains("hack") -> SECURITY
                lower.contains("business") || lower.contains("econ") || lower.contains("market") || lower.contains("finance") -> BUSINESS
                lower.contains("world") || lower.contains("global") || lower.contains("nation") -> WORLD
                lower.contains("film") || lower.contains("movie") || lower.contains("music") || lower.contains("entertainment") -> ENTERTAINMENT
                else -> GENERAL
            }
        }
    }
}
