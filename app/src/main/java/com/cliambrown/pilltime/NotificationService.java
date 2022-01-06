package com.cliambrown.pilltime;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import static com.cliambrown.pilltime.PillTimeApplication.CHANNEL_ID;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

public class NotificationService extends Service {
    public NotificationService() {
    }

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

        String publicTitle = "PillTime: " + getString(R.string.dose) + " " + getString(R.string.expired);

        String textTitle = med.getName() + " " + getString(R.string.dose).toLowerCase(Locale.ROOT) + " " +
                getString(R.string.expired);
        String textContent = Utils.getStrFromDbl(med.getCurrentTotalDoseCount()) + " " +
                getString(R.string.taken_in_past) + " " +
                med.getDoseHours() + " " + getString(R.string.hours);

        Intent notifIntent = new Intent(this, MedActivity.class);
        notifIntent.putExtra("id", med.getId());
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);

        Intent deleteIntent = new Intent(this, NotificationService.class);
        deleteIntent.putExtra("cancel", true);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder publicBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_access_time_24)
                .setContentTitle(publicTitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

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

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}