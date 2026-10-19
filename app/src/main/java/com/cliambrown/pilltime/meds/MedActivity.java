package com.cliambrown.pilltime.meds;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.MainActivity;
import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.doses.DosesRecycleViewAdapter;
import com.cliambrown.pilltime.doses.EditDoseActivity;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MedActivity extends AppCompatActivity {

    Menu optionsMenu;
    TextView tv_med_name;
    TextView tv_med_maxDoseInfo;
    TextView tv_med_takenInPast;
    TextView tv_med_takenInPastDay;

    TextView tv_med_inventory;
    LinearLayout ll_med_no_doses;
    ExtendedFloatingActionButton btn_med_add_dose;

    private RecyclerView recyclerView;
    private DosesRecycleViewAdapter mAdapter;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter broadcastReceiverFilter;

    PillTimeApplication mApp;
    Timer timer;
    int medID;
    Med med;
    List<Dose> doses = new ArrayList<>();

    OnBackPressedCallback onBackPressedCallback;
    public boolean isMultiSelectActive = false;
    public HashSet<Integer> selectedDoseIDs = new HashSet<>();

    protected void getDoses() {
        doses = med.getDoses();
        onDoseCountChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tv_med_name = findViewById(R.id.tv_med_name);
        tv_med_maxDoseInfo = findViewById(R.id.tv_med_maxDoseInfo);
        tv_med_takenInPast = findViewById(R.id.tv_med_takenInPast);
        tv_med_takenInPastDay = findViewById(R.id.tv_med_takenInPastDay);
        tv_med_inventory = findViewById(R.id.tv_med_inventory);
        ll_med_no_doses = findViewById(R.id.ll_med_no_doses);
        btn_med_add_dose = findViewById(R.id.btn_med_add_dose);

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        med = mApp.getMed(medID);

        if (med == null) {
            MedActivity.this.finish();
            return;
        }

        recyclerView = findViewById(R.id.rv_med_doses);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DosesRecycleViewAdapter(med, this, MedActivity.this, mApp);
        recyclerView.setAdapter(mAdapter);
        float offsetPx = getResources().getDimension(R.dimen.bottom_offset_dp);
        Utils.BottomOffsetDecoration bottomOffsetDecoration = new Utils.BottomOffsetDecoration((int) offsetPx);
        recyclerView.addItemDecoration(bottomOffsetDecoration);

        setTitle(getString(R.string.med));

        broadcastReceiver = new MedActivity.MedBroadcastReceiver();
        broadcastReceiverFilter = new IntentFilter();
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DB_CLEARED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.MED_EDITED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.MED_ARCHIVED_OR_UNARCHIVED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSES_LOADED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSE_ADDED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSE_EDITED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSE_MOVED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSE_REMOVED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSES_REMOVED");
        broadcastReceiverFilter.addAction("com.cliambrown.broadcast.DOSE_AND_OLDER_REMOVED");

        btn_med_add_dose.setOnClickListener(view -> {
            Intent intent1 = new Intent(MedActivity.this, EditDoseActivity.class);
            intent1.putExtra("medID", medID);
            startActivity(intent1);
        });

        btn_med_add_dose.setOnLongClickListener(view -> {
            double count = med.getDefaultDoseCount();
            long takenAt = System.currentTimeMillis() / 1000L;
            Dose dose1 = new Dose(-1, medID, count, takenAt,
                    med.getNetNotifyDefault(this),
                    med.getNetNotifySoundDefault(this),
                    this);
            mApp.addDose(med, dose1);
            return true;
        });

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                deactivateMultiSelect();
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        onMedInfoChanged();

        getDoses();
        med.updateTimes(mApp.getDbHelper());
        onMedTimesChanged();
        for (Dose dose: doses) {
            dose.updateTimes(med);
        }
        mAdapter.setDoses(doses);

        // As of Android 14, registerReceiver now requires a flag (RECEIVER_EXPORTED or _NOT_).
        // No need to receive broadcasts from other apps, so this should be RECEIVER_NOT_EXPORTED.
        // HOWEVER, the BR does not receive broadcasts unless RECEIVER_EXPORTED is used (??).
        // Although not ideal, using RECEIVER_EXPORTED here does not present any obvious risks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, broadcastReceiverFilter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, broadcastReceiverFilter);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        startUpdateTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
        if (broadcastReceiver != null) unregisterReceiver(broadcastReceiver);
    }

    public class MedBroadcastReceiver extends BroadcastReceiver {
        @SuppressWarnings({"UnnecessaryReturnStatement"})
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action == null) return;

            if (action.equals("com.cliambrown.broadcast.DB_CLEARED")) {
                MedActivity.this.finish();
                return;
            }

            if (mAdapter == null) return;
            int intentMedID = intent.getIntExtra("medID", -1);
            if (intentMedID != medID) return;

            switch (action) {
                case "com.cliambrown.broadcast.MED_EDITED":
                    onMedInfoChanged();
                    updateAllTimes();
                    return;
                case "com.cliambrown.broadcast.MED_ARCHIVED_OR_UNARCHIVED":
                    onIsArchivedChanged();
                    return;
            }

            getDoses();

            if (action.equals("com.cliambrown.broadcast.DOSE_MOVED")) {
                int fromPosition = intent.getIntExtra("fromPosition", -1);
                int toPosition = intent.getIntExtra("toPosition", -1);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                mAdapter.notifyItemChanged(toPosition);
                return;
            }

            switch (action) {
                case "com.cliambrown.broadcast.DOSES_LOADED":
                    try {
                        List<Integer> doseIDs = intent.getIntegerArrayListExtra("doseIDs");
                        for (int doseID : Objects.requireNonNull(doseIDs)) {
                            for (int i=0; i<doses.size(); ++i) {
                                if (doses.get(i).getId() == doseID) {
                                    mAdapter.notifyItemInserted(i);
                                    break;
                                }
                            }
                        }
                        onMedTimesChanged();
                    } catch (Exception e) {
                        Log.w(MedActivity.class.getName(), e);
                    }
                    return;
                case "com.cliambrown.broadcast.DOSES_REMOVED":
                    try {
                        List<Integer> doseIDs = intent.getIntegerArrayListExtra("doseIDs");
                        for (int doseID : Objects.requireNonNull(doseIDs)) {
                            for (int i=0; i<doses.size(); ++i) {
                                if (doses.get(i).getId() == doseID) {
                                    doses.remove(i);
                                    mAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                        }
                        onMedTimesChanged();
                    } catch (Exception e) {
                        Log.w(MedActivity.class.getName(), e);
                    }
                    return;
                case "com.cliambrown.broadcast.DOSE_AND_OLDER_REMOVED":
                    onMedTimesChanged();
                    return;
            }

            int doseID = intent.getIntExtra("doseID", -1);

            switch (action) {
                case "com.cliambrown.broadcast.DOSE_ADDED":
                    for (int i = 0; i < doses.size(); ++i) {
                        if (doses.get(i).getId() == doseID) {
                            mAdapter.notifyItemInserted(i);
                            recyclerView.scrollToPosition(i);
                            break;
                        }
                    }
                    onMedTimesChanged();
                    return;
                case "com.cliambrown.broadcast.DOSE_EDITED":
                    for (int i = 0; i < doses.size(); ++i) {
                        if (doses.get(i).getId() == doseID) {
                            mAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    onMedTimesChanged();
                    return;
                case "com.cliambrown.broadcast.DOSE_REMOVED":
                    for (int i = 0; i < doses.size(); ++i) {
                        if (doses.get(i).getId() == doseID) {
                            doses.remove(i);
                            mAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    onMedTimesChanged();
                    return;
            }
        }
    }

    public void onDoseCountChanged() {
        if (ll_med_no_doses == null) return;
        if (doses.isEmpty()) {
            ll_med_no_doses.setVisibility(View.VISIBLE);
        } else {
            ll_med_no_doses.setVisibility(View.GONE);
            if (mAdapter != null) {
                mAdapter.notifyItemChanged(mAdapter.getItemCount() - 1, "dose_count_changed");
            }
        }
    }

    // Update Med values that change only when modified
    public void onMedInfoChanged() {
        if (med == null) return;
        tv_med_name.setText(med.getName());
        int textColor = Utils.getTextColor(med.getColor(), MedActivity.this);
        tv_med_name.setTextColor(textColor);
        tv_med_maxDoseInfo.setText(med.getMaxDoseInfoStr());
        btn_med_add_dose.setBackgroundColor(textColor);
        for (int i=0; i<doses.size(); ++i) {
            mAdapter.notifyItemChanged(i, "med_info_changed");
        }
    }

    // Update Med values that change over time
    public void onMedTimesChanged() {
        if (med == null) return;
        if (mAdapter == null) return;
        if (med.getIsInventoryTracked()) {
            if (med.getIsInventoryLow()) {
                tv_med_inventory.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
            } else {
                tv_med_inventory.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            tv_med_inventory.setVisibility(View.VISIBLE);
            tv_med_inventory.setText(getString(R.string.inventory, med.getInventoryStr()));
        } else {
            tv_med_inventory.setVisibility(View.GONE);
        }
        int colorAttrResId = R.attr.textColorPrimary;
        if (med.getActiveDoseCount() >= med.getMaxDose()) {
            colorAttrResId = R.attr.redText;
        }
        tv_med_takenInPast.setText(Utils.buildTakenInPastString(this, colorAttrResId, med.getActiveDoseCount(),
                med.getDoseHours()));
        if (!med.getShowDayDoseCount() || med.getDoseHours() == 24) {
            tv_med_takenInPastDay.setVisibility(View.GONE);
        } else {
            tv_med_takenInPastDay.setText(
                    getResources().getQuantityString(R.plurals.taken_in_past_days, 1, Utils.getStrFromDbl(med.getPastDayDoseCount()))
            );
            tv_med_takenInPastDay.setVisibility(View.VISIBLE);
        }
    }

    public void updateAllTimes() {
        onMedTimesChanged();
        for (int i=0; i<doses.size(); ++i) {
            doses.get(i).updateTimes(med);
            mAdapter.notifyItemChanged(i, "times_changed");
        }
    }

    private void startUpdateTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(MedActivity.this::updateAllTimes);
            }
        };
        timer.schedule(doAsynchronousTask, 60000, 60000); // once every minute
    }

    public void activateMultiSelect(int doseID) {
        if (isMultiSelectActive) return;
        isMultiSelectActive = true;
        optionsMenu.findItem(R.id.mi_med_edit).setVisible(false);
        optionsMenu.findItem(R.id.mi_med_archive).setVisible(false);
        optionsMenu.findItem(R.id.mi_med_select_all).setVisible(true);
        selectDose(doseID, true);
        int doseCount = doses.size();
        for (int i = 0; i < doseCount; i++) {
            mAdapter.notifyItemChanged(i, "multiselect_activated");
        }
        onBackPressedCallback.setEnabled(true);
    }

    protected void deactivateMultiSelect() {
        isMultiSelectActive = false;
        optionsMenu.findItem(R.id.mi_med_edit).setVisible(true);
        optionsMenu.findItem(R.id.mi_med_archive).setVisible(true);
        optionsMenu.findItem(R.id.mi_med_select_all).setVisible(false);
        selectedDoseIDs.clear();
        setTitle(getString(R.string.med));
        int doseCount = doses.size();
        for (int i = 0; i < doseCount; i++) {
            mAdapter.notifyItemChanged(i, "multiselect_deactivated");
        }
        onBackPressedCallback.setEnabled(false);
    }

    public void selectDose(int doseID, boolean isSelected) {
        if (isSelected) {
            selectedDoseIDs.add(doseID);
        } else {
            selectedDoseIDs.remove(doseID);
            if (selectedDoseIDs.isEmpty()) {
                deactivateMultiSelect();
                return;
            }
        }
        setTitle(this.getResources().getQuantityString(R.plurals.doses_selected, selectedDoseIDs.size(), selectedDoseIDs.size()));
    }

    protected void onIsArchivedChanged() {
        optionsMenu.findItem(R.id.mi_med_archive).setVisible(!med.isArchived());
        optionsMenu.findItem(R.id.mi_med_unarchive).setVisible(med.isArchived());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.med_menu, menu);
        optionsMenu = menu;
        onIsArchivedChanged();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            if (isMultiSelectActive) {
                deactivateMultiSelect();
                return true;
            }
            MedActivity.this.finish();
            return true;
        }
        if (itemID == R.id.mi_med_edit) {
            intent = new Intent(MedActivity.this, EditMedActivity.class);
            intent.putExtra("id", medID);
            startActivity(intent);
            return true;
        }
        if (itemID == R.id.mi_med_archive) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_archive_med)
                    .setTitle(R.string.archive)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        mApp.archiveMed(med, true);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            builder.show();
            return true;
        }
        if (itemID == R.id.mi_med_unarchive) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_unarchive_med)
                    .setTitle(R.string.unarchive)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        mApp.archiveMed(med, false);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            builder.show();
            return true;
        }
        if (itemID == R.id.mi_med_select_all) {
            if (!isMultiSelectActive) return true;
            for (int i = 0; i< doses.size(); i++) {
                int doseID = doses.get(i).getId();
                selectDose(doseID, true);
                mAdapter.notifyItemChanged(i, "dose_selected");
            }
        }
        if (itemID == R.id.mi_med_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (isMultiSelectActive) {
                if (selectedDoseIDs.isEmpty()) return false;
                builder.setMessage(getResources().getQuantityString(
                            R.plurals.dialog_delete_selected_doses,
                            selectedDoseIDs.size(),
                            selectedDoseIDs.size()
                        ))
                        .setTitle(R.string.delete_doses)
                        .setPositiveButton(R.string.yes, (dialog, id) -> {
                            mApp.removeDosesByIDs(med, selectedDoseIDs);
                            deactivateMultiSelect();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                builder.show();
                return true;
            }
            builder.setMessage(R.string.dialog_delete_med)
                    .setTitle(R.string.delete)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        mApp.deleteMedById(medID);
                        Intent homeIntent = new Intent(MedActivity.this, MainActivity.class);
                        startActivity(homeIntent);
                        MedActivity.this.finish();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}