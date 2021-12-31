package com.cliambrown.pilltime;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
    }

    public PillTimeApplication() {
//        PillTimeApplication.context = getApplicationContext();
//        dbHelper = new DbHelper(context);
//        loadMeds();
    }

    private void loadMeds() {
        meds = dbHelper.getAllMeds();
        Toast.makeText(this, meds.toString(), Toast.LENGTH_SHORT).show();
    }

    public static List<Med> getMeds() {
        return meds;
    }

    public static Med getMed(int medID) {
        for (int i=0; i<meds.size(); ++i) {
            Med med = meds.get(i);
            if (med.getId() == medID) {
                return med;
            }
        }
        return null;
    }

    public static void addMed(Med med) {
        meds.add(med);
    }

    public static void setMeds(List<Med> meds) {
        PillTimeApplication.meds = meds;
    }

    public static void setMed(Med med) {
        int medID = med.getId();
        for (int i=0; i<meds.size(); ++i) {
            if (meds.get(i).getId() == medID) {
                meds.set(i, med);
                break;
            }
        }
    }

    public static Context getAppContext() {
        return PillTimeApplication.context;
    }

    public boolean removeMedById(int medID) {
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
}
