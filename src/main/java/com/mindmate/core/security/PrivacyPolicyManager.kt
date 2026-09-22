package com.mindmate.core.security

import com.mindmate.domain.model.JournalEntry
import com.mindmate.domain.model.Message
import com.mindmate.domain.model.MoodEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrivacyPolicyManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun buildExportMarkdown(
        messages: List<Message>,
        moods: List<MoodEntry>,
        journals: List<JournalEntry>
    ): String {
        val sb = StringBuilder()
        sb.append("# MindMate Private Data Export\n")
        sb.append("Generated on: ${dateFormat.format(Date())}\n")
        sb.append("Privacy Notice: This export was generated locally on your device. MindMate does not retain any cloud copy.\n\n")

        sb.append("## Mood Check-Ins (${moods.size})\n\n")
        moods.forEach { mood ->
            sb.append("- **${dateFormat.format(Date(mood.timestamp))}**: ${mood.score.label} (${mood.score.level}/5)\n")
            if (mood.note.isNotBlank()) sb.append("  Note: ${mood.note}\n")
            if (mood.tags.isNotEmpty()) sb.append("  Tags: ${mood.tags.joinToString(", ")}\n")
        }
        sb.append("\n")

        sb.append("## Private Journal Reflections (${journals.size})\n\n")
        journals.forEach { j ->
            sb.append("### Reflection - ${dateFormat.format(Date(j.timestamp))}\n")
            sb.append("- **What Happened**: ${j.whatHappened}\n")
            sb.append("- **How I Felt**: ${j.howIFelt}\n")
            sb.append("- **What Was Difficult**: ${j.whatWasDifficult}\n")
            sb.append("- **What Helped**: ${j.whatHelped}\n")
            if (j.tags.isNotEmpty()) sb.append("- **Tags**: ${j.tags.joinToString(", ")}\n")
            sb.append("\n")
        }

        sb.append("## Conversations (${messages.size})\n\n")
        messages.forEach { m ->
            val senderLabel = if (m.sender.name == "USER") "You" else "MindMate"
            sb.append("**[$senderLabel - ${dateFormat.format(Date(m.timestamp))}]**:\n")
            sb.append("${m.text}\n\n")
        }

        return sb.toString()
    }
}
