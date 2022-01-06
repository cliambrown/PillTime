package com.cliambrown.pilltime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int doseID;
        try {
            doseID = intent.getIntExtra("doseID", -1);
        } catch (Exception e) {
            return;
        }
        if (doseID == -1) return;
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.putExtra("doseID", doseID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}