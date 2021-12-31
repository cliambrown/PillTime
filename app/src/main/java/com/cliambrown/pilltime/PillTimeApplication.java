package com.cliambrown.pilltime;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
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

    private void loadMeds() {
        meds = dbHelper.getAllMeds();
        Toast.makeText(this, meds.toString(), Toast.LENGTH_SHORT).show();
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
            if (meds.get(i).getId() == medID) {
                meds.set(i, med);
                return true;
            }
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
            if (meds.get(i).getId() == medID) {
                position = i;
                break;
            }
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
        } else {
            Toast.makeText(context, "Error saving dose", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void removeDose(Dose dose) {
        int medID = dose.getMedID();
        int doseID = dose.getId();
        dbHelper.deleteDoseById(doseID);
        Med med;
        for (int i=0; i<meds.size(); ++i) {
            med = meds.get(i);
            if (med.getId() == medID) {
                List<Dose> doses = med.getDoses();
                int position = -1;
                for (int j=0; j<doses.size(); ++j) {
                    if (doses.get(j).getId() == doseID) {
                        position = j;
                        break;
                    }
                }
                if (position > -1) {
                    doses.remove(position);
                }
            }
        }
    }
}
