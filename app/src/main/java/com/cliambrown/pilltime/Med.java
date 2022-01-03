package com.cliambrown.pilltime;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Med {

    private int id;
    private String name;
    private int maxDose;
    private int doseHours;
    private List<Dose> doses = new ArrayList<Dose>();
    private Context context;

    public Med(int id, String name, int maxDose, int doseHours, Context context) {
        this.id = id;
        this.name = name;
        this.maxDose = maxDose;
        this.doseHours = doseHours;
        this.context = context;
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

    public String getMaxDoseInfo() {
        return context.getString(R.string.max) + " " + maxDose + " / " + doseHours + " " + context.getString(R.string.hours_short);
    }

    public List<Dose> getDoses() {
        return doses;
    }

    public void addDose(Dose dose) {
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
            doses.add(dose);
        }
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

    public void setDose(Dose dose) {
        int position = -1;
        for (int i=0; i<doses.size(); ++i) {
            if (doses.get(i).getId() == dose.getId()) {
                position = i;
            };
        }
        if (position > -1) {
            doses.set(position, dose);
        }
    }

    // Assumes doses are correctly ordered by time desc
    public double getCurrentTotalDoseCount() {
        double count = 0.0D;
        long now = System.currentTimeMillis() / 1000L;
        long doseTimeAgo = now - (doseHours * 60L * 60L);
        Dose dose;
        for (int i=0; i<doses.size(); ++i) {
            dose = doses.get(i);
            long takenAt = dose.getTakenAt();
            if (takenAt <= doseTimeAgo) break;
            if (takenAt <= now) count += dose.getCount();
        }
        return count;
    }

    public Dose getLatestDose() {
        long now = System.currentTimeMillis() / 1000L;
        long doseTimeAgo = now - (doseHours * 60L * 60L);
        Dose dose;
        for (int i=0; i<doses.size(); ++i) {
            dose = doses.get(i);
            long takenAt = dose.getTakenAt();
            if (takenAt > now) continue;
            return dose;
        }
        return null;
    }

    // Assumes doses are correctly ordered by time desc
    public long getLastTakenAt() {
        if (doses.size() > 0) {
            return doses.get(0).getTakenAt();
        }
        return -1;
    }
//
//    public String getLatestDoseExpiresAtString() {
//        if (doses.size() > 0) {
//            return doses.get(0).getExpiresAtString(this);
//        }
//        return null;
//    }

}
