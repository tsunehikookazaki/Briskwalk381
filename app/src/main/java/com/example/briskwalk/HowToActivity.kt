package com.example.briskwalk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.TextView

class HowToActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_howto)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_howto)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "使い方"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        val textView = findViewById<TextView>(R.id.tvHowTo)
        textView.text ="""
◆◆このアプリについて◆◆\n●早歩きと普通の歩きを3分で繰り返します。
●一日に早歩きを連続しなくても合計15分行うと筋力・持久力の向上が期待できます。
ピッチ音に合わせて早歩きを1日に5回以上しましょう！！ 週4日以上実行しましょう

◆◆メイン画面◆◆\n●早歩きの速度に1分当たりの歩数を入力します。デフォルトは122です。　音が交互に変わるので、右左とテンポよく歩きましょう
●スタートボタンで始めて、ストップボタンで終わります。早歩きと普通歩きは3分ごと交互に変ります。3分未満の場合は記録されません
●モードボタンに次にスタートボタンで始めるモードが表示されます。モードボタンをタップすると、次にスターするモードが変わります
●リスタートボタンで、ストップした続きから始められます。信号で止った時などで再開したい場合に使います

◆◆早歩き詳細記録画面◆◆
●早歩きの詳細な記録が表示されます
●記録が多い場合はスクロールしてください
←でメイン画面に戻ります

◆◆テンポ（歩数）の目安画面◆◆
●身長と強度別の早歩きのテンポを表示
←でメイン画面に戻ります

◆◆使い方画面◆◆
n●この画面
←でメイン画面に戻ります

◆◆Bluetoothシャッターボタン◆◆
●Bluetoothのシャッターボタンでストップ、リスタートが出来ます
シャッターボタンをクリックでストップまたはリスタート
ダブルクリックでスタートです
㊟スマフォの音声ボリュームを使っているので、ボリュームを変えると、ボタンを押したことになります。
㊟シャッターボタンの機種により操作出来ないことがあります
"""
        .trimIndent()
    }

    // 戻る矢印タップ時の動作
    override fun onSupportNavigateUp(): Boolean {
        finish() // Activity を閉じて前の画面に戻る
        return true
    }
}