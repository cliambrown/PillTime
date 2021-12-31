package com.cliambrown.pilltime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbHelper extends SQLiteOpenHelper {

    public static final String MEDS_TABLE = "meds";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MED_NAME = "med_name";
    public static final String COLUMN_MAX_DOSE = "max_dose";
    public static final String COLUMN_DOSE_HOURS = "dose_hours";

    public static final String DOSES_TABLE = "doses";
    public static final String COLUMN_MED_ID = "med_id";
    public static final String COLUMN_COUNT = "count";
    public static final String COLUMN_TAKEN_AT = "taken_at";

    private final Context context;

    public DbHelper(@Nullable Context context) {
        super(context, "pilltime.db", null, 1);
        this.context = context;
//        context.deleteDatabase("pilltime.db");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String stmt = "CREATE TABLE IF NOT EXISTS " + MEDS_TABLE +" (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MED_NAME + " TEXT, " +
                COLUMN_MAX_DOSE + " INT, " +
                COLUMN_DOSE_HOURS + " INT)";
        db.execSQL(stmt);
        String stmt2 = "CREATE TABLE IF NOT EXISTS " + DOSES_TABLE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MED_ID + " INT, " +
                COLUMN_COUNT + " REAL, " +
                COLUMN_TAKEN_AT + " INT)";
        db.execSQL(stmt2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }

    public int insertMed(Med med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MED_NAME, med.getName());
        cv.put(COLUMN_MAX_DOSE, med.getMaxDose());
        cv.put(COLUMN_DOSE_HOURS, med.getDoseHours());
        long insertID = db.insert(MEDS_TABLE, null, cv);
        Log.d("clb-debug", insertID + "");
        return (int) insertID;
    }

    public boolean updateMed(Med med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MED_NAME, med.getName());
        cv.put(COLUMN_MAX_DOSE, med.getMaxDose());
        cv.put(COLUMN_DOSE_HOURS, med.getDoseHours());
        String[] whereArgs = new String[]{String.valueOf(med.getId())};
        int update = db.update(MEDS_TABLE, cv, COLUMN_ID + " = ?", whereArgs);
        return (update > 0);
    }

    public List<Med> getAllMeds() {
        List<Med> returnList = new ArrayList<>();

        String stmt = "SELECT * FROM " + MEDS_TABLE +
                " LEFT JOIN " + DOSES_TABLE +
                " ON " + DOSES_TABLE + "." + COLUMN_MED_ID +
                " = " + MEDS_TABLE + "." + COLUMN_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            List<Integer> medIDs = new ArrayList<>();
            Map<String, Med> medMap = new HashMap<String, Med>();
            do {
                Med med;
                int medID = cursor.getInt(0);
                boolean medExists = false;
                for (int i=0; i<medIDs.size(); ++i) {
                    if (medIDs.get(i) == medID) {
                        medExists = true;
                        break;
                    }
                }
                if (!medExists) {
                    medIDs.add(medID);
                    String medName = cursor.getString(1);
                    int maxDose = cursor.getInt(2);
                    int doseHours = cursor.getInt(3);
                    med = new Med(medID, medName, maxDose, doseHours, context);
                    medMap.put(String.valueOf(medID), med);
                } else {
                    med = medMap.get(String.valueOf(medID));
                }
                if (!cursor.isNull(4)) {
                    int doseID = cursor.getInt(4);
                    float count = cursor.getFloat(5);
                    int takenAt = cursor.getInt(6);
                    Dose dose = new Dose(doseID, medID, count, takenAt, context);
                    med.addDose(dose);
                }
            } while (cursor.moveToNext());

            for (int i=0; i<medIDs.size(); ++i) {
                int medID = medIDs.get(i);
                returnList.add(medMap.get(String.valueOf(medID)));
            }
        }

        cursor.close();
        db.close();
        return returnList;
    }

    public boolean deleteMedById(int medID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(medID)};
        db.rawQuery("DELETE FROM " + DOSES_TABLE + " WHERE " + COLUMN_MED_ID + " = ?", selectionArgs);
        String stmt = "DELETE FROM " + MEDS_TABLE + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        boolean deleted = !cursor.moveToFirst();
        cursor.close();
        db.close();
        return (deleted);
    }

    public boolean deleteMed(Med med) {
        return deleteMedById(med.getId());
    }

    public int addDose(Dose dose) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MED_ID, dose.getMedID());
        cv.put(COLUMN_COUNT, dose.getCount());
        cv.put(COLUMN_TAKEN_AT, dose.getTakenAt());
        long insertID = db.insert(DOSES_TABLE, null, cv);
        return (int) insertID;
    }
}
