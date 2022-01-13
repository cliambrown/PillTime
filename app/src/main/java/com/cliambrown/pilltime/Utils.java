package com.cliambrown.pilltime;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public class Utils {

    public static String getStrFromDbl(double val) {
        NumberFormat nf = new DecimalFormat("##.###");
        return nf.format(val);
    }

    public static String decapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }

        char c[] = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);

        return new String(c);
    }

    public static String simpleFutureTime(Context context, long unixTime) {
        long unixTimeMs = unixTime * 1000L;
        String str = DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase();
        long now = System.currentTimeMillis() / 1000L;
        long timeDiffSec = unixTime - now;
        if (timeDiffSec < (24 * 60 * 60)) {
            return str;
        }
        str = str + " " + DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
        if (timeDiffSec < (365 * 24 * 60 * 60)) {
            return str;
        }
        return str + " " + DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_YEAR);
    }

    public static int getResourceIdentifier(Context context, String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public static String readTextFromUri(Uri uri, Context context) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }
}
