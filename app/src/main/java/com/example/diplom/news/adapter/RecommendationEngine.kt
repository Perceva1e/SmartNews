package com.example.diplom.news.adapter

import com.example.diplom.api.model.News
import com.example.diplom.database.entity.SavedNews
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ln

class RecommendationEngine {
    private val stemmer = PorterStemmer()
    private val stopWords = setOf("the", "and", "or", "a", "an", "in", "on", "at", "to", "of", "for")
    private var recommendationsCache: RecommendationCache? = null

    fun extractKeywords(newsList: List<SavedNews>): List<String> {
        val documents = newsList.map { "${it.title} ${it.content}" }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() } ?: return emptyList()

        val processedDocs = documents.map { preprocessText(it) }
        val tokenizedDocs = processedDocs.map { tokenizeAndStem(it) }
        val tfidfScores = calculateTFIDF(tokenizedDocs)

        return tfidfScores.entries
            .filter { it.value >= 0.15 && !stopWords.contains(it.key) }
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    private fun preprocessText(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-zа-яё ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenizeAndStem(text: String): List<String> {
        return text.split(" ")
            .map { stemmer.stem(it) }
            .filter { it.length > 2 && !stopWords.contains(it) }
    }

    private fun calculateTFIDF(documents: List<List<String>>): Map<String, Double> {
        val tf = mutableMapOf<String, MutableMap<String, Int>>()
        val df = mutableMapOf<String, Int>()
        val totalDocs = documents.size

        // Calculate Term Frequency (TF)
        documents.forEachIndexed { docId, terms ->
            val termCount = mutableMapOf<String, Int>()
            terms.forEach { term ->
                termCount[term] = termCount.getOrDefault(term, 0) + 1
            }
            tf[docId.toString()] = termCount

            terms.toSet().forEach { term ->
                df[term] = df.getOrDefault(term, 0) + 1
            }
        }

        // Calculate TF-IDF scores
        return df.keys.associateWith { term ->
            val termFrequency = tf.values.sumOf { it.getOrDefault(term, 0).toDouble() }
            val docFrequency = df[term]?.toDouble() ?: 0.0
            val idf = ln((totalDocs + 1) / (docFrequency + 1)) + 1
            termFrequency * idf
        }
    }

    fun recommendNews(articles: List<News>, keywords: List<String>): List<News> {
        val now = System.currentTimeMillis()
        return articles.distinctBy { it.url }
            .sortedByDescending { article ->
                val titleScore = calculateRelevance(article.title ?: "", keywords)
                val contentScore = calculateRelevance(article.description ?: "", keywords)
                val freshness = 1 - (now - parseDate(article.publishedAt)) / 3_600_000.0

                0.4 * titleScore + 0.3 * contentScore + 0.3 * freshness
            }
            .take(10)
    }

    private fun calculateRelevance(text: String, keywords: List<String>): Double {
        val processed = preprocessText(text)
        val terms = tokenizeAndStem(processed)
        return keywords.sumOf { keyword ->
            terms.count { it == keyword }.toDouble()
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun updateCache(data: List<News>) {
        recommendationsCache = RecommendationCache(
            data = data,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getCachedRecommendations(): List<News>? {
        return recommendationsCache?.takeIf {
            System.currentTimeMillis() - it.timestamp < 600_000 // 10 минут
        }?.data
    }

    private data class RecommendationCache(
        val data: List<News>,
        val timestamp: Long
    )
}

// Porter Stemmer implementation (часть класса)
private class PorterStemmer {
    fun stem(term: String): String {
        var stem = term
        if (stem.length > 3) {
            stem = removeSuffixes(stem)
        }
        return stem
    }
}

private fun removeSuffixes(word: String): String {
    val suffixes = listOf("ing", "ed", "ly", "es", "s")
    var result = word
    for (suffix in suffixes) {
        if (result.endsWith(suffix)) {
            result = result.substring(0, result.length - suffix.length)
            break
        }
    }
    return result
}