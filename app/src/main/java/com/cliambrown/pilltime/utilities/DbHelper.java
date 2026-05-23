package com.cliambrown.pilltime.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.meds.Med;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "pilltime.db";
    public static final int DB_VERSION = 6;

    public static final String MEDS_TABLE = "meds";
    public static final String MEDS_COL_NAME = "name";
    public static final String MEDS_COL_MAX_DOSE = "max_dose";
    public static final String MEDS_COL_DOSE_HOURS = "dose_hours";
    public static final String MEDS_COL_COLOR = "color";
    public static final String MEDS_COL_IS_INVENTORY_TRACKED = "is_inventory_tracked";
    public static final String MEDS_COL_REPORTED_INVENTORY = "reported_inventory";
    public static final String MEDS_COL_INVENTORY_REPORTED_AT = "inventory_reported_at";
    public static final String MEDS_COL_DEFAULT_DOSE_COUNT = "default_dose_count";
    public static final String MEDS_COL_SHOW_DAY_DOSE_COUNT = "show_day_dose_count";

    public static final String DOSES_TABLE = "doses";
    public static final String DOSES_COL_MED_ID = "med_id";
    public static final String DOSES_COL_COUNT = "count";
    public static final String DOSES_COL_TAKEN_AT = "taken_at";
    public static final String DOSES_COL_NOTIFY = "notify";
    public static final String DOSES_COL_NOTIFY_SOUND = "notify_sound";

    private final Context context;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    private static class MedCursorParser {
        int col_id;
        int col_name;
        int col_maxDose;
        int col_doseHours;
        int col_color;
        int col_isInventoryTracked;
        int col_reportedInventory;
        int col_inventoryReportedAt;
        int col_defaultDoseCount;
        int col_showDayDoseCount;

        MedCursorParser(Cursor cursor) {
            col_id = cursor.getColumnIndex("id");
            col_name = cursor.getColumnIndex(MEDS_COL_NAME);
            col_maxDose = cursor.getColumnIndex(MEDS_COL_MAX_DOSE);
            col_doseHours = cursor.getColumnIndex(MEDS_COL_DOSE_HOURS);
            col_color = cursor.getColumnIndex(MEDS_COL_COLOR);
            col_isInventoryTracked = cursor.getColumnIndex(MEDS_COL_IS_INVENTORY_TRACKED);
            col_reportedInventory = cursor.getColumnIndex(MEDS_COL_REPORTED_INVENTORY);
            col_inventoryReportedAt = cursor.getColumnIndex(MEDS_COL_INVENTORY_REPORTED_AT);
            col_defaultDoseCount = cursor.getColumnIndex(MEDS_COL_DEFAULT_DOSE_COUNT);
            col_showDayDoseCount = cursor.getColumnIndex(MEDS_COL_SHOW_DAY_DOSE_COUNT);
        }

        public Med getMedAtCursor(Cursor cursor, Context context) {
            return new Med(
                    cursor.getInt(col_id),
                    cursor.getString(col_name),
                    cursor.getInt(col_maxDose),
                    cursor.getInt(col_doseHours),
                    cursor.getString(col_color),
                    cursor.getInt(col_isInventoryTracked) != 0,
                    cursor.getDouble(col_reportedInventory),
                    cursor.getLong(col_inventoryReportedAt),
                    cursor.getInt(col_defaultDoseCount),
                    cursor.getInt(col_showDayDoseCount) != 0,
                    context
            );
        }
    }

    private static class DoseCursorParser {
        int col_id;
        int medId = -1;
        int col_medId = -1;
        int col_count;
        int col_takenAt;
        int col_notify;
        int col_notifySound;

        DoseCursorParser(Cursor cursor, String doseIdColName, int medId) {
            col_id = cursor.getColumnIndex(doseIdColName);
            if (medId > 0) this.medId = medId;
            else col_medId = cursor.getColumnIndex(DOSES_COL_MED_ID);
            col_count = cursor.getColumnIndex(DOSES_COL_COUNT);
            col_takenAt = cursor.getColumnIndex(DOSES_COL_TAKEN_AT);
            col_notify = cursor.getColumnIndex(DOSES_COL_NOTIFY);
            col_notifySound = cursor.getColumnIndex(DOSES_COL_NOTIFY_SOUND);
        }

        public Dose getDoseAtCursor(Cursor cursor, Context context) {
            int medId = this.col_medId >= 0 ? cursor.getInt(col_medId) : this.medId;
            return new Dose(
                    cursor.getInt(col_id),
                    medId,
                    cursor.getDouble(col_count),
                    cursor.getLong(col_takenAt),
                    (cursor.getInt(col_notify) == 1),
                    (cursor.getInt(col_notifySound) == 1),
                    context
            );
        }
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
        String stmt = "CREATE TABLE IF NOT EXISTS " + MEDS_TABLE + " " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MEDS_COL_NAME + " TEXT, " +
                MEDS_COL_MAX_DOSE + " INTEGER, " +
                MEDS_COL_DOSE_HOURS + " INTEGER," +
                MEDS_COL_COLOR + " TEXT," +
                MEDS_COL_IS_INVENTORY_TRACKED + " INTEGER," +
                MEDS_COL_REPORTED_INVENTORY + " REAL," +
                MEDS_COL_INVENTORY_REPORTED_AT + " REAL," +
                MEDS_COL_DEFAULT_DOSE_COUNT + " INTEGER," +
                MEDS_COL_SHOW_DAY_DOSE_COUNT + " INTEGER)";
        db.execSQL(stmt);
        String stmt2 = "CREATE TABLE IF NOT EXISTS " + DOSES_TABLE + " " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DOSES_COL_MED_ID + " INTEGER, " +
                DOSES_COL_COUNT + " REAL, " +
                DOSES_COL_TAKEN_AT + " INTEGER, " +
                DOSES_COL_NOTIFY + " INTEGER, " +
                DOSES_COL_NOTIFY_SOUND + " INTEGER)";
        db.execSQL(stmt2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_COLOR + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + DOSES_TABLE + " ADD COLUMN " + DOSES_COL_NOTIFY + " INTEGER");
            db.execSQL("ALTER TABLE " + DOSES_TABLE + " ADD COLUMN " + DOSES_COL_NOTIFY_SOUND + " INTEGER");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_IS_INVENTORY_TRACKED + " INTEGER");
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_REPORTED_INVENTORY + " REAL");
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_INVENTORY_REPORTED_AT + " REAL");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_DEFAULT_DOSE_COUNT + " INTEGER");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + MEDS_TABLE + " ADD COLUMN " + MEDS_COL_SHOW_DAY_DOSE_COUNT + " INTEGER");
        }
    }

    public List<Med> getAllMeds() {

        List<Med> returnList = new ArrayList<>();

        long now = System.currentTimeMillis() / 1000L;
        String stmt = "SELECT * FROM " + MEDS_TABLE +
                " LEFT JOIN ("
                    + "SELECT id AS dose_id, " + DOSES_COL_MED_ID + ", MAX(" + DOSES_COL_TAKEN_AT + ") AS last_taken_at"
                    + " FROM " + DOSES_TABLE
                    + " WHERE " + DOSES_COL_TAKEN_AT + " <= " + now
                    + " GROUP BY " + DOSES_COL_MED_ID
                + ") AS D ON " + MEDS_TABLE + ".id = D." + DOSES_COL_MED_ID
                + " ORDER BY last_taken_at DESC, dose_id DESC, id DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, null);

        if (cursor.moveToFirst()) {

            MedCursorParser mcp = new MedCursorParser(cursor);

            do {
                returnList.add(mcp.getMedAtCursor(cursor, context));
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
            MedCursorParser mcp = new MedCursorParser(cursor);
            med = mcp.getMedAtCursor(cursor, context);
        }
        cursor.close();
        db.close();
        return med;
    }

    private boolean medIsInvalid(Med med) {
        return (med.getName() == null ||
                med.getName().trim().isEmpty() ||
                med.getMaxDose() <= 0 ||
                med.getDoseHours() <= 0 ||
                med.getColor() == null ||
                !Arrays.asList(context.getResources().getStringArray(R.array.color_options)).contains(med.getColor()) ||
                (med.getIsInventoryTracked() && med.getReportedInventory() < 0) ||
                med.getDefaultDoseCount() <= 0
        );
    }

    public int insertMed(Med med) {
        if (medIsInvalid(med)) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MEDS_COL_NAME, med.getName());
        cv.put(MEDS_COL_MAX_DOSE, med.getMaxDose());
        cv.put(MEDS_COL_DOSE_HOURS, med.getDoseHours());
        cv.put(MEDS_COL_COLOR, med.getColor());
        cv.put(MEDS_COL_IS_INVENTORY_TRACKED, med.getIsInventoryTracked());
        cv.put(MEDS_COL_REPORTED_INVENTORY, med.getReportedInventory());
        cv.put(MEDS_COL_INVENTORY_REPORTED_AT, med.getInventoryReportedAt());
        cv.put(MEDS_COL_DEFAULT_DOSE_COUNT, med.getDefaultDoseCount());
        cv.put(MEDS_COL_SHOW_DAY_DOSE_COUNT, med.getShowDayDoseCount());
        long insertID = db.insert(MEDS_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateMed(Med med) {
        if (medIsInvalid(med)) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MEDS_COL_NAME, med.getName());
        cv.put(MEDS_COL_MAX_DOSE, med.getMaxDose());
        cv.put(MEDS_COL_DOSE_HOURS, med.getDoseHours());
        cv.put(MEDS_COL_COLOR, med.getColor());
        cv.put(MEDS_COL_IS_INVENTORY_TRACKED, med.getIsInventoryTracked());
        cv.put(MEDS_COL_REPORTED_INVENTORY, med.getReportedInventory());
        cv.put(MEDS_COL_INVENTORY_REPORTED_AT, med.getInventoryReportedAt());
        cv.put(MEDS_COL_DEFAULT_DOSE_COUNT, med.getDefaultDoseCount());
        cv.put(MEDS_COL_SHOW_DAY_DOSE_COUNT, med.getShowDayDoseCount());
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

    private boolean doseIsInvalid(Dose dose) {
        return (dose.getMedID() <= 0 ||
                dose.getCount() <= 0 ||
                dose.getTakenAt() <= 0
        );
    }

    public int insertDose(Dose dose) {
        if (doseIsInvalid(dose)) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DOSES_COL_MED_ID, dose.getMedID());
        cv.put(DOSES_COL_COUNT, dose.getCount());
        cv.put(DOSES_COL_TAKEN_AT, dose.getTakenAt());
        cv.put(DOSES_COL_NOTIFY, dose.getNotify());
        cv.put(DOSES_COL_NOTIFY_SOUND, dose.getNotifySound());
        long insertID = db.insert(DOSES_TABLE, null, cv);
        db.close();
        return (int) insertID;
    }

    public boolean updateDose(Dose dose) {
        if (doseIsInvalid(dose)) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DOSES_COL_MED_ID, dose.getMedID());
        cv.put(DOSES_COL_COUNT, dose.getCount());
        cv.put(DOSES_COL_TAKEN_AT, dose.getTakenAt());
        cv.put(DOSES_COL_NOTIFY, dose.getNotify());
        cv.put(DOSES_COL_NOTIFY_SOUND, dose.getNotifySound());
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

    public List<Dose> loadDoses(Med med) {
        List<Dose> returnList = new ArrayList<>();
        String stmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + DOSES_COL_MED_ID + " = ? ";
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
        stmt = stmt + "ORDER BY " + DOSES_COL_TAKEN_AT + " DESC, id DESC LIMIT 21";
        String[] selectionArgs = new String[]{String.valueOf(med.getId())};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            DoseCursorParser dcp = new DoseCursorParser(cursor, "id", med.getId());
            do {
                returnList.add(dcp.getDoseAtCursor(cursor, context));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    public Double getDoseCountBetween(SQLiteDatabase db, Integer medId, long startTime, long endTime) {
        double doseCount = 0D;
        String stmt = "SELECT SUM(" + DOSES_COL_COUNT + ") AS doseCount FROM " + DOSES_TABLE
                + " WHERE " + DOSES_COL_MED_ID + " = ?"
                + " AND " + DOSES_COL_TAKEN_AT + " >= ?"
                + " AND " + DOSES_COL_TAKEN_AT + " <= ?";
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
                + " WHERE " + DOSES_COL_MED_ID + " = ?"
                + " AND " + DOSES_COL_TAKEN_AT + " <= ?"
                + " ORDER BY " + DOSES_COL_TAKEN_AT + " DESC, id DESC" +
                " LIMIT 1";
        String[] selectionArgs = new String[]{
                String.valueOf(med.getId()),
                String.valueOf(now)
        };
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (cursor.moveToFirst()) {
            DoseCursorParser dcp = new DoseCursorParser(cursor, "id", med.getId());
            Dose latestDose = dcp.getDoseAtCursor(cursor, context);
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
                    + " WHERE " + DOSES_COL_MED_ID + " = ?"
                    + " AND " + DOSES_COL_TAKEN_AT + " >= ?"
                    + " AND " + DOSES_COL_TAKEN_AT + " <= ?"
                    + " ORDER BY " + DOSES_COL_TAKEN_AT + " ASC, id ASC" +
                    " LIMIT 1";
            selectionArgs = new String[]{
                    String.valueOf(med.getId()),
                    String.valueOf(earliestActiveTakenAt),
                    String.valueOf(now)
            };
            cursor = db.rawQuery(stmt, selectionArgs);
            if (cursor.moveToFirst()) {
                DoseCursorParser dcp = new DoseCursorParser(cursor, "id", med.getId());
                Dose nextExpiringDose = dcp.getDoseAtCursor(cursor, context);
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
                "(D." + DOSES_COL_TAKEN_AT + " + M." + MEDS_COL_DOSE_HOURS + " * 60 * 60) AS expires_at " +
                "FROM " + MEDS_TABLE + " M " +
                "LEFT JOIN " + DOSES_TABLE + " D " +
                "ON D." + DOSES_COL_MED_ID +" = M.id " +
                "WHERE D." + DOSES_COL_NOTIFY + " > 0 AND expires_at > " + now;
        Cursor cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            DoseCursorParser dcp = new DoseCursorParser(cursor, "dose_id", -1);
            int col_expiresAt = cursor.getColumnIndex("expires_at");
            do {
                Dose dose = dcp.getDoseAtCursor(cursor, context);
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
        String whereClause = DOSES_COL_MED_ID + " = ? AND (" +
                DOSES_COL_TAKEN_AT + " < ?  OR (" +
                DOSES_COL_TAKEN_AT + " = ? AND " +
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
        colCodesMap.put(MEDS_COL_NAME, "m1");
        colCodesMap.put(MEDS_COL_MAX_DOSE, "m2");
        colCodesMap.put(MEDS_COL_DOSE_HOURS, "m3");
        colCodesMap.put(MEDS_COL_COLOR, "m4");
        colCodesMap.put(MEDS_COL_IS_INVENTORY_TRACKED, "m5");
        colCodesMap.put(MEDS_COL_REPORTED_INVENTORY, "m6");
        colCodesMap.put(MEDS_COL_INVENTORY_REPORTED_AT, "m7");
        colCodesMap.put(MEDS_COL_DEFAULT_DOSE_COUNT, "m8");
        colCodesMap.put(MEDS_COL_SHOW_DAY_DOSE_COUNT, "m9");
        colCodesMap.put(DOSES_COL_COUNT, "d1");
        colCodesMap.put(DOSES_COL_TAKEN_AT, "d2");
        colCodesMap.put(DOSES_COL_NOTIFY, "d3");
        colCodesMap.put(DOSES_COL_NOTIFY_SOUND, "d4");

        JSONObject colCodesObject = new JSONObject();
        colCodesObject.put(MEDS_COL_NAME, colCodesMap.get(MEDS_COL_NAME));
        colCodesObject.put(MEDS_COL_MAX_DOSE, colCodesMap.get(MEDS_COL_MAX_DOSE));
        colCodesObject.put(MEDS_COL_DOSE_HOURS, colCodesMap.get(MEDS_COL_DOSE_HOURS));
        colCodesObject.put(MEDS_COL_COLOR, colCodesMap.get(MEDS_COL_COLOR));
        colCodesObject.put(MEDS_COL_IS_INVENTORY_TRACKED, colCodesMap.get(MEDS_COL_IS_INVENTORY_TRACKED));
        colCodesObject.put(MEDS_COL_REPORTED_INVENTORY, colCodesMap.get(MEDS_COL_REPORTED_INVENTORY));
        colCodesObject.put(MEDS_COL_INVENTORY_REPORTED_AT, colCodesMap.get(MEDS_COL_INVENTORY_REPORTED_AT));
        colCodesObject.put(MEDS_COL_DEFAULT_DOSE_COUNT, colCodesMap.get(MEDS_COL_DEFAULT_DOSE_COUNT));
        colCodesObject.put(MEDS_COL_SHOW_DAY_DOSE_COUNT, colCodesMap.get(MEDS_COL_SHOW_DAY_DOSE_COUNT));
        colCodesObject.put(DOSES_COL_COUNT, colCodesMap.get(DOSES_COL_COUNT));
        colCodesObject.put(DOSES_COL_TAKEN_AT, colCodesMap.get(DOSES_COL_TAKEN_AT));
        colCodesObject.put(DOSES_COL_NOTIFY, colCodesMap.get(DOSES_COL_NOTIFY));
        colCodesObject.put(DOSES_COL_NOTIFY_SOUND, colCodesMap.get(DOSES_COL_NOTIFY_SOUND));
        dbObject.put("col_codes", colCodesObject);

        rootJsonObject.put("db", dbObject);

        JSONArray medsArray = new JSONArray();
        String stmt = "SELECT * FROM " + MEDS_TABLE;
        Cursor medCursor = db.rawQuery(stmt, null);
        int i = 0;
        if (medCursor.moveToFirst()) {
            MedCursorParser mcp = new MedCursorParser(medCursor);
            do {
                Med med = mcp.getMedAtCursor(medCursor, context);
                JSONObject medObject = new JSONObject();
                medObject.put(colCodesMap.get(MEDS_COL_NAME), med.getName());
                medObject.put(colCodesMap.get(MEDS_COL_MAX_DOSE), med.getMaxDose());
                medObject.put(colCodesMap.get(MEDS_COL_DOSE_HOURS), med.getDoseHours());
                medObject.put(colCodesMap.get(MEDS_COL_COLOR), med.getColor());
                medObject.put(colCodesMap.get(MEDS_COL_IS_INVENTORY_TRACKED), med.getIsInventoryTracked());
                medObject.put(colCodesMap.get(MEDS_COL_REPORTED_INVENTORY), med.getReportedInventory());
                medObject.put(colCodesMap.get(MEDS_COL_INVENTORY_REPORTED_AT), med.getInventoryReportedAt());
                medObject.put(colCodesMap.get(MEDS_COL_DEFAULT_DOSE_COUNT), med.getDefaultDoseCount());
                medObject.put(colCodesMap.get(MEDS_COL_SHOW_DAY_DOSE_COUNT), med.getShowDayDoseCount());

                JSONArray dosesArray = new JSONArray();
                String doseStmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + DOSES_COL_MED_ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(med.getId())};
                Cursor doseCursor = db.rawQuery(doseStmt, selectionArgs);
                int j = 0;
                if (doseCursor.moveToFirst()) {
                    DoseCursorParser dcp = new DoseCursorParser(doseCursor, "id", 1);
                    do {
                        Dose dose = dcp.getDoseAtCursor(doseCursor, context);
                        JSONObject doseObject = new JSONObject();
                        doseObject.put(colCodesMap.get(DOSES_COL_COUNT), dose.getCount());
                        doseObject.put(colCodesMap.get(DOSES_COL_TAKEN_AT), dose.getTakenAt());
                        doseObject.put(colCodesMap.get(DOSES_COL_NOTIFY), dose.getNotify());
                        doseObject.put(colCodesMap.get(DOSES_COL_NOTIFY_SOUND), dose.getNotifySound());
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

            if (!colCodesMap.containsKey(MEDS_COL_NAME) ||
                    !colCodesMap.containsKey(MEDS_COL_MAX_DOSE) ||
                    !colCodesMap.containsKey(MEDS_COL_DOSE_HOURS) ||
                    !colCodesMap.containsKey(MEDS_COL_COLOR) ||
                    !colCodesMap.containsKey(DOSES_COL_COUNT) ||
                    !colCodesMap.containsKey(DOSES_COL_TAKEN_AT) ||
                    !colCodesMap.containsKey(DOSES_COL_NOTIFY) ||
                    !colCodesMap.containsKey(DOSES_COL_NOTIFY_SOUND)) {
                throw new Exception("Missing column code");
            }

            JSONArray medsArray = rootJsonObject.getJSONArray("meds");
            for (int i=0; i<medsArray.length(); ++i) {
                JSONObject medObject = medsArray.getJSONObject(i);
                String medName = medObject.getString(colCodesMap.get(MEDS_COL_NAME));
                int maxDose = medObject.getInt(colCodesMap.get(MEDS_COL_MAX_DOSE));
                int doseHours = medObject.getInt(colCodesMap.get(MEDS_COL_DOSE_HOURS));
                String color = medObject.getString(colCodesMap.get(MEDS_COL_COLOR));

                boolean isInventoryTracked = colCodesMap.containsKey(MEDS_COL_IS_INVENTORY_TRACKED)
                        && medObject.getBoolean(colCodesMap.get(MEDS_COL_IS_INVENTORY_TRACKED));
                double reportedInventory = (isInventoryTracked && colCodesMap.containsKey(MEDS_COL_REPORTED_INVENTORY))
                        ? medObject.getDouble(colCodesMap.get(MEDS_COL_REPORTED_INVENTORY))
                        : 0d;
                long inventoryReportedAt = (isInventoryTracked && colCodesMap.containsKey(MEDS_COL_INVENTORY_REPORTED_AT))
                        ? medObject.getLong(colCodesMap.get(MEDS_COL_INVENTORY_REPORTED_AT))
                        : 0L;
                int defaultDoseCount = colCodesMap.containsKey(MEDS_COL_DEFAULT_DOSE_COUNT)
                        ? medObject.getInt(colCodesMap.get(MEDS_COL_DEFAULT_DOSE_COUNT))
                        : 1;
                boolean showDayDoseCount = colCodesMap.containsKey(MEDS_COL_SHOW_DAY_DOSE_COUNT)
                        && medObject.getBoolean(colCodesMap.get(MEDS_COL_SHOW_DAY_DOSE_COUNT));

                int medID;
                String stmt = "SELECT id FROM " + MEDS_TABLE + " " +
                        "WHERE " + MEDS_COL_NAME + " = ? AND " +
                        MEDS_COL_MAX_DOSE + " = ? AND " +
                        MEDS_COL_DOSE_HOURS + " = ?";
                String[] selectionArgs = new String[]{
                        medName,
                        String.valueOf(maxDose),
                        String.valueOf(doseHours)
                };
                cursor = db.rawQuery(stmt, selectionArgs);
                cv = new ContentValues();
                cv.put(MEDS_COL_COLOR, color);
                cv.put(MEDS_COL_IS_INVENTORY_TRACKED, isInventoryTracked);
                cv.put(MEDS_COL_REPORTED_INVENTORY, reportedInventory);
                cv.put(MEDS_COL_INVENTORY_REPORTED_AT, inventoryReportedAt);
                cv.put(MEDS_COL_DEFAULT_DOSE_COUNT, defaultDoseCount);
                cv.put(MEDS_COL_SHOW_DAY_DOSE_COUNT, showDayDoseCount);
                if (cursor.moveToFirst()) {
                    int col_id = cursor.getColumnIndex("id");
                    medID = cursor.getInt(col_id);
                    String[] whereArgs = new String[]{String.valueOf(medID)};
                    db.update(MEDS_TABLE, cv, "id = ?", whereArgs);
                } else {
                    cv.put(MEDS_COL_NAME, medName);
                    cv.put(MEDS_COL_MAX_DOSE, maxDose);
                    cv.put(MEDS_COL_DOSE_HOURS, doseHours);
                    medID = (int) db.insert(MEDS_TABLE, null, cv);
                }

                JSONArray dosesArray = medObject.getJSONArray("doses");
                for (int j=0; j<dosesArray.length(); ++j) {
                    JSONObject doseObject = dosesArray.getJSONObject(j);
                    double count = doseObject.getDouble(colCodesMap.get(DOSES_COL_COUNT));
                    long takenAt = doseObject.getLong(colCodesMap.get(DOSES_COL_TAKEN_AT));
                    boolean notify = doseObject.getInt(colCodesMap.get(DOSES_COL_NOTIFY)) == 1;
                    boolean notifySound = doseObject.getInt(colCodesMap.get(DOSES_COL_NOTIFY_SOUND)) == 1;
                    cv = new ContentValues();
                    cv.put(DOSES_COL_MED_ID, medID);
                    cv.put(DOSES_COL_COUNT, count);
                    cv.put(DOSES_COL_TAKEN_AT, takenAt);
                    cv.put(DOSES_COL_NOTIFY, notify);
                    cv.put(DOSES_COL_NOTIFY_SOUND, notifySound);
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
