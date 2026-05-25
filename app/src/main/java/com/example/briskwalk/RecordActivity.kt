package com.example.briskwalk
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class RecordActivity : AppCompatActivity() {

    private lateinit var tvRecord: TextView
    private lateinit var btnDelete: Button
    private val PREF_FILE = "fast_log"
    private val DATE_FORMAT = "yyyy/MM/dd HH:mm"
    private val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar_record)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // 戻る矢印
        supportActionBar?.title = "早歩き詳細記録"


        // View取得
        tvRecord = findViewById(R.id.tvRecord)

        // 古いログ削除
        cleanOldLogs()

        // 表示更新
        showLog()


    }

    // 戻る矢印処理
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /** 表示更新 */
    private fun showLog() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString("logs", "[]"))

        if (jsonArray.length() == 0) {
            tvRecord.text = "記録はありません。"
            return
        }

        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val log = jsonArray.getString(i)
            val parts = log.split(",")
            if (parts.size >= 2) {
                list.add(parts[1])  // 日付文字列
            }
        }

        // 日付順で表示
        tvRecord.text = list.joinToString("\n")
    }

    /** 古いログ削除（8日以上前） */
    private fun cleanOldLogs() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString("logs", "[]"))
        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000

        val newArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val log = jsonArray.getString(i)
            val parts = log.split(",")
            if (parts.size >= 2) {
                val timestamp = try {
                    sdf.parse(parts[1])?.time ?: now
                } catch (e: Exception) { now }
                if (now - timestamp <= sevenDaysMillis) {
                    newArray.put(log)
                }
            }
        }

        prefs.edit().putString("logs", newArray.toString()).apply()
    }

    /** 全ログ削除 */
    private fun clearAllLogs() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}