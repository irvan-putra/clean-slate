package com.vanipy.kemas.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Resource rendering only; actual widget-host qualification is separate. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderTest {
    private var w = 660
    private var h = 460

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
        assertTrue(out.length() > 300)
    }

    @Test
    @Config(qualifiers = "mdpi")
    fun allThirtyGridSpans() {
        val c = RuntimeEnvironment.getApplication()
        com.vanipy.kemas.demo.DemoMode.set(c, true)
        for (columns in 1..5) for (rows in 1..6) {
            w = columns * 73 - 16; h = rows * 96 - 16
            val rv = KemasWidgetProvider.buildViews(c, h < 110, w < 150)
            val view = rv.apply(c, FrameLayout(c))
            assertEquals(View.GONE, view.findViewById<View>(com.vanipy.kemas.R.id.widget_demo_badge).visibility)
            snapshot(rv, c, "span-${columns}x${rows}.png")
        }
        com.vanipy.kemas.demo.DemoMode.set(c, false)
    }

    @Test
    @Config(qualifiers = "mdpi")
    fun counterAndCompactResize() {
        val c = RuntimeEnvironment.getApplication()
        com.vanipy.kemas.demo.DemoMode.set(c, true)
        val options = android.os.Bundle()
        options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 64)
        options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
        w = 48; h = 48
        snapshot(KemasWidgetProvider.buildViews(c, true, true), c, "widget-counter-48dp.png")
        w = 64; h = 64
        snapshot(KemasWidgetProvider.buildViews(c, true, true), c, "widget-counter-64dp.png")
        options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
        w = 350
        snapshot(KemasWidgetProvider.buildViews(c, true, false), c, "widget-strip-350x64dp.png")
        options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 240)
        h = 240
        snapshot(KemasWidgetProvider.buildViews(c, false, false), c, "widget-expanded-350x240dp.png")
        com.vanipy.kemas.demo.DemoMode.set(c, false)
    }

    @Test
    fun widgetDemoMode() {
        val c = RuntimeEnvironment.getApplication()
        com.vanipy.kemas.demo.DemoMode.set(c, true)
        snapshot(KemasWidgetProvider.buildViews(c), c, "widget-demo.png")
        c.getSharedPreferences("kemas_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun widgetDemoHidesRealStore() {
        val c = RuntimeEnvironment.getApplication()
        // Real store present with a real zone...
        File(c.filesDir, "data.json").delete()
        val store = com.vanipy.kemas.data.Store(File(c.filesDir, "data.json"))
        store.addArea("Real bedroom")
        // ...but demo mode must show only the made-up zone.
        com.vanipy.kemas.demo.DemoMode.set(c, true)
        val parent = FrameLayout(c)
        val v = KemasWidgetProvider.buildViews(c).apply(c, parent)
        assertEquals(
            com.vanipy.kemas.demo.DemoData.ZONE,
            v.findViewById<android.widget.TextView>(com.vanipy.kemas.R.id.widget_zone).text.toString(),
        )
        assertEquals(
            View.GONE,
            v.findViewById<android.widget.TextView>(com.vanipy.kemas.R.id.widget_demo_badge).visibility,
        )
        c.getSharedPreferences("kemas_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        File(c.filesDir, "data.json").delete()
    }

    @Test
    fun widgetEmptyStore() {
        val c = RuntimeEnvironment.getApplication()
        File(c.filesDir, "data.json").delete()
        snapshot(KemasWidgetProvider.buildViews(c), c, "widget-empty.png")
    }
}
