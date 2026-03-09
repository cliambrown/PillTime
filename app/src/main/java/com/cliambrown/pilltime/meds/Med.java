package com.cliambrown.pilltime.meds;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class Med {

    // Once the remaining doses ratio has fallen under this threshold,
    // the user should be warned
    public static final double INVENTORY_WARN_THRESHOLD = 0.2;

    private final Context context;
    private int id;
    private String name;
    private int maxDose;
    private int doseHours;
    private String color;
    private boolean isInventoryTracked;
    private double reportedInventory;
    private long inventoryReportedAt;

    private final List<Dose> doses = new ArrayList<>();
    private boolean hasLoadedAllDoses;
    Dose latestDose;
    Dose nextExpiringDose;
    double activeDoseCount;
    private double currentInventory;

    public Med(int id, String name, int maxDose, int doseHours, String color, boolean isInventoryTracked,
               double reportedInventory, long inventoryReportedAt, Context context) {
        this.context = context;
        this.id = id;
        this.name = name;
        this.maxDose = maxDose;
        this.doseHours = doseHours;
        this.color = color;
        this.isInventoryTracked = isInventoryTracked;
        this.reportedInventory = reportedInventory;
        this.inventoryReportedAt = inventoryReportedAt;
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
                ", color=" + color +
                ", isInventoryTracked=" + isInventoryTracked +
                ", reportedInventory=" + reportedInventory +
                ", inventoryReportedAt=" + inventoryReportedAt +
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

    public long getDoseDurationInSeconds() { return doseHours * 60L * 60L; }

    public String getColor() {
        if (color == null) return "pink";
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /**
     * @return boolean indicating whether the user wants the remaining doses to be tracked
     */
    public boolean getIsInventoryTracked() {
        return isInventoryTracked;
    }

    /**
     * @param isInventoryTracked boolean indicating whether the user wants the remaining doses to be tracked
     */
    public void setIsInventoryTracked(boolean isInventoryTracked) {
        this.isInventoryTracked = isInventoryTracked;
    }

    /**
     * @return timestamp in seconds since epoch, which tells us when the user has last reported their remaining doses
     */
    public long getInventoryReportedAt() {
        return inventoryReportedAt;
    }

    /**
     * @param inventoryReportedAt timestamp in seconds since epoch, which tells us when the user has last reported
     *                                their remaining doses
     */
    public void setInventoryReportedAt(long inventoryReportedAt) {
        this.inventoryReportedAt = inventoryReportedAt;
    }

    /**
     * @return the amount of remaining doses reported by the user
     */
    public double getReportedInventory() {
        return reportedInventory;
    }

    /**
     * @param reportedInventory the amount of remaining doses reported by the user
     */
    public void setReportedInventory(double reportedInventory) {
        this.reportedInventory = reportedInventory;
    }

    /**
     * @return number that has been decremented by however many doses have been taken since last report of remaining
     * doses. Indicates the actually still available amount of remaining doses.
     */
    public double getCurrentInventory() {
        return currentInventory;
    }

    public boolean getIsInventoryLow() {
        return this.isInventoryTracked
                && this.currentInventory == 0
                || (
                    this.reportedInventory > 0
                    && this.currentInventory / this.reportedInventory <= INVENTORY_WARN_THRESHOLD
                );
    }

    public String getMaxDoseInfoStr() {
        return Utils.buildMaxDosePerHourString(context, maxDose, doseHours);
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
        for (Dose dose : doses) {
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

    public Dose getNextExpiringDose() { return nextExpiringDose; }

    public double getActiveDoseCount() {
        return activeDoseCount;
    }

    public void setActiveDoseCount(double activeDoseCount) {
        this.activeDoseCount = activeDoseCount;
    }

    public String getInventoryStr() {
        return (int) getCurrentInventory() + " / " + (int) getReportedInventory();
    }

    public void updateTimes() {
        long now = System.currentTimeMillis() / 1000L;
        long earliestActiveTakenAt = now - getDoseDurationInSeconds();
        activeDoseCount = 0.0D;
        nextExpiringDose = null;
        latestDose = null;
        currentInventory = reportedInventory;
        for (Dose dose : doses) {
            if (dose.getTakenAt() >= inventoryReportedAt && currentInventory > 0)
                currentInventory -= dose.getCount();
            if (dose.getTakenAt() > now) continue;
            if (latestDose == null) latestDose = dose;
            if (dose.getTakenAt() > earliestActiveTakenAt) {
                activeDoseCount += dose.getCount();
                nextExpiringDose = dose;
            } else {
                break;
            }
        }
        currentInventory = Math.max(0, currentInventory);
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
