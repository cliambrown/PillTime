package com.cliambrown.pilltime;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Dose {

    private int id;
    private int medID;
    private double count;
    private long takenAt; // Unix time
    private Context context;
    boolean isActive;
    String takenAtTimeAgo;
    String expiresAtTimeAgo;

    public Dose(int id, int medID, double count, long takenAt, Context context) {
        this.id = id;
        this.medID = medID;
        this.count = count;
        this.takenAt = takenAt;
        this.context = context;
    }

    @Override
    public String toString() {
        return "Dose{" +
                "id=" + id +
                ", medID=" + medID +
                ", count=" + count +
                ", takenAt=" + takenAt +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedID() {
        return medID;
    }

    public void setMedID(int medID) {
        this.medID = medID;
    }

    public double getCount() { return count; }

    public void setCount(double count) {
        this.count = count;
    }

    public long getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(long takenAt) {
        this.takenAt = takenAt;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getTimeString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(takenAt * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("kk:mm");
        return dateFormat.format(calendar.getTime());
    }

    public String getDateString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(takenAt * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return dateFormat.format(calendar.getTime());
    }

    public boolean isActive() {
        return isActive;
    }

    public String getTakenAtTimeAgo() {
        return takenAtTimeAgo;
    }

    public String getExpiresAtTimeAgo() {
        return expiresAtTimeAgo;
    }

    public void updateDoseStatus(Med med) {
        long expiresAt = takenAt + (med.getDoseHours() * 60L * 60L);
        long now = System.currentTimeMillis() / 1000L;
        isActive = (takenAt <= now && expiresAt > now);
        takenAtTimeAgo = DateUtils.getRelativeTimeSpanString(
                takenAt * 1000L,
                System.currentTimeMillis(),
                0,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
        expiresAtTimeAgo = DateUtils.getRelativeTimeSpanString(
                expiresAt * 1000L,
                System.currentTimeMillis(),
                0,
                DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString();
    }
}
