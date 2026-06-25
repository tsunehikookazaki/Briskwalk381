package com.example.briskwalk

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imageview)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar_image)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // 戻る矢印
        supportActionBar?.title = "テンポ（歩数）の目安"

        // XML の ZoomableImageView を取得
        val zoomableImageView = findViewById<ZoomableImageView>(R.id.zoomable_image_view)

        // 画像リソースIDを取得 (デフォルトを tempo から kyoudo に変更)
        val imageResId = intent.getIntExtra("image_res_id", R.raw.kyoudo)

        // PDFか通常の画像かを判断してセット
        if (isPdfResource(imageResId)) {
            displayPdf(imageResId, zoomableImageView)
        } else {
            zoomableImageView.setImageResource(imageResId)
        }
    }

    /**
     * 指定されたリソースがPDFかどうかを判定します。
     */
    private fun isPdfResource(resId: Int): Boolean {
        return try {
            val resName = resources.getResourceEntryName(resId)
            val resType = resources.getResourceTypeName(resId)
            // ファイル名に "kyoudo" が含まれるか、rawリソースの場合はPDFとして扱う
            resName.contains("kyoudo") || resType == "raw"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * PDFリソースをBitmapにレンダリングしてImageViewに表示します。
     */
    private fun displayPdf(resourceId: Int, zoomableImageView: ZoomableImageView) {
        try {
            // PDFをレンダリングするために、一旦キャッシュファイルにコピーする
            val tempFile = File(cacheDir, "temp_guide.pdf")
            resources.openRawResource(resourceId).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            
            if (renderer.pageCount > 0) {
                // 最初のページをレンダリング
                val page = renderer.openPage(0)
                
                // 画面密度に合わせて高解像度で作成 (ズームに対応するため2倍の密度)
                val density = resources.displayMetrics.density
                val width = (page.width * density * 2).toInt()
                val height = (page.height * density * 2).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                zoomableImageView.setImageBitmap(bitmap)
                page.close()
            }
            renderer.close()
            fd.close()
        } catch (e: IOException) {
            Log.e("ImageViewActivity", "PDFの表示に失敗しました", e)
            // 失敗した場合はフォールバックとして元の画像を表示
            zoomableImageView.setImageResource(R.drawable.tempo)
        }
    }

    // 戻る矢印タップ時の動作
    override fun onSupportNavigateUp(): Boolean {
        finish() // Activity を閉じて前の画面に戻る
        return true
    }
}
