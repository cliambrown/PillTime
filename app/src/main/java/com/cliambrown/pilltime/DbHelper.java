package com.cliambrown.pilltime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        getAllMeds();
        return true;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String stmt = "CREATE TABLE IF NOT EXISTS " + MEDS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MEDS_COL_NAME + " TEXT, " +
                MEDS_COL_MAX_DOSE + " INTEGER, " +
                MEDS_COL_DOSE_HOURS + " INTEGER," +
                MEDS_COL_COLOR + " TEXT)";
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

        String stmt = "SELECT * FROM " + MEDS_TABLE +
                " LEFT JOIN (SELECT id AS dose_id, " + DOSES_COL_MED_ID + ", MAX(" + DOSES_COL_TAKEN_AT + ") AS " + DOSES_COL_TAKEN_AT + " " +
                "FROM " + DOSES_TABLE + " GROUP BY " + DOSES_COL_MED_ID + ") AS D " +
                "ON " + MEDS_TABLE + ".id = D." + DOSES_COL_MED_ID + " " +
                "ORDER BY " + DOSES_COL_TAKEN_AT + " DESC, dose_id DESC, id DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, null);
//        Log.d("clb", DatabaseUtils.dumpCursorToString(cursor));

        if (cursor.moveToFirst()) {
            int col_id = cursor.getColumnIndex("id");
            int col_name = cursor.getColumnIndex(MEDS_COL_NAME);
            int col_maxDose = cursor.getColumnIndex(MEDS_COL_MAX_DOSE);
            int col_doseHours = cursor.getColumnIndex(MEDS_COL_DOSE_HOURS);
            int col_color = cursor.getColumnIndex(MEDS_COL_COLOR);

            do {
                int medID = cursor.getInt(col_id);
                String medName = cursor.getString(col_name);
                int maxDose = cursor.getInt(col_maxDose);
                int doseHours = cursor.getInt(col_doseHours);
                String color = cursor.getString(col_color);
                returnList.add(new Med(medID, medName, maxDose, doseHours, color, context));
            } while (cursor.moveToNext());
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

    public List<Dose> loadDoses(Med med) {
        List<Dose> returnList = new ArrayList<Dose>();
        String stmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + DOSES_COL_MED_ID + " = ? ";
        List<Integer> doseIDs = new ArrayList<Integer>();
        for (Dose dose : med.getDoses()) {
            doseIDs.add(dose.getId());
        }
        if (doseIDs.size() > 0) {
            String inClause = doseIDs.toString();
            inClause = inClause.replace("[","(");
            inClause = inClause.replace("]",")");
            stmt = stmt + "AND id NOT IN " + inClause + " ";
        }
        stmt = stmt + "ORDER BY " + DOSES_COL_TAKEN_AT + " DESC, id DESC LIMIT 21";
        String[] selectionArgs = new String[]{String.valueOf(med.getId())};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            int col_id = cursor.getColumnIndex("id");
            int col_count = cursor.getColumnIndex(DOSES_COL_COUNT);
            int col_takenAt = cursor.getColumnIndex(DOSES_COL_TAKEN_AT);
            do {
                int doseID = cursor.getInt(col_id);
                double count = cursor.getDouble(col_count);
                long takenAt = cursor.getLong(col_takenAt);
                Dose dose = new Dose(doseID, med.getId(), count, takenAt, context);
                returnList.add(dose);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

//    public List<Dose> loadMoreDoses(int medID, List<Integer> doseIDs) {
//        List<Dose> returnList = new ArrayList<Dose>();
//        String inClause = doseIDs.toString();
//        inClause = inClause.replace("[","(");
//        inClause = inClause.replace("]",")");
//        String stmt = "SELECT * FROM " + DOSES_TABLE + " " +
//                "WHERE " + DOSES_COL_MED_ID + " = ? AND " +
//                "id NOT IN " + inClause + " " +
//                "ORDER BY " + DOSES_COL_TAKEN_AT + " DESC, " +
//                "id DESC";
//        String[] selectionArgs = new String[]{String.valueOf(medID)};
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(stmt, selectionArgs);
//        if (cursor.moveToFirst()) {
//            int doses_col_id = cursor.getColumnIndex("id");
//            int doses_col_count = cursor.getColumnIndex(DOSES_COL_COUNT);
//            int doses_col_takenAt = cursor.getColumnIndex(DOSES_COL_TAKEN_AT);
//            do {
//                int doseID = cursor.getInt(doses_col_id);
//                double count = cursor.getDouble(doses_col_count);
//                long takenAt = cursor.getLong(doses_col_takenAt);
//                Dose dose = new Dose(doseID, medID, count, takenAt, context);
//                returnList.add(dose);
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        db.close();
//        return returnList;
//    }
}
