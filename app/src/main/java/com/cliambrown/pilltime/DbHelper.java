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

    public static final String DB_NAME = "pilltime.db";

    public static final String MEDS_TABLE = "meds";
    public static final String MEDS_COL_NAME = "name";
    public static final String MEDS_COL_MAX_DOSE = "max_dose";
    public static final String MEDS_COL_DOSE_HOURS = "dose_hours";
    public static final String MEDS_COL_COLOR = "color";

    public static final String DOSES_TABLE = "doses";
    public static final String DOSES_COL_MED_ID = "med_id";
    public static final String DOSES_COL_COUNT = "count";
    public static final String DOSES_COL_TAKEN_AT = "taken_at";

    private final Context context;

    public DbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 2);
        this.context = context;
    }

    public boolean deleteDB() {
        context.deleteDatabase(DB_NAME);
        return true;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String stmt = "CREATE TABLE IF NOT EXISTS " + MEDS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MEDS_COL_NAME + " TEXT, " +
                MEDS_COL_MAX_DOSE + " INTEGER, " +
                MEDS_COL_DOSE_HOURS + " INTEGER)";
        db.execSQL(stmt);
        String stmt2 = "CREATE TABLE IF NOT EXISTS " + DOSES_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DOSES_COL_MED_ID + " INTEGER, " +
                DOSES_COL_COUNT + " REAL, " +
                DOSES_COL_TAKEN_AT + " INTEGER)";
        db.execSQL(stmt2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_COLOR + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<Med> getAllMeds() {
        List<Med> returnList = new ArrayList<>();

        String stmt = "SELECT " +
                MEDS_TABLE + ".id AS " + MEDS_TABLE + "_id, " +
                MEDS_TABLE + "." + MEDS_COL_NAME + " AS " + MEDS_TABLE + "_" + MEDS_COL_NAME + ", " +
                MEDS_TABLE + "." + MEDS_COL_MAX_DOSE + " AS " + MEDS_TABLE + "_" + MEDS_COL_MAX_DOSE + ", " +
                MEDS_TABLE + "." + MEDS_COL_DOSE_HOURS + " AS " + MEDS_TABLE + "_" + MEDS_COL_DOSE_HOURS + ", " +
                MEDS_TABLE + "." + MEDS_COL_COLOR + " AS " + MEDS_TABLE + "_" + MEDS_COL_COLOR + ", " +
                "JT.id AS " + DOSES_TABLE + "_id, " +
                "JT." + DOSES_COL_MED_ID + " AS " + DOSES_TABLE + "_" + DOSES_COL_MED_ID + ", " +
                "JT." + DOSES_COL_COUNT + " AS " + DOSES_TABLE + "_" + DOSES_COL_COUNT + ", " +
                "JT." + DOSES_COL_TAKEN_AT + " AS " + DOSES_TABLE + "_" + DOSES_COL_TAKEN_AT + " " +
                "FROM " + MEDS_TABLE + " " +
                "LEFT JOIN (SELECT " +
                DOSES_TABLE + ".id, " +
                DOSES_TABLE + "." + DOSES_COL_MED_ID + ", " +
                DOSES_TABLE + "." + DOSES_COL_COUNT + ", " +
                DOSES_TABLE + "." + DOSES_COL_TAKEN_AT + " " +
                "FROM " + DOSES_TABLE + " " +
                "ORDER BY " + DOSES_TABLE + "." + DOSES_COL_TAKEN_AT + " DESC, " +
                DOSES_TABLE + ".id DESC " +
                "LIMIT 20) JT ON JT.id = " + MEDS_TABLE + "_id " +
                "ORDER BY " + DOSES_TABLE + "_" + DOSES_COL_TAKEN_AT + " DESC, " +
                DOSES_TABLE + "_id DESC, " +
                MEDS_TABLE + "_id DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, null);
//        Log.d("clb", DatabaseUtils.dumpCursorToString(cursor));

        if (cursor.moveToFirst()) {
            List<Integer> medIDs = new ArrayList<>();
            Map<String, Med> medMap = new HashMap<String, Med>();
            int meds_col_id = cursor.getColumnIndex(MEDS_TABLE + "_id");
            int meds_col_name = cursor.getColumnIndex(MEDS_TABLE + "_" + MEDS_COL_NAME);
            int meds_col_maxDose = cursor.getColumnIndex(MEDS_TABLE + "_" + MEDS_COL_MAX_DOSE);
            int meds_col_color = cursor.getColumnIndex(MEDS_TABLE + "_" + MEDS_COL_COLOR);
            int meds_col_doseHours = cursor.getColumnIndex(MEDS_TABLE + "_" + MEDS_COL_DOSE_HOURS);
            int doses_col_id = cursor.getColumnIndex(DOSES_TABLE + "_id");
            int doses_col_count = cursor.getColumnIndex(DOSES_TABLE + "_" + DOSES_COL_COUNT);
            int doses_col_takenAt = cursor.getColumnIndex(DOSES_TABLE + "_" + DOSES_COL_TAKEN_AT);
            do {
                Med med;
                int medID = cursor.getInt(meds_col_id);
                boolean medExists = false;
                for (int i=0; i<medIDs.size(); ++i) {
                    if (medIDs.get(i) == medID) {
                        medExists = true;
                        break;
                    }
                }
                if (!medExists) {
                    medIDs.add(medID);
                    String medName = cursor.getString(meds_col_name);
                    int maxDose = cursor.getInt(meds_col_maxDose);
                    int doseHours = cursor.getInt(meds_col_doseHours);
                    String color = cursor.getString(meds_col_color);
                    med = new Med(medID, medName, maxDose, doseHours, color, context);
                    medMap.put(String.valueOf(medID), med);
                } else {
                    med = medMap.get(String.valueOf(medID));
                }
                if (!cursor.isNull(doses_col_id)) {
                    int doseID = cursor.getInt(doses_col_id);
                    double count = cursor.getDouble(doses_col_count);
                    long takenAt = cursor.getLong(doses_col_takenAt);
                    Dose dose = new Dose(doseID, medID, count, takenAt, context);
                    med.addDose(dose);
                }
            } while (cursor.moveToNext());

            for (int i=0; i<medIDs.size(); ++i) {
                int medID = medIDs.get(i);
                Med med = medMap.get(String.valueOf(medID));
                int doseCount = med.getDoses().size();
                med.setHasLoadedAllDoses(doseCount < 20);
                returnList.add(med);
            }
        }

        cursor.close();
        db.close();
        return returnList;
    }

    public int insertMed(Med med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MEDS_COL_NAME, med.getName());
        cv.put(MEDS_COL_MAX_DOSE, med.getMaxDose());
        cv.put(MEDS_COL_DOSE_HOURS, med.getDoseHours());
        cv.put(MEDS_COL_COLOR, med.getColor());
        long insertID = db.insert(MEDS_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateMed(Med med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MEDS_COL_NAME, med.getName());
        cv.put(MEDS_COL_MAX_DOSE, med.getMaxDose());
        cv.put(MEDS_COL_DOSE_HOURS, med.getDoseHours());
        cv.put(MEDS_COL_COLOR, med.getColor());
        String[] whereArgs = new String[]{String.valueOf(med.getId())};
        int update = db.update(MEDS_TABLE, cv, "id = ?", whereArgs);
        db.close();
        return (update > 0);
    }

    public boolean deleteMedById(int medID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(medID)};
        db.delete(DOSES_TABLE, DOSES_COL_MED_ID + " = ?", selectionArgs);
        int deleted = db.delete(MEDS_TABLE,"id = ?", selectionArgs);
        db.close();
        return (deleted > 0);
    }

    public boolean deleteMed(Med med) {
        return deleteMedById(med.getId());
    }

    public int insertDose(Dose dose) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DOSES_COL_MED_ID, dose.getMedID());
        cv.put(DOSES_COL_COUNT, dose.getCount());
        cv.put(DOSES_COL_TAKEN_AT, dose.getTakenAt());
        long insertID = db.insert(DOSES_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateDose(Dose dose) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DOSES_COL_MED_ID, dose.getMedID());
        cv.put(DOSES_COL_COUNT, dose.getCount());
        cv.put(DOSES_COL_TAKEN_AT, dose.getTakenAt());
        String[] selectionArgs = new String[]{String.valueOf(dose.getId())};
        int update = db.update(DOSES_TABLE, cv, "id = ?", selectionArgs);
        return (update > 0);
    }

    public void deleteDoseById(int doseID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(doseID)};
        db.delete(DOSES_TABLE, "id = ?", selectionArgs);
        db.close();
    }
}
