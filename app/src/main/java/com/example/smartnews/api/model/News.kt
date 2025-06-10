package com.example.smartnews.api.model

import com.google.gson.annotations.SerializedName

data class News(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    val url: String,
    val publishedAt: String,
    val content: String?
) {
    fun analyzeMood(): String {
        val text = (content + " " + description).orEmpty().lowercase()
        return when {
            text.containsPositiveKeywords() -> MOOD_HAPPY
            text.containsNegativeKeywords() -> MOOD_SAD
            else -> MOOD_NEUTRAL
        }
    }

    private fun String.containsPositiveKeywords(): Boolean {
        val keywords = listOf(
            "happy", "happiness", "joy", "joyful", "success", "successful", "win", "winner", "growth",
            "profit", "profits", "achievement", "achieve", "breakthrough", "peace", "celebration", "celebrate",
            "victory", "love", "progress", "solution", "positive", "improvement", "benefit", "hope", "inspiration",
            "support", "strong", "stronger", "opportunity", "record high", "rise", "soar", "optimistic", "brilliant",
            "award", "reward", "good news", "milestone", "relief", "recover", "recovery", "secure", "stability"
        )
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.containsNegativeKeywords(): Boolean {
        val keywords = listOf(
            "crisis", "problem", "problems", "conflict", "conflicts", "loss", "losses", "death", "deaths", "attack",
            "attacks", "decline", "declining", "war", "wars", "accident", "accidents", "failure", "fail", "fails",
            "disaster", "catastrophe", "collapse", "fall", "doubt", "negative", "fraud", "scandal", "struggle", "weak",
            "weaker", "drop", "plunge", "fear", "panic", "violence", "threat", "layoff", "unemployment", "bankruptcy",
            "downturn", "recession", "inflation", "tension", "deadlock", "strike", "shutdown", "chaos", "unstable"
        )
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    companion object {
        const val MOOD_HAPPY = "happy"
        const val MOOD_SAD = "sad"
        const val MOOD_NEUTRAL = "neutral"
    }
}