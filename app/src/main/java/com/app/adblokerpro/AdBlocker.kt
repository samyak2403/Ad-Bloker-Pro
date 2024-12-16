package com.app.adblokerpro


import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

object AdBlocker {

    private const val AD_HOSTS_FILE = "host.txt"
    private val AD_HOSTS = mutableSetOf<String>()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                loadFromAssets(context)
            } catch (e: IOException) {
                // noop
                Log.e("AdBlocker", "Error loading ad hosts file", e)
            }
        }
    }

    @WorkerThread
    private suspend fun loadFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            context.assets.open(AD_HOSTS_FILE).use { stream ->
                InputStreamReader(stream).use { inputStreamReader ->
                    BufferedReader(inputStreamReader).use { bufferedReader ->
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            line?.let { AD_HOSTS.add(it) }
                        }
                    }
                }
            }
        }
    }

    fun isAd(url: String): Boolean {
        return try {
            val host = getHost(url)
            isAdHost(host) || AD_HOSTS.contains(Uri.parse(url).lastPathSegment)
        } catch (e: MalformedURLException) {
            Log.d("AdBlocker", e.toString())
            false
        }
    }

    private fun isAdHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) {
            return false
        }
        val index = host.indexOf(".")
        return index >= 0 && (AD_HOSTS.contains(host) ||
                (index + 1 < host.length && isAdHost(host.substring(index + 1))))
    }

    private fun getHost(url: String): String {
        return URL(url).host
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }
}
