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
    private const val AUTHORITY = "net.the_okazakis.letsdoit.stats"
    private val CONTENT_URI = android.net.Uri.parse("content://$AUTHORITY/stats")

    fun updateStats(context: Context, appKey: String, value: String) {
        // 1. LetsDoItの窓口（ContentProvider）へ送信
        try {
            val values = android.content.ContentValues().apply {
                put("app_key", appKey)
                put("value", value)
            }
            context.contentResolver.insert(CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 念のためこれまでのファイル保存も継続（バックアップ用）
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
     * 指定したアプリの統計データを共有ファイルから削除し、LetsDoItにも通知する
     */
    fun clearAppStats(context: Context, appKey: String) {

        // ************* 1. 【追加】LetsDoItの窓口（ContentProvider）へ「未実施」を送信 *************
        try {
            val values = android.content.ContentValues().apply {
                put("app_key", appKey)
                put("value", "未実施") // LetsDoIt側で「未実施」として判定される文字列を送る
            }
            context.contentResolver.insert(CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ************* 2. これまでのファイル保存からも項目を削除（バックアップ用） *************
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
