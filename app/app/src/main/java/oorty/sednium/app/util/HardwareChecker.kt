package oorty.sednium.app.util

import android.app.ActivityManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import oorty.sednium.app.ui.theme.SedniumColors

enum class HardwareFit(
    val title: String,
    val description: String,
    val color: Color
) {
    COMFORTABLE(
        title = "Comfortable Fit",
        description = "This model comfortably fits in your available RAM and should run smoothly.",
        color = SedniumColors.Green500
    ),
    TIGHT(
        title = "Tight Fit (Might Lag)",
        description = "This model consumes most of your available RAM. Other background apps may be killed.",
        color = SedniumColors.Orange
    ),
    DANGEROUS(
        title = "High Crash Risk",
        description = "This model requires more RAM than your device currently has free. Loading may trigger Android Low Memory Killer or cause app crashes.",
        color = SedniumColors.Red500
    )
}

enum class ModelSuitability(
    val label: String,
    val badgeColor: Color,
    val textColor: Color,
    val description: String
) {
    RECOMMENDED(
        label = "Recommended",
        badgeColor = Color(0x2610B981),
        textColor = Color(0xFF10B981),
        description = "Comfortably fits within available RAM with low thermal load."
    ),
    MAY_OVERHEAT(
        label = "May Overheat",
        badgeColor = Color(0x26F59E0B),
        textColor = Color(0xFFF59E0B),
        description = "High compute requirements; device may get warm during extended generation."
    ),
    NOT_ABLE_TO_RUN(
        label = "Not Able to Run",
        badgeColor = Color(0x26EF4444),
        textColor = Color(0xFFEF4444),
        description = "Model size exceeds physical RAM capacity on this device."
    )
}

object HardwareChecker {

    fun getAvailableRamMb(context: Context): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val mb = (memoryInfo.availMem / (1024 * 1024)).toInt()
            if (mb > 0) mb else (Runtime.getRuntime().freeMemory() / (1024 * 1024)).toInt().coerceAtLeast(1024)
        } catch (e: Exception) {
            2048
        }
    }

    fun getTotalRamMb(context: Context): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val mb = (memoryInfo.totalMem / (1024 * 1024)).toInt()
            if (mb > 0) mb else (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt().coerceAtLeast(2048)
        } catch (e: Exception) {
            4096
        }
    }

    fun assessModelFit(modelSizeMb: Int, availableRamMb: Int): HardwareFit {
        if (modelSizeMb <= 0) return HardwareFit.COMFORTABLE
        val ratio = modelSizeMb.toFloat() / availableRamMb.coerceAtLeast(512).toFloat()
        return when {
            ratio <= 0.55f -> HardwareFit.COMFORTABLE
            ratio <= 0.85f -> HardwareFit.TIGHT
            else -> HardwareFit.DANGEROUS
        }
    }

    fun assessModelFitTotalRam(modelSizeMb: Int, totalRamMb: Int): HardwareFit {
        if (modelSizeMb <= 0) return HardwareFit.COMFORTABLE
        val ratio = modelSizeMb.toFloat() / totalRamMb.coerceAtLeast(1024).toFloat()
        return when {
            ratio <= 0.35f -> HardwareFit.COMFORTABLE
            ratio <= 0.55f -> HardwareFit.TIGHT
            else -> HardwareFit.DANGEROUS
        }
    }

    fun isModelRecommended(modelName: String, context: Context): Boolean {
        return getSuitability(modelName, context) == ModelSuitability.RECOMMENDED
    }

    fun getSuitability(modelName: String, context: Context): ModelSuitability {
        val totalRamMb = getTotalRamMb(context)
        val lower = modelName.lowercase()
        val isLarge = lower.contains("14b") || lower.contains("27b") || lower.contains("32b") || lower.contains("70b") || lower.contains("72b")
        val isMedium = lower.contains("7b") || lower.contains("8b") || lower.contains("9b")
        val isSmall = lower.contains("0.5b") || lower.contains("1b") || lower.contains("1.5b") || lower.contains("2b") || lower.contains("3b") || lower.contains("mini")

        return when {
            isLarge -> {
                if (totalRamMb >= 16384) ModelSuitability.MAY_OVERHEAT else ModelSuitability.NOT_ABLE_TO_RUN
            }
            isMedium -> {
                if (totalRamMb <= 6144) ModelSuitability.NOT_ABLE_TO_RUN
                else ModelSuitability.MAY_OVERHEAT
            }
            isSmall -> {
                ModelSuitability.RECOMMENDED
            }
            else -> {
                if (totalRamMb >= 8192) ModelSuitability.RECOMMENDED else ModelSuitability.MAY_OVERHEAT
            }
        }
    }
}
