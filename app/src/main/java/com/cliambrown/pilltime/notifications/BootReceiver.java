package com.cliambrown.pilltime.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.doses.Dose;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            DbHelper dbHelper = new DbHelper(context);
            List<Dose> activeDoses = dbHelper.getActiveDoses();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            for (Dose dose : activeDoses) {
                Intent alarmIntent = new Intent(context, AlarmBroadcastReceiver.class);
                alarmIntent.putExtra("doseID", dose.getId());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, dose.getId(), alarmIntent, 0);
                long triggerAtMillis = dose.getExpiresAt() * 1000L;
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }
}