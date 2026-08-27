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

object HardwareChecker {

    fun getAvailableRamMb(context: Context): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            (memoryInfo.availMem / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            2048 // Fallback estimate
        }
    }

    fun getTotalRamMb(context: Context): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            (memoryInfo.totalMem / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            4096 // Fallback estimate
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
        val totalRamMb = getTotalRamMb(context)
        val lower = modelName.lowercase()
        return when {
            totalRamMb <= 4096 -> lower.contains("0.5b") || lower.contains("1b")
            totalRamMb <= 6144 -> lower.contains("0.5b") || lower.contains("1b") || lower.contains("2b")
            else -> true
        }
    }
}
