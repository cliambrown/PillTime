package com.cliambrown.pilltime.notifications;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import static com.cliambrown.pilltime.PillTimeApplication.CHANNEL_ID;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cliambrown.pilltime.MainActivity;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.MedActivity;
import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;

import java.util.Locale;

public class NotificationService extends Service {
    public NotificationService() {
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) return START_NOT_STICKY;

        boolean cancel = intent.getBooleanExtra("cancel", false);
        if (cancel) {
            stopForeground(true);
            stopSelf();
        }

        int doseID = -1;
        Dose dose = null;
        Med med = null;
        try {
            doseID = intent.getIntExtra("doseID", -1);
        } catch (Exception e) {
            return START_NOT_STICKY;
        }
        DbHelper dbHelper = new DbHelper(this);
        if (doseID < 0) return START_NOT_STICKY;
        dose = dbHelper.getDoseById(doseID);
        if (dose == null) return START_NOT_STICKY;
        if (!dose.getNotify()) return START_NOT_STICKY;
        med = dbHelper.getMedById(dose.getMedID());
        if (med == null) return START_NOT_STICKY;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent fgPendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("PillTime notification service")
//                        .setContentText("Preparing notification")
                        .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                        .setContentIntent(fgPendingIntent)
//                        .setTicker("Preparing notification")
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .build();
            startForeground(med.getId(), notification);
        }


        String publicTitle = "PillTime: " + getString(R.string.dose) + " " + getString(R.string.expired);

        String textTitle = med.getName() + " " + getString(R.string.dose).toLowerCase(Locale.ROOT) + " " +
                getString(R.string.expired);
        String textContent = Utils.getStrFromDbl(med.getActiveDoseCount()) + " " +
                getString(R.string.taken_in_past) + " " +
                med.getDoseHours() + " " + getString(R.string.hours);

        Intent notifIntent = new Intent(this, MedActivity.class);
        notifIntent.putExtra("id", med.getId());
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);
        PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(this, 0, notifIntent, FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(this, 0, notifIntent, 0);
        }

        Intent deleteIntent = new Intent(this, NotificationService.class);
        deleteIntent.putExtra("cancel", true);
        PendingIntent pendingDeleteIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        NotificationCompat.Builder publicBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                .setContentTitle(publicTitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSilent(!dose.getNotifySound())
                .setDeleteIntent(pendingDeleteIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicBuilder.build())
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSilent(!dose.getNotifySound())
                .setDeleteIntent(pendingDeleteIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(dose.getId(), builder.build());

        stopForeground(true);
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}