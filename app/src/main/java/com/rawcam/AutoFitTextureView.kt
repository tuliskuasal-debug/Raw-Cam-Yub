package com.rawcam

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.view.Surface
import android.view.TextureView
import kotlin.math.max

/** TextureView yang menjaga rasio preview + koreksi rotasi untuk layar landscape. */
class AutoFitTextureView(context: Context) : TextureView(context) {
    private var rw = 0   // lebar buffer preview (landscape, orientasi sensor)
    private var rh = 0

    fun setAspectRatio(w: Int, h: Int) {
        rw = w; rh = h
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        if (rw == 0 || rh == 0) setMeasuredDimension(w, h)
        else if (w * rh < h * rw) setMeasuredDimension(w, w * rh / rw)
        else setMeasuredDimension(h * rw / rh, h)
    }

    fun updateTransform(rotation: Int) {
        if (rw == 0 || width == 0 || height == 0) return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val m = Matrix()
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            val viewRect = RectF(0f, 0f, vw, vh)
            val bufRect = RectF(0f, 0f, rh.toFloat(), rw.toFloat())
            val cx = viewRect.centerX()
            val cy = viewRect.centerY()
            bufRect.offset(cx - bufRect.centerX(), cy - bufRect.centerY())
            m.setRectToRect(viewRect, bufRect, Matrix.ScaleToFit.FILL)
            val s = max(vh / rh, vw / rw)
            m.postScale(s, s, cx, cy)
            m.postRotate(90f * (rotation - 2), cx, cy)
        } else if (rotation == Surface.ROTATION_180) {
            m.postRotate(180f, vw / 2f, vh / 2f)
        }
        setTransform(m)
    }
}
