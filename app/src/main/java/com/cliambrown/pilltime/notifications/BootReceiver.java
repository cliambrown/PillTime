package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.doses.Dose;

import java.util.List;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            DbHelper dbHelper = new DbHelper(context);
            List<Dose> activeDoses = dbHelper.getActiveDoses();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            for (Dose dose : activeDoses) {
                Med med = dbHelper.getMedById(dose.getMedID());
                Intent alarmIntent = new Intent(context, AlarmBroadcastReceiver.class);
                alarmIntent.putExtra("doseID", dose.getId());
                alarmIntent.putExtra("medID", med.getId());
                alarmIntent.putExtra("medName", med.getName());
                alarmIntent.putExtra("setSilent", !dose.getNotifySound());
                PendingIntent pendingIntent = null;
                pendingIntent = PendingIntent.getBroadcast(context, dose.getId(), alarmIntent, FLAG_IMMUTABLE);
                long triggerAtMillis = dose.getExpiresAt() * 1000L;
//                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 10 * 60 * 1000, pendingIntent);
            }
        }
    }
}