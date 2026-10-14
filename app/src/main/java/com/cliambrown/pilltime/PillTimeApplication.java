package com.cliambrown.pilltime;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.doses.InvalidDoseException;
import com.cliambrown.pilltime.meds.InvalidMedException;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.notifications.AlarmBroadcastReceiver;
import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.cliambrown.pilltime.widget.TakeDoseWidgetProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PillTimeApplication extends Application {

    private Context context;
    DbHelper dbHelper;
    private final List<Med> meds = new ArrayList<>();
    public static final String CHANNEL_ID = "NOTIFICATION_SERVICE_CHANNEL";
    public String medSort;

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        dbHelper = new DbHelper(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        medSort = prefs.getString("med_sort", getString(R.string.med_sort_value_latest));
        loadMeds();
        AppCompatDelegate.setDefaultNightMode(ThemeHelper.getThemeFromPrefs(context));
        createNotificationChannel();
    }

    public DbHelper getDbHelper() { return dbHelper; }

    public void showToast(int stringID) {
        showToast(getString(stringID));
    }
    public void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public void handleDbError(String introMessage, String message) {
        showToast(introMessage + " " + message);
    }

    public void handleDbError(int introMessageID, int messageID) {
        showToast(getString(introMessageID) + " " + getString(messageID));
    }

    public void handleDbError(String introMessage, Exception e) {
        Throwable cause = e.getCause();
        String message;
        if (cause != null) message = cause.getLocalizedMessage();
        else message = e.getLocalizedMessage();
        handleDbError(introMessage, message);
    }

    public void handleMedSaveError(String message) {
        handleDbError(getString(R.string.error_saving_med), message);
    }

    public void handleMedSaveError(Exception e) {
        handleDbError(getString(R.string.error_saving_med), e);
    }

    public void handleDoseSaveError(String message) {
        handleDbError(getString(R.string.error_saving_dose), message);
    }

    public void handleDoseSaveError(Exception e) {
        handleDbError(getString(R.string.error_saving_dose), e);
    }

    public void loadMeds() {
        meds.clear();
        meds.addAll(dbHelper.getAllMeds());
        for (Med med : meds) {
            List<Dose> doses = dbHelper.getDoses(med);
            med.setHasLoadedAllDoses(doses.size() < 21);
            int doseCount = 0;
            for (Dose dose : doses) {
                med.addDoseToEnd(dose);
                ++doseCount;
                if (doseCount >= 20) break;
            }
            med.updateTimes(dbHelper);
        }
    }

    public void clearMeds() {
        dbHelper.clearDB();
        meds.clear();
        TakeDoseWidgetProvider.updateAllWidgets(context);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DB_CLEARED");
        sendBroadcast(intent);
        showToast(R.string.database_cleared);
    }

    public void repairDb() {
        dbHelper.repairDb();
        showToast(R.string.database_repaired);
    }

    public List<Med> getMeds() {
        return meds;
    }

    public Med getMed(int medID) {
        for (Med med : meds) {
            if (med.getId() == medID) return med;
        }
        return null;
    }

    public boolean saveNewMed(Med med) {
        int insertID;
        try {
            insertID = dbHelper.insertMed(med);
        } catch (InvalidMedException | SQLException e) {
            handleMedSaveError(e);
            return false;
        }
        if (insertID < 0) {
            handleMedSaveError(getString(R.string.error_saving_to_database));
            return false;
        }
        med.setId(insertID);
        med.updateTimes(dbHelper);
        Med listMed;
        int position = -1;
        for (int i=0; i<meds.size(); ++i) {
            listMed = meds.get(i);
            if (med.sortBefore(listMed, medSort, context)) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            meds.add(position, med);
        } else {
            meds.add(med);
        }
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.MED_ADDED");
        intent.putExtra("medID", med.getId());
        sendBroadcast(intent);
        showToast(R.string.med_saved);
        return true;
    }

    public boolean setMed(Med med) {
        boolean updated;
        try {
            updated = dbHelper.updateMed(med);
        } catch (InvalidMedException | SQLException e) {
            handleMedSaveError(e);
            return false;
        }
        if (!updated) {
            handleMedSaveError(getString(R.string.error_saving_to_database));
            return false;
        }
        int medID = med.getId();
        for (Med listMed : meds) {
            if (listMed.getId() == medID) {
                // Check expiry times BEFORE rescheduling (use listMed, not med)
                long now = System.currentTimeMillis() * 1000L;
                long rescheduleBeforeTakenAt = now - listMed.getDoseDurationInSeconds();

                listMed.setName(med.getName());
                listMed.setMaxDose(med.getMaxDose());
                listMed.setDoseHours(med.getDoseHours());
                listMed.setColor(med.getColor());
                listMed.setIsInventoryTracked(med.getIsInventoryTracked());
                listMed.setReportedInventory(med.getReportedInventory());
                listMed.setInventoryReportedAt(med.getInventoryReportedAt());
                listMed.setDefaultDoseCount(med.getDefaultDoseCount());
                listMed.setShowDayDoseCount(med.getShowDayDoseCount());
                listMed.setOverrideGlobalNotifyDefaults(med.getOverrideGlobalNotifyDefaults());
                listMed.setNotifyDefault(med.getNotifyDefault());
                listMed.setNotifySoundDefault(med.getNotifySoundDefault());
                listMed.updateTimes(dbHelper);
                TakeDoseWidgetProvider.updateAllWidgets(context);
                Intent intent = new Intent();
                intent.setAction("com.cliambrown.broadcast.MED_EDITED");
                intent.putExtra("medID", med.getId());
                sendBroadcast(intent);

                if (medSort.equals(getString(R.string.med_sort_value_alpha))) repositionMed(listMed);

                List<Dose> doses = listMed.getDoses();
                for (Dose dose : doses) {
                    if (dose.getTakenAt() <= rescheduleBeforeTakenAt) break;
                    scheduleNotification(listMed, dose);
                }
                showToast(R.string.med_saved);
                return true;
            }
        }
        return false;
    }

    public boolean archiveMed(Med med, boolean doArchive) {
        boolean updated;
        med.setArchived(doArchive);
        try {
            updated = dbHelper.updateMed(med);
        } catch (InvalidMedException | SQLException e) {
            handleMedSaveError(e);
            return false;
        }
        if (!updated) {
            handleMedSaveError(getString(R.string.error_saving_to_database));
            return false;
        }
        Med listMed = getMed(med.getId());
        listMed.setArchived(doArchive);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.MED_ARCHIVED_OR_UNARCHIVED");
        intent.putExtra("medID", med.getId());
        sendBroadcast(intent);
        if (doArchive) {
            showToast(R.string.med_archived);
        } else {
            showToast(R.string.med_unarchived);
        }
        return false;
    }

    public void deleteMedById(int medID) {
        boolean deleted = dbHelper.deleteMedById(medID);
        if (!deleted) {
            showToast(R.string.error_deleting_med);
            return;
        }
        for (int i = 0; i < meds.size(); ++i) {
            if (meds.get(i).getId() == medID) {
                meds.remove(i);
                break;
            }
        }
        TakeDoseWidgetProvider.updateAllWidgets(context);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.MED_REMOVED");
        intent.putExtra("medID", medID);
        sendBroadcast(intent);
        showToast(R.string.med_deleted);
    }

    public void sortMeds() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        medSort = prefs.getString("med_sort", getString(R.string.med_sort_value_latest));
        loadMeds();
    }

    public boolean addDose(Med med, Dose dose) {
        int insertID;
        try {
            insertID = dbHelper.insertDose(dose);
        } catch (InvalidDoseException | SQLException e) {
            handleDoseSaveError(e);
            return false;
        }
        if (insertID < 0) {
            handleDoseSaveError(getString(R.string.error_saving_to_database));
            return false;
        }
        dose.setId(insertID);

        scheduleNotification(med, dose);

        med.addDose(dose);
        med.updateTimes(dbHelper);
        repositionMed(med);
        TakeDoseWidgetProvider.updateAllWidgets(context);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSE_ADDED");
        intent.putExtra("medID", med.getId());
        intent.putExtra("doseID", dose.getId());
        sendBroadcast(intent);
        showToast(R.string.dose_saved);
        return true;
    }

    public void scheduleNotification(Med med, Dose dose) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        intent.putExtra("doseID", dose.getId());
        intent.putExtra("medID", med.getId());
        intent.putExtra("medName", med.getName());
        intent.putExtra("setSilent", !dose.getNotifySound());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, dose.getId(), intent, FLAG_IMMUTABLE);
        am.cancel(pendingIntent);
        if (!dose.getNotify()) return;
        long now = System.currentTimeMillis();
        long triggerAtMillis = (dose.getTakenAt() + med.getDoseDurationInSeconds()) * 1000L;
        if (triggerAtMillis < now) return;
//        Exact alarm would probably be preferred but caused too many permission complexities
//        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//        Instead, set an inexact alarm with 10-minute window (the minimum allowed)
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
            if (med.sortBefore(listMed, medSort, context)) {
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
            intent.putExtra("medID", med.getId());
            sendBroadcast(intent);
        }
    }

    public boolean setDose(Med med, Dose dose) {

        boolean updated;
        try {
            updated = dbHelper.updateDose(dose);
        } catch (InvalidDoseException | SQLException e) {
            handleDoseSaveError(e);
            return false;
        }
        if (!updated) {
            handleDoseSaveError(getString(R.string.error_saving_to_database));
            return false;
        }

        med.updateTimes(dbHelper);
        dose.updateTimes(med);
        int fromPosition = med.setDose(dose);
        int toPosition = med.repositionDose(dose, fromPosition);
        Intent intent = new Intent();
        intent.putExtra("medID", med.getId());
        intent.putExtra("doseID", dose.getId());
        if (toPosition != fromPosition) {
            intent.setAction("com.cliambrown.broadcast.DOSE_MOVED");
            intent.putExtra("fromPosition", fromPosition);
            intent.putExtra("toPosition", toPosition);
            sendBroadcast(intent);
        } else {
            intent.setAction("com.cliambrown.broadcast.DOSE_EDITED");
        }
        sendBroadcast(intent);
        scheduleNotification(med, dose);
        repositionMed(med);
        TakeDoseWidgetProvider.updateAllWidgets(context);
        showToast(R.string.dose_saved);
        return true;
    }

    public void removeDose(Dose dose) {
        dose.setNotify(false);
        dbHelper.deleteDoseById(dose.getId());
        for (Med med : meds) {
            if (med.getId() == dose.getMedID()) {
                scheduleNotification(med, dose);
                List<Dose> doses = med.getDoses();
                for (int i = 0; i < doses.size(); ++i) {
                    if (doses.get(i).getId() == dose.getId()) {
                        doses.remove(i);
                        med.updateTimes(dbHelper);
                        Intent intent = new Intent();
                        intent.setAction("com.cliambrown.broadcast.DOSE_REMOVED");
                        intent.putExtra("medID", med.getId());
                        intent.putExtra("doseID", dose.getId());
                        sendBroadcast(intent);
                        repositionMed(med);
                        TakeDoseWidgetProvider.updateAllWidgets(context);
                        showToast(R.string.dose_deleted);
                        return;
                    }
                }
                return;
            }
            break;
        }
    }

    public void loadMoreDoses(Med med) {
        List<Dose> moreDoses = dbHelper.getDoses(med);
        med.setHasLoadedAllDoses(moreDoses.size() < 21);
        List<Integer> doseIDs = new ArrayList<>();
        for (Dose dose : moreDoses) {
            med.addDose(dose);
            doseIDs.add(dose.getId());
            if (doseIDs.size() >= 20) break;
        }
        med.updateTimes(dbHelper);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSES_LOADED");
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
        med.updateTimes(dbHelper);
        Intent intent = new Intent();
        intent.setAction("com.cliambrown.broadcast.DOSES_REMOVED");
        intent.putExtra("medID", med.getId());
        sendBroadcast(intent);
        repositionMed(med);
        TakeDoseWidgetProvider.updateAllWidgets(context);
        showToast(R.string.doses_deleted);
    }

    public void importFromUri(Uri uri) {
        boolean imported = false;
        try (DbHelper dbHelper = new DbHelper(context)) {
            String jsonText = Utils.readTextFromUri(uri, context);
            imported = dbHelper.importFromString(jsonText);
        } catch (IOException e) {
            e.printStackTrace();
            handleDbError(R.string.error_importing_database, R.string.io_exception);
        } catch (Exception e) {
            e.printStackTrace();
            handleDbError(getString(R.string.error_importing_database), e.getMessage());
        }
        if (imported) {
            loadMeds();
            TakeDoseWidgetProvider.updateAllWidgets(context);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.cliambrown.broadcast.DATA_IMPORTED");
            sendBroadcast(broadcastIntent);
            showToast(R.string.data_imported);
        }
    }

    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.areNotificationsEnabled()) return false;
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
            return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }
}
