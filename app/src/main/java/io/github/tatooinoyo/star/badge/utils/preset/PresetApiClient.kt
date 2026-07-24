package io.github.tatooinoyo.star.badge.utils.preset

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

class PresetApiClient(
    private val baseUrl: String,
    private val gson: Gson = Gson(),
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) {
    suspend fun fetchPresets(): Result<PresetsResponse> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("preset API URL empty"))
        }
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val attemptResult = runCatching { requestPresetsOnce() }
            if (attemptResult.isSuccess) {
                return@withContext attemptResult
            }
            lastError = attemptResult.exceptionOrNull()
            Log.w(TAG, "fetchPresets attempt ${attempt + 1}/$MAX_ATTEMPTS failed: $lastError")
            if (attempt < MAX_ATTEMPTS - 1) {
                Thread.sleep(400)
            }
        }
        Result.failure(lastError ?: IllegalStateException("fetchPresets failed"))
    }

    suspend fun submitSubmission(request: SubmissionRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("preset API URL empty"))
            }
            runCatching { submitOnce(request) }
        }

    private fun requestPresetsOnce(): PresetsResponse {
        val url = URL("${baseUrl.trimEnd('/')}/presets")
        Log.d(TAG, "GET $url")
        val connection = openConnection(url, "GET")
        try {
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) {
                error("HTTP $code: $body")
            }
            return gson.fromJson(body, PresetsResponse::class.java)
                ?: error("Empty presets response")
        } finally {
            connection.disconnect()
        }
    }

    private fun submitOnce(request: SubmissionRequest) {
        val url = URL("${baseUrl.trimEnd('/')}/submissions")
        Log.d(TAG, "POST $url sk=${request.sk}")
        val connection = openConnection(url, "POST").apply {
            doOutput = true
        }
        try {
            val payload = gson.toJson(request).toByteArray(StandardCharsets.UTF_8)
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) {
                error("HTTP $code: $body")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "BadgeManager-PresetClient")
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    companion object {
        private const val TAG = "PresetApiClient"
        private const val MAX_ATTEMPTS = 2
        const val CONNECT_TIMEOUT_MS = 12_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
