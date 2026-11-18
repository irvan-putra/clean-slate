package com.vanipy.kemas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.vanipy.kemas.MainActivity
import com.vanipy.kemas.R
import com.vanipy.kemas.data.Area
import com.vanipy.kemas.data.Session
import com.vanipy.kemas.data.Store
import com.vanipy.kemas.demo.DemoData
import com.vanipy.kemas.demo.DemoMode
import java.io.File

/** Home-screen widget: current zone progress with before/after thumbnails. Tap opens kemas. */
class KemasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, buildViews(context, mgr.getAppWidgetOptions(id)))
    }

    override fun onAppWidgetOptionsChanged(c: Context, mgr: AppWidgetManager, id: Int, options: Bundle) {
        mgr.updateAppWidget(id, buildViews(c, options))
    }

    companion object {
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, KemasWidgetProvider::class.java))
            for (id in ids) mgr.updateAppWidget(id, buildViews(context, mgr.getAppWidgetOptions(id)))
        }

        private const val THUMB_W = 240
        private const val THUMB_H = 180

        internal fun buildViews(c: Context, options: Bundle): RemoteViews {
            // Let modern hosts choose the matching layout atomically during resize.
            // In particular, a photo-strip update must not remain after a tall resize.
            if (Build.VERSION.SDK_INT >= 31) return RemoteViews(linkedMapOf(
                SizeF(48f, 48f) to buildViews(c, true, true),
                SizeF(100f, 48f) to buildViews(c, true, false, true),
                SizeF(260f, 48f) to buildViews(c, true, false),
                SizeF(200f, 110f) to buildViews(c, false, false),
            ))
            return buildViews(c,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 48) < 110,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 48) < 100,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 48) < 260)
        }

        internal fun buildViews(c: Context): RemoteViews = buildViews(c, false)

        internal fun buildViews(c: Context, compact: Boolean, tiny: Boolean = false, textOnly: Boolean = false): RemoteViews {
            val v = RemoteViews(c.packageName, if (tiny) R.layout.widget_kemas_tiny else if (compact) R.layout.widget_kemas_compact else R.layout.widget_kemas)
            val demo = DemoMode.isOn(c)
            v.setViewVisibility(R.id.widget_demo_badge, View.GONE)

            if (demo) {
                v.setTextViewText(R.id.widget_zone, DemoData.ZONE)
                v.setTextViewText(R.id.widget_progress_text,
                    "${DemoData.DONE} of ${DemoData.TOTAL} checks done - streak ${DemoData.STREAK_DAYS} days")
                v.setProgressBar(R.id.widget_progress, DemoData.TOTAL, DemoData.DONE, false)
                v.setImageViewBitmap(R.id.widget_before, loadAsset(c, DemoData.BEFORE_ASSET))
                v.setImageViewBitmap(R.id.widget_after, loadAsset(c, DemoData.AFTER_ASSET))
                v.setViewVisibility(R.id.widget_after, View.VISIBLE)
                v.setTextViewText(R.id.widget_hint, "")
            } else {
                bindReal(c, v)
            }

            if (tiny) {
                if (demo) {
                    v.setTextViewText(R.id.widget_zone, "${DemoData.DONE}/${DemoData.TOTAL}")
                    v.setTextViewText(R.id.widget_progress_text, "checks")
                } else {
                    val store = Store(File(c.filesDir, "data.json"))
                    val sessions = store.areas().flatMap { store.sessionsFor(it.id) }
                    val done = sessions.count { it.afterPath != null }
                    v.setContentDescription(R.id.widget_root, "kemas: $done of ${sessions.size} checks complete across all zones. Tap for details.")
                    v.setTextViewText(R.id.widget_zone, if (done > 999) "999+" else "$done")
                    v.setTextViewText(R.id.widget_progress_text, "done")
                }
                v.setViewVisibility(R.id.widget_progress, View.GONE)
                v.setViewVisibility(R.id.widget_before, View.GONE)
                v.setViewVisibility(R.id.widget_after, View.GONE)
            }

            // Narrow short widgets prioritize readable zone/progress over tiny photos.
            if (compact && !tiny && textOnly) {
                v.setViewVisibility(R.id.widget_before, View.GONE)
                v.setViewVisibility(R.id.widget_after, View.GONE)
            }

            val pi = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            return v
        }

        private fun bindReal(c: Context, v: RemoteViews) {
            val store = Store(File(c.filesDir, "data.json"))
            val areas = store.areas()
            var bestArea: Area? = null
            var bestSession: Session? = null
            for (area in areas) {
                val latest = store.sessionsFor(area.id).firstOrNull() ?: continue
                if (bestSession == null || latest.createdAt > bestSession!!.createdAt) {
                    bestArea = area
                    bestSession = latest
                }
            }
            if (bestArea == null || bestSession == null) {
                v.setTextViewText(R.id.widget_zone, "No checks yet")
                v.setTextViewText(R.id.widget_progress_text, "Open kemas to start a before/after check")
                v.setProgressBar(R.id.widget_progress, 1, 0, false)
                v.setViewVisibility(R.id.widget_before, View.GONE)
                v.setViewVisibility(R.id.widget_after, View.GONE)
                v.setTextViewText(R.id.widget_hint, "")
                return
            }
            val sessions = store.sessionsFor(bestArea.id)
            val done = sessions.count { it.afterPath != null }
            v.setTextViewText(R.id.widget_zone, bestArea.name)
            v.setTextViewText(R.id.widget_progress_text,
                "$done of ${sessions.size} check${if (sessions.size == 1) "" else "s"} done - streak ${store.streakDays()} day${if (store.streakDays() == 1) "" else "s"}")
            v.setProgressBar(R.id.widget_progress, sessions.size, done, false)
            v.setViewVisibility(R.id.widget_before, View.VISIBLE)
            v.setImageViewBitmap(R.id.widget_before, loadScaled(bestSession.beforePath))
            val after = bestSession.afterPath
            if (after == null) {
                v.setViewVisibility(R.id.widget_after, View.GONE)
                v.setTextViewText(R.id.widget_hint, "Check in progress - take the AFTER photo")
            } else {
                v.setViewVisibility(R.id.widget_after, View.VISIBLE)
                v.setImageViewBitmap(R.id.widget_after, loadScaled(after))
                v.setTextViewText(R.id.widget_hint, "")
            }
        }

        private fun loadAsset(c: Context, name: String): Bitmap? = runCatching {
            c.assets.open(name).use { input ->
                BitmapFactory.decodeStream(input)?.let { Bitmap.createScaledBitmap(it, THUMB_W, THUMB_H, true) }
            }
        }.getOrNull()

        private fun loadScaled(path: String): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= THUMB_W) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)?.let { Bitmap.createScaledBitmap(it, THUMB_W, THUMB_H, true) }
        }.getOrNull()
    }
}
