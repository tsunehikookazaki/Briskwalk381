package com.example.briskwalk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast

// インポートの追加
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// 結果を待ち受けるためのリクエストコード (任意の値)
private const val CONGRATULATIONS_REQUEST_CODE = 100

// CongratulationsActivityから返される結果コード
private const val RESULT_CONTINUE = 1 // 続ける
private const val RESULT_FINISH = 2   // 終了する


class MainActivity : AppCompatActivity() {

    // 💡【修正】センサー用の変数をクラスのプロパティ（内部）へ移動
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount = -1f // アプリ起動時（またはスタート時）の累計歩数を保持
    private var currentSessionSteps = 0 // 今回の計測中に歩いた歩数
    private lateinit var tvLiveSteps: TextView // 歩数表示用のTextView

    // 💡【修正】リスナーをクラス内部に移動し、handler.post で画面更新するように変更
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val totalSteps = event.values[0]
                if (initialStepCount < 0) {
                    initialStepCount = totalSteps
                }
                currentSessionSteps = (totalSteps - initialStepCount).toInt()

                // メインスレッドのhandlerを使って安全にUI更新
                handler.post {
                    tvLiveSteps.text = "スタートからの歩数：${currentSessionSteps} 歩"
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private lateinit var tvMode: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTempoLabel2 : TextView  // 現在のテンポラベル
    private lateinit var tvTempoLabel : TextView  // 現在のテンポラベル
    private lateinit var tvCurrentTempo: EditText  // テンポ入力
    private lateinit var label0: TextView
    private lateinit var btnStart: Button
    private lateinit var btnMood: Button
    private lateinit var btnReStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnDerete: Button
    private lateinit var txtLogA: TextView

    private var Fastmood = false
    private var isFastWalk = false
    private var fastWalkCount = 0
    private var tempoBpm = 125
    private var nokorisec = 0
    private var timerlength :Long = 3 * 60 * 1000    //3分
    //private var timerlength :Long = 15 * 1000    //15秒  ***********テスト用
    private var timerlength0 :Long= 0
    private var restartFlag = false
    private var pausenokori :Long = 0

    private lateinit var handler: Handler
    private lateinit var vibrator: Vibrator
    private var beatTimer: CountDownTimer? = null
    private var modeTimer: CountDownTimer? = null
    private var soundPool: SoundPool? = null
    private var soundIdchoice = 0
    private var soundIdchoice2 = 0
    private var soundIdfast0 = 0
    private var soundIdnorm = 0
    private var soundId5sec = 0
    private var soundIdstopclick = 0
    private var soundIdkai1 = 0
    private var soundIdkai2 = 0
    private var soundIdkai3 = 0
    private var soundIdkai4 = 0
    private var rightleg = true

    private val PREF_NAME = "WalkPrefs"
    private val KEY_COUNT = "fastWalkCount"
    private val KEY_TEMPO = "tempoBpm"
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    private val KEY_MODE = "Fastmood"

    private var fastWalkCount2 = 0

    // ダブルクリック判定用の変数
    private var clickCount = 0
    private val doubleClickDelayHandler = Handler(Looper.getMainLooper())
    private val doubleClickDelay = 300L // 判定時間（ミリ秒）

    private var shutterEnabled = true
    private lateinit var switchShutter: com.google.android.material.switchmaterial.SwitchMaterial

    private var button_show = 0
    var countVolume = 1.0f  //ボリューム初期値

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 💡 画面を常にオンに保つ設定
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        // ハンドラーを一番最初に初期化（ステップリスナーで使うため）
        handler = Handler(Looper.getMainLooper())

        checkPermissions()

        val voltextView = findViewById<TextView>(R.id.voltext)
        val seekBar = findViewById<SeekBar>(R.id.seek_bar)
        seekBar.max = 5

        var volume0 = "100%"
        var prog0: Int
        voltextView.text = "Volume $volume0"

        // 💡 センサーマネージャー等の初期化
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        tvLiveSteps = findViewById(R.id.tvLiveSteps)

        // イベントリスナーの追加
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prog0 = progress
                when (prog0) {
                    5 -> { countVolume = 1.0f; volume0 = "100%" }
                    4 -> { countVolume = 0.8f; volume0 = "80%" }
                    3 -> { countVolume = 0.4f; volume0 = "40%" }
                    2 -> { countVolume = 0.2f; volume0 = "20%" }
                    1 -> { countVolume = 0.1f; volume0 = "10%" }
                    0 -> { countVolume = 0.05f; volume0 = "5%" }
                }
                voltextView.text = "Volume $volume0"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 保存されているカウントを読み込み
        val pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        fastWalkCount = pref.getInt(KEY_COUNT, 0)
        tempoBpm = pref.getInt(KEY_TEMPO, 125)
        Fastmood = pref.getBoolean(KEY_MODE, true)
        isFastWalk = Fastmood

        // View取得
        tvMode = findViewById(R.id.tvMode)
        label0= findViewById(R.id.label0)
        tvCount = findViewById(R.id.tvCount)
        tvTimer = findViewById(R.id.tvTimer)
        tvTempoLabel2 = findViewById(R.id.tvTempoLabel2)
        tvTempoLabel = findViewById(R.id.tvTempoLabel)
        tvCurrentTempo = findViewById(R.id.tvCurrentTempo)
        btnStart = findViewById(R.id.btnStart)
        btnMood = findViewById(R.id.btnMood)
        btnReStart = findViewById(R.id.btnRestart)
        btnStop = findViewById(R.id.btnStop)
        btnDerete = findViewById(R.id.btnDelete)
        txtLogA = findViewById(R.id.txtLogA)
        tvTempoLabel2.text = "現在のテンポ："

        tvCount.text = "早歩き回数：$fastWalkCount2"
        updateNextModeLabel()

        // 保存されているカウントを読み込み
        fastWalkCount = pref.getInt(KEY_COUNT, 0)
        tempoBpm = pref.getInt(KEY_TEMPO, 125)

        // UIに反映
        tvCurrentTempo.setText(tempoBpm.toString())
        tvTempoLabel2.text = "現在のテンポ：$tempoBpm bpm"
        tvCount.text = "早歩き回数：$fastWalkCount2"

        btnStart.isEnabled = true
        btnMood.isEnabled = true
        btnStop.isEnabled = false
        btnReStart.isEnabled = false
        btnDerete.isEnabled = true
        tvTempoLabel2.visibility = View.GONE
        tvCurrentTempo.visibility = View.VISIBLE
        tvTempoLabel.visibility = View.VISIBLE

        Fastmood = true
        isFastWalk = Fastmood

        updateNextModeLabel()
        cleanOldLogs()
        showLog()

        // 音初期化
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
        soundIdchoice = soundPool!!.load(this, R.raw.choice, 1)
        soundIdchoice2 = soundPool!!.load(this, R.raw.choice2, 1)
        soundIdfast0 = soundPool!!.load(this, R.raw.fast0, 1)
        soundIdnorm = soundPool!!.load(this, R.raw.norm, 1)
        soundId5sec = soundPool!!.load(this, R.raw.start5sec, 1)
        soundIdstopclick = soundPool!!.load(this,R.raw.stopclick,1)
        soundIdkai1 = soundPool!!.load(this, R.raw.kai1, 1)
        soundIdkai2 = soundPool!!.load(this, R.raw.kai2, 1)
        soundIdkai3 = soundPool!!.load(this, R.raw.kai3, 1)
        soundIdkai4 = soundPool!!.load(this, R.raw.kai4, 1)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        btnMood.setOnClickListener {
            Fastmood = !Fastmood
            updateNextModeLabel()
        }

        btnStart.setOnClickListener{
            startclick()
        }

        btnStop.setOnClickListener {
            stopclick()
        }

        btnReStart.setOnClickListener {
            restartclick()
        }

        btnDerete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("記録の削除")
                .setMessage("全ての記録を削除しますか？\nこの操作は元に戻せません。")
                .setPositiveButton("削除") { _, _ ->
                    restartFlag = false
                    cleanAll()
                    showLog()

                    btnStart.isEnabled = true
                    btnMood.isEnabled = true
                    btnStop.isEnabled = false
                    btnReStart.isEnabled = false
                    btnDerete.isEnabled = true
                    tvTempoLabel2.visibility = View.GONE
                    tvCurrentTempo.visibility = View.VISIBLE
                    tvTempoLabel.visibility = View.VISIBLE
                    button_show = 1
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        val pref2 = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        shutterEnabled = pref2.getBoolean("shutterEnabled", true)
        switchShutter = findViewById(R.id.switchShutter)
        switchShutter.setOnCheckedChangeListener(null)
        switchShutter.isChecked = shutterEnabled
        switchShutter.text = if (shutterEnabled) "シャッターボタンON" else "シャッターボタンOFF"

        switchShutter.setOnCheckedChangeListener { _, isChecked ->
            shutterEnabled = isChecked
            switchShutter.text = if (isChecked) "シャッターボタンON" else "シャッターボタンOFF"
            pref2.edit().putBoolean("shutterEnabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "シャッターボタン操作：ON" else "シャッターボタン操作：OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13以上の処理
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        cleanOldLogs()
        updateTodayCount()
        showLog()

        // 💡 歩数センサーの監視を開始
        stepCounterSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // 💡 バッテリー消費を防ぐためにセンサーを解除
        sensorManager.unregisterListener(stepListener)
    }

    private fun updateTodayCount() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val logs = JSONArray(prefs.getString("logs", "[]"))
        val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        var count = 0
        for (i in 0 until logs.length()) {
            val entry = logs.getString(i)
            if (entry.contains(todayStr)) {
                count++
            }
        }
        fastWalkCount2 = count
        tvCount.text = "早歩き回数：$fastWalkCount2"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!shutterEnabled) {
            return super.onKeyDown(keyCode, event)
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0) {
                processClick()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun processClick() {
        clickCount++
        if (clickCount == 1) {
            doubleClickDelayHandler.postDelayed(singleClickRunnable, doubleClickDelay)
        } else if (clickCount == 2) {
            doubleClickDelayHandler.removeCallbacks(singleClickRunnable)
            if(button_show==0) {
                startclick()
                button_show=1
                Toast.makeText(this, "スタートボタンクリック", Toast.LENGTH_SHORT).show()
                vibratett2()
            }
            clickCount = 0
        }
    }

    private val singleClickRunnable = Runnable {
        if(button_show==0){
            restartclick()
            Toast.makeText(this, "リスタートボタンクリック", Toast.LENGTH_SHORT).show()
            vibratett2()
        }else{
            stopclick()
            Toast.makeText(this, "ストップボタンクリック", Toast.LENGTH_SHORT).show()
            soundPool?.play(soundIdstopclick, countVolume, countVolume, 1, 0, 1.0f)
            vibratett2()
        }
        clickCount = 0
    }

    private fun vibratett2(){
        if(shutterEnabled) {
            // ⭕ 正しいスペルに修正し、重複していた余分なコードを削除
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibratett() {
        val pattern = longArrayOf(0, 200, 50, 200, 50, 200, 50, 200)
        vibrator.vibrate(pattern, -1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_howto -> {
                startActivity(Intent(this, HowToActivity::class.java))
                true
            }
            R.id.menu_record -> {
                startActivity(Intent(this, RecordActivity::class.java))
                true
            }
            R.id.menu_open_image -> {
                startActivity(Intent(this, ImageViewActivity::class.java))
                true
            }
            R.id.menu_stop_app -> {
                finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateNextModeLabel() {
        if (Fastmood) {
            btnMood.text = "早歩きでスタート"
        } else {
            btnMood.text = "普通歩きでスタート"
        }
        tvMode.text = "ボタンを押して開始"
    }

    private fun startCycle() {
        pausenokori = 0
        timerlength0 = timerlength
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        startModeTimer(timerlength0)
    }

    private fun startModeTimer(duration: Long) {
        modeTimer?.cancel()
        if (isFastWalk) {
            fastwalksnd()
        } else {
            normwalksnd()
        }

        modeTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                nokorisec = (millisUntilFinished / 1000).toInt()
                val elapsedSec = ((timerlength - millisUntilFinished) / 1000).toInt()
                tvTimer.text = "経過：$elapsedSec 秒"
                pausenokori = millisUntilFinished

                if (nokorisec == 5 && !isFastWalk) {
                    soundPool?.play(soundId5sec, countVolume, countVolume, 1, 0, 1.0f)
                } else if (nokorisec == 2 && !isFastWalk) {
                    vibratett()
                }
            }

            override fun onFinish() {
                if (isFastWalk) {
                    fastWalkCount2++
                    tvCount.text = "早歩き回数：$fastWalkCount2"
                    saveCount()
                    saveLog()
                    showLog()
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

                    if (fastWalkCount2 > 0 && fastWalkCount2 % 5 == 0) {
                        val intent = Intent(this@MainActivity, CongratulationsActivity::class.java).apply {
                            putExtra("COUNT", fastWalkCount2)
                        }
                        startActivityForResult(intent, CONGRATULATIONS_REQUEST_CODE)

                        isFastWalk = false
                        Fastmood = false

                        tvMode.text = "現在のモード：普通歩き"
                        btnMood.text = "普通歩きでスタート"

                        stopclick()
                        return
                    }
                } else {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }

                isFastWalk = !isFastWalk
                if (isFastWalk) {
                    tvMode.text = "現在のモード：早歩き"
                    btnMood.text = "早歩きでスタート"
                } else {
                    tvMode.text = "現在のモード：普通歩き"
                    btnMood.text = "普通歩きでスタート"
                }
                startModeTimer(timerlength)
            }
        }.start()

        startBeatTimer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONGRATULATIONS_REQUEST_CODE) {
            when (resultCode) {
                RESULT_CONTINUE -> {
                    startclick()
                }
                RESULT_FINISH -> {
                    stopCycle()
                }
            }
        }
    }

    private fun startBeatTimer() {
        beatTimer?.cancel()
        if (!isFastWalk) return

        val interval = (60000 / tempoBpm).toLong()
        handler.postDelayed({
            beatTimer = object : CountDownTimer(timerlength + 1000, interval) {
                override fun onTick(millisUntilFinished: Long) {
                    soundPool?.play(if (rightleg) soundIdchoice else soundIdchoice2, countVolume, countVolume, 1, 0, 1.0f)
                    rightleg = !rightleg
                }
                override fun onFinish() {}
            }.start()
        }, 800)
    }

    private fun fastwalksnd() {
        soundPool?.play(soundIdfast0, countVolume, countVolume, 0, 0, 1.0f)
    }

    private fun normwalksnd() {
        when (fastWalkCount2) {
            1 -> soundPool?.play(soundIdkai1, countVolume, countVolume, 0, 0, 1.0f)
            2 -> soundPool?.play(soundIdkai2, countVolume, countVolume, 0, 0, 1.0f)
            3 -> soundPool?.play(soundIdkai3, countVolume, countVolume, 0, 0, 1.0f)
            4 -> soundPool?.play(soundIdkai4, countVolume, countVolume, 0, 0, 1.0f)
        }
        handler.postDelayed({
            soundPool?.play(soundIdnorm, countVolume, countVolume, 0, 0, 1.0f)
        }, 800)
    }

    private fun stopCycle() {
        modeTimer?.cancel()
        beatTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        tvTimer.text = "停止中"
        tvMode.text = "ボタンを押して開始"
        soundPool?.play(soundIdstopclick,countVolume, countVolume, 1, 0, 1.0f)
    }

    private fun restart() {
        if (pausenokori > 0) {
            startModeTimer(pausenokori)
        } else {
            startCycle()
        }
    }

    private fun saveCount() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit {
            putInt(KEY_COUNT, fastWalkCount2)
            putInt(KEY_TEMPO, tempoBpm)
            putBoolean(KEY_MODE, Fastmood)
        }
    }

    private fun saveLog() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val logs = JSONArray(prefs.getString("logs", "[]"))
        val logEntry = "早歩き,${dateFormat.format(Date())}"
        logs.put(logEntry)
        prefs.edit().putString("logs", logs.toString()).apply()
        SharedRecordManager.updateStats(this, "brisk", logEntry)
    }

    private fun showLog() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val logs = JSONArray(prefs.getString("logs", "[]"))
        val countsByDate = mutableMapOf<String, Int>()

        for (i in 0 until logs.length()) {
            val entry = logs.getString(i)
            try {
                val datePart = entry.split(",")[1].substring(0, 10)
                countsByDate[datePart] = countsByDate.getOrDefault(datePart, 0) + 1
            } catch (e: Exception) {}
        }

        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()

        for (i in 0 until 7) {
            val dateStr = sdf.format(cal.time)
            val count = countsByDate.getOrDefault(dateStr, 0)
            if (count > 0) {
                sb.append("$dateStr:${count}回達成\n")
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        txtLogA.text = sb.toString()
    }

    private fun cleanOldLogs() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val logs = JSONArray(prefs.getString("logs", "[]"))
        val newLogs = JSONArray()

        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
        val limitTime = cal.timeInMillis

        for (i in 0 until logs.length()) {
            val entry = logs.getString(i)
            try {
                val dateStr = entry.split(",")[1]
                val date = dateFormat.parse(dateStr)
                if (date != null && date.time >= limitTime) {
                    newLogs.put(entry)
                }
            } catch (e: Exception) {}
        }
        prefs.edit().putString("logs", newLogs.toString()).apply()
    }

    private fun cleanAll() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("fast_log", MODE_PRIVATE).edit().clear().apply()
        fastWalkCount2 = 0
        tvCount.text = "早歩き回数：0"
        txtLogA.text = ""
        SharedRecordManager.clearAppStats(this, "brisk")
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun startclick(){
        // 💡【修正】新しくウォーキングを開始した時に歩数を「0」にリセットする
        initialStepCount = -1f
        tvLiveSteps.text = "スタートからの歩数：0 歩"

        tempoBpm = tvCurrentTempo.text.toString().toIntOrNull() ?: tempoBpm
        tvTempoLabel2.text = "現在のテンポ：$tempoBpm bpm"

        isFastWalk = Fastmood
        tvMode.text = if (isFastWalk) "現在のモード：早歩き" else "現在のモード：普通歩き"

        restartFlag = false
        startCycle()

        btnStart.isEnabled = false
        btnMood.isEnabled = false
        btnStop.isEnabled = true
        btnReStart.isEnabled = false
        btnDerete.isEnabled = false
        tvTempoLabel2.visibility = View.VISIBLE
        tvCurrentTempo.visibility = View.INVISIBLE
        tvTempoLabel.visibility = View.GONE
        button_show = 1
    }

    private fun stopclick(){
        restartFlag = false
        stopCycle()

        btnStart.isEnabled = true
        btnMood.isEnabled = true
        btnStop.isEnabled = false
        btnReStart.isEnabled = true
        btnDerete.isEnabled = true
        tvTempoLabel2.visibility = View.GONE
        tvCurrentTempo.visibility = View.VISIBLE
        tvTempoLabel.visibility = View.VISIBLE
        button_show = 0
    }

    private fun restartclick(){
        restartFlag = true
        tvMode.text = if (isFastWalk) "現在のモード：早歩き" else "現在のモード：普通歩き"
        restart()

        btnStart.isEnabled = false
        btnMood.isEnabled = false
        btnStop.isEnabled = true
        btnReStart.isEnabled = false
        btnDerete.isEnabled = false
        tvTempoLabel2.visibility = View.VISIBLE
        tvCurrentTempo.visibility = View.INVISIBLE
        tvTempoLabel.visibility = View.GONE
        button_show = 1
    }
}