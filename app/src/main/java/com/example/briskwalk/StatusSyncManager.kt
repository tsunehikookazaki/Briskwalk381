package com.example.briskwalk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object StatusSyncManager {
    private const val PHP_URL = "https://www.the-okazakis.net/LetsDoIt/status_sync.php"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_id"
    }

    fun uploadStatus(context: Context, appType: String, dateStr: String, statusStr: String) {
        val androidId = getAndroidId(context)
        executor.execute {
            try {
                val postBody = JSONObject().apply {
                    put("android_id", androidId)
                    put("app", appType)
                    put("date", dateStr)
                    put("status_str", statusStr)
                }.toString()

                val conn = URL(PHP_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(conn.outputStream).use { it.write(postBody) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchStatus(context: Context, callback: (JSONObject?) -> Unit) {
        val androidId = getAndroidId(context)
        executor.execute {
            try {
                val urlWithAuth = "$PHP_URL?android_id=$androidId"
                val conn = URL(urlWithAuth).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    if (responseStr.trim().startsWith("{")) {
                        val responseJson = JSONObject(responseStr)
                        if (responseJson.optString("status") != "empty" && responseJson.optString("status") != "error") {
                            val dataObj = if (responseJson.has("status")) {
                                responseJson.optJSONObject("data") ?: JSONObject()
                            } else {
                                responseJson
                            }
                            mainHandler.post { callback(dataObj) }
                            return@execute
                        }
                    }
                }
                mainHandler.post { callback(null) }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { callback(null) }
            }
        }
    }
}
