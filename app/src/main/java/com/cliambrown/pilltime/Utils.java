package com.cliambrown.pilltime;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

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
        String str = DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_TIME);
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
}
