package com.mindmate.core.safety

/**
 * Non-diagnostic safety and wellness signal.
 *
 * MindMate is NOT a therapist, doctor, or diagnostic system.
 * These signals represent immediate conversational safety screening states only.
 */
enum class SafetyLevel {
    /** Ordinary stress, academic venting, everyday check-in */
    GREEN,
    /** Elevated distress, persistent hopelessness, chronic burnout */
    YELLOW,
    /** Immediate safety concern requiring human crisis escalation */
    RED
}

data class SafetyEvaluation(
    val level: SafetyLevel,
    val reason: String,
    val matchedPatterns: List<String> = emptyList(),
    val escalationMessage: String? = null
)
