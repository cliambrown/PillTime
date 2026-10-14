package com.cliambrown.pilltime.widget;

import android.app.PendingIntent;
import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.utilities.Utils;

import java.util.Calendar;

public class TakeDoseWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_TAKE_DOSE =
            "com.cliambrown.pilltime.widget.action.TAKE_DOSE";
    private static final String ACTION_REFRESH_LIMIT =
            "com.cliambrown.pilltime.widget.action.REFRESH_LIMIT";
    private static final String ACTION_REFRESH_DAY =
            "com.cliambrown.pilltime.widget.action.REFRESH_DAY";
    private static final String PREFS_NAME = "take_dose_widget";
    private static final String PREF_MED_ID_PREFIX = "med_id_";

    static void saveMedId(Context context, int appWidgetId, int medId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(PREF_MED_ID_PREFIX + appWidgetId, medId)
                .apply();
    }

    private static int loadMedId(Context context, int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(PREF_MED_ID_PREFIX + appWidgetId, -1);
    }

    private static void deleteMedId(Context context, int appWidgetId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_MED_ID_PREFIX + appWidgetId)
                .apply();
    }

    static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        int medId = loadMedId(context, appWidgetId);
        DbHelper dbHelper = new DbHelper(context);
        Med med = medId > 0 ? dbHelper.getMedById(medId) : null;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.take_dose_widget);
        boolean canTakeDose = false;
        if (med == null) {
            deleteMedId(context, appWidgetId);
            views.setTextViewText(R.id.widget_title,
                    context.getString(R.string.widget_med_unavailable));
            views.setViewVisibility(R.id.widget_taken_today, View.GONE);
            views.setViewVisibility(R.id.widget_ready, View.GONE);
            cancelLimitRefresh(context, appWidgetId);
            cancelDayRefresh(context, appWidgetId);
        } else {
            med.updateTimes(dbHelper);
            views.setTextViewText(R.id.widget_title,
                    context.getString(R.string.widget_take_dose, med.getName()));
            views.setViewVisibility(R.id.widget_taken_today, View.VISIBLE);
            views.setTextViewText(
                    R.id.widget_taken_today,
                    context.getString(
                            R.string.widget_taken_today,
                            Utils.getStrFromDbl(getTodayDoseCount(dbHelper, medId))
                    )
            );
            scheduleDayRefresh(context, appWidgetId);
            canTakeDose = med.getActiveDoseCount() < med.getMaxDose();
            views.setTextViewText(
                    R.id.widget_ready,
                    context.getString(
                            canTakeDose ? R.string.widget_ready : R.string.widget_limit_reached
                    )
            );
            views.setViewVisibility(R.id.widget_ready, View.VISIBLE);
            if (canTakeDose) {
                cancelLimitRefresh(context, appWidgetId);
            } else {
                scheduleLimitRefresh(context, appWidgetId, med);
            }
        }

        views.setBoolean(R.id.widget_root, "setEnabled", canTakeDose);
        views.setFloat(R.id.widget_root, "setAlpha", canTakeDose ? 1.0f : 0.55f);
        views.setOnClickPendingIntent(
                R.id.widget_root,
                canTakeDose ? getTakeDoseIntent(context, appWidgetId) : null
        );
        manager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent getTakeDoseIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, TakeDoseWidgetProvider.class);
        intent.setAction(ACTION_TAKE_DOSE);
        intent.setData(Uri.parse("pilltime://take-dose/" + appWidgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static double getTodayDoseCount(DbHelper dbHelper, int medId) {
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        double count = dbHelper.getDoseCountBetween(
                database,
                medId,
                startOfToday.getTimeInMillis() / 1000L,
                System.currentTimeMillis() / 1000L
        );
        database.close();
        return count;
    }

    private static PendingIntent getDayRefreshIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, TakeDoseWidgetProvider.class);
        intent.setAction(ACTION_REFRESH_DAY);
        intent.setData(Uri.parse("pilltime://refresh-day/" + appWidgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void scheduleDayRefresh(Context context, int appWidgetId) {
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_YEAR, 1);
        nextDay.set(Calendar.HOUR_OF_DAY, 0);
        nextDay.set(Calendar.MINUTE, 0);
        nextDay.set(Calendar.SECOND, 0);
        nextDay.set(Calendar.MILLISECOND, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC,
                nextDay.getTimeInMillis(),
                getDayRefreshIntent(context, appWidgetId)
        );
    }

    private static void cancelDayRefresh(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getDayRefreshIntent(context, appWidgetId));
    }

    private static PendingIntent getLimitRefreshIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, TakeDoseWidgetProvider.class);
        intent.setAction(ACTION_REFRESH_LIMIT);
        intent.setData(Uri.parse("pilltime://refresh-limit/" + appWidgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void scheduleLimitRefresh(Context context, int appWidgetId, Med med) {
        Dose nextExpiringDose = med.getNextExpiringDose();
        if (nextExpiringDose == null) return;

        long triggerAtMillis = (nextExpiringDose.getTakenAt()
                + med.getDoseDurationInSeconds() + 1L) * 1000L;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC,
                triggerAtMillis,
                getLimitRefreshIntent(context, appWidgetId)
        );
    }

    private static void cancelLimitRefresh(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getLimitRefreshIntent(context, appWidgetId));
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TakeDoseWidgetProvider.class);
        for (int appWidgetId : manager.getAppWidgetIds(provider)) {
            updateWidget(context, manager, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, manager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            cancelLimitRefresh(context, appWidgetId);
            cancelDayRefresh(context, appWidgetId);
            deleteMedId(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_TAKE_DOSE.equals(action)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            recordDose(context, appWidgetId);
            return;
        }
        if (ACTION_REFRESH_LIMIT.equals(action) || ACTION_REFRESH_DAY.equals(action)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
            return;
        }
        super.onReceive(context, intent);
    }

    private void recordDose(Context context, int appWidgetId) {
        int medId = loadMedId(context, appWidgetId);
        PillTimeApplication application = (PillTimeApplication) context.getApplicationContext();
        Med med = application.getMed(medId);
        if (med == null) {
            Toast.makeText(context, R.string.widget_med_unavailable, Toast.LENGTH_SHORT).show();
            updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            return;
        }

        med.updateTimes(application.getDbHelper());
        if (med.getActiveDoseCount() >= med.getMaxDose()) {
            updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            return;
        }

        Dose dose = new Dose(
                -1,
                medId,
                med.getDefaultDoseCount(),
                System.currentTimeMillis() / 1000L,
                med.getNetNotifyDefault(context),
                med.getNetNotifySoundDefault(context),
                context
        );
        application.addDose(med, dose);
    }
}
