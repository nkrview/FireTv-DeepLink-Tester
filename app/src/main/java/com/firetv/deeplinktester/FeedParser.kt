package com.firetv.deeplinktester

import org.json.JSONArray
import org.json.JSONObject

object FeedParser {

    private val URL_KEYS = listOf("firetvUrl", "fireTvUrl", "fire_tv_url", "fireTVUrl")
    private val TITLE_KEYS = listOf("title", "name", "label", "displayName", "headline")
    private val ID_KEYS = listOf("id", "guid", "feedId", "itemId")

    fun parse(json: String): List<FeedItem> {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()

        when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                return collectFromArray(array)
            }
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                return collectFromObject(root, inheritedTitle = null, inheritedId = null)
            }
            else -> error("Unexpected feed format")
        }
    }

    private fun collectFromArray(
        array: JSONArray,
        inheritedTitle: String? = null,
        inheritedId: String? = null,
    ): List<FeedItem> {
        val result = mutableListOf<FeedItem>()
        for (index in 0 until array.length()) {
            when (val entry = array.opt(index)) {
                is JSONObject -> result += collectFromObject(entry, inheritedTitle, inheritedId)
                is JSONArray -> result += collectFromArray(entry, inheritedTitle, inheritedId)
            }
        }
        return result
    }

    private fun collectFromObject(
        obj: JSONObject,
        inheritedTitle: String?,
        inheritedId: String?,
    ): List<FeedItem> {
        val result = mutableListOf<FeedItem>()
        val currentTitle = obj.extractTitle() ?: inheritedTitle
        val currentId = obj.extractId() ?: inheritedId

        obj.extractFiretvUrl()?.let { firetvUrl ->
            val resolvedTitle = currentTitle ?: "Feed ${result.size + 1}"
            val resolvedId = currentId ?: "${resolvedTitle.hashCode()}_${firetvUrl.hashCode()}"
            result += FeedItem(
                id = resolvedId,
                title = resolvedTitle,
                firetvUrl = firetvUrl,
            )
        }

        val iterator = obj.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            when (val child = obj.opt(key)) {
                is JSONObject -> result += collectFromObject(child, currentTitle, currentId)
                is JSONArray -> result += collectFromArray(child, currentTitle, currentId)
            }
        }

        return result
    }

    private fun JSONObject.extractFiretvUrl(): String? {
        for (key in URL_KEYS) {
            optString(key).trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private fun JSONObject.extractTitle(): String? {
        for (key in TITLE_KEYS) {
            optString(key).trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private fun JSONObject.extractId(): String? {
        for (key in ID_KEYS) {
            optString(key).trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }
}
