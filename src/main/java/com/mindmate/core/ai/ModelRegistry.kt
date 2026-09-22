package com.mindmate.core.ai

import android.content.Context
import android.os.Build
import java.io.File

object ModelRegistry {

    private val CANDIDATE_NAMES = listOf(
        "gemma-2b-it.bin",
        "gemma-2b-it-gpu-int4.bin",
        "gemma-2b-it-cpu-int4.bin",
        "llama-3.2-1b.bin",
        "mindmate-model.bin",
        "mindmate-model.task",
        "mindmate-model.gguf",
        "model.bin",
        "model.task"
    )

    private var customModelPath: String? = null

    fun getPreferredModelPath(context: Context): File {
        if (!customModelPath.isNullOrBlank()) {
            val custom = File(customModelPath!!)
            if (custom.exists() && custom.length() > 0) return custom
        }

        // 1. Check app internal files directory
        val internalModelsDir = File(context.filesDir, "models")
        if (internalModelsDir.exists() && internalModelsDir.isDirectory) {
            val found = internalModelsDir.listFiles()?.firstOrNull { 
                it.isFile && (it.extension == "bin" || it.extension == "task" || it.extension == "gguf") && it.length() > 0 
            }
            if (found != null) return found
        }

        // 2. Check if a model was bundled directly inside the APK assets (models/)
        val extractedFromAssets = extractBundledModelIfPresent(context)
        if (extractedFromAssets != null && extractedFromAssets.exists() && extractedFromAssets.length() > 0) {
            return extractedFromAssets
        }

        // 2. Check standard MediaPipe deployment location (/data/local/tmp/llm/)
        val localTmpDir = File("/data/local/tmp/llm")
        if (localTmpDir.exists() && localTmpDir.isDirectory) {
            val found = localTmpDir.listFiles()?.firstOrNull { 
                it.isFile && (it.extension == "bin" || it.extension == "task" || it.extension == "gguf") && it.length() > 0 
            }
            if (found != null) return found
        }

        // 3. Check public external storage (/sdcard/models/)
        val externalModelsDir = File("/sdcard/models")
        if (externalModelsDir.exists() && externalModelsDir.isDirectory) {
            val found = externalModelsDir.listFiles()?.firstOrNull { 
                it.isFile && (it.extension == "bin" || it.extension == "task" || it.extension == "gguf") && it.length() > 0 
            }
            if (found != null) return found
        }

        // 4. Check candidates directly
        for (name in CANDIDATE_NAMES) {
            val internalCandidate = File(internalModelsDir, name)
            if (internalCandidate.exists() && internalCandidate.length() > 0) return internalCandidate

            val externalCandidate = File(externalModelsDir, name)
            if (externalCandidate.exists() && externalCandidate.length() > 0) return externalCandidate

            val tmpCandidate = File(localTmpDir, name)
            if (tmpCandidate.exists() && tmpCandidate.length() > 0) return tmpCandidate
        }

        return File(internalModelsDir, "mindmate-model.bin")
    }

    fun setCustomModelPath(path: String?) {
        customModelPath = path
    }

    fun detectHardwareAcceleration(): String {
        val hardware = (Build.HARDWARE ?: "unknown").lowercase()
        val soc = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (Build.SOC_MODEL ?: "").lowercase()
            } else {
                ""
            }
        } catch (_: Throwable) {
            ""
        }

        return when {
            hardware.contains("qcom") || soc.contains("snapdragon") || soc.contains("sm") -> {
                "Qualcomm Hexagon / Adreno Acceleration Detected ($hardware)"
            }
            hardware.contains("mt") || soc.contains("dimensity") -> {
                "MediaTek APU / Mali Acceleration Detected ($hardware)"
            }
            hardware.contains("exynos") -> {
                "Samsung NPU / Xclipse Acceleration ($hardware)"
            }
            else -> {
                "Android Neural Networks API / Multi-Threaded CPU ($hardware)"
            }
        }
    }

    fun extractBundledModelIfPresent(context: Context): File? {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        // Check if already extracted
        val existing = modelsDir.listFiles()?.firstOrNull { 
            it.isFile && (it.extension == "bin" || it.extension == "task" || it.extension == "gguf") && it.length() > 0 
        }
        if (existing != null) return existing

        // Check APK bundled assets (assets/models/)
        return try {
            val assetList = context.assets.list("models") ?: emptyArray()
            val bundledName = assetList.firstOrNull { 
                it.endsWith(".bin") || it.endsWith(".task") || it.endsWith(".gguf") 
            }
            if (bundledName != null) {
                val targetFile = File(modelsDir, bundledName)
                context.assets.open("models/$bundledName").use { input ->
                    java.io.FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    targetFile
                } else null
            } else null
        } catch (_: Throwable) {
            null
        }
    }
}
