package com.cliambrown.pilltime;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PillTimeApplication extends Application {

    private static Context context;
    DbHelper dbHelper;
    private static List<Med> meds = new ArrayList<Med>();

    public void onCreate() {
        super.onCreate();
        PillTimeApplication.context = getApplicationContext();
        dbHelper = new DbHelper(context);
        loadMeds();
        AppCompatDelegate.setDefaultNightMode(ThemeProvider.getThemeFromPrefs(context));
    }

    public PillTimeApplication() {
    }

    public void loadMeds() {
        meds = dbHelper.getAllMeds();
//        Toast.makeText(this, meds.toString(), Toast.LENGTH_SHORT).show();
    }

    public void clearMeds() {
        dbHelper.deleteDB();
        meds = new ArrayList<Med>();
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
        meds.add(0, med);
        return true;
    }

    public void setMeds(List<Med> meds) {
        PillTimeApplication.meds = meds;
    }

    public boolean setMed(Med med) {
        boolean updated = dbHelper.updateMed(med);
        if (!updated) {
            Toast.makeText(context, "Error updating med", Toast.LENGTH_SHORT).show();
            return false;
        }
        int medID = med.getId();
        for (int i=0; i<meds.size(); ++i) {
            if (meds.get(i).getId() != medID) continue;
            meds.set(i, med);
            return true;
        }
        return false;
    }

    public static Context getAppContext() {
        return PillTimeApplication.context;
    }

    public boolean removeMedById(int medID) {
        boolean deleted = dbHelper.deleteMedById(medID);
        if (!deleted) {
            Toast.makeText(context, "Error deleting med", Toast.LENGTH_SHORT).show();
            return false;
        }
        int position = -1;
        for (int i=0; i<meds.size(); ++i) {
            if (meds.get(i).getId() != medID) continue;
            position = i;
            break;
        }
        if (position > -1) {
            meds.remove(position);
            return true;
        }
        return false;
    }

    public boolean addDose(Med med, Dose dose) {
        int insertID = dbHelper.insertDose(dose);
        if (insertID >= 0) {
            dose.setId(insertID);
            med.addDose(dose);
            repositionMed(med);
        } else {
            Toast.makeText(context, "Error saving dose", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void repositionMed(Med med) {
        int currentPosition = -1;
        int newPosition = -1;
        long lastTakenAt = med.getLastTakenAt();
        int medID = med.getId();
        Med listMed;
        int listMedID;
        int medsSize = meds.size();
        for (int i=0; i<medsSize; ++i) {
            listMed = meds.get(i);
            listMedID = listMed.getId();
            if (listMedID == medID) {
                currentPosition = i;
                if (newPosition > -1) break;
                continue;
            }
            if (newPosition > -1) continue;
            long takenDiff = listMed.getLastTakenAt() - lastTakenAt;
            if (takenDiff < 0 || (takenDiff == 0 && medID > listMedID)) {
                newPosition = i;
                if (currentPosition > -1) break;
            }
        }
        if (newPosition == -1) {
            newPosition = medsSize - 1;
        }
        Log.d("clb", "lastTakenAt = " + lastTakenAt);
        Log.d("clb", currentPosition + " to " + newPosition);
        Collections.swap(meds, currentPosition, newPosition);
    }

    public boolean setDose(Med med, Dose dose) {
        boolean updated = dbHelper.updateDose(dose);
        if (!updated) {
            Toast.makeText(context, "Error updating dose", Toast.LENGTH_SHORT).show();
            return false;
        }
        med.setDose(dose);
        repositionMed(med);
        return true;
    }

    public void removeDose(Dose dose) {
        int medID = dose.getMedID();
        int doseID = dose.getId();
        dbHelper.deleteDoseById(doseID);
        Med med;
        List<Dose> doses;
        for (int i=0; i<meds.size(); ++i) {
            med = meds.get(i);
            if (med.getId() != medID) continue;
            doses = med.getDoses();
            int position = -1;
            for (int j=0; j<doses.size(); ++j) {
                if (doses.get(j).getId() != doseID) continue;
                position = j;
                break;
            }
            if (position > -1) {
                doses.remove(position);
                repositionMed(med);
            }
            break;
        }
    }
}
