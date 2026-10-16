package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.DbHelper;

import java.util.List;
import java.util.Objects;

public class RecreateNotificationsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("CLB_TAG", "RecreateNotificationsReceiver action = " + action);
        if (
                Objects.equals(action, "android.intent.action.BOOT_COMPLETED")
                || Objects.equals(action, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        ) {
            try (DbHelper dbHelper = new DbHelper(context)) {
                List<Dose> activeDoses = dbHelper.getActiveDoses();
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                boolean canSetExactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
                for (Dose dose : activeDoses) {
                    Med med = dbHelper.getMedById(dose.getMedID());
                    if (med == null) continue;
                    Intent alarmIntent = new Intent(context, AlarmBroadcastReceiver.class);
                    alarmIntent.putExtra("doseID", dose.getId());
                    alarmIntent.putExtra("medID", med.getId());
                    alarmIntent.putExtra("medName", med.getName());
                    alarmIntent.putExtra("setSilent", !dose.getNotifySound());
                    PendingIntent pendingIntent =
                            PendingIntent.getBroadcast(context, dose.getId(), alarmIntent, FLAG_IMMUTABLE);
                    long triggerAtMillis = dose.getExpiresAt() * 1000L;
                    if (canSetExactAlarms) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    } else {
                        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 10 * 60 * 1000, pendingIntent);
                    }
                }
            }
        }
    }
}