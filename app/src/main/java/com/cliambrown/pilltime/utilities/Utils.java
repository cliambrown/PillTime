package com.cliambrown.pilltime.utilities;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.ParcelableSpan;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.R;

import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Utils {

    public static String getStrFromDbl(double val) {
        NumberFormat nf = new DecimalFormat("##.###");
        return nf.format(val);
    }

    public static String buildTimeOnDateString(Context context, long unixTime) {
        long unixTimeMs = unixTime * 1000L;
        String timeStr = DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase();
        String dateStr = DateUtils.formatDateTime(context, unixTimeMs, DateUtils.FORMAT_ABBREV_ALL |
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
        return context.getString(R.string.time_on_date, timeStr, dateStr);
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

    public static String getRelativeTimeSpanString(Context context, long unixTime, boolean preferPast) {
        long now = System.currentTimeMillis() / 1000L;
        long timeDiffSec = Math.round(unixTime - now);
        boolean isPast;
        if (timeDiffSec == 0) isPast = preferPast;
        else isPast = (timeDiffSec < 0);
        timeDiffSec = Math.abs(timeDiffSec);

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

        String timeDiffString = "";
        for (int i=0; i<intervalsCount; i++) {
            TimeInterval timeInterval = timeIntervals[i];
            if (timeInterval.maxSeconds > 0d && timeDiffSec >= timeInterval.maxSeconds) {
                continue;
            }
            int timeDiff = (int) Math.floor((double) timeDiffSec / timeInterval.divisor);
            timeDiffString = timeDiff + timeInterval.label;
            timeDiffSec = timeDiffSec - (long) (timeDiff * timeInterval.divisor);
            if (i > 0) {
                TimeInterval prevTimeInterval = timeIntervals[i-1];
                int prevTimeDiff = (int) Math.floor((double) timeDiffSec / prevTimeInterval.divisor);
                if (prevTimeDiff > 0) { // Never show "1d 0h"
                    timeDiffString += " " + prevTimeDiff + prevTimeInterval.label;
                } else if (i > 2) { // if first part is at least in days, go down one layer deeper to "1d 3m" for example"
                    prevTimeInterval = timeIntervals[i-2];
                    prevTimeDiff = (int) Math.floor((double) timeDiffSec / prevTimeInterval.divisor);
                    if (prevTimeDiff > 0) { // Don't show "1d 0m" either
                        timeDiffString += " " + prevTimeDiff + prevTimeInterval.label;
                    }
                }
            }
            break;
        }
        int timeStringRes = isPast ? R.string.time_past_ago : R.string.time_future_in;
        return context.getString(timeStringRes, timeDiffString);
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

    public static SpannableString buildTakenInPastString(Context context, @AttrRes int colorAttrResId, double takenAmount, int doseHours) {
        String takenInPastRaw;
        int dose = doseHours;
        if (doseHours % 24 == 0) {
            dose /= 24;
            takenInPastRaw = context.getResources().getQuantityString(R.plurals.taken_in_past_days, dose);
        } else {
            takenInPastRaw = context.getResources().getQuantityString(R.plurals.taken_in_past_hours, dose);
        }

        String takenAmountString = Utils.getStrFromDbl(takenAmount);

        List<List<ParcelableSpan>> spansList = new ArrayList<>();
        List<ParcelableSpan> spans = new ArrayList<>();
        spans.add(new RelativeSizeSpan(1.5f));
        int color = ThemeHelper.getThemeAttr(colorAttrResId, context);
        spans.add(new ForegroundColorSpan(color));
        spans.add(new StyleSpan(Typeface.BOLD));
        spansList.add(spans);
        return styleString(takenInPastRaw, spansList, takenAmountString, dose);
    }

    /**
     * Styles a given string by adding the given style-spans only to the parts that will be substituted with the given
     * formatArgs
     * @param unformatted The unformatted String. Still contains "%1$s" placeholders.
     * @param spansList A 2D-List of spans that will be applied to the parts that will be substituted with the given
     *                 formatArgs
     * @param formatArgs The formatArgs that are substituted with the placeholders
     * @return A SpannableString that contains the formatted String with all formatArgs and all style-spans. Can be used
     * directly in .setText(...)
     */
    public static SpannableString styleString(String unformatted, List<List<ParcelableSpan>> spansList, Object... formatArgs) {
        String formatted = String.format(Locale.getDefault(), unformatted, formatArgs);
        SpannableString spannableString = new SpannableString(formatted);
        int i = 0;
        int correction = 0;
        for (List<ParcelableSpan> spans : spansList) {
            String param = "%" + (i + 1) + "$s";
            int start = unformatted.indexOf(param) - correction;
            int argLength = String.valueOf(formatArgs[i]).length();
            int end = start + argLength;
            correction += 4 - argLength;
            for (ParcelableSpan span : spans) {
                spannableString.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            i++;
        }
        return spannableString;
    }

    public static String buildMaxDosePerHourString(Context context, int maxDose, int doseHours) {
        String maxDosePerHour;
        if (doseHours % 24 == 0) {
            int doseDays = doseHours / 24;
            maxDosePerHour = context.getResources().getQuantityString(R.plurals.max_dose_per_day, doseDays, maxDose,
                    doseDays);
        } else {
            maxDosePerHour = context.getResources().getQuantityString(R.plurals.max_dose_per_hour, doseHours, maxDose,
                    doseHours);
        }
        return maxDosePerHour;
    }

    public static class BottomOffsetDecoration extends RecyclerView.ItemDecoration {
        private final int mBottomOffset;

        public BottomOffsetDecoration(int bottomOffset) {
            mBottomOffset = bottomOffset;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, RecyclerView.@NonNull State state) {
            super.getItemOffsets(outRect, view, parent, state);

            int dataSize = state.getItemCount();
            int position = parent.getChildAdapterPosition(view);
            if (dataSize > 0 && position == dataSize - 1) {
                outRect.set(0, 0, 0, mBottomOffset);
            } else {
                outRect.set(0, 0, 0, 0);
            }

        }
    }
}
