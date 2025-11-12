package com.cliambrown.pilltime.meds;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.utilities.Utils;
import com.cliambrown.pilltime.doses.Dose;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldMayBeFinal")
public class Med {

    private int id;
    private String name;
    private int maxDose;
    private int doseHours;
    private String color;
    private final List<Dose> doses = new ArrayList<Dose>();
    private Context context;
    private boolean hasLoadedAllDoses;

    Dose latestDose;
    Dose nextExpiringDose;
    double activeDoseCount;
    String nextExpiringDoseExpiresInStr;
    String lastTakenAtStr;

    public Med(int id, String name, int maxDose, int doseHours, String color, Context context) {
        this.id = id;
        this.name = name;
        this.maxDose = maxDose;
        this.doseHours = doseHours;
        this.color = color;
        this.context = context;
        this.hasLoadedAllDoses = false;
    }

    @NonNull
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
            }
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

    public Dose getLatestDose() {
        return latestDose;
    }

    public double getActiveDoseCount() {
        return activeDoseCount;
    }

    public void setActiveDoseCount(double activeDoseCount) {
        this.activeDoseCount = activeDoseCount;
    }

    public String getNextExpiringDoseExpiresInStr() {
        return nextExpiringDoseExpiresInStr;
    }

    public String getLastTakenAtStr() {
        return lastTakenAtStr;
    }

    public void updateTimes() {
        double doseCount = 0.0D;
        long now = System.currentTimeMillis() / 1000L;
        long doseDuration = doseHours * 60L * 60L;
        long earliestActiveTakenAt = now - doseDuration;
        Dose dose;
        Dose loopNextExpiringDose = null;
        Dose loopLatestDose = null;
        for (int i=0; i<doses.size(); i++) {
            dose = doses.get(i);
            if (dose.getTakenAt() > now) continue;
            if (loopLatestDose == null) loopLatestDose = dose;
            if (dose.getTakenAt() > earliestActiveTakenAt) {
                doseCount += dose.getCount();
                loopNextExpiringDose = dose;
            } else {
                break;
            }
        }
        activeDoseCount = doseCount;
        latestDose = loopLatestDose;
        nextExpiringDose = loopNextExpiringDose;

        if (nextExpiringDose != null) {
            double nextExpiringDoseCount = nextExpiringDose.getCount();
            long expiresAtUnix = nextExpiringDose.getTakenAt() + doseDuration;
            String timeAgo = Utils.getRelativeTimeSpanString(context, expiresAtUnix);
            nextExpiringDoseExpiresInStr = context.getString(R.string.expires) + " " +
                    timeAgo + " (" + Utils.simpleFutureTime(context, expiresAtUnix) + ")";
            if (nextExpiringDoseCount < activeDoseCount) {
                nextExpiringDoseExpiresInStr = "x" + Utils.getStrFromDbl(nextExpiringDoseCount)
                        + " " + nextExpiringDoseExpiresInStr;
            }
        }

        if (latestDose == null) {
            lastTakenAtStr = context.getString(R.string.never);
        } else {
            lastTakenAtStr = Utils.getRelativeTimeSpanString(context, latestDose.getTakenAt());
        }
    }

    public boolean hasLoadedAllDoses() {
        return hasLoadedAllDoses;
    }

    public void setHasLoadedAllDoses(boolean hasLoadedAllDoses) {
        this.hasLoadedAllDoses = hasLoadedAllDoses;
    }

    public void removeDoseAndOlder(Dose dose) {
        int position = -1;
        for (int i=0; i<doses.size(); ++i) {
            Dose listDose = doses.get(i);
            if (listDose.getId() == dose.getId()) {
                position = i;
                break;
            }
        }
        if (position == -1) return;
        while (doses.size() > position) {
            doses.remove(doses.size() - 1);
        }
        this.hasLoadedAllDoses = true;
    }
}
