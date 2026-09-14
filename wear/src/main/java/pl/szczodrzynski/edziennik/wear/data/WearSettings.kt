/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.data

import android.content.Context
import pl.szczodrzynski.edziennik.wear.ui.theme.WearTheme

object WearSettings {
    private const val PREFS = "wear_settings"
    private const val KEY_THEME = "theme"

    private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getTheme(context: Context): WearTheme =
            WearTheme.fromKey(prefs(context).getString(KEY_THEME, null))

    fun setTheme(context: Context, theme: WearTheme) {
        prefs(context).edit().putString(KEY_THEME, theme.key).apply()
    }
}
