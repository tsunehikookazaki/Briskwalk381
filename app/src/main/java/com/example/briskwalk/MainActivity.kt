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


// 結果を待ち受けるためのリクエストコード (任意の値)
private const val CONGRATULATIONS_REQUEST_CODE = 100

// CongratulationsActivityから返される結果コード
private const val RESULT_CONTINUE = 1 // 続ける
private const val RESULT_FINISH = 2   // 終了する



class MainActivity : AppCompatActivity() {

    private lateinit var tvMode: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvTimer: TextView
    private  lateinit var tvTempoLabel2 : TextView  // 現在のテンポラベル
    private  lateinit var tvTempoLabel : TextView  // 現在のテンポラベル
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
    //private var timerlength :Long = 15 * 1000    //15秒
    private var timerlength0 :Long= 0
    private var restartFlag = false
    private var pausenokori :Long = 0

    private lateinit var handler: Handler
    private  lateinit var vibrator: Vibrator
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

    private  var fastWalkCount2 = 0


    // ダブルクリック判定用の変数
    private var clickCount = 0
    private val doubleClickDelayHandler = Handler(Looper.getMainLooper())
    private val doubleClickDelay = 300L // 判定時間（ミリ秒）。お好みで300~500で調整


    private var shutterEnabled = true
    private lateinit var switchShutter: com.google.android.material.switchmaterial.SwitchMaterial

    private var button_show = 0
    //　　button_show = 1 スタートボタン又はリスタートボタンが押された状況　ストップボタンのみ表示
    //　　button_show = 0 ストップボタンが押された状況、ストップボタン以外が表示
    var countVolume = 1.0f  //ボリューム初期値

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 💡 画面を常にオンに保つ設定を追加 💡
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val voltextView = findViewById<TextView>(R.id.voltext)   //ボリューム表示ようのテキストビュー
        val seekBar = findViewById<SeekBar>(R.id.seek_bar)  //シークバー
        seekBar.max = 5  // シークバーの範囲　0~5　6段階

        var volume0 = "100%"  //ボリューム初期値
        var prog0: Int
        //ボリュームの6段階の値　0~5
        voltextView.text = "Volume $volume0"  //ボリュームの値を表示

        // イベントリスナーの追加　　シークバーの値を変える
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            // 値が変更された時に呼ばれる
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prog0 = progress  //変更されたらボリューム値を変える
                when (prog0) {
                    5 -> {
                        countVolume = 1.0f;volume0 = "100%"
                    }
                    4 -> {
                        countVolume = 0.8f;volume0 = "80%"
                    }
                    3 -> {
                        countVolume = 0.4f;volume0 = "40%"
                    }
                    2 -> {
                        countVolume = 0.2f;volume0 = "20%"
                    }
                    1 -> {
                        countVolume = 0.1f;volume0 = "10%"
                    }
                    0 -> {
                        countVolume = 0.05f;volume0 = "5%"
                    }
                }
                voltextView.text = "Volume $volume0"
            }

            // つまみがタッチされた時に呼ばれる
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            // つまみが離された時に呼ばれる
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
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
        tvCurrentTempo = findViewById(R.id.tvCurrentTempo) // 追加 TextView
        btnStart = findViewById(R.id.btnStart)
        btnMood = findViewById(R.id.btnMood)
        btnReStart = findViewById(R.id.btnRestart)
        btnStop = findViewById(R.id.btnStop)
        btnDerete = findViewById(R.id.btnDelete)
        txtLogA = findViewById(R.id.txtLogA)
        tvTempoLabel2.text = "現在のテンポ："

        tvCount.text = "早歩き回数：$fastWalkCount2"
        updateNextModeLabel()
        txtLogA.text

        // 保存されているカウントを読み込み
        fastWalkCount = pref.getInt(KEY_COUNT, 0)
        tempoBpm = pref.getInt(KEY_TEMPO, 125)    //pリファレンスに規定値が無ければ、125を初期値にする


        // UIに反映
        tvCurrentTempo.setText(tempoBpm.toString())
        tvTempoLabel2.text = "現在のテンポ：$tempoBpm bpm"
        tvCount.text = "早歩き回数：$fastWalkCount2"


        btnStart.isEnabled = true
        btnMood.isEnabled = true
        btnStop.isEnabled = false
        btnReStart.isEnabled = false
        btnDerete.isEnabled = true
        tvTempoLabel2.visibility = View.GONE   //現在のテンポ
        tvCurrentTempo.visibility = View.VISIBLE  //入力欄
        tvTempoLabel.visibility = View.VISIBLE    //テンポ（歩数/分）を入力"

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
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build()
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

        handler = Handler(Looper.getMainLooper())    //周期処理



        btnMood.setOnClickListener {         //モードボタンが押されたら
            Fastmood = !Fastmood
            updateNextModeLabel()
        }



        btnStart.setOnClickListener{      //スタートボタンが押されたら
            startclick()
        }

        btnStop.setOnClickListener {     //ストップボタンが押されたら
            stopclick()

        }

        btnReStart.setOnClickListener {     //リスタートボタンが押されたら
           restartclick()
        }


        btnDerete.setOnClickListener {     //削除ボタンが押されたら
            // 確認ダイアログを表示
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("記録の削除")
                .setMessage("全ての記録を削除しますか？\nこの操作は元に戻せません。")
                .setPositiveButton("削除") { _, _ ->
                    // 削除が選ばれたときの処理
                    restartFlag = false
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
                .setNegativeButton("キャンセル", null) // 何もしないで閉じる
                .show()

        }


        // シャッターボタンの ON / OFF
        val pref2 = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

// シャッター有効/無効を読み込み
        shutterEnabled = pref2.getBoolean("shutterEnabled", true)

// Switch取得
        switchShutter = findViewById(R.id.switchShutter)

// ★① Listener を一旦外す（ここ！）
        switchShutter.setOnCheckedChangeListener(null)

// ★② 初期状態を反映（ここで isChecked を触る）
        switchShutter.isChecked = shutterEnabled
        switchShutter.text = if (shutterEnabled) {
            "シャッターボタンON"
        } else {
            "シャッターボタンOFF"
        }

// ★③ Listener を設定（最後
// 切替時
        switchShutter.setOnCheckedChangeListener { _, isChecked ->
            shutterEnabled = isChecked

            // ★ ここが抜けていた
            switchShutter.text = if (isChecked) {
                "シャッターボタンON"
            } else {
                "シャッターボタンOFF"
            }

            // 保存
            pref2.edit().putBoolean("shutterEnabled", isChecked).apply()

            // Toast
            Toast.makeText(
                this,
                if (isChecked)
                    "シャッターボタン操作：ON"
                else
                    "シャッターボタン操作：OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // データの整理と更新
        cleanOldLogs()
        updateTodayCount()
        showLog()
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

    //*********************************************************************************
    // キーイベントの検知
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        // ★ Switch が OFF のときは通常の音量キー動作
        if (!shutterEnabled) {
            return super.onKeyDown(keyCode, event)
        }


        // 音量キーのみを対象にする
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

            // 長押しによる連続イベントは無視する（重要）
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
            // 1回目のクリック：
            // すぐに実行せず、一定時間待ってから「シングルクリック処理」を行う予約をする
            doubleClickDelayHandler.postDelayed(singleClickRunnable, doubleClickDelay)
        } else if (clickCount == 2) {
            // 2回目のクリック（時間内に来た場合）：
            // 予約していた「シングルクリック処理」をキャンセルする
            doubleClickDelayHandler.removeCallbacks(singleClickRunnable)

            if(button_show==0) {   //ストップボタンが押された時はスタートかリスタート
                // 「ダブルクリック処理（スタートボタン）」を実行
                //btnStart.performClick()
                startclick()
                button_show=1
                Toast.makeText(this, "スタートボタンクリック", Toast.LENGTH_SHORT).show()
                vibratett2()

            }
            // カウントをリセット
            clickCount = 0
        }
    }

    // 遅延実行されるシングルクリックのタスク
    private val singleClickRunnable = Runnable {
        // ここまでキャンセルされずに到達したら、シングルクリック確定
        if(button_show==0){  //ストップボタンが押された時はスタートかリスタート
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
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibratett() {
        // バイブレーター5秒前用（残り2秒時点で鳴動）
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
        // 停止時は常にこのテキストを表示
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
        
        // 開始時の音声アナウンス
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

                // 普通歩きのときの早歩き直前（5秒前）のみ、1回だけお知らせ音を鳴らす
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
                    
                    // 早歩きが5回に達したら CongratulationsActivity を呼び出す
                    if (fastWalkCount2 == 5) {
                        val intent = Intent(this@MainActivity, CongratulationsActivity::class.java)
                        intent.putExtra("COUNT", fastWalkCount2)
                        startActivityForResult(intent, CONGRATULATIONS_REQUEST_CODE)
                        
                        // モードを切り替えて次のタイマーの準備をする
                        isFastWalk = !isFastWalk
                        if (isFastWalk) {
                           tvMode.text = "現在のモード：早歩き"
                           btnMood.text = "早歩きでスタート"
                        } else {
                           tvMode.text = "現在のモード：普通歩き"
                           btnMood.text = "普通歩きでスタート"
                        }
                        
                        // タイマーを止めておく
                        stopclick()
                        return // 次のタイマーへは進めない
                    }
                } else {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }
                
                // モードを切り替えて次のタイマーを開始
                isFastWalk = !isFastWalk
                if (isFastWalk) {
                   tvMode.text = "現在のモード：早歩き"
                   btnMood.text = "早歩きでスタート" // 実行中も一応更新
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
                    // 「続ける」場合は次のタイマーを開始する
                    startclick()
                }
                RESULT_FINISH -> {
                    // 「終了する」場合はすでに stopclick() されているので何もしないか念のため stopCycle()
                    stopCycle()
                }
            }
        }
    }

    private fun startBeatTimer() {
        beatTimer?.cancel()
        // 普通歩きの時はビート（メトロノーム）を鳴らさない
        if (!isFastWalk) return 

        val interval = (60000 / tempoBpm).toLong()
        // 音声アナウンスが終わるまで少し待ってからビートを開始
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
        
        // 共有ファイルも更新
        SharedRecordManager.updateStats(this, "brisk", logEntry)
        
        // Android IDを用いて回数をサーバーにアップ
        StatusSyncManager.uploadStatus(this, "brisk", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), "$fastWalkCount2 回")
    }

    private fun showLog() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val logs = JSONArray(prefs.getString("logs", "[]"))
        val countsByDate = mutableMapOf<String, Int>()
        
        // ログから日付ごとの回数を集計
        for (i in 0 until logs.length()) {
            val entry = logs.getString(i)
            try {
                val datePart = entry.split(",")[1].substring(0, 10) // yyyy/MM/dd
                countsByDate[datePart] = countsByDate.getOrDefault(datePart, 0) + 1
            } catch (e: Exception) {}
        }
        
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        
        // 今日から遡って7日分のうち、実績がある日のみ表示する
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
        
        // 今日を含む過去7日分を保持する（厳密に日付で判定）
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6) // 今日を入れて7日分なので、6日前まで
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
        
        // 共有ファイルもクリア
        SharedRecordManager.clearAppStats(this, "brisk")
        
        // Android IDを用いてサーバー側の「今日」と「昨日」のステータスも「未実施」で上書きする
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        StatusSyncManager.uploadStatus(this, "brisk", todayStr, "未実施")
        
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        StatusSyncManager.uploadStatus(this, "brisk", yesterdayStr, "未実施")
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun startclick(){
        // 入力テンポを反映
        tempoBpm = tvCurrentTempo.text.toString().toIntOrNull() ?:  tempoBpm
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
        tvTempoLabel2.visibility = View.VISIBLE //現在のテンポ
        tvCurrentTempo.visibility = View.INVISIBLE  //入力欄
        tvTempoLabel.visibility = View.GONE   //テンポ（歩数/分）を入力"
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
        tvTempoLabel2.visibility = View.GONE   //現在のテンポ
        tvCurrentTempo.visibility = View.VISIBLE  //入力欄
        tvTempoLabel.visibility = View.VISIBLE    //テンポ（歩数/分）を入力"
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
        tvTempoLabel2.visibility = View.VISIBLE //現在のテンポ
        tvCurrentTempo.visibility = View.INVISIBLE  //入力欄
        tvTempoLabel.visibility = View.GONE   //テンポ（歩数/分）を入力"
        button_show = 1
    }
}