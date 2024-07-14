package com.cliambrown.pilltime;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;

import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.notifications.AlarmBroadcastReceiver;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PillTimeApplication extends Application {

    private Context context;
    DbHelper dbHelper;
    private List<Med> meds = new ArrayList<Med>();
    public static final String CHANNEL_ID = "NOTIFICATION_SERVICE_CHANNEL";

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        dbHelper = new DbHelper(context);
        loadMeds();
        AppCompatDelegate.setDefaultNightMode(ThemeHelper.getThemeFromPrefs(context));
        createNotificationChannel();
    }

    public PillTimeApplication() {
    }

    public void loadMeds() {
        meds.clear();
        meds.addAll(dbHelper.getAllMeds());
        for (Med med : meds) {
            List<Dose> doses = dbHelper.loadDoses(med);
            med.setHasLoadedAllDoses(doses.size() < 21);
            int doseCount = 0;
            for (Dose dose : doses) {
                med.addDoseToEnd(dose);
                ++doseCount;
                if (doseCount >= 20) break;
            }
            med.updateTimes();
        }
//        Toast.makeText(this, meds.toString(), Toast.LENGTH_SHORT).show();
    }

    public void clearMeds() {
        dbHelper.clearDB();
        meds.clear();
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DB_CLEARED");
        sendBroadcast(intent);
    }

    public List<Med> getMeds() {
        return meds;
    }

    public Med getMed(int medID) {
        for (int i=0; i<meds.size(); ++i) {
            Med med = meds.get(i);
            if (med.getId() == medID) {
                return med;
            }
        }
        return null;
    }

    public boolean addMed(Med med) {
        int insertID = dbHelper.insertMed(med);
        if (insertID >= 0) {
            med.setId(insertID);
        } else {
            Toast.makeText(context, "Error saving med", Toast.LENGTH_SHORT).show();
            return false;
        }
        med.updateTimes();
        Med listMed;
        int position = -1;
        for (int i=0; i<meds.size(); ++i) {
            listMed = meds.get(i);
            if (med.sortBefore(listMed)) {
                position = i;
                break;
            }
        }
        if (position == -1) {
            meds.add(med);
        } else {
            meds.add(position, med);
        }
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.MED_ADDED");
        intent.putExtra("medID", med.getId());
        sendBroadcast(intent);
        return true;
    }

    public void setMeds(List<Med> meds) {
        this.meds = meds;
    }

    public boolean setMed(Med med) {
        boolean updated = dbHelper.updateMed(med);
        if (!updated) {
            Toast.makeText(context, "Error updating med", Toast.LENGTH_SHORT).show();
            return false;
        }
        int medID = med.getId();
        Med listMed;
        for (int i=0; i<meds.size(); ++i) {
            listMed = meds.get(i);
            if (listMed.getId() != medID) continue;

            // Check expiry times BEFORE rescheduling (use listMed, not med)
            long now = System.currentTimeMillis() * 1000L;
            long rescheduleBeforeTakenAt = now - (listMed.getDoseHours() * 60L * 60L);

            listMed.setName(med.getName());
            listMed.setMaxDose(med.getMaxDose());
            listMed.setDoseHours(med.getDoseHours());
            listMed.setColor(med.getColor());
            listMed.updateTimes();
            Intent intent = new Intent();
            intent.setAction("com.cliambrown.broadcast.MED_EDITED");
            intent.putExtra("medID", medID);
            sendBroadcast(intent);

            List<Dose> doses = listMed.getDoses();
            for (Dose dose : doses) {
                if (dose.getTakenAt() <= rescheduleBeforeTakenAt) break;
                scheduleNotification(listMed, dose);
            }
            return true;
        }
        return false;
    }

    public void removeMedById(int medID) {
        boolean deleted = dbHelper.deleteMedById(medID);
        if (!deleted) {
            Toast.makeText(context, "Error deleting med", Toast.LENGTH_SHORT).show();
            return;
        }
        int position = -1;
        for (int i=0; i<meds.size(); ++i) {
            if (meds.get(i).getId() != medID) continue;
            position = i;
            break;
        }
        if (position > -1) {
            meds.remove(position);
            Intent intent = new Intent();
            intent.setAction("com.cliambrown.broadcast.MED_REMOVED");
            intent.putExtra("medID", medID);
            sendBroadcast(intent);
        }
    }

    public boolean addDose(Med med, Dose dose) {
        int insertID = dbHelper.insertDose(dose);
        if (insertID < 0) {
            Toast.makeText(context, "Error saving dose", Toast.LENGTH_SHORT).show();
            return false;
        }
        dose.setId(insertID);

        scheduleNotification(med, dose);

        med.addDose(dose);
        med.updateTimes();
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSE_ADDED");
        intent.putExtra("medID", med.getId());
        intent.putExtra("doseID", dose.getId());
        sendBroadcast(intent);
        repositionMed(med);
        return true;
    }

    public void scheduleNotification(Med med, Dose dose) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        intent.putExtra("doseID", dose.getId());
        intent.putExtra("medID", med.getId());
        intent.putExtra("medName", med.getName());
        intent.putExtra("setSilent", !dose.getNotifySound());

        PendingIntent pendingIntent = null;
        pendingIntent = PendingIntent.getBroadcast(context, dose.getId(), intent, FLAG_IMMUTABLE);
        am.cancel(pendingIntent);
        if (!dose.getNotify() || med == null) return;
        long now = System.currentTimeMillis();
        long triggerAtMillis = (dose.getTakenAt() + (med.getDoseHours() * 60L * 60L)) * 1000L;
        if (triggerAtMillis < now) return;
//        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 10 * 60 * 1000, pendingIntent);
    }

    public void repositionMed(Med med) {
        int currentPosition = -1;
        int medID = med.getId();
        Med listMed;
        int listMedID;
        int medsSize = meds.size();
        for (int i=0; i<medsSize; ++i) {
            listMed = meds.get(i);
            listMedID = listMed.getId();
            if (listMedID == medID) {
                currentPosition = i;
                break;
            }
        }
        if (currentPosition == -1) return;
        int newPosition = -1;
        meds.remove(currentPosition);
        medsSize = meds.size();
        for (int j=0; j<medsSize; ++j) {
            listMed = meds.get(j);
            if (med.sortBefore(listMed)) {
                newPosition = j;
                break;
            }
        }
        if (newPosition > -1) {
            meds.add(newPosition, med);
        } else {
            meds.add(med);
            newPosition = meds.size() - 1;
        }
        if (newPosition != currentPosition) {
            Intent intent = new Intent();
            intent.setAction("com.cliambrown.broadcast.MED_MOVED");
            intent.putExtra("fromPosition", currentPosition);
            intent.putExtra("toPosition", newPosition);
            sendBroadcast(intent);
        }
    }

    public boolean setDose(Med med, Dose dose) {
        boolean updated = dbHelper.updateDose(dose);
        if (!updated) {
            Toast.makeText(context, "Error updating dose", Toast.LENGTH_SHORT).show();
            return false;
        }
        med.updateTimes();
        dose.updateTimes(med);
        int fromPosition = med.setDose(dose);
        int toPosition = med.repositionDose(dose, fromPosition);
        Intent intent = new Intent();
        if (toPosition == fromPosition) {
            intent.setAction("com.cliambrown.broadcast.DOSE_EDITED");
            intent.putExtra("medID", med.getId());
            intent.putExtra("doseID", dose.getId());
        } else {
            intent.setAction("com.cliambrown.broadcast.DOSE_MOVED");
            intent.putExtra("fromPosition", fromPosition);
            intent.putExtra("toPosition", toPosition);
        }
        sendBroadcast(intent);
        scheduleNotification(med, dose);
        repositionMed(med);
        return true;
    }

    public void removeDose(Dose dose) {
        dose.setNotify(false);
        int medID = dose.getMedID();
        int doseID = dose.getId();
        dbHelper.deleteDoseById(doseID);
        Med med;
        List<Dose> doses;
        for (int i=0; i<meds.size(); ++i) {
            med = meds.get(i);
            if (med.getId() != medID) continue;
            scheduleNotification(med, dose);
            doses = med.getDoses();
            int position = -1;
            for (int j=0; j<doses.size(); ++j) {
                if (doses.get(j).getId() != doseID) continue;
                position = j;
                break;
            }
            if (position > -1) {
                doses.remove(position);
                med.updateTimes();
                Intent intent = new Intent();
                intent.setAction("com.cliambrown.broadcast.DOSE_REMOVED");
                intent.putExtra("medID", medID);
                intent.putExtra("doseID", doseID);
                sendBroadcast(intent);
                repositionMed(med);
            }
            break;
        }
    }

    public void loadMoreDoses(Med med) {
        List<Dose> doses = dbHelper.loadDoses(med);
        med.setHasLoadedAllDoses(doses.size() < 21);
        List<Integer> doseIDs = new ArrayList<Integer>();
        for (Dose dose : doses) {
            med.addDose(dose);
            doseIDs.add(dose.getId());
            if (doseIDs.size() >= 20) break;
        }
        med.updateTimes();
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSES_ADDED");
        intent.putExtra("medID", med.getId());
        intent.putExtra("doseIDs", (Serializable) doseIDs);
        sendBroadcast(intent);
        repositionMed(med);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void removeDoseAndOlder(Med med, Dose dose) {
        dbHelper.removeDoseAndOlder(med, dose);
        med.removeDoseAndOlder(dose);
        med.updateTimes();
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSES_REMOVED");
        intent.putExtra("medID", med.getId());
        sendBroadcast(intent);
        repositionMed(med);
    }


    public void importFromUri(Uri uri) {
        try {
            String jsonText = Utils.readTextFromUri(uri, context);
            DbHelper dbHelper = new DbHelper(context);
            dbHelper.importFromString(jsonText);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error importing database (IO exception)", Toast.LENGTH_SHORT).show();
        }
        loadMeds();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.cliambrown.broadcast.DB_CLEARED");
        sendBroadcast(broadcastIntent);
    }

    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.areNotificationsEnabled()) return false;
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
            if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
            return true;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }
}
