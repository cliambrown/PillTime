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

    public static final String MEDS_TABLE = "meds";
    public static final String MEDS_COL_NAME = "name";
    public static final String MEDS_COL_MAX_DOSE = "max_dose";
    public static final String MEDS_COL_DOSE_HOURS = "dose_hours";
    public static final String MEDS_COL_COLOR = "color";

    public static final String DOSES_TABLE = "doses";
    public static final String DOSES_COL_MED_ID = "med_id";
    public static final String DOSES_COL_COUNT = "count";
    public static final String DOSES_COL_TAKEN_AT = "taken_at";
    public static final String DOSES_COL_NOTIFY = "notify";
    public static final String DOSES_COL_NOTIFY_SOUND = "notify_sound";

    private final Context context;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, 3);
        this.context = context;
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
                MEDS_COL_COLOR + " TEXT)";
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
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<Med> getAllMeds() {

        List<Med> returnList = new ArrayList<>();

        String stmt = "SELECT * FROM " + MEDS_TABLE + " " +
                "LEFT JOIN (SELECT id AS dose_id, " + DOSES_COL_MED_ID + ", MAX(" + DOSES_COL_TAKEN_AT + ") AS " + DOSES_COL_TAKEN_AT + " " +
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

    public Med getMedById(int medID) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] selectionArgs = new String[]{String.valueOf(medID)};
        String stmt = "SELECT * FROM " + MEDS_TABLE + " WHERE id = ?";
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (!cursor.moveToFirst()) return null;
        int col_name = cursor.getColumnIndex(MEDS_COL_NAME);
        int col_maxDose = cursor.getColumnIndex(MEDS_COL_MAX_DOSE);
        int col_doseHours = cursor.getColumnIndex(MEDS_COL_DOSE_HOURS);
        int col_color = cursor.getColumnIndex(MEDS_COL_COLOR);
        String medName = cursor.getString(col_name);
        int maxDose = cursor.getInt(col_maxDose);
        int doseHours = cursor.getInt(col_doseHours);
        String color = cursor.getString(col_color);
        Med med = new Med(medID, medName, maxDose, doseHours, color, context);

        long now = System.currentTimeMillis() / 1000L;
        long startTime = now - (med.getDoseHours() * 60L * 60L);
        selectionArgs = new String[]{
                String.valueOf(med.getId()),
                String.valueOf(now),
                String.valueOf(startTime)
        };
        stmt = "SELECT sum(" + DOSES_COL_COUNT + ") AS dose_count " +
                "FROM " + DOSES_TABLE + " " +
                "WHERE " + DOSES_COL_MED_ID + " = ? " +
                "AND " + DOSES_COL_TAKEN_AT + " < ? " +
                "AND " + DOSES_COL_TAKEN_AT + " > ?";
        cursor = db.rawQuery(stmt, selectionArgs);
        if (!cursor.moveToFirst()) {
            med.setCurrentTotalDoseCount(0D);
            return med;
        }
        int col_doseCount = cursor.getColumnIndex("dose_count");
        med.setCurrentTotalDoseCount(cursor.getDouble(col_doseCount));

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
                !Arrays.asList(context.getResources().getStringArray(R.array.color_options)).contains(med.getColor())
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

    public Dose getDoseById(int doseID) {
        SQLiteDatabase db = this.getReadableDatabase();
        String stmt = "SELECT " + DOSES_TABLE + ".* FROM " + DOSES_TABLE + " " +
                "WHERE " + DOSES_TABLE + ".id = ?";
        String[] selectionArgs = new String[]{String.valueOf(doseID)};
        Cursor cursor = db.rawQuery(stmt, selectionArgs);
        if (!cursor.moveToFirst()) return null;
        int col_medID = cursor.getColumnIndex(DOSES_COL_MED_ID);
        int col_count = cursor.getColumnIndex(DOSES_COL_COUNT);
        int col_takenAt = cursor.getColumnIndex(DOSES_COL_TAKEN_AT);
        int col_notify = cursor.getColumnIndex(DOSES_COL_NOTIFY);
        int col_notifySound = cursor.getColumnIndex(DOSES_COL_NOTIFY_SOUND);
        int medID = cursor.getInt(col_medID);
        double count = cursor.getDouble(col_count);
        long takenAt = cursor.getLong(col_takenAt);
        boolean notify = (cursor.getInt(col_notify) == 1);
        boolean notifySound = (cursor.getInt(col_notifySound) == 1);
        Dose dose = new Dose(doseID, medID, count, takenAt, notify, notifySound, context);
        cursor.close();
        db.close();
        return dose;
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
            int col_notify = cursor.getColumnIndex(DOSES_COL_NOTIFY);
            int col_notifySound = cursor.getColumnIndex(DOSES_COL_NOTIFY_SOUND);
            do {
                int doseID = cursor.getInt(col_id);
                double count = cursor.getDouble(col_count);
                long takenAt = cursor.getLong(col_takenAt);
                boolean notify = (cursor.getInt(col_notify) == 1);
                boolean notifySound = (cursor.getInt(col_notifySound) == 1);
                Dose dose = new Dose(doseID, med.getId(), count, takenAt, notify, notifySound, context);
                returnList.add(dose);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    public List<Dose> getActiveDoses() {
        List<Dose> returnList = new ArrayList<Dose>();
        List<Med> meds = new ArrayList<Med>();
        long now = System.currentTimeMillis() / 1000L;
        SQLiteDatabase db = this.getReadableDatabase();
        // NOTE: using selectionArgs here for now didn't work for some reason
        String stmt = "SELECT *, D.id as dose_id, " +
                "(D." + DOSES_COL_TAKEN_AT + " + M." + MEDS_COL_DOSE_HOURS + " * 60 * 60) AS expires_at " +
                "FROM " + MEDS_TABLE + " M " +
                "LEFT JOIN " + DOSES_TABLE + " D " +
                "ON D." + DOSES_COL_MED_ID +" = M.id " +
                "WHERE D." + DOSES_COL_NOTIFY + " > 0 AND expires_at > " + now;
        Cursor cursor = db.rawQuery(stmt, null);
        if (cursor.moveToFirst()) {
            int col_id = cursor.getColumnIndex("dose_id");
            int col_expiresAt = cursor.getColumnIndex("expires_at");
            int col_medID = cursor.getColumnIndex(DOSES_COL_MED_ID);
            int col_count = cursor.getColumnIndex(DOSES_COL_COUNT);
            int col_takenAt = cursor.getColumnIndex(DOSES_COL_TAKEN_AT);
            int col_notify = cursor.getColumnIndex(DOSES_COL_NOTIFY);
            int col_notifySound = cursor.getColumnIndex(DOSES_COL_NOTIFY_SOUND);
            do {
                int doseID = cursor.getInt(col_id);
                int medID = cursor.getInt(col_medID);
                double count = cursor.getDouble(col_count);
                long takenAt = cursor.getLong(col_takenAt);
                boolean notify = (cursor.getInt(col_notify) == 1);
                boolean notifySound = (cursor.getInt(col_notifySound) == 1);
                Dose dose = new Dose(doseID, medID, count, takenAt, notify, notifySound, context);
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

        HashMap<String, String> colCodesMap = new HashMap<String, String>();
        colCodesMap.put(MEDS_COL_NAME, "m1");
        colCodesMap.put(MEDS_COL_MAX_DOSE, "m2");
        colCodesMap.put(MEDS_COL_DOSE_HOURS, "m3");
        colCodesMap.put(MEDS_COL_COLOR, "m4");
        colCodesMap.put(DOSES_COL_COUNT, "d1");
        colCodesMap.put(DOSES_COL_TAKEN_AT, "d2");
        colCodesMap.put(DOSES_COL_NOTIFY, "d3");
        colCodesMap.put(DOSES_COL_NOTIFY_SOUND, "d4");

        JSONObject colCodesObject = new JSONObject();
        colCodesObject.put(MEDS_COL_NAME, colCodesMap.get(MEDS_COL_NAME));
        colCodesObject.put(MEDS_COL_MAX_DOSE, colCodesMap.get(MEDS_COL_MAX_DOSE));
        colCodesObject.put(MEDS_COL_DOSE_HOURS, colCodesMap.get(MEDS_COL_DOSE_HOURS));
        colCodesObject.put(MEDS_COL_COLOR, colCodesMap.get(MEDS_COL_COLOR));
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
            int col_id = medCursor.getColumnIndex("id");
            int col_name = medCursor.getColumnIndex(MEDS_COL_NAME);
            int col_maxDose = medCursor.getColumnIndex(MEDS_COL_MAX_DOSE);
            int col_doseHours = medCursor.getColumnIndex(MEDS_COL_DOSE_HOURS);
            int col_color = medCursor.getColumnIndex(MEDS_COL_COLOR);
            do {
                int medID = medCursor.getInt(col_id);
                String medName = medCursor.getString(col_name);
                int maxDose = medCursor.getInt(col_maxDose);
                int doseHours = medCursor.getInt(col_doseHours);
                String color = medCursor.getString(col_color);
                JSONObject medObject = new JSONObject();
                medObject.put(colCodesMap.get(MEDS_COL_NAME), medName);
                medObject.put(colCodesMap.get(MEDS_COL_MAX_DOSE), maxDose);
                medObject.put(colCodesMap.get(MEDS_COL_DOSE_HOURS), doseHours);
                medObject.put(colCodesMap.get(MEDS_COL_COLOR), color);

                JSONArray dosesArray = new JSONArray();
                String doseStmt = "SELECT * FROM " + DOSES_TABLE + " WHERE " + DOSES_COL_MED_ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(medID)};
                Cursor doseCursor = db.rawQuery(doseStmt, selectionArgs);
                int j = 0;
                if (doseCursor.moveToFirst()) {
                    int col_count = doseCursor.getColumnIndex(DOSES_COL_COUNT);
                    int col_takenAt = doseCursor.getColumnIndex(DOSES_COL_TAKEN_AT);
                    int col_notify = doseCursor.getColumnIndex(DOSES_COL_NOTIFY);
                    int col_notifySound = doseCursor.getColumnIndex(DOSES_COL_NOTIFY_SOUND);
                    do {
                        double count = doseCursor.getDouble(col_count);
                        long takenAt = doseCursor.getLong(col_takenAt);
                        int notify = doseCursor.getInt(col_notify);
                        int notifySound = doseCursor.getInt(col_notifySound);
                        JSONObject doseObject = new JSONObject();
                        doseObject.put(colCodesMap.get(DOSES_COL_COUNT), count);
                        doseObject.put(colCodesMap.get(DOSES_COL_TAKEN_AT), takenAt);
                        doseObject.put(colCodesMap.get(DOSES_COL_NOTIFY), notify);
                        doseObject.put(colCodesMap.get(DOSES_COL_NOTIFY_SOUND), notifySound);
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

    @SuppressWarnings("ConstantConditions")
    public void importFromString(String jsonText) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        ContentValues cv;
        try {
            JSONObject rootJsonObject = new JSONObject(jsonText);
            JSONObject dbObject = rootJsonObject.getJSONObject("db");
            JSONObject colCodesObject = dbObject.getJSONObject("col_codes");
            Iterator<String> keys = colCodesObject.keys();
            HashMap<String, String> colCodesMap = new HashMap<String, String>();
            while (keys.hasNext()) {
                String key = keys.next();
                String val = colCodesObject.getString(key);
                colCodesMap.put(key, val);
            }

            if (!colCodesMap.containsKey(MEDS_COL_NAME) ||
                    colCodesMap.containsKey(MEDS_COL_MAX_DOSE) ||
                    colCodesMap.containsKey(MEDS_COL_DOSE_HOURS) ||
                    colCodesMap.containsKey(MEDS_COL_COLOR)) {
                throw new Exception("Missing column code");
            }

            JSONArray medsArray = rootJsonObject.getJSONArray("meds");
            for (int i=0; i<medsArray.length(); ++i) {
                JSONObject medObject = medsArray.getJSONObject(i);
                String medName = medObject.getString(colCodesMap.get(MEDS_COL_NAME));
                int maxDose = medObject.getInt(colCodesMap.get(MEDS_COL_MAX_DOSE));
                int doseHours = medObject.getInt(colCodesMap.get(MEDS_COL_DOSE_HOURS));
                String color = medObject.getString(colCodesMap.get(MEDS_COL_COLOR));

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
                if (cursor.moveToFirst()) {
                    int col_id = cursor.getColumnIndex("id");
                    medID = cursor.getInt(col_id);
                } else {
                    cv = new ContentValues();
                    cv.put(MEDS_COL_NAME, medName);
                    cv.put(MEDS_COL_MAX_DOSE, maxDose);
                    cv.put(MEDS_COL_DOSE_HOURS, doseHours);
                    cv.put(MEDS_COL_COLOR, color);
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
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error importing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if (cursor != null) cursor.close();
        db.close();
    }
}
