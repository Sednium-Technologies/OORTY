package oorty.sednium.app.api

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

enum class HfModelFormat {
    GGUF, LITERT
}

data class HfGgufFile(
    val path: String,
    val sizeBytes: Long,
    val format: HfModelFormat = if (path.endsWith(".tflite", ignoreCase = true) || path.endsWith(".litertlm", ignoreCase = true)) HfModelFormat.LITERT else HfModelFormat.GGUF
)

object HuggingFaceApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listGgufFiles(repoId: String): List<HfGgufFile> = withContext(Dispatchers.IO) {
        val cleanRepo = repoId.trim().trim('/')
        val url = "https://huggingface.co/api/models/$cleanRepo/tree/main"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val array = json.parseToJsonElement(body).jsonArray

            array.mapNotNull { element ->
                val obj = element.jsonObject
                val path = obj["path"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val size = obj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val lower = path.lowercase()
                if (lower.endsWith(".gguf") || lower.endsWith(".tflite") || lower.endsWith(".litertlm")) {
                    val fmt = if (lower.endsWith(".tflite") || lower.endsWith(".litertlm")) HfModelFormat.LITERT else HfModelFormat.GGUF
                    HfGgufFile(path = path, sizeBytes = size, format = fmt)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadModelFile(
        context: Context,
        repoId: String,
        fileName: String,
        onProgress: (progressFraction: Float, speedMbPerSec: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanRepo = repoId.trim().trim('/')
        val cleanFile = fileName.trim().substringAfterLast("/")
        val downloadUrl = "https://huggingface.co/$cleanRepo/resolve/main/$cleanFile?download=true"

        val targetDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Oorty/models"
        ).apply { mkdirs() }
        val targetFile = File(if (targetDir.exists()) targetDir else File(context.filesDir, "models").apply { mkdirs() }, cleanFile)

        val request = Request.Builder().url(downloadUrl).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
            val totalBytes = body.contentLength()

            val buffer = ByteArray(8 * 1024)
            var bytesReadTotal = 0L
            val startTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesReadTotal += read
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        val speedMb = if (elapsedSec > 0) (bytesReadTotal / (1024 * 1024f)) / elapsedSec else 0f
                        val progress = if (totalBytes > 0) bytesReadTotal.toFloat() / totalBytes.toFloat() else 0.5f
                        onProgress(progress, speedMb)
                    }
                    output.flush()
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
