package com.cliambrown.pilltime;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Med {

    private int id;
    private String name;
    private int maxDose;
    private int doseHours;
    private String color;
    private List<Dose> doses = new ArrayList<Dose>();
    private Context context;
    private boolean hasLoadedAllDoses;

    Dose latestDose;
    double currentTotalDoseCount;
    String latestDoseExpiresInStr;

    public Med(int id, String name, int maxDose, int doseHours, String color, Context context) {
        this.id = id;
        this.name = name;
        this.maxDose = maxDose;
        this.doseHours = doseHours;
        this.color = color;
        this.context = context;
        this.hasLoadedAllDoses = false;
    }

    @Override
    public String toString() {
        return "Med{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxDose=" + maxDose +
                ", doseHours=" + doseHours +
                ", doses=" + doses +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxDose() {
        return maxDose;
    }

    public void setMaxDose(int maxDose) {
        this.maxDose = maxDose;
    }

    public int getDoseHours() {
        return doseHours;
    }

    public void setDoseHours(int doseHours) {
        this.doseHours = doseHours;
    }

    public String getColor() {
        if (color == null) return "pink";
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMaxDoseInfo() {
        return context.getString(R.string.max) + " " + maxDose + " / " + doseHours + " " + context.getString(R.string.hours_short);
    }

    public List<Dose> getDoses() {
        return doses;
    }

    public int addDose(Dose dose) {
        int position = -1;
        int doseID = dose.getId();
        long takenAt = dose.getTakenAt();
        for (int i=0; i<doses.size(); ++i) {
            Dose listDose = doses.get(i);
            long listTakenAt = listDose.getTakenAt();
            if (takenAt > listTakenAt) {
                position = i;
                break;
            }
            if (takenAt == listTakenAt && doseID > listDose.getId()) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            doses.add(position, dose);
        } else {
            position = doses.size();
            doses.add(dose);
        }
        return position;
    }

    public void addDoseToEnd(Dose dose) {
        doses.add(dose);
    }

    public Dose getDoseById(int doseID) {
        Dose dose;
        for (int i=0; i<doses.size(); ++i) {
            dose = doses.get(i);
            if (dose.getId() == doseID) {
                return dose;
            }
        }
        return null;
    }

    public int setDose(Dose dose) {
        int position = -1;
        for (int i=0; i<doses.size(); ++i) {
            if (doses.get(i).getId() == dose.getId()) {
                position = i;
            };
        }
        if (position > -1) {
            doses.set(position, dose);
        }
        return position;
    }

    public int repositionDose(Dose dose, int currentPosition) {
        int newPosition = -1;
        int doseID = dose.getId();
        long takenAt = dose.getTakenAt();
        doses.remove(currentPosition);
        Dose listDose;
        int listDoseID;
        long takenDiff;
        for (int i=0; i<doses.size(); ++i) {
            listDose = doses.get(i);
            listDoseID = listDose.getId();
            takenDiff = listDose.getTakenAt() - takenAt;
            if (takenDiff < 0 || (takenDiff == 0 && doseID > listDoseID)) {
                newPosition = i;
                break;
            }
        }
        if (newPosition > -1) {
            doses.add(newPosition, dose);
        } else {
            doses.add(dose);
            newPosition = doses.size() - 1;
        }
        return newPosition;
    }

    public boolean sortBefore(Med compareMed) {
        long lastTakenAt = -1;
        int lastTakenId = -1;
        Dose latestDose = getLatestDose();
        if (latestDose != null) {
            lastTakenAt = latestDose.getTakenAt();
            lastTakenId = latestDose.getId();
        }
        long compareLastTakenAt = -1;
        int compareLastTakenId = -1;
        Dose compareLatestDose = compareMed.getLatestDose();
        if (compareLatestDose != null) {
            compareLastTakenAt = compareLatestDose.getTakenAt();
            compareLastTakenId = compareLatestDose.getId();
        }
        if (lastTakenAt > compareLastTakenAt) return true;
        if (lastTakenAt < compareLastTakenAt) return false;
        if (lastTakenId > compareLastTakenId) return true;
        if (lastTakenId < compareLastTakenId) return false;
        return (id > compareMed.getId());
    }

    // Assumes doses are correctly ordered by time desc
    public double calculateCurrentTotalDoseCount() {
        double count = 0.0D;
        long now = System.currentTimeMillis() / 1000L;
        long doseTimeAgo = now - (doseHours * 60L * 60L);
        Dose dose;
        for (int i=0; i<doses.size(); ++i) {
            dose = doses.get(i);
            long takenAt = dose.getTakenAt();
            if (takenAt >= now) continue;
            if (takenAt <= doseTimeAgo) break;
            count += dose.getCount();
        }
        return count;
    }

    public Dose calculateLatestDose() {
        long now = System.currentTimeMillis() / 1000L;
        Dose dose;
        for (int i=0; i<doses.size(); ++i) {
            dose = doses.get(i);
            long takenAt = dose.getTakenAt();
            if (takenAt > now) continue;
            return dose;
        }
        return null;
    }

    public Dose getLatestDose() {
        return latestDose;
    }

    public void setLatestDose(Dose latestDose) {
        this.latestDose = latestDose;
    }

    public double getCurrentTotalDoseCount() {
        return currentTotalDoseCount;
    }

    public void setCurrentTotalDoseCount(double currentTotalDoseCount) {
        this.currentTotalDoseCount = currentTotalDoseCount;
    }

    public String getLatestDoseExpiresInStr() {
        return latestDoseExpiresInStr;
    }

    public void setLatestDoseExpiresInStr(String latestDoseExpiresInStr) {
        this.latestDoseExpiresInStr = latestDoseExpiresInStr;
    }

    public void updateDoseStatus() {
        latestDose = calculateLatestDose();
        currentTotalDoseCount = calculateCurrentTotalDoseCount();

        if (latestDose == null) {
            latestDoseExpiresInStr = "";
        } else {
            double latestDoseCount = latestDose.getCount();
            long expiresAtUnix = latestDose.getTakenAt() + (doseHours * 60L * 60L);
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    expiresAtUnix * 1000L,
                    System.currentTimeMillis(),
                    0,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
            latestDoseExpiresInStr = context.getString(R.string.expires) + " " +
                    Utils.decapitalize(timeAgo) + " (" + Utils.simpleFutureTime(context, expiresAtUnix) + ")";
            if (latestDoseCount < currentTotalDoseCount) {
                latestDoseExpiresInStr = "x" + Utils.getStrFromDbl(latestDoseCount) + " " + latestDoseExpiresInStr;
            }
        }
    }

    public boolean hasLoadedAllDoses() {
        return hasLoadedAllDoses;
    }

    public void setHasLoadedAllDoses(boolean hasLoadedAllDoses) {
        this.hasLoadedAllDoses = hasLoadedAllDoses;
    }
}
