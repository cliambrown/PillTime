package com.cliambrown.pilltime.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "pilltime.db";
    public static final int DB_VERSION = 8;

    private final Context context;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    public static final String MEDS_TABLE = "meds";
    public static final String DOSES_TABLE = "doses";

    static class DbCol {
        int colId = -1;
        String name;
        boolean existsInDb = false;

        public DbCol(String name) { this.name = name; }

        public void setColId(Cursor cursor) {
            this.colId = cursor.getColumnIndex(this.name);
        }

        public String getSqlType() { return "INTEGER"; }
    }

    static class DbColString extends DbCol {
        public DbColString(String name) { super(name); }

        public String getValFromCursor(Cursor cursor) {
            if (this.colId < 0) this.setColId(cursor);
            if (this.colId < 0) return null;
            return cursor.getString(this.colId);
        }

        public String getSqlType() { return "TEXT"; }
    }

    static class DbColBoolean extends DbCol {
        public DbColBoolean(String name) { super(name); }

        public boolean getValFromCursor(Cursor cursor) {
            if (this.colId < 0) this.setColId(cursor);
            if (this.colId < 0) return false;
            return cursor.getInt(this.colId) != 0;
        }
    }

    static class DbColInt extends DbCol {
        public DbColInt(String name) { super(name); }

        public int getValFromCursor(Cursor cursor) {
            if (this.colId < 0) this.setColId(cursor);
            if (this.colId < 0) return 0;
            return cursor.getInt(this.colId);
        }
    }

    static class DbColDouble extends DbCol {
        public DbColDouble(String name) { super(name); }

        public double getValFromCursor(Cursor cursor) {
            if (this.colId < 0) this.setColId(cursor);
            if (this.colId < 0) return 0.0;
            return cursor.getDouble(this.colId);
        }

        public String getSqlType() { return "REAL"; }
    }

    static class DbColLong extends DbCol {
        public DbColLong(String name) { super(name); }

        public long getValFromCursor(Cursor cursor) {
            if (this.colId < 0) this.setColId(cursor);
            if (this.colId < 0) return 0L;
            return cursor.getLong(this.colId);
        }
    }

    DbColInt medDbColId = new DbColInt("id");
    DbColString medDbColName = new DbColString("name");
    DbColInt medDbColMaxDose = new DbColInt("max_dose");
    DbColInt medDbColDoseHours = new DbColInt("dose_hours");
    DbColString medDbColColor = new DbColString("color");
    DbColBoolean medDbColIsInventoryTracked = new DbColBoolean("is_inventory_tracked");
    DbColDouble medDbColReportedInventory = new DbColDouble("reported_inventory");
    DbColLong medDbColInventoryReportedAt = new DbColLong("inventory_reported_at");
    DbColInt medDbColDefaultDoseCount = new DbColInt("default_dose_count");
    DbColBoolean medDbColShowDayDoseCount = new DbColBoolean("show_day_dose_count");
    DbColBoolean medDbColOverrideGlobalNotifyDefaults = new DbColBoolean("override_global_notify_defaults");
    DbColBoolean medDbColNotifyDefault = new DbColBoolean("notify_default");
    DbColBoolean medDbColNotifySoundDefault = new DbColBoolean("notify_sound_default");

    private final DbCol[] medCols = {
            medDbColId,
            medDbColName,
            medDbColMaxDose,
            medDbColDoseHours,
            medDbColColor,
            medDbColIsInventoryTracked,
            medDbColReportedInventory,
            medDbColInventoryReportedAt,
            medDbColDefaultDoseCount,
            medDbColShowDayDoseCount,
            medDbColOverrideGlobalNotifyDefaults,
            medDbColNotifyDefault,
            medDbColNotifySoundDefault
    };

    public Med getMedAtCursor(Cursor cursor, Context context) {
        return new Med(
                medDbColId.getValFromCursor(cursor),
                medDbColName.getValFromCursor(cursor),
                medDbColMaxDose.getValFromCursor(cursor),
                medDbColDoseHours.getValFromCursor(cursor),
                medDbColColor.getValFromCursor(cursor),
                medDbColIsInventoryTracked.getValFromCursor(cursor),
                medDbColReportedInventory.getValFromCursor(cursor),
                medDbColInventoryReportedAt.getValFromCursor(cursor),
                medDbColDefaultDoseCount.getValFromCursor(cursor),
                medDbColShowDayDoseCount.getValFromCursor(cursor),
                medDbColOverrideGlobalNotifyDefaults.getValFromCursor(cursor),
                medDbColNotifyDefault.getValFromCursor(cursor),
                medDbColNotifySoundDefault.getValFromCursor(cursor),
                context
        );
    }

    DbColInt doseDbColId = new DbColInt("id");
    DbColInt doseDbColMedId = new DbColInt("med_id");
    DbColInt doseDbColCount = new DbColInt("count");
    DbColLong doseDbColTakenAt = new DbColLong("taken_at");
    DbColBoolean doseDbColNotify = new DbColBoolean("notify");
    DbColBoolean doseDbColNotifySound = new DbColBoolean("notify_sound");

    private final DbCol[] doseCols = {
            doseDbColId,
            doseDbColMedId,
            doseDbColCount,
            doseDbColTakenAt,
            doseDbColNotify,
            doseDbColNotifySound
    };

    public Dose getDoseAtCursor(Cursor cursor, Context context) {
        return new Dose(
                doseDbColId.getValFromCursor(cursor),
                doseDbColMedId.getValFromCursor(cursor),
                doseDbColCount.getValFromCursor(cursor),
                doseDbColTakenAt.getValFromCursor(cursor),
                doseDbColNotify.getValFromCursor(cursor),
                doseDbColNotifySound.getValFromCursor(cursor),
                context
        );
    }

    public void clearDB() {
        if (context == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        String stmt = "DROP TABLE IF EXISTS " + MEDS_TABLE;
        db.execSQL(stmt);
        String stmt2 = "DROP TABLE IF EXISTS " + DOSES_TABLE;
        db.execSQL(stmt2);
        onCreate(db);
        getAllMeds();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " + MEDS_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT)");
        for (DbCol dbCol : medCols) {
            if (dbCol.name.equals("id")) continue;
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + dbCol.name + " " + dbCol.getSqlType());
        }

        db.execSQL("CREATE TABLE IF NOT EXISTS " + DOSES_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT)");
        for (DbCol dbCol : doseCols) {
            if (dbCol.name.equals("id")) continue;
            db.execSQL("ALTER TABLE " + DOSES_TABLE + " ADD COLUMN " + dbCol.name + " " + dbCol.getSqlType());
        }
    }

    public void repairDb() {
        repairDb(this.getWritableDatabase());
    }

    public void repairDb(SQLiteDatabase db) {

        for (DbCol dbCol : medCols) {
            dbCol.existsInDb = false;
        }

        String stmt = "PRAGMA table_info(" + MEDS_TABLE + ")";

        // Remove any columns that are no longer used
        Cursor cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            do {
                int nameIndex = cursor.getColumnIndex("name");
                String colName = cursor.getString(nameIndex);
                boolean keepCol = false;
                for (DbCol dbCol : medCols) {
                    if (dbCol.name.equals(colName)) {
                        keepCol = true;
                        dbCol.existsInDb = true;
                        break;
                    }
                }
                if (!keepCol) {
                    db.execSQL("ALTER TABLE " + MEDS_TABLE + " DROP COLUMN " + colName);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Change any columns that have the incorrect type
        cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            do {
                int nameIndex = cursor.getColumnIndex("name");
                int typeIndex = cursor.getColumnIndex("type");
                String colName = cursor.getString(nameIndex);
                String colType = cursor.getString(typeIndex);
                for (DbCol dbCol : medCols) {
                    if (dbCol.name.equals(colName)) {
                        if (!colType.equals(dbCol.getSqlType())) {
                            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + dbCol.name + "_TEMP " + dbCol.getSqlType());
                            db.execSQL("UPDATE " + MEDS_TABLE + " SET " + dbCol.name + "_TEMP = CAST (" + dbCol.name + " as " + dbCol.getSqlType() + ")");
                            db.execSQL("ALTER TABLE " + MEDS_TABLE + " DROP COLUMN " + dbCol.name);
                            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + dbCol.name + " " + dbCol.getSqlType());
                            db.execSQL("UPDATE " + MEDS_TABLE + " SET " + dbCol.name + " = " + dbCol.name + "_TEMP");
                            db.execSQL("ALTER TABLE " + MEDS_TABLE + " DROP COLUMN " + dbCol.name + "_TEMP");
                        }
                        break;
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        for (DbCol dbCol : medCols) {
            if (!dbCol.existsInDb) {
                db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + dbCol.name + " " + dbCol.getSqlType());
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        repairDb(db);
    }

    public List<Med> getAllMeds() {

        List<Med> returnList = new ArrayList<>();

        long now = System.currentTimeMillis() / 1000L;
        String stmt;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String medSortLatest = context.getString(R.string.med_sort_value_latest);
        String medSort = prefs.getString("med_sort", medSortLatest);
        if (medSort.equals(medSortLatest)) {
            stmt = "SELECT * FROM " + MEDS_TABLE
                    + " LEFT JOIN ("
                    + "SELECT id AS dose_id, " + doseDbColMedId.name + ", MAX(" + doseDbColTakenAt.name + ") AS last_taken_at"
                    + " FROM " + DOSES_TABLE
                    + " WHERE " + doseDbColTakenAt.name + " <= " + now
                    + " GROUP BY " + doseDbColMedId.name + ") AS D ON " + MEDS_TABLE + ".id = D." + doseDbColMedId.name + " "
                    + "ORDER BY last_taken_at DESC, dose_id DESC, id DESC";
        } else {
            stmt = "SELECT * FROM " + MEDS_TABLE + " ORDER BY LOWER(name) ASC, id ASC";
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, null);

        if (cursor.moveToFirst()) {
            do {
                returnList.add(getMedAtCursor(cursor, context));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return returnList;
    }

    public Med getMedById(int medID) {
        Med med = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(medID)};
        String stmt = "SELECT * FROM " + MEDS_TABLE + " WHERE id = ? LIMIT 1";
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            med = getMedAtCursor(cursor, context);
        }
        cursor.close();
        db.close();
        return med;
    }

    public int insertMed(Med med) {
        med.checkValidity();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(medDbColName.name, med.getName());
        cv.put(medDbColMaxDose.name, med.getMaxDose());
        cv.put(medDbColDoseHours.name, med.getDoseHours());
        cv.put(medDbColColor.name, med.getColor());
        cv.put(medDbColIsInventoryTracked.name, med.getIsInventoryTracked());
        cv.put(medDbColReportedInventory.name, med.getReportedInventory());
        cv.put(medDbColInventoryReportedAt.name, med.getInventoryReportedAt());
        cv.put(medDbColDefaultDoseCount.name, med.getDefaultDoseCount());
        cv.put(medDbColShowDayDoseCount.name, med.getShowDayDoseCount());
        cv.put(medDbColOverrideGlobalNotifyDefaults.name, med.getOverrideGlobalNotifyDefaults());
        cv.put(medDbColNotifyDefault.name, med.getNotifyDefault());
        cv.put(medDbColNotifySoundDefault.name, med.getNotifySoundDefault());
        long insertID = db.insertOrThrow(MEDS_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateMed(Med med) {
        med.checkValidity();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(medDbColName.name, med.getName());
        cv.put(medDbColMaxDose.name, med.getMaxDose());
        cv.put(medDbColDoseHours.name, med.getDoseHours());
        cv.put(medDbColColor.name, med.getColor());
        cv.put(medDbColIsInventoryTracked.name, med.getIsInventoryTracked());
        cv.put(medDbColReportedInventory.name, med.getReportedInventory());
        cv.put(medDbColInventoryReportedAt.name, med.getInventoryReportedAt());
        cv.put(medDbColDefaultDoseCount.name, med.getDefaultDoseCount());
        cv.put(medDbColShowDayDoseCount.name, med.getShowDayDoseCount());
        cv.put(medDbColOverrideGlobalNotifyDefaults.name, med.getOverrideGlobalNotifyDefaults());
        cv.put(medDbColNotifyDefault.name, med.getNotifyDefault());
        cv.put(medDbColNotifySoundDefault.name, med.getNotifySoundDefault());
        String[] whereArgs = new String[]{String.valueOf(med.getId())};
        int update = db.update(MEDS_TABLE, cv, "id = ?", whereArgs);
        db.close();
        return (update > 0);
    }

    public boolean deleteMedById(int medID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(medID)};
        db.delete(DOSES_TABLE, doseDbColMedId.name + " = ?", selectionArgs);
        int deleted = db.delete(MEDS_TABLE,"id = ?", selectionArgs);
        db.close();
        return (deleted > 0);
    }

    public int insertDose(Dose dose) {
        dose.checkValidity();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(doseDbColMedId.name, dose.getMedID());
        cv.put(doseDbColCount.name, dose.getCount());
        cv.put(doseDbColTakenAt.name, dose.getTakenAt());
        cv.put(doseDbColNotify.name, dose.getNotify());
        cv.put(doseDbColNotifySound.name, dose.getNotifySound());
        long insertID = db.insertOrThrow(DOSES_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateDose(Dose dose) {
        dose.checkValidity();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(doseDbColMedId.name, dose.getMedID());
        cv.put(doseDbColCount.name, dose.getCount());
        cv.put(doseDbColTakenAt.name, dose.getTakenAt());
        cv.put(doseDbColNotify.name, dose.getNotify());
        cv.put(doseDbColNotifySound.name, dose.getNotifySound());
        String[] selectionArgs = new String[]{String.valueOf(dose.getId())};
        int update = db.update(DOSES_TABLE, cv, "id = ?", selectionArgs);
        db.close();
        return (update > 0);
    }

    public void deleteDoseById(int doseID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(doseID)};
        db.delete(DOSES_TABLE, "id = ?", selectionArgs);
        db.close();
    }

    public List<Dose> getDoses(Med med) {
        List<Dose> returnList = new ArrayList<>();
        String stmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + doseDbColMedId.name + " = ? ";
        List<Integer> doseIDs = new ArrayList<>();
        for (Dose dose : med.getDoses()) {
            doseIDs.add(dose.getId());
        }
        if (!doseIDs.isEmpty()) {
            String inClause = doseIDs.toString();
            inClause = inClause.replace("[","(");
            inClause = inClause.replace("]",")");
            stmt = stmt + "AND id NOT IN " + inClause + " ";
        }
        stmt = stmt + "ORDER BY " + doseDbColTakenAt.name + " DESC, id DESC LIMIT 21";
        String[] selectionArgs = new String[]{String.valueOf(med.getId())};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            do {
                returnList.add(getDoseAtCursor(cursor, context));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    public Double getDoseCountBetween(SQLiteDatabase db, Integer medId, long startTime, long endTime) {
        double doseCount = 0D;
        String stmt = "SELECT SUM(" + doseDbColCount.name + ") AS doseCount FROM " + DOSES_TABLE
                + " WHERE " + doseDbColMedId.name + " = ?"
                + " AND " + doseDbColTakenAt.name + " >= ?"
                + " AND " + doseDbColTakenAt.name + " <= ?";
        String[] selectionArgs = new String[]{
                String.valueOf(medId),
                String.valueOf(startTime),
                String.valueOf(endTime)
        };
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            int col_doseCount = cursor.getColumnIndex("doseCount");
            doseCount = cursor.getDouble(col_doseCount);
        }
        cursor.close();
        return doseCount;
    }

    public void updateMedTimes(Med med) {
        long now = System.currentTimeMillis() / 1000L;
        long earliestActiveTakenAt = now - med.getDoseDurationInSeconds();
        SQLiteDatabase db = this.getReadableDatabase();
        med.setActiveDoseCount(getDoseCountBetween(db, med.getId(), earliestActiveTakenAt, now));

        // Get latest dose
        String stmt = "SELECT * FROM " + DOSES_TABLE
                + " WHERE " + doseDbColMedId.name + " = ?"
                + " AND " + doseDbColTakenAt.name + " <= ?"
                + " ORDER BY " + doseDbColTakenAt.name + " DESC, id DESC" +
                " LIMIT 1";
        String[] selectionArgs = new String[]{
                String.valueOf(med.getId()),
                String.valueOf(now)
        };
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            Dose latestDose = getDoseAtCursor(cursor, context);
            latestDose.updateTimes(med);
            med.setLatestDose(latestDose);
        } else {
            med.setLatestDose(null);
        }
        cursor.close();

        if (med.getLatestDose() == null) {
            med.setNextExpiringDose(null);
        } else {
            // Get next expiring dose
            stmt = "SELECT * FROM " + DOSES_TABLE
                    + " WHERE " + doseDbColMedId.name + " = ?"
                    + " AND " + doseDbColTakenAt.name + " >= ?"
                    + " AND " + doseDbColTakenAt.name + " <= ?"
                    + " ORDER BY " + doseDbColTakenAt.name + " ASC, id ASC" +
                    " LIMIT 1";
            selectionArgs = new String[]{
                    String.valueOf(med.getId()),
                    String.valueOf(earliestActiveTakenAt),
                    String.valueOf(now)
            };
            cursor = db.rawQuery(stmt, selectionArgs);
            if (cursor.moveToFirst()) {
                Dose nextExpiringDose = getDoseAtCursor(cursor, context);
                nextExpiringDose.updateTimes(med);
                med.setNextExpiringDose(nextExpiringDose);
            } else {
                med.setNextExpiringDose(null);
            }
            cursor.close();
        }

        med.setPastDayDoseCount(getDoseCountBetween(db, med.getId(), now - 86400, now));
        if (med.getReportedInventory() > 0) {
            med.setCurrentInventory(
                    Math.max(0,
                            med.getReportedInventory() - getDoseCountBetween(
                                    db, med.getId(), med.getInventoryReportedAt(), now
                            )
                    )
            );
        } else {
            med.setCurrentInventory(0D);
        }
        db.close();
    }

    public List<Dose> getActiveDoses() {
        List<Dose> returnList = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000L;
        SQLiteDatabase db = this.getReadableDatabase();
        // NOTE: using selectionArgs here for `now` didn't work for some reason
        String stmt = "SELECT *, D.id as dose_id, " +
                "(D." + doseDbColTakenAt.name + " + M." + medDbColDoseHours.name + " * 60 * 60) AS expires_at " +
                "FROM " + MEDS_TABLE + " M " +
                "LEFT JOIN " + DOSES_TABLE + " D " +
                "ON D." + doseDbColMedId.name +" = M.id " +
                "WHERE D." + doseDbColNotify.name + " > 0 AND expires_at > " + now;
        Cursor cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            int col_expiresAt = cursor.getColumnIndex("expires_at");
            do {
                Dose dose = getDoseAtCursor(cursor, context);
                long expiresAt = cursor.getLong(col_expiresAt);
                dose.setExpiresAt(expiresAt);
                returnList.add(dose);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    public void removeDoseAndOlder(Med med, Dose dose) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] selectionArgs = new String[]{
                String.valueOf(med.getId()),
                String.valueOf(dose.getTakenAt()),
                String.valueOf(dose.getTakenAt()),
                String.valueOf(dose.getId())
        };
        String whereClause = doseDbColMedId.name + " = ? AND (" +
                doseDbColTakenAt.name + " < ?  OR (" +
                doseDbColTakenAt.name + " = ? AND " +
                "id <= ?))";
        db.delete(DOSES_TABLE, whereClause, selectionArgs);
        db.close();
    }

    public JSONObject getExportedDb() throws JSONException {
        SQLiteDatabase db = this.getReadableDatabase();
        JSONObject rootJsonObject = new JSONObject();

        JSONObject dbObject = new JSONObject();
        dbObject.put("version", db.getVersion());

        HashMap<String, String> colCodesMap = new HashMap<>();
        colCodesMap.put(medDbColName.name, "m1");
        colCodesMap.put(medDbColMaxDose.name, "m2");
        colCodesMap.put(medDbColDoseHours.name, "m3");
        colCodesMap.put(medDbColColor.name, "m4");
        colCodesMap.put(medDbColIsInventoryTracked.name, "m5");
        colCodesMap.put(medDbColReportedInventory.name, "m6");
        colCodesMap.put(medDbColInventoryReportedAt.name, "m7");
        colCodesMap.put(medDbColDefaultDoseCount.name, "m8");
        colCodesMap.put(medDbColShowDayDoseCount.name, "m9");
        colCodesMap.put(medDbColOverrideGlobalNotifyDefaults.name, "m10");
        colCodesMap.put(medDbColNotifyDefault.name, "m11");
        colCodesMap.put(medDbColNotifySoundDefault.name, "m12");
        colCodesMap.put(doseDbColCount.name, "d1");
        colCodesMap.put(doseDbColTakenAt.name, "d2");
        colCodesMap.put(doseDbColNotify.name, "d3");
        colCodesMap.put(doseDbColNotifySound.name, "d4");

        JSONObject colCodesObject = new JSONObject();
        colCodesObject.put(medDbColName.name, colCodesMap.get(medDbColName.name));
        colCodesObject.put(medDbColMaxDose.name, colCodesMap.get(medDbColMaxDose.name));
        colCodesObject.put(medDbColDoseHours.name, colCodesMap.get(medDbColDoseHours.name));
        colCodesObject.put(medDbColColor.name, colCodesMap.get(medDbColColor.name));
        colCodesObject.put(medDbColIsInventoryTracked.name, colCodesMap.get(medDbColIsInventoryTracked.name));
        colCodesObject.put(medDbColReportedInventory.name, colCodesMap.get(medDbColReportedInventory.name));
        colCodesObject.put(medDbColInventoryReportedAt.name, colCodesMap.get(medDbColInventoryReportedAt.name));
        colCodesObject.put(medDbColDefaultDoseCount.name, colCodesMap.get(medDbColDefaultDoseCount.name));
        colCodesObject.put(medDbColShowDayDoseCount.name, colCodesMap.get(medDbColShowDayDoseCount.name));
        colCodesObject.put(medDbColOverrideGlobalNotifyDefaults.name, colCodesMap.get(medDbColOverrideGlobalNotifyDefaults.name));
        colCodesObject.put(medDbColNotifyDefault.name, colCodesMap.get(medDbColNotifyDefault.name));
        colCodesObject.put(medDbColNotifySoundDefault.name, colCodesMap.get(medDbColNotifySoundDefault.name));
        colCodesObject.put(doseDbColCount.name, colCodesMap.get(doseDbColCount.name));
        colCodesObject.put(doseDbColTakenAt.name, colCodesMap.get(doseDbColTakenAt.name));
        colCodesObject.put(doseDbColNotify.name, colCodesMap.get(doseDbColNotify.name));
        colCodesObject.put(doseDbColNotifySound.name, colCodesMap.get(doseDbColNotifySound.name));
        dbObject.put("col_codes", colCodesObject);

        rootJsonObject.put("db", dbObject);

        JSONArray medsArray = new JSONArray();
        String stmt = "SELECT * FROM " + MEDS_TABLE;
        Cursor medCursor = db.rawQuery(stmt, null);
        int i = 0;
        if (medCursor.moveToFirst()) {
            do {
                Med med = getMedAtCursor(medCursor, context);
                JSONObject medObject = new JSONObject();
                medObject.put(colCodesMap.get(medDbColName.name), med.getName());
                medObject.put(colCodesMap.get(medDbColMaxDose.name), med.getMaxDose());
                medObject.put(colCodesMap.get(medDbColDoseHours.name), med.getDoseHours());
                medObject.put(colCodesMap.get(medDbColColor.name), med.getColor());
                medObject.put(colCodesMap.get(medDbColIsInventoryTracked.name), (med.getIsInventoryTracked() ? 1 : 0));
                medObject.put(colCodesMap.get(medDbColReportedInventory.name), med.getReportedInventory());
                medObject.put(colCodesMap.get(medDbColInventoryReportedAt.name), med.getInventoryReportedAt());
                medObject.put(colCodesMap.get(medDbColDefaultDoseCount.name), med.getDefaultDoseCount());
                medObject.put(colCodesMap.get(medDbColShowDayDoseCount.name), (med.getShowDayDoseCount() ? 1 : 0));
                medObject.put(colCodesMap.get(medDbColOverrideGlobalNotifyDefaults.name), (med.getOverrideGlobalNotifyDefaults() ? 1 : 0));
                medObject.put(colCodesMap.get(medDbColNotifyDefault.name), (med.getNotifyDefault() ? 1 : 0));
                medObject.put(colCodesMap.get(medDbColNotifySoundDefault.name), (med.getNotifySoundDefault() ? 1 : 0));

                JSONArray dosesArray = new JSONArray();
                String doseStmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + doseDbColMedId.name + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(med.getId())};
                Cursor doseCursor = db.rawQuery(doseStmt, selectionArgs);
                int j = 0;
                if (doseCursor.moveToFirst()) {
                    do {
                        Dose dose = getDoseAtCursor(doseCursor, context);
                        JSONObject doseObject = new JSONObject();
                        doseObject.put(colCodesMap.get(doseDbColCount.name), dose.getCount());
                        doseObject.put(colCodesMap.get(doseDbColTakenAt.name), dose.getTakenAt());
                        doseObject.put(colCodesMap.get(doseDbColNotify.name), (dose.getNotify() ? 1 : 0));
                        doseObject.put(colCodesMap.get(doseDbColNotifySound.name), (dose.getNotifySound() ? 1 : 0));
                        dosesArray.put(j, doseObject);
                        ++j;
                    } while (doseCursor.moveToNext());
                }
                medObject.put("doses", dosesArray);
                doseCursor.close();

                medsArray.put(i, medObject);
                ++i;
            } while (medCursor.moveToNext());
            rootJsonObject.put("meds", medsArray);
        }

        medCursor.close();
        db.close();
        return rootJsonObject;
    }

    // Depending on version, exported booleans may be ints or actual booleans
    private boolean getBoolFromJsonObj(JSONObject jsonObj, String colKey) {
        try {
            Object valObj = jsonObj.get(colKey);
            if (valObj instanceof Integer || valObj instanceof Long) {
                return ((Number) valObj).intValue() == 1;
            } else if (valObj instanceof Boolean) {
                return (Boolean) valObj;
            }
        } catch (Exception e) {
            // do nothing
        }
        return false;
    }

    public void importFromString(String jsonText) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        ContentValues cv;
        try {
            JSONObject rootJsonObject = new JSONObject(jsonText);
            JSONObject dbObject = rootJsonObject.getJSONObject("db");
            JSONObject colCodesObject = dbObject.getJSONObject("col_codes");
            Iterator<String> keys = colCodesObject.keys();
            HashMap<String, String> colCodesMap = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                String val = colCodesObject.getString(key);
                colCodesMap.put(key, val);
            }

            if (!colCodesMap.containsKey(medDbColName.name) ||
                    !colCodesMap.containsKey(medDbColMaxDose.name) ||
                    !colCodesMap.containsKey(medDbColDoseHours.name) ||
                    !colCodesMap.containsKey(medDbColColor.name) ||
                    !colCodesMap.containsKey(doseDbColCount.name) ||
                    !colCodesMap.containsKey(doseDbColTakenAt.name) ||
                    !colCodesMap.containsKey(doseDbColNotify.name) ||
                    !colCodesMap.containsKey(doseDbColNotifySound.name)) {
                throw new Exception("Missing column code");
            }

            JSONArray medsArray = rootJsonObject.getJSONArray("meds");
            for (int i=0; i<medsArray.length(); ++i) {
                JSONObject medObject = medsArray.getJSONObject(i);
                String medName = medObject.getString(colCodesMap.get(medDbColName.name));
                int maxDose = medObject.getInt(colCodesMap.get(medDbColMaxDose.name));
                int doseHours = medObject.getInt(colCodesMap.get(medDbColDoseHours.name));
                String color = medObject.getString(colCodesMap.get(medDbColColor.name));

                boolean isInventoryTracked = colCodesMap.containsKey(medDbColIsInventoryTracked.name)
                        && getBoolFromJsonObj(medObject, colCodesMap.get(medDbColIsInventoryTracked.name));
                double reportedInventory = (isInventoryTracked && colCodesMap.containsKey(medDbColReportedInventory.name))
                        ? medObject.getDouble(colCodesMap.get(medDbColReportedInventory.name))
                        : 0d;
                long inventoryReportedAt = (isInventoryTracked && colCodesMap.containsKey(medDbColInventoryReportedAt.name))
                        ? medObject.getLong(colCodesMap.get(medDbColInventoryReportedAt.name))
                        : 0L;
                int defaultDoseCount = colCodesMap.containsKey(medDbColDefaultDoseCount.name)
                        ? medObject.getInt(colCodesMap.get(medDbColDefaultDoseCount.name))
                        : 1;
                boolean showDayDoseCount = colCodesMap.containsKey(medDbColShowDayDoseCount.name)
                        && getBoolFromJsonObj(medObject, colCodesMap.get(medDbColShowDayDoseCount.name));
                boolean overrideGlobalNotifyDefaults = colCodesMap.containsKey(medDbColOverrideGlobalNotifyDefaults.name)
                        && getBoolFromJsonObj(medObject, colCodesMap.get(medDbColOverrideGlobalNotifyDefaults.name));
                boolean notifyDefault = colCodesMap.containsKey(medDbColNotifyDefault.name)
                        && getBoolFromJsonObj(medObject, colCodesMap.get(medDbColNotifyDefault.name));
                boolean notifySoundDefault = colCodesMap.containsKey(medDbColNotifySoundDefault.name)
                        && getBoolFromJsonObj(medObject, colCodesMap.get(medDbColNotifySoundDefault.name));

                int medID;
                String stmt = "SELECT id FROM " + MEDS_TABLE + " " +
                        "WHERE name = ? AND " +
                        medDbColMaxDose.name + " = ? AND " +
                        medDbColDoseHours.name + " = ?";
                String[] selectionArgs = new String[]{
                        medName,
                        String.valueOf(maxDose),
                        String.valueOf(doseHours)
                };
                cursor = db.rawQuery(stmt, selectionArgs);
                cv = new ContentValues();
                cv.put(medDbColColor.name, color);
                cv.put(medDbColIsInventoryTracked.name, isInventoryTracked);
                cv.put(medDbColReportedInventory.name, reportedInventory);
                cv.put(medDbColInventoryReportedAt.name, inventoryReportedAt);
                cv.put(medDbColDefaultDoseCount.name, defaultDoseCount);
                cv.put(medDbColShowDayDoseCount.name, showDayDoseCount);
                cv.put(medDbColOverrideGlobalNotifyDefaults.name, overrideGlobalNotifyDefaults);
                cv.put(medDbColNotifyDefault.name, notifyDefault);
                cv.put(medDbColNotifySoundDefault.name, notifySoundDefault);
                if (cursor.moveToFirst()) {
                    int col_id = cursor.getColumnIndex("id");
                    medID = cursor.getInt(col_id);
                    String[] whereArgs = new String[]{String.valueOf(medID)};
                    db.update(MEDS_TABLE, cv, "id = ?", whereArgs);
                } else {
                    cv.put(medDbColName.name, medName);
                    cv.put(medDbColMaxDose.name, maxDose);
                    cv.put(medDbColDoseHours.name, doseHours);
                    medID = (int) db.insert(MEDS_TABLE, null, cv);
                }

                JSONArray dosesArray = medObject.getJSONArray("doses");
                for (int j=0; j<dosesArray.length(); ++j) {
                    JSONObject doseObject = dosesArray.getJSONObject(j);
                    double count = doseObject.getDouble(colCodesMap.get(doseDbColCount.name));
                    long takenAt = doseObject.getLong(colCodesMap.get(doseDbColTakenAt.name));
                    boolean notify = getBoolFromJsonObj(doseObject, colCodesMap.get(doseDbColNotify.name));
                    boolean notifySound = getBoolFromJsonObj(doseObject, colCodesMap.get(doseDbColNotifySound.name));
                    cv = new ContentValues();
                    cv.put(doseDbColMedId.name, medID);
                    cv.put(doseDbColCount.name, count);
                    cv.put(doseDbColTakenAt.name, takenAt);
                    cv.put(doseDbColNotify.name, notify);
                    cv.put(doseDbColNotifySound.name, notifySound);
                    db.insert(DOSES_TABLE, null, cv);
                }
            }
            Toast.makeText(context, context.getString(R.string.toast_data_imported), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context,context.getString(R.string.toast_error_importing_database) + ": " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
        if (cursor != null) cursor.close();
        db.close();
    }
}
