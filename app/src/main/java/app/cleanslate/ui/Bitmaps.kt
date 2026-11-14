package app.cleanslate.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory

object Bitmaps {

    fun decode(path: String, maxDim: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(path, opts)
            ?: throw IllegalStateException("cannot decode $path")
        val scale = maxDim.toFloat() / maxOf(raw.width, raw.height)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else raw
    }

    fun pixels(bitmap: Bitmap): IntArray {
        val arr = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(arr, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return arr
    }

    fun fromPixels(pixels: IntArray, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    /** Center-crops [src] to [targetW]x[targetH] aspect, then scales to exactly that size. */
    fun cropTo(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcAspect = src.width.toFloat() / src.height
        val dstAspect = targetW.toFloat() / targetH
        val cropped = if (srcAspect > dstAspect) {
            val w = (src.height * dstAspect).toInt()
            Bitmap.createBitmap(src, (src.width - w) / 2, 0, w, src.height)
        } else {
            val h = (src.width / dstAspect).toInt()
            Bitmap.createBitmap(src, 0, (src.height - h) / 2, src.width, h)
        }
        return Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
    }
}
