package com.mindmate.core.ai

import com.mindmate.domain.model.AiResponse
import com.mindmate.domain.model.Message
import com.mindmate.domain.model.UserContext

enum class ModelInstallationState {
    MODEL_NOT_INSTALLED,
    MODEL_LOADING,
    MODEL_READY,
    MODEL_ERROR
}

data class EngineStatus(
    val modelState: ModelInstallationState,
    val activeBackend: String,
    val modelPath: String?,
    val hardwareAcceleration: String,
    val isOfflineReady: Boolean,
    val statusDescription: String
)

interface LocalAiEngine {
    suspend fun generateResponse(
        conversation: List<Message>,
        context: UserContext
    ): AiResponse

    fun getStatus(): EngineStatus
}
