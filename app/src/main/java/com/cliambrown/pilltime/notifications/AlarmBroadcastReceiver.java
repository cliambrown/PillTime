package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.cliambrown.pilltime.PillTimeApplication.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.MedActivity;

import java.util.Locale;
import java.util.logging.Logger;

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

//        Intent serviceIntent = new Intent(context, NotificationService.class);
//        serviceIntent.putExtra("doseID", doseID);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(serviceIntent);
//        } else {
//            context.startService(serviceIntent);
//        }

        Intent notifIntent = new Intent(context, MedActivity.class);
        notifIntent.putExtra("id", intent.getIntExtra("medID", -1));
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = null;
        pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, FLAG_IMMUTABLE);

        String doseStr = context.getString(R.string.dose);
        String publicTitle = "PillTime: " + doseStr + " " + context.getString(R.string.expired);
        String privateTitle = intent.getStringExtra("medName") + " " + doseStr.toLowerCase(Locale.ROOT) + " " +
                context.getString(R.string.expired);

        NotificationCompat.Builder publicBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                .setContentTitle(publicTitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSilent(intent.getBooleanExtra("setSilent", true));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                .setContentTitle(privateTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicBuilder.build())
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSilent(intent.getBooleanExtra("setSilent", true));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(doseID, builder.build());
    }
}