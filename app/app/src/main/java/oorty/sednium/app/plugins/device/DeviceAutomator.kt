package oorty.sednium.app.plugins.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build

object DeviceAutomator {

    fun launchApp(context: Context, appName: String): String {
        val query = appName.lowercase().trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trimEnd('.', '!', '?')
            .replace("whats app", "whatsapp")
        val pm = context.packageManager

        // Standard well-known package mapping
        val knownPackages = mapOf(
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "whatsapp business" to listOf("com.whatsapp.w4b", "com.whatsapp"),
            "telegram" to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            "discord" to listOf("com.discord"),
            "obsidian" to listOf("md.obsidian"),
            "termux" to listOf("com.termux", "com.termux.x11"),
            "terminal" to listOf("com.termux"),
            "chrome" to listOf("com.android.chrome"),
            "google chrome" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome", "org.mozilla.firefox"),
            "youtube" to listOf("com.google.android.youtube", "app.morphe.android.youtube"),
            "yt" to listOf("com.google.android.youtube", "app.morphe.android.youtube"),
            "youtube music" to listOf("com.google.android.apps.youtube.music", "app.morphe.android.apps.youtube.music"),
            "yt music" to listOf("com.google.android.apps.youtube.music"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera"),
            "settings" to listOf("com.android.settings"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "files" to listOf("com.google.android.documentsui", "com.android.documentsui"),
            "gmail" to listOf("com.google.android.gm"),
            "mail" to listOf("com.google.android.gm"),
            "spotify" to listOf("com.spotify.music"),
            "instagram" to listOf("com.instagram.android"),
            "insta" to listOf("com.instagram.android"),
            "twitter" to listOf("com.twitter.android"),
            "x" to listOf("com.twitter.android"),
            "photos" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
            "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
            "drive" to listOf("com.google.android.apps.docs"),
            "play store" to listOf("com.android.vending"),
            "playstore" to listOf("com.android.vending")
        )

        // 1. Check known packages first
        val packageCandidates = knownPackages[query] ?: emptyList()
        for (pkg in packageCandidates) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val friendlyName = query.replaceFirstChar { it.uppercase() }
                return "Opening $friendlyName..."
            }
        }

        // 2. Query all launcher-visible apps for fuzzy name/package matching
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val allLauncherApps: List<ResolveInfo> = try {
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            try {
                pm.queryIntentActivities(launcherIntent, 0)
            } catch (e2: Exception) { emptyList() }
        }

        var targetResolveInfo = allLauncherApps.find { ri ->
            val label = ri.loadLabel(pm).toString().lowercase()
            val pkg = ri.activityInfo.packageName.lowercase()
            label == query || label.contains(query) || pkg.contains(query)
        }

        if (targetResolveInfo != null) {
            val pkg = targetResolveInfo.activityInfo.packageName
            val cls = targetResolveInfo.activityInfo.name
            val intent = pm.getLaunchIntentForPackage(pkg) ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val label = targetResolveInfo.loadLabel(pm).toString()
            return "Opening $label..."
        }

        // 3. Fallback: query installed applications directly
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val matchedApp = installedApps.find { ai ->
                val label = pm.getApplicationLabel(ai).toString().lowercase()
                label.contains(query) || ai.packageName.lowercase().contains(query)
            }
            if (matchedApp != null) {
                val intent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    val label = pm.getApplicationLabel(matchedApp).toString()
                    return "Opening $label..."
                }
            }
        } catch (e: Exception) {}

        // 4. Fallback: open Play Store search
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return "App '$appName' not found locally. Searching Play Store..."
        } catch (e: Exception) {
            return "Could not launch '$appName': ${e.message}"
        }
    }

    fun getBatteryInfo(context: Context): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100
            "Battery Level: $batteryPct% (${if (isCharging) "Charging ⚡" else "Discharging"})"
        } catch (e: Exception) {
            "Battery status unavailable"
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Oorty"): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            "Copied to clipboard: \"${text.take(40)}...\""
        } catch (e: Exception) {
            "Failed to copy to clipboard"
        }
    }

    fun toggleFlashlight(context: Context, enable: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return "No flashlight available"
            cameraManager.setTorchMode(cameraId, enable)
            if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
        } catch (e: Exception) {
            "Flashlight control error: ${e.message}"
        }
    }

    data class ActionExecutionResult(
        val executedActions: List<String>,
        val cleanText: String
    )

    fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a, EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        return "It is currently ${sdf.format(java.util.Date())}"
    }

    fun searchWeb(context: Context, query: String): String {
        val cleanQuery = query.trim()
        val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(cleanQuery, "UTF-8")}"
        return openUrl(context, searchUrl, "Searching for \"$cleanQuery\"...")
    }

    fun openUrl(context: Context, url: String, label: String = "Opening browser..."): String {
        return try {
            val uri = android.net.Uri.parse(if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            label
        } catch (e: Exception) {
            "Unable to open link: ${e.message}"
        }
    }

    /**
     * Executes any [ACTION: ...] tags emitted by the AI model, executes the corresponding
     * Android hardware/app action, and returns clean text stripped of action tags.
     */
    fun executeActionTags(context: Context, text: String): ActionExecutionResult {
        var clean = text
        val executed = mutableListOf<String>()

        // 1. [ACTION: OPEN_APP name="..."] or [ACTION: OPEN_APP app="..."]
        val openAppRegex = Regex("\\[ACTION:\\s*OPEN_APP\\s+(?:name|app)=[\"']?([^\"'\\]]+)[\"']?\\s*\\]", RegexOption.IGNORE_CASE)
        openAppRegex.findAll(text).forEach { match ->
            val appName = match.groupValues[1].trim()
            val result = launchApp(context, appName)
            executed.add(result)
            clean = clean.replace(match.value, "")
        }

        // 2. [ACTION: SEARCH query="..."]
        val searchRegex = Regex("\\[ACTION:\\s*SEARCH\\s+query=[\"']?([^\"'\\]]+)[\"']?\\s*\\]", RegexOption.IGNORE_CASE)
        searchRegex.findAll(text).forEach { match ->
            val query = match.groupValues[1].trim()
            val result = searchWeb(context, query)
            executed.add(result)
            clean = clean.replace(match.value, "")
        }

        // 3. [ACTION: OPEN_URL url="..."]
        val openUrlRegex = Regex("\\[ACTION:\\s*OPEN_URL\\s+url=[\"']?([^\"'\\]]+)[\"']?\\s*\\]", RegexOption.IGNORE_CASE)
        openUrlRegex.findAll(text).forEach { match ->
            val url = match.groupValues[1].trim()
            val result = openUrl(context, url)
            executed.add(result)
            clean = clean.replace(match.value, "")
        }

        // 4. [ACTION: FLASHLIGHT enable=true/false]
        val flashlightRegex = Regex("\\[ACTION:\\s*FLASHLIGHT\\s+enable=(true|false)\\s*\\]", RegexOption.IGNORE_CASE)
        flashlightRegex.findAll(text).forEach { match ->
            val enable = match.groupValues[1].toBoolean()
            val result = toggleFlashlight(context, enable)
            executed.add(result)
            clean = clean.replace(match.value, "")
        }

        // 5. [ACTION: BATTERY]
        val batteryRegex = Regex("\\[ACTION:\\s*BATTERY\\s*\\]", RegexOption.IGNORE_CASE)
        if (batteryRegex.containsMatchIn(text)) {
            val result = getBatteryInfo(context)
            executed.add(result)
            clean = clean.replace(batteryRegex, "")
        }

        // 6. [ACTION: TIME]
        val timeRegex = Regex("\\[ACTION:\\s*TIME\\s*\\]", RegexOption.IGNORE_CASE)
        if (timeRegex.containsMatchIn(text)) {
            val result = getCurrentTime()
            executed.add(result)
            clean = clean.replace(timeRegex, "")
        }

        // 7. [ACTION: CLIPBOARD text="..."]
        val clipboardRegex = Regex("\\[ACTION:\\s*CLIPBOARD\\s+text=[\"']?([^\"'\\]]+)[\"']?\\s*\\]", RegexOption.IGNORE_CASE)
        clipboardRegex.findAll(text).forEach { match ->
            val clipText = match.groupValues[1].trim()
            val result = copyToClipboard(context, clipText)
            executed.add(result)
            clean = clean.replace(match.value, "")
        }

        return ActionExecutionResult(executed, clean.trim())
    }

    /**
     * Inspects prompt text for direct device actions (time, search, open/launch app, flashlight, battery info).
     * Handles natural conversational spoken queries (e.g. "what time is it", "just open whatsapp right now").
     */
    fun tryHandleDirectIntent(context: Context, text: String): String? {
        var trimmed = text.trim().lowercase().trimEnd('.', '!', '?')
        if (trimmed.isBlank()) return null

        // Strip conversational leading fillers
        val conversationalPrefixes = listOf(
            "can you please ", "could you please ", "can you ", "could you ",
            "please ", "just ", "hey oorty ", "oorty ", "kindly ", "i want you to ",
            "would you ", "go ahead and ", "help me ", "let's "
        )
        for (filler in conversationalPrefixes) {
            if (trimmed.startsWith(filler)) {
                trimmed = trimmed.removePrefix(filler).trim()
            }
        }

        // Strip conversational trailing fillers
        val conversationalSuffixes = listOf(
            " right now", " right away", " for me", " please", " now",
            " immediately", " thanks", " thank you", " asap"
        )
        for (suffix in conversationalSuffixes) {
            if (trimmed.endsWith(suffix)) {
                trimmed = trimmed.removeSuffix(suffix).trim()
            }
        }

        // 1. Time and Date commands
        val timePhrases = listOf(
            "what time is it", "what's the time", "whats the time", "what is the time",
            "tell me the time", "current time", "time now", "what is today's date",
            "what's today's date", "whats todays date", "what is the date today",
            "what is the date", "what day is today", "what day is it today", "what day is it"
        )
        if (timePhrases.any { trimmed == it || trimmed.startsWith("$it ") || trimmed.endsWith(" $it") }) {
            return getCurrentTime()
        }

        // 2. Web Search commands: "search for ...", "google ...", "search google for ..."
        val searchPrefixes = listOf("search for ", "search google for ", "search web for ", "google ")
        for (sp in searchPrefixes) {
            if (trimmed.startsWith(sp)) {
                val query = trimmed.removePrefix(sp).trim()
                if (query.isNotBlank()) {
                    return searchWeb(context, query)
                }
            }
        }

        // 3. App launch commands
        val openPrefixes = listOf("open app ", "launch app ", "open up ", "open ", "launch ", "start ", "switch to ", "go to ")
        for (prefix in openPrefixes) {
            if (trimmed.startsWith(prefix)) {
                val appQuery = trimmed.removePrefix(prefix).trim().trimEnd('.', '!', '?')
                if (appQuery.isNotBlank() && appQuery.length <= 40 && !appQuery.contains("\n")) {
                    return launchApp(context, appQuery)
                }
            }
        }

        // Direct app mentions with open/launch verbs anywhere
        val knownKeywords = listOf(
            "whatsapp", "telegram", "discord", "obsidian", "termux", "terminal",
            "chrome", "browser", "youtube", "yt music", "spotify", "instagram", "insta",
            "twitter", "camera", "settings", "calculator", "clock", "files", "gmail",
            "photos", "gallery", "maps", "play store", "playstore"
        )
        if (trimmed.contains("open") || trimmed.contains("launch") || trimmed.contains("start") || trimmed.contains("run")) {
            for (kw in knownKeywords) {
                if (trimmed.contains(kw)) {
                    return launchApp(context, kw)
                }
            }
        }

        // 2. Flashlight commands
        if (trimmed.contains("flashlight") || trimmed.contains("torch")) {
            if (trimmed.contains("off") || trimmed.contains("disable") || trimmed.contains("stop") || trimmed.contains("close") || trimmed.contains("shut")) {
                return toggleFlashlight(context, false)
            }
            if (trimmed.contains("on") || trimmed.contains("enable") || trimmed.contains("start") || trimmed.contains("turn on") || trimmed.contains("open")) {
                return toggleFlashlight(context, true)
            }
        }

        // 3. Battery commands
        if (trimmed.contains("battery") || trimmed.contains("charge") || trimmed.contains("power level") || trimmed.contains("power percent")) {
            return getBatteryInfo(context)
        }

        return null
    }
}
