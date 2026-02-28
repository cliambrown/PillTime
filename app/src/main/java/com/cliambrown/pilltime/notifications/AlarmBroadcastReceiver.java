package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.cliambrown.pilltime.PillTimeApplication.CHANNEL_ID;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.MedActivity;

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

        Intent notifIntent = new Intent(context, MedActivity.class);
        notifIntent.putExtra("id", intent.getIntExtra("medID", -1));
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, FLAG_IMMUTABLE);

        String publicTitle = context.getString(R.string.app_name) + ": " +
                context.getString(R.string.notification_public_title);
        String medName = intent.getStringExtra("medName");
        String privateTitle = context.getString(R.string.notification_private_title, medName);

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

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.w(AlarmBroadcastReceiver.class.getName(), "Can't show notification. Permission NOT granted!");
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(doseID, builder.build());
    }
}