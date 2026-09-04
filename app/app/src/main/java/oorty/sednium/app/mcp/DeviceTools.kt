package oorty.sednium.app.mcp

import android.content.Context
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import oorty.sednium.app.plugins.device.DeviceAutomator

object DeviceTools {

    val LAUNCH_APP_TOOL = Tool(
        name = "launch_app",
        description = "Launch or open an installed Android application (e.g. WhatsApp, Chrome, Obsidian, Termux, YouTube, Maps, Camera, Calculator, Settings, etc.)",
        inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                putJsonObject("app_name") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("The name of the app to launch (e.g. whatsapp, chrome, termux, camera)"))
                }
            }
            put("required", buildJsonArray {
                add(JsonPrimitive("app_name"))
            })
        }
    )

    val BATTERY_INFO_TOOL = Tool(
        name = "get_battery_status",
        description = "Get the current device battery percentage and charging status",
        inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {}
        }
    )

    val FLASHLIGHT_TOOL = Tool(
        name = "toggle_flashlight",
        description = "Turn the device flashlight / torch on or off",
        inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                putJsonObject("enable") {
                    put("type", JsonPrimitive("boolean"))
                    put("description", JsonPrimitive("True to turn flashlight ON, false to turn OFF"))
                }
            }
            put("required", buildJsonArray {
                add(JsonPrimitive("enable"))
            })
        }
    )

    fun getBuiltInTools(): List<QualifiedTool> = listOf(
        QualifiedTool("builtin", "launch_app", LAUNCH_APP_TOOL),
        QualifiedTool("builtin", "get_battery_status", BATTERY_INFO_TOOL),
        QualifiedTool("builtin", "toggle_flashlight", FLASHLIGHT_TOOL)
    )

    fun execute(context: Context, qualifiedName: String, arguments: JsonObject): CallToolResult {
        val simpleName = qualifiedName.substringAfter("::").substringAfter("__")
        return when (simpleName) {
            "launch_app" -> {
                val appName = (arguments["app_name"] as? JsonPrimitive)?.content ?: ""
                val result = DeviceAutomator.launchApp(context, appName)
                CallToolResult(listOf(ContentBlock.Text(result)), isError = false)
            }
            "get_battery_status" -> {
                val result = DeviceAutomator.getBatteryInfo(context)
                CallToolResult(listOf(ContentBlock.Text(result)), isError = false)
            }
            "toggle_flashlight" -> {
                val enable = (arguments["enable"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
                val result = DeviceAutomator.toggleFlashlight(context, enable)
                CallToolResult(listOf(ContentBlock.Text(result)), isError = false)
            }
            else -> CallToolResult(listOf(ContentBlock.Text("Unknown device tool: $qualifiedName")), isError = true)
        }
    }
}
