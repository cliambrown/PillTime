package com.cliambrown.pilltime;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;

public class Dose {

    private int id;
    private int medID;
    private float count;
    private int takenAt; // Unix time
    private Context context;

    public Dose(int id, int medID, float count, int takenAt, Context context) {
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

    public float getCount() { return count; }

    public void setCount(float count) {
        this.count = count;
    }

    public int getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(int takenAt) {
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
        calendar.setTimeInMillis((long) takenAt * 1000);
        DateFormat dateFormat = new SimpleDateFormat("kk:mm");
        return dateFormat.format(calendar.getTime());
    }

    public String getDateString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis((long) takenAt * 1000);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return dateFormat.format(calendar.getTime());
    }

    public String getDateTimeString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis((long) takenAt * 1000);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd kk:mm");
        return dateFormat.format(calendar.getTime());
    }
}
