package com.cliambrown.pilltime.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.R;

public class ThemeHelper {

    public static int getThemeFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String selectedTheme = prefs.getString("theme", "");
        return getTheme(context, selectedTheme);
    }

    public static int getTheme(Context context, String themeStr) {
        if (themeStr.equals(context.getString(R.string.theme_value_dark))) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (themeStr.equals(context.getString(R.string.theme_value_light))) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (themeStr.equals(context.getString(R.string.theme_value_system_default))) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        if (themeStr.equals(context.getString(R.string.theme_value_auto_battery))) {
            return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
        }
        if (usingDarkMode(context)) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        return AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static int getThemeAttr(int attr, Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private static boolean usingDarkMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
