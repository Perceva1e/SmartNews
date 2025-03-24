package com.example.diplom.news.adapter

import android.util.Log
import com.example.diplom.api.model.News
import com.example.diplom.database.entity.SavedNews
import kotlin.math.sqrt
import java.util.*
import kotlin.math.ln

class RecommendationEngine {
    private var userProfile: Map<String, Double> = emptyMap()
    private val vectorCache = mutableMapOf<String, Map<String, Double>>()
    private val stemmer = PorterStemmer()


    private fun precomputeVectors(newsList: List<News>) {
        newsList.forEach { news ->
            if (!vectorCache.containsKey(news.url)) {
                val text = news.title + " " + (news.description ?: "") + " " + (news.content ?: "")
                vectorCache[news.url] = createTfIdfVector(text)
            }
        }
    }


    private class PorterStemmer {
        fun stem(word: String): String {
            var result = word
                .removeSuffix("'s")
                .removeSuffix("s")
                .removeSuffix("ing")
                .removeSuffix("ed")
                .removeSuffix("ly")
                .removeSuffix("er")
                .removeSuffix("est")
                .removeSuffix("tion")
                .removeSuffix("ness")
                .removeSuffix("ment")

            result = when {
                result.endsWith("ied") -> result.replace("ied", "y")
                result.endsWith("ies") -> result.replace("ies", "y")
                result.endsWith("ful") -> result.removeSuffix("ful")
                result.endsWith("ize") -> result.removeSuffix("ize")
                else -> result
            }

            return result.lowercase()
        }
    }

    fun updateUserProfile(savedNews: List<SavedNews>) {
        val allTexts = savedNews.joinToString(" ") { "${it.title} ${it.content}" }
        userProfile = createTfIdfVector(allTexts)
        Log.d("RecommendEngine", "User profile updated with ${savedNews.size} items")
    }

    fun extractKeywords(savedNews: List<SavedNews>, topN: Int = 5): List<String> {
        val allText = savedNews.joinToString(" ") {
            "${it.title} ${it.content}".trim().ifEmpty { "<empty>" }
        }

        Log.d("TextProcessing", "Raw text: ${allText.take(500)}")

        val tfidf = createTfIdfVector(allText)
        Log.d("Keywords", "TF-IDF entries: ${tfidf.entries.take(10)}")

        return tfidf.entries
            .sortedByDescending { it.value }
            .take(topN * 5) // Увеличили охват
            .map { it.key }
            .filter { it.length > 3 } // Фильтр коротких слов
            .take(topN)
            .also {
                Log.d("Keywords", "Final keywords: $it")
            }
    }

    private fun createTfIdfVector(text: String): Map<String, Double> {
        val words = preprocessText(text)
        if (words.size < 5) {
            Log.w("TextProcessing", "Insufficient words: ${words.size}")
            return emptyMap()
        }

        val tf = calculateTf(words)
        val idf = calculateIdf(tf)

        return tf.mapValues { (term, tfValue) ->
            tfValue * (idf[term] ?: 0.0)
        }.filterValues { it > 0.0001 }
    }

    private fun preprocessText(text: String): List<String> {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-zа-я ]"), " ")
            .split(" ")
            .filter {
                it.length in 3..20 &&
                        it !in stopWords &&
                        !it.matches(Regex("\\d+"))
            }
            .map { stemmer.stem(it) }
            .filter { it.isNotBlank() }
            .also {
                Log.d("TextProcessing", "After stemming: $it")
            }
    }


    private fun calculateTf(words: List<String>): Map<String, Double> {
        val total = words.size.toDouble()
        return words.groupingBy { it }.eachCount()
            .mapValues { it.value.toDouble() / total }
    }

    private fun calculateIdf(tf: Map<String, Double>): Map<String, Double> {
        val totalDocs = vectorCache.size + 1
        return tf.mapValues { (term, _) ->
            val docsWithTerm = vectorCache.values.count { it.containsKey(term) }
            val idf = ln(totalDocs.toDouble() / (docsWithTerm + 1))
            Log.d("TFIDF", "Term: $term, IDF: $idf")
            idf
        }
    }

    fun recommendNews(allNews: List<News>): List<News> {
        precomputeVectors(allNews)

        return allNews.mapNotNull { news ->
            val newsVector = vectorCache[news.url] ?: return@mapNotNull null
            val similarity = cosineSimilarity(userProfile, newsVector)
            news to similarity
        }.sortedByDescending { it.second }
            .filter { it.second > 0.1 }
            .map { it.first }
    }

    private fun cosineSimilarity(vec1: Map<String, Double>, vec2: Map<String, Double>): Double {
        val terms = (vec1.keys + vec2.keys).toSet()
        var dot = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        terms.forEach { term ->
            val v1 = vec1[term] ?: 0.0
            val v2 = vec2[term] ?: 0.0
            dot += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }

        return if (norm1 == 0.0 || norm2 == 0.0) 0.0
        else dot / (sqrt(norm1) * sqrt(norm2))
    }

    companion object {
        private val stopWords = setOf(
            "a", "an", "the", "in", "on", "at", "to", "for", "of", "and", "or", "but",
            "is", "are", "was", "were", "this", "that", "it", "as", "with", "by", "from"
        )
    }
}