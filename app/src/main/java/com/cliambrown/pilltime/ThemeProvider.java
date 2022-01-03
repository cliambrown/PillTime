package com.cliambrown.pilltime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class ThemeProvider {

    public static int getThemeFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String selectedTheme = prefs.getString(
                "theme",
                context.getString(R.string.theme_value_system_default)
        );
        return getTheme(context, selectedTheme);
    }

    public static int getTheme(Context context, String themeStr) {
        if (themeStr.equals(context.getString(R.string.theme_value_light))) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (themeStr.equals(context.getString(R.string.theme_value_dark))) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (themeStr.equals(context.getString(R.string.theme_value_auto_battery))) {
            return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static int getDefaultTextColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.textColorPrimary, typedValue, true);
        return typedValue.data;
    }
}
