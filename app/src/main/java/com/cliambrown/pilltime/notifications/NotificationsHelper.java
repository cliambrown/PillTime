package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.DbHelper;

import java.util.List;

public class NotificationsHelper {

    public static void rescheduleAllNotifications(Context context) {
        try (DbHelper dbHelper = new DbHelper(context)) {
            List<Dose> activeDoses = dbHelper.getActiveDoses();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                am.cancelAll();
            }
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    am.cancel(pendingIntent);
                }
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
