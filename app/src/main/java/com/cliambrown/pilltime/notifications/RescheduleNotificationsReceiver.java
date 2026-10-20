package com.cliambrown.pilltime.notifications;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class RescheduleNotificationsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (
                Objects.equals(action, "android.intent.action.BOOT_COMPLETED")
                || Objects.equals(action, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        ) {
            NotificationsHelper.rescheduleAllNotifications(context);
        }
    }
}