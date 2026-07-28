package com.orbit.browser.data.news

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NewsPatternLearner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("orbit_news_patterns", Context.MODE_PRIVATE)
    }

    /**
     * Record a direct user click on a news article of a specific category.
     */
    fun recordArticleClick(category: NewsCategory) {
        val currentScore = prefs.getFloat(category.name, 0f)
        prefs.edit().putFloat(category.name, currentScore + 5.0f).apply()
    }

    /**
     * Infer user interest from web navigation (URL or Page Title).
     */
    fun recordUrlVisit(url: String, title: String) {
        val category = inferCategoryFromUrlOrTitle(url, title)
        if (category != NewsCategory.GENERAL) {
            val currentScore = prefs.getFloat(category.name, 0f)
            prefs.edit().putFloat(category.name, currentScore + 1.5f).apply()
        }
    }

    /**
     * Get user score for a given category.
     */
    fun getCategoryScore(category: NewsCategory): Float {
        return prefs.getFloat(category.name, 0f)
    }

    /**
     * Identify the user's top interest category.
     */
    fun getTopCategory(): NewsCategory? {
        var topCategory: NewsCategory? = null
        var maxScore = 0f
        NewsCategory.values().forEach { cat ->
            val score = prefs.getFloat(cat.name, 0f)
            if (score > maxScore) {
                maxScore = score
                topCategory = cat
            }
        }
        return if (maxScore > 2.0f) topCategory else null
    }

    /**
     * Ranks a list of real news articles according to user pattern scores.
     */
    fun rankArticles(articles: List<RealNewsArticle>): List<RealNewsArticle> {
        val topCat = getTopCategory()
        return articles.map { article ->
            val cat = NewsCategory.fromTag(article.category)
            val userScore = getCategoryScore(cat)
            val isTopMatch = topCat != null && cat == topCat
            
            article.copy(
                recommendationScore = userScore + (if (isTopMatch) 10f else 0f),
                isRecommended = isTopMatch || userScore >= 5.0f,
                recommendationReason = if (isTopMatch) "RECOMMENDED FOR YOU" else if (userScore >= 5.0f) "BASED ON YOUR READING" else null
            )
        }.sortedByDescending { it.recommendationScore }
    }

    private fun inferCategoryFromUrlOrTitle(url: String, title: String): NewsCategory {
        val text = "$url $title".lowercase()
        return when {
            text.contains("techcrunch") || text.contains("wired") || text.contains("theverge") ||
                    text.contains("github") || text.contains("android") || text.contains("gadget") -> NewsCategory.TECHNOLOGY

            text.contains("openai") || text.contains("chatgpt") || text.contains("claude") ||
                    text.contains("ai") || text.contains("machine learning") || text.contains("gemini") -> NewsCategory.AI_SCIENCE

            text.contains("cyber") || text.contains("privacy") || text.contains("security") ||
                    text.contains("malware") || text.contains("hacker") || text.contains("vulnerability") -> NewsCategory.SECURITY

            text.contains("bloomberg") || text.contains("reuters") || text.contains("finance") ||
                    text.contains("market") || text.contains("stock") || text.contains("crypto") -> NewsCategory.BUSINESS

            text.contains("bbc") || text.contains("cnn") || text.contains("news") ||
                    text.contains("times") || text.contains("guardian") -> NewsCategory.WORLD

            else -> NewsCategory.GENERAL
        }
    }
}
