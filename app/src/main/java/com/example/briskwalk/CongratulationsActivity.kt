package com.example.briskwalk

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

// MainActivityに戻ってサイクルを再開するための結果コード
private const val RESULT_CONTINUE = 1

class CongratulationsActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var soundIdCongratulations = 0
    private var hasCheerPlayed = false // 効果音の重複再生防止フラグ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_congratulations)

        // 画面を常にオンに保つ
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // MainActivityから回数を取得
        val count = intent.getIntExtra("FAST_WALK_COUNT", 5)

        // UI要素の取得
        val tvCongratulations = findViewById<TextView>(R.id.tvCongratulations)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val ivConfetti = findViewById<ImageView>(R.id.ivConfetti)

        // メッセージを設定
        tvCongratulations.text = "早歩き${count}回達成おめでとうございます！\n素晴らしい！"

        // --- 🎶 音声設定 ---

        // BGM (fan.mp3) の準備と再生
        mediaPlayer = MediaPlayer.create(this, R.raw.fan).apply {
            isLooping = true
            setVolume(0.8f, 0.8f)
            start()
        }

        // 効果音 (goodjob.mp3) の準備と1回再生
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build()
        soundIdCongratulations = soundPool!!.load(this, R.raw.goodjob, 1)

        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
            if (status == 0 && sampleId == soundIdCongratulations) {
                if (!hasCheerPlayed) {
                    soundPool.play(soundIdCongratulations, 1.0f, 1.0f, 0, 0, 1f)
                    hasCheerPlayed = true
                }
            }
        }

        // --- 🏃 ボタンリスナー ---

        btnContinue.setOnClickListener {
            // 結果を RESULT_CONTINUE (再開) に設定してActivityを終了
            setResult(RESULT_CONTINUE)
            finish()
        }

        // --- ✨ 紙吹雪アニメーション ---

        // 画面の描画完了を待ってからアニメーションを開始する (高さ取得のため)
        ivConfetti.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                ivConfetti.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // ビューの高さ
                val viewHeight = ivConfetti.height.toFloat()

                // 1. 初期状態を設定 (画面外の下に配置)
                ivConfetti.translationY = viewHeight
                ivConfetti.alpha = 0.0f

                // 2. post{} で次の描画フレームを待ってからアニメーションを開始
                ivConfetti.post {

                    // --- 🚀 1段階目: 上がるアニメーション (フェードインしながら上へ打ち上げる) ---
                    ivConfetti.animate()
                        .alpha(1.0f)           // フェードイン
                        .translationY(viewHeight * 0.1f) // 画面の高さの30%の位置まで上げる
                        .setDuration(1000)     // 1秒で上昇
                        .setStartDelay(300)
                        .withEndAction {
                            // 1段階目が終わった直後に、2段階目（落ちる）アニメーションを開始する

                            // --- 📉 2段階目: 落ちるアニメーション (画面外の下へ) ---
                            ivConfetti.animate()
                                .translationY(viewHeight * 1.5f) // 画面外の下へ大きく落とす
                                .alpha(0.0f)                     // 落ちながらフェードアウトさせる
                                .setDuration(3000)               // 3秒かけてゆっくり落とす
                                .setStartDelay(0)                // 遅延なしで即座に開始
                                .start()
                        }
                        .start()
                }
            }
        })

        // --- 🔙 バック操作制御 ---

        // OnBackPressedCallback を使ってバックジェスチャー/キーをインターセプト
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@CongratulationsActivity, "ボタンを押してメイン画面に戻ってください", Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // --- 🧹 ライフサイクルとリソース解放 ---

    override fun onDestroy() {
        super.onDestroy()
        // 音声リソースの解放
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool?.release()
        soundPool = null
        // 画面オン維持のフラグを解除
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}