package com.mindmate.core.officekit

import com.mindmate.domain.model.InsightSummary
import com.mindmate.domain.model.Message
import com.mindmate.domain.model.MoodEntry
import kotlinx.coroutines.flow.StateFlow

enum class OfficeKitStatus {
    DISCONNECTED,
    LISTENING,
    CONNECTED,
    ERROR
}

data class OfficeKitCapability(
    val id: String,
    val name: String,
    val isRealSupported: Boolean,
    val description: String
)

data class OfficeKitReceivedJournal(
    val id: String,
    val title: String,
    val timestamp: Long,
    val preview: String,
    val moodScore: Int,
    val storedLocally: Boolean = true
)

data class OfficeKitBridgeState(
    val status: OfficeKitStatus,
    val localIp: String? = null,
    val port: Int = 8844,
    val pairingPin: String = "",
    val activeConnections: Int = 0,
    val lastAction: String = "Idle",
    val isMoodAuthorized: Boolean = false,
    val isInsightsAuthorized: Boolean = false,
    val isSessionAuthorized: Boolean = false,
    val authorizedSessionTitle: String? = null
)

interface OfficeKitAdapter {
    val bridgeState: StateFlow<OfficeKitBridgeState>
    val receivedJournals: StateFlow<List<OfficeKitReceivedJournal>>

    fun startCompanionSession(exportDataProvider: () -> String)
    fun stopCompanionSession()

    fun authorizeMoodSharing(authorized: Boolean, mood: MoodEntry? = null)
    fun authorizeInsightsSharing(authorized: Boolean, insights: InsightSummary? = null)
    fun authorizeSessionSharing(authorized: Boolean, sessionTitle: String? = null, messages: List<Message> = emptyList())
    fun revokeAllShares()

    fun getSupportedCapabilities(): List<OfficeKitCapability>
}
