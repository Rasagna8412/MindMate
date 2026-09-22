package com.mindmate.core.ai

import java.util.Locale

enum class DetectedLanguage(val label: String, val code: String) {
    TELUGU("Telugu / Tenglish", "te"),
    HINDI("Hindi / Hinglish", "hi"),
    ENGLISH("English", "en")
}

object CodeSwitchLanguageDetector {

    private val teluguMarkers = listOf(
        "naaku", "chala", "chaala", "undi", "vundi", "undhi", "ga undi",
        "chesthunna", "cheyali", "ardham", "kavatledu", "kastam", "kashtam",
        "avvalekapothunna", "ra", "mari", "em cheyalo", "teliyatledu",
        "enti", "kadha", "gurinchi", "bhayanga", "chaduvu", "chadavalekapothunna",
        "intlo", "clg", "amma", "nanna", "aagam", "koddiga", "cheppandi",
        "mama", "bro", "ela", "unnav", "ela unnav", "bagunnava", "emi",
        "kottustondi", "godava", "cheppu", "ikkada", "akkada", "eeroju",
        "repu", "ippudu", "pedda", "chinna", "kuda", "paduko", "nidra"
    )

    private val hindiMarkers = listOf(
        "mujhe", "bohot", "hai", "raha", "rahi", "yaar", "bhai", "kya",
        "karein", "samajh", "nahi", "aa raha", "hogaya", "ho gaya", "padhai",
        "ghar", "pareshan", "kaise", "kuch", "lag raha", "karna", "baat",
        "kaisa", "kaisa hai", "kya haal", "theek", "mast", "accha", "suno",
        "batao", "bata", "dost", "kamra", "khana", "neend", "gussa", "tension mat"
    )

    fun detect(text: String): DetectedLanguage {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Script checks
        for (char in text) {
            val code = char.code
            if (code in 0x0C00..0x0C7F) return DetectedLanguage.TELUGU
            if (code in 0x0900..0x097F) return DetectedLanguage.HINDI
        }

        // 2. Tokenized marker frequency
        val tokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotBlank() }
        var teluguCount = 0
        var hindiCount = 0

        for (token in tokens) {
            if (teluguMarkers.contains(token)) teluguCount++
            if (hindiMarkers.contains(token)) hindiCount++
        }

        // Also check two-word markers like "ga undi", "aa raha"
        for (marker in teluguMarkers) {
            if (marker.contains(" ") && lower.contains(marker)) teluguCount += 2
        }
        for (marker in hindiMarkers) {
            if (marker.contains(" ") && lower.contains(marker)) hindiCount += 2
        }

        return when {
            teluguCount > 0 && teluguCount >= hindiCount -> DetectedLanguage.TELUGU
            hindiCount > 0 && hindiCount > teluguCount -> DetectedLanguage.HINDI
            else -> DetectedLanguage.ENGLISH
        }
    }
}
