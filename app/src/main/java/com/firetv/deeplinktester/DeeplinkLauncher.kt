package com.firetv.deeplinktester

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

class DeeplinkLauncher(private val context: Context) {

    fun open(url: String): OpenResult {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            OpenResult.Opened
        } catch (_: ActivityNotFoundException) {
            OpenResult.NoHandler
        }
    }

    fun showResultToast(result: OpenResult) {
        if (result == OpenResult.NoHandler) {
            Toast.makeText(context, R.string.forward_prod_missing, Toast.LENGTH_LONG).show()
        }
    }

    sealed class OpenResult {
        data object Opened : OpenResult()
        data object NoHandler : OpenResult()
    }
}
