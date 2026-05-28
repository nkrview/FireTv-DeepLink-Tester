package com.firetv.deeplinktester

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL

class FeedRepository {

    @Throws(FeedException::class)
    fun fetchFeed(
        feedUrl: String,
        username: String? = null,
        password: String? = null,
    ): List<FeedItem> {
        val authHeader = buildBasicAuthHeader(username, password)
        val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "FireTvDeeplinkTester/1.0")
            if (!authHeader.isNullOrBlank()) {
                setRequestProperty("Authorization", authHeader)
            }
        }

        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw FeedException.HttpError(code, body.ifBlank { "HTTP $code" })
            }
            if (body.isBlank()) {
                throw FeedException.ParseError("Empty feed response")
            }

            return try {
                FeedParser.parse(body).ifEmpty {
                    throw FeedException.ParseError("No items with firetvUrl in feed")
                }
            } catch (e: FeedException) {
                throw e
            } catch (e: Exception) {
                throw FeedException.ParseError(e.message ?: "Invalid JSON")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildBasicAuthHeader(username: String?, password: String?): String? {
        if (username.isNullOrBlank() || password.isNullOrBlank()) return null
        val raw = "${username.trim()}:${password.trim()}"
        val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}

sealed class FeedException(message: String) : Exception(message) {
    class HttpError(val statusCode: Int, message: String) : FeedException(message)
    class ParseError(message: String) : FeedException(message)
    class NetworkError(message: String) : FeedException(message)
}
