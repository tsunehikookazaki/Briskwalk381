package com.example.briskwalk

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ピンチ操作による拡大・縮小とドラッグによる移動に対応したカスタムImageView。
 * 初期表示時に画像をViewの中央にフィットさせます。
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener {

    // 1. 状態管理のための変数
    private val matrix = Matrix()      // 現在の画像変換マトリックス
    private val savedMatrix = Matrix() // ズーム/ドラッグ開始時のマトリックス

    private var mode = Mode.NONE       // 現在の操作モード
    private var start = PointF()       // ドラッグ開始点
    private var mid = PointF()         // ズーム操作の中心点
    private var oldDist = 1f           // ズーム開始時の指間の距離
    private var isInitialized = false  // 初期設定が完了したかを示すフラグ

    private enum class Mode { NONE, DRAG, ZOOM }

    init {
        // マトリックス制御を有効にする
        super.setScaleType(ScaleType.MATRIX)
        setOnTouchListener(this)
    }

    // Viewのサイズが確定または変更されたときに呼ばれる (初期化に利用)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Viewのサイズが確定した最初の1回だけ初期設定を実行
        if (!isInitialized) {
            setInitialMatrix()
            isInitialized = true
        }
    }

    // 画像がViewにフィットするようにMatrixを初期設定する
    private fun setInitialMatrix() {
        val d = drawable ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val drawableWidth = d.intrinsicWidth.toFloat()
        val drawableHeight = d.intrinsicHeight.toFloat()

        // Fit Center (画像全体が View に収まるようにスケールを計算)
        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val initialScale = min(scaleX, scaleY)

        // スケールを適用
        matrix.postScale(initialScale, initialScale)

        // センタリングの計算
        val scaledWidth = drawableWidth * initialScale
        val scaledHeight = drawableHeight * initialScale

        // Viewの中心に来るように移動量を計算
        val translateX = (viewWidth - scaledWidth) / 2
        val translateY = (viewHeight - scaledHeight) / 2

        // 移動を適用
        matrix.postTranslate(translateX, translateY)

        // ズーム/ドラッグのための初期状態を保存
        savedMatrix.set(matrix)

        // 画像にMatrixを適用して表示
        imageMatrix = matrix
    }

    // タッチイベントの処理
    override fun onTouch(v: View, event: MotionEvent): Boolean {

        when (event.action and MotionEvent.ACTION_MASK) {

            // 最初の指が触れたとき (ドラッグ開始)
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = Mode.DRAG
            }

            // 2本目の指が触れたとき (ピンチ操作開始)
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) { // 距離が一定以上ならズームモード
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = Mode.ZOOM
                }
            }

            // 1本以上の指が離れたとき (操作終了)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
            }

            // 指が移動したとき
            MotionEvent.ACTION_MOVE -> {
                when (mode) {

                    // ドラッグモード (画像を移動)
                    Mode.DRAG -> {
                        matrix.set(savedMatrix)
                        // 移動量だけ移動
                        matrix.postTranslate(event.x - start.x, event.y - start.y)
                    }

                    // ズームモード (画像を拡大・縮小)
                    Mode.ZOOM -> {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / oldDist // スケールファクタを計算

                            // 中心点 (mid) を基準にスケールを適用
                            matrix.postScale(scale, scale, mid.x, mid.y)
                        }
                    }

                    Mode.NONE -> { /* 何もしない */ }
                }
            }
        }

        // 最終的なマトリックスを画像に適用し、再描画する
        imageMatrix = matrix
        return true // イベントを消費 (システムに処理させない)
    }

    // 2点間の距離を計算
    private fun spacing(event: MotionEvent): Float {
        // 2点以上の指があることを前提とする
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    // 2点間の中間点を計算
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
}