package com.cliambrown.pilltime.doses;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.Utils;

public class Dose {

    private int id;
    private int medID;
    private double count;
    private long takenAt; // Unix time (s)
    private boolean notify;
    private boolean notifySound;
    private Context context;
    boolean isActive;
    private long expiresAt; // Unix time (s)
    String takenAtTimeAgo;
    String expiresAtTimeAgo;

    public Dose(int id, int medID, double count, long takenAt, boolean notify, boolean notifySound, Context context) {
        this.id = id;
        this.medID = medID;
        this.count = count;
        this.takenAt = takenAt;
        this.notify = notify;
        this.notifySound = notifySound;
        this.context = context;
    }

    @NonNull
    @Override
    public String toString() {
        return "Dose{" +
                "id=" + id +
                ", medID=" + medID +
                ", count=" + count +
                ", takenAt=" + takenAt +
                ", notify=" + notify +
                ", notifySound=" + notifySound +
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

    public long getTakenAt() {
        return takenAt;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean getNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public boolean getNotifySound() {
        return notifySound;
    }

    public String getTakenAtTimeAgo() {
        return takenAtTimeAgo;
    }

    public String getExpiresAtTimeAgo() {
        return expiresAtTimeAgo;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void updateTimes(Med med) {
        expiresAt = takenAt + (med.getDoseHours() * 60L * 60L);
        long now = System.currentTimeMillis() / 1000L;
        isActive = (takenAt <= now && expiresAt > now);
        takenAtTimeAgo = Utils.getRelativeTimeSpanString(context, takenAt);
        expiresAtTimeAgo = Utils.getRelativeTimeSpanString(context, expiresAt);
    }
}
