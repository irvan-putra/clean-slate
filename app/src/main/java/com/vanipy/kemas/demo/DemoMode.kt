package com.vanipy.kemas.demo

import android.content.Context
import com.vanipy.kemas.widget.KemasWidgetProvider

/** Opt-in switch that makes the widget show the bundled made-up zone instead of real checks. */
object DemoMode {
    private const val PREFS = "kemas_prefs"
    private const val KEY = "demo_mode"

    fun isOn(c: Context): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun set(c: Context, on: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        KemasWidgetProvider.updateAll(c)
    }
}
