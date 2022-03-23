package com.protone.seenn.theme

import android.content.Context
import com.protone.database.sp.SpTool
import com.protone.database.sp.toSpProvider

class ThemeProvider(context: Context) {
    private val themeProvider =
        SpTool(
            context
                .getSharedPreferences(
                    THEME_FILE_NAME,
                    Context.MODE_PRIVATE
                ).toSpProvider()
        )

    val isCustomTheme by themeProvider.boolean(
        "CustomTheme",
        false
    )

    companion object {
        @JvmStatic
        val THEME_FILE_NAME = "SeennTheme"
    }
}