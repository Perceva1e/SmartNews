package com.example.smartnews.api.model

import android.content.Context
import com.example.smartnews.R
import com.google.gson.annotations.SerializedName

data class News(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("content") val content: String?,
    val category: String = "general"
) {
    fun analyzeMood(context: Context): String {
        val text = (content + " " + description).orEmpty().lowercase()
        return when {
            text.containsPositiveKeywords() -> context.getString(R.string.mood_happy)
            text.containsNegativeKeywords() -> context.getString(R.string.mood_sad)
            else -> context.getString(R.string.mood_neutral)
        }
    }

    private fun String.containsPositiveKeywords(): Boolean {
        val keywords = setOf(
            "happy", "happiness", "joy", "joyful", "cheerful", "cheer", "delight", "delightful", "ecstasy", "bliss",
            "euphoria", "glee", "elation", "contentment", "satisfaction", "pleasure", "thrill", "excited", "excitement",
            "enthusiasm", "zest", "radiant", "vibrant", "upbeat", "buoyant", "lighthearted", "jovial", "merry",
            "success", "successful", "win", "winner", "victory", "triumph", "achievement", "achieve", "accomplishment",
            "milestone", "breakthrough", "mastery", "excellence", "outstanding", "remarkable", "stellar", "superb",
            "brilliant", "exceptional", "phenomenal", "award", "reward", "honor", "prestige", "recognition", "laurels",
            "kudos", "applause", "commendation", "praise", "acclaim",
            "growth", "progress", "advancement", "improvement", "development", "innovation", "innovative", "expansion",
            "rise", "soar", "surge", "boost", "upswing", "prosperity", "prosper", "flourish", "thriving", "bloom",
            "boom", "record high", "upward", "escalation", "acceleration", "solution", "resolution", "fix", "remedy",
            "profit", "profits", "gain", "benefit", "wealth", "affluence", "abundance", "riches", "fortune", "luxury",
            "security", "stability", "secure", "safe", "safeguard", "reliable", "sustainable", "viable", "lucrative",
            "bountiful", "plentiful",
            "love", "affection", "adoration", "care", "compassion", "kindness", "empathy", "sympathy", "friendship",
            "camaraderie", "unity", "togetherness", "harmony", "peace", "peaceful", "tranquility", "serenity",
            "cooperation", "collaboration", "support", "encouragement", "inspiration", "inspiring", "motivation",
            "uplifting", "heartwarming", "reassuring", "comforting", "nurturing", "solidarity", "trust", "loyalty",
            "hope", "hopeful", "optimism", "optimistic", "promise", "promising", "bright", "rosy", "encouraging",
            "potential", "opportunity", "possibility", "vision", "dream", "aspiration", "ambition", "confidence",
            "courage", "bravery", "resilience", "perseverance", "determination", "tenacity",
            "celebration", "celebrate", "festivity", "jubilation", "rejoice", "glory", "pride", "proud", "honored",
            "exalted", "gratification", "gratitude", "thankful", "appreciation", "cherish", "revel", "festive",
            "beautiful", "wonderful", "amazing", "awesome", "fantastic", "fabulous", "great", "good", "excellent",
            "perfect", "ideal", "marvelous", "spectacular", "incredible", "empowerment", "liberation", "freedom",
            "fulfillment", "enrichment", "vitality", "energy", "dynamism", "sparkle", "shine", "good news", "blessing"
        )
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.containsNegativeKeywords(): Boolean {
        val keywords = setOf(
            "sad", "sadness", "grief", "sorrow", "misery", "despair", "hopeless", "hopelessness", "depression",
            "gloom", "melancholy", "anguish", "pain", "suffering", "agony", "distress", "anguish", "torment",
            "heartbreak", "heartache", "woe", "mourn", "mourning", "lament", "regret", "remorse", "guilt", "shame",
            "fear", "fearful", "anxiety", "worry", "concern", "dread", "panic", "terror", "horror", "alarm",
            "crisis", "crises", "disaster", "catastrophe", "tragedy", "devastation", "destruction", "ruin", "calamity",
            "havoc", "collapse", "breakdown", "meltdown", "upheaval", "turmoil", "chaos", "disorder", "anarchy",
            "accident", "accidents", "mishap", "misfortune", "cataclysm", "plague", "epidemic", "pandemic",
            "conflict", "conflicts", "war", "wars", "battle", "fight", "fighting", "clash", "confrontation",
            "hostility", "aggression", "violence", "attack", "attacks", "assault", "ambush", "terrorism", "insurgency",
            "rebellion", "revolt", "uprising", "riot", "unrest", "tension", "strife", "feud", "dispute", "quarrel",
            "disagreement", "division", "polarization", "schism",
            "loss", "losses", "death", "deaths", "fatality", "fatalities", "casualty", "casualties", "failure", "fail",
            "fails", "defeat", "setback", "flop", "fiasco", "debacle", "blunder", "mistake", "error", "fault",
            "shortcoming", "weakness", "weak", "weaker", "vulnerability", "fragility",
            "decline", "declining", "drop", "fall", "plunge", "slump", "downturn", "recession", "depression",
            "bankruptcy", "insolvency", "debt", "deficit", "shortage", "scarcity", "poverty", "destitution", "hardship",
            "austerity", "unemployment", "layoff", "jobless", "inflation", "devaluation", "crash", "bust",
            "fraud", "scandal", "corruption", "deception", "betrayal", "dishonesty", "cheating", "misconduct",
            "malpractice", "abuse", "exploitation", "oppression", "injustice", "discrimination", "prejudice", "bias",
            "inequality", "unfairness", "persecution", "harassment", "bullying", "intimidation", "tyranny",
            "struggle", "problem", "problems", "issue", "issues", "challenge", "difficulty", "difficult", "obstacle",
            "hurdle", "barrier", "complication", "adversity", "trouble", "dilemma", "predicament", "plight",
            "hardship", "ordeal", "tribulation", "affliction",
            "instability", "unstable", "uncertainty", "volatility", "risk", "danger", "hazard", "threat", "menace",
            "peril", "jeopardy", "insecurity", "vulnerable", "precarious", "shaky", "unreliable", "doubt", "skepticism",
            "mistrust", "suspicion",
            "disruption", "disturbance", "interruption", "strike", "shutdown", "blockade", "protest", "demonstration",
            "boycott", "resistance", "opposition", "defiance", "noncompliance", "deadlock", "stalemate", "impasse",
            "harm", "damage", "injury", "hurt", "wound", "trauma", "devastate", "ravage", "wreck", "demolish",
            "obliterate", "undermine", "sabotage", "subvert",
            "negative", "bad", "awful", "terrible", "horrible", "dreadful", "appalling", "ghastly", "grim", "bleak",
            "dire", "severe", "harsh", "cruel", "brutal", "savage", "vicious", "heinous", "atrocious", "outrage",
            "shock", "shocking", "alarming", "disturbing", "upsetting", "troubling"
        )
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}