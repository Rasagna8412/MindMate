package com.mindmate.core.safety

import java.util.Locale

object SafetyEngine {

    // High risk / Crisis signals (English, Telugu, Hindi, code-switched transliterations)
    private val redKeywords = listOf(
        // English
        "suicide", "kill myself", "end my life", "want to die", "harm myself",
        "cut myself", "self harm", "overdose", "no reason to live", "better off dead",
        "hang myself", "take my life", "end it all", "don't want to live",
        // Hindi / Hinglish
        "marne ka man", "zindagi khatam", "jaan deni hai", "suicide karna",
        "marna chahta", "marna chahti", "mar jana chahta", "mar jana chahti",
        "khatam karna chahta", "khatam karna chahti", "aatmhatya", "jaan de dunga",
        // Telugu / Tenglish
        "chachipovalani", "chavalanipistundi", "life vaddu", "bathakalanipinchatledu",
        "pranam teeskuntanu", "antham cheskovalani", "bathiki waste", "chachi pothanu"
    )

    // Moderate distress / Elevated concern signals
    private val yellowKeywords = listOf(
        // English
        "hopeless", "can't take this anymore", "breaking down", "panic attack",
        "so overwhelmed", "falling apart", "can't breathe", "terrified",
        "nobody cares", "completely empty", "unbearable", "losing my mind",
        // Hindi / Hinglish
        "bohot pareshan", "dimag fat raha", "himmat toot", "ro raha hu",
        "ro rahi hu", "kuch nahi bacha", "dard bardasht nahi", "akela pad gaya",
        // Telugu / Tenglish
        "tattukolekapothunna", "kannellu aagatledu", "bayamesthondi", "chala helpless",
        "okkadinai poyanu", "okkadaanini aipoyanu", "motham aagamai poindi"
    )

    fun evaluate(userText: String): SafetyEvaluation {
        val normalized = userText.lowercase(Locale.ROOT).trim()

        // 1. Immediate RED evaluation
        val matchedRed = redKeywords.filter { keyword ->
            containsPattern(normalized, keyword)
        }

        if (matchedRed.isNotEmpty()) {
            return SafetyEvaluation(
                level = SafetyLevel.RED,
                reason = "Potential crisis or self-harm distress detected.",
                matchedPatterns = matchedRed,
                escalationMessage = "I hear how much pain you're carrying right now, but please know you do not have to carry this alone. I cannot replace human help in moments like this. Please connect right away with a free, confidential counselor at Tele-MANAS (14416) or emergency services (112). People who care are ready to listen."
            )
        }

        // 2. YELLOW evaluation
        val matchedYellow = yellowKeywords.filter { keyword ->
            containsPattern(normalized, keyword)
        }

        if (matchedYellow.isNotEmpty()) {
            return SafetyEvaluation(
                level = SafetyLevel.YELLOW,
                reason = "Elevated emotional distress or acute overwhelm signal.",
                matchedPatterns = matchedYellow,
                escalationMessage = null
            )
        }

        // 3. Default GREEN
        return SafetyEvaluation(
            level = SafetyLevel.GREEN,
            reason = "Conversational wellness and stress management context.",
            matchedPatterns = emptyList(),
            escalationMessage = null
        )
    }

    private fun containsPattern(text: String, pattern: String): Boolean {
        // Match whole word or substring boundary safely
        val regex = Regex("\\b${Regex.escape(pattern)}\\b", RegexOption.IGNORE_CASE)
        return if (pattern.contains(" ")) {
            text.contains(pattern)
        } else {
            regex.containsMatchIn(text) || text.contains(pattern)
        }
    }
}
