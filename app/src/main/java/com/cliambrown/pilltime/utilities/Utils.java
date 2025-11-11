package com.cliambrown.pilltime.utilities;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import com.cliambrown.pilltime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.lang.Math;
import java.util.Objects;

public class Utils {

    public static String getStrFromDbl(double val) {
        NumberFormat nf = new DecimalFormat("##.###");
        return nf.format(val);
    }

    public static String simpleFutureTime(Context context, long unixTime) {
        long unixTimeMs = unixTime * 1000L;
        String str = DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_TIME)
                .toLowerCase()
                .replace("a.m.", "am")
                .replace("p.m.", "pm");
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

    public static String getRelativeTimeSpanString(Context context, long unixTime) {
        long now = System.currentTimeMillis() / 1000L;
        long timeDiffSec = Math.round(unixTime - now);
        boolean isPast = (timeDiffSec < 0);
        timeDiffSec = Math.abs(timeDiffSec);
        String str = "";

        class TimeInterval {
            final double maxSeconds;
            final double divisor;
            final String label;
            TimeInterval(double maxSeconds, double divisor, String label) {
                this.maxSeconds = maxSeconds;
                this.divisor = divisor;
                this.label = label;
            }
        }

        TimeInterval[] timeIntervals = {
            new TimeInterval(60d, 1d, context.getString(R.string.seconds_short)),
            new TimeInterval(3600d, 60d, context.getString(R.string.minutes_short)),
            new TimeInterval(86400d, 3600d, context.getString(R.string.hours_short)),
            new TimeInterval(604800d, 86400d, context.getString(R.string.days_short)),
            new TimeInterval(2629800d, 604800d, context.getString(R.string.weeks_short)),
            new TimeInterval(31556952d, 2629800d, context.getString(R.string.months_short)),
            new TimeInterval(-1d, 31556952d, context.getString(R.string.years_short)),
        };

        int intervalsCount = timeIntervals.length;

        for (int i=0; i<intervalsCount; i++) {
            TimeInterval timeInterval = timeIntervals[i];
            if (timeInterval.maxSeconds > 0d && timeDiffSec >= timeInterval.maxSeconds) {
                continue;
            }
            int timeDiff = (int) Math.floor((double) timeDiffSec / timeInterval.divisor);
            str = timeDiff + timeInterval.label;
            timeDiffSec = timeDiffSec - (long) (timeDiff * timeInterval.divisor);
            if (i > 0) {
                TimeInterval prevTimeInterval = timeIntervals[i-1];
                int prevTimeDiff = (int) Math.floor((double) timeDiffSec / prevTimeInterval.divisor);
                str = str + " " + prevTimeDiff + prevTimeInterval.label;
            }
            break;
        }
        if (isPast) str = str + " " + context.getString(R.string.ago);
        else str = context.getString(R.string.in) + " " + str;
        return str;
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
