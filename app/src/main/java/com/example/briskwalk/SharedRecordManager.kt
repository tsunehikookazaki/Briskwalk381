package com.example.briskwalk

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object SharedRecordManager {
    private const val FOLDER_NAME = "LetsDoIt"
    private const val FILE_NAME = "stats.json"

    fun updateStats(context: Context, appKey: String, value: String) {
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, FILE_NAME)

            val json = if (file.exists()) {
                try {
                    JSONObject(file.readText())
                } catch (e: Exception) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dayData = json.optJSONObject(today) ?: JSONObject()
            dayData.put(appKey, value)
            dayData.put("last_update", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            
            json.put(today, dayData)
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 指定したアプリの統計データを共有ファイルから削除する
     */
    fun clearAppStats(context: Context, appKey: String) {
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME)
            val file = File(dir, FILE_NAME)
            if (file.exists()) {
                val json = JSONObject(file.readText())
                val keys = json.keys()
                while (keys.hasNext()) {
                    val dateKey = keys.next()
                    val dayData = json.optJSONObject(dateKey)
                    dayData?.remove(appKey)
                }
                file.writeText(json.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
