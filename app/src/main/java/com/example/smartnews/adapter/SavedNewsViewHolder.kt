package com.example.smartnews.adapter

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartnews.R
import com.example.smartnews.bd.DatabaseHelper
import com.example.smartnews.bd.SavedNews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun SavedNews.analyzeMood(context: android.content.Context): String {
    val text = (title + " " + description).orEmpty().lowercase()
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

class SavedNewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    private val tvSource: TextView = itemView.findViewById(R.id.tvSource)
    private val tvMood: TextView = itemView.findViewById(R.id.tvMood)
    private val ivNewsImage: ImageView = itemView.findViewById(R.id.ivNewsImage)
    private val ivShare: ImageView = itemView.findViewById(R.id.ivShare)
    private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

    fun bind(email: String, savedNews: SavedNews, listener: SavedNewsAdapter.OnNewsDeletedListener?) {
        tvTitle.text = savedNews.title ?: "No title"
        tvDescription.text = savedNews.description ?: "No description"
        tvDate.text = savedNews.publishedAt?.substring(0, 10) ?: "No date"
        val sourceText = savedNews.url?.substringAfter("://")?.substringBefore("/") ?: "Unknown source"
        tvSource.text = sourceText
        tvSource.setOnClickListener {
            savedNews.url?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                itemView.context.startActivity(intent)
            }
        }

        val mood = savedNews.analyzeMood(itemView.context)
        tvMood.text = "${itemView.context.getString(R.string.mood)} $mood"
        val (moodDrawableRes, moodContentDescRes) = when (mood) {
            itemView.context.getString(R.string.mood_happy) -> R.drawable.happy to R.string.mood_happy_desc
            itemView.context.getString(R.string.mood_sad) -> R.drawable.sad to R.string.mood_sad_desc
            else -> R.drawable.neutral to R.string.mood_neutral_desc
        }
        val drawable = ResourcesCompat.getDrawable(itemView.context.resources, moodDrawableRes, null)
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
            DrawableCompat.setTint(wrappedDrawable, android.graphics.Color.MAGENTA)
            wrappedDrawable.setBounds(0, 0, 56, 56)
            tvMood.setCompoundDrawables(wrappedDrawable, null, null, null)
        }
        tvMood.contentDescription = itemView.context.getString(moodContentDescRes)

        if (savedNews.urlToImage != null) {
            Glide.with(itemView.context)
                .load(savedNews.urlToImage)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_close_clear_cancel)
                .into(ivNewsImage)
        } else {
            ivNewsImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        ivShare.setOnClickListener {
            savedNews.url?.let { url ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, savedNews.title ?: "News Article")
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                itemView.context.startActivity(Intent.createChooser(shareIntent, "Share news via"))
            }
        }

        ivDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val dbHelper = DatabaseHelper(itemView.context)
                val result = dbHelper.deleteNews(email, savedNews.id)
                if (result > 0) {
                    withContext(Dispatchers.Main) {
                        showCustomDialog(
                            title = itemView.context.getString(R.string.success_title),
                            message = itemView.context.getString(R.string.news_deleted_success),
                            layoutResId = R.layout.custom_dialog_success
                        )
                        listener?.onNewsDeleted()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showCustomDialog(
                            title = itemView.context.getString(R.string.error_title),
                            message = itemView.context.getString(R.string.news_delete_failed),
                            layoutResId = R.layout.custom_dialog_error
                        )
                    }
                }
            }
        }
    }

    private fun showCustomDialog(title: String, message: String, layoutResId: Int) {
        val dialogView = LayoutInflater.from(itemView.context).inflate(layoutResId, null)
        val dialogBuilder = AlertDialog.Builder(itemView.context)
            .setView(dialogView)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.tvMessage)?.text = title
        dialogView.findViewById<TextView>(R.id.tvDescription)?.text = message
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}