package com.cliambrown.pilltime;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public String getDoseInfo() {
        return maxDose +
                " " + context.getString(R.string.every) + " " +
                doseHours +
                " " + context.getString(R.string.hours);
    }

    public List<Dose> getDoses() {
        return doses;
    }

    public void addDose(Dose dose) {
        doses.add(dose);
        Collections.sort(doses, new Comparator<Dose>() {
            @Override
            public int compare(Dose d1, Dose d2) {
                return d2.getTakenAt() - d1.getTakenAt();
            }
        });
    }
}
