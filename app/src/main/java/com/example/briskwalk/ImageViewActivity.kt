package com.example.briskwalk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ImageViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imageview)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar_image)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // 戻る矢印
        supportActionBar?.title = "テンポ（歩数）の目安"


/**
        // XML の ImageView を取得して画像をセット
        val imageView = findViewById<ImageView>(R.id.imageView)
        val imageResId = intent.getIntExtra("image_res_id", R.drawable.tempo)
        imageView.setImageResource(imageResId)
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        */

        // XML の ZoomableImageView を取得
        val zoomableImageView = findViewById<ZoomableImageView>(R.id.zoomable_image_view)

        // 画像リソースIDを取得
        val imageResId = intent.getIntExtra("image_res_id", R.drawable.tempo)

        // 画像をセット
            zoomableImageView.setImageResource(imageResId)

        // ※ scaleTypeの設定は、ZoomableImageViewクラスの内部で処理されるため不要です。

    }

    // 戻る矢印タップ時の動作
    override fun onSupportNavigateUp(): Boolean {
        finish() // Activity を閉じて前の画面に戻る
        return true
    }
}