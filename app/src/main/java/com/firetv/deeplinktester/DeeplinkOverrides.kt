package com.firetv.deeplinktester

import android.content.Context

class DeeplinkOverrides(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUrl(itemId: String, defaultUrl: String): String =
        prefs.getString(key(itemId), null)?.takeIf { it.isNotBlank() } ?: defaultUrl

    fun saveUrl(itemId: String, url: String) {
        prefs.edit().putString(key(itemId), url.trim()).apply()
    }

    fun resetUrl(itemId: String) {
        prefs.edit().remove(key(itemId)).apply()
    }

    private fun key(itemId: String) = "url_$itemId"

    companion object {
        private const val PREFS_NAME = "deeplink_overrides"
    }
}
