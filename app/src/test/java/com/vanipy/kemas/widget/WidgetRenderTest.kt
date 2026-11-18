package com.vanipy.kemas.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Renders the home-screen widget exactly as the launcher would, to PNG. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderTest {
    private val w = 660
    private val h = 460

    private fun snapshot(rv: RemoteViews, c: Context, outName: String) {
        val parent = FrameLayout(c)
        val v = rv.apply(c, parent)
        v.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        v.layout(0, 0, w, v.measuredHeight)
        val bmp = Bitmap.createBitmap(w, v.measuredHeight, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val out = File("build/widget-snapshots", outName)
        out.parentFile!!.mkdirs()
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 2000)
    }

    @Test
    fun widgetDemoMode() {
        val c = RuntimeEnvironment.getApplication()
        com.vanipy.kemas.demo.DemoMode.set(c, true)
        snapshot(KemasWidgetProvider.buildViews(c), c, "widget-demo.png")
        c.getSharedPreferences("kemas_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun widgetEmptyStore() {
        val c = RuntimeEnvironment.getApplication()
        File(c.filesDir, "data.json").delete()
        snapshot(KemasWidgetProvider.buildViews(c), c, "widget-empty.png")
    }
}
