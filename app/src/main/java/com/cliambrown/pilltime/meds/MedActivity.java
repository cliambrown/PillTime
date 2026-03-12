package com.cliambrown.pilltime.meds;

import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.doses.DosesRecycleViewAdapter;
import com.cliambrown.pilltime.doses.EditDoseActivity;
import com.cliambrown.pilltime.settings.SettingsActivity;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MedActivity extends AppCompatActivity {

    TextView tv_med_name;
    TextView tv_med_maxDoseInfo;
    TextView tv_med_takenInPast;

    TextView tv_med_inventory;
    LinearLayout ll_med_no_doses;
    ExtendedFloatingActionButton btn_med_add_dose;

    private RecyclerView recyclerView;
    private DosesRecycleViewAdapter mAdapter;

    PillTimeApplication mApp;
    Timer timer;
    int medID;
    Med med;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        setTitle(getString(R.string.dose_history));

        updateInfo();
        updateTimes();
        onUpdateDoses();

        recyclerView = findViewById(R.id.rv_med_doses);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DosesRecycleViewAdapter(med, med.getDoses(), this, mApp);
        recyclerView.setAdapter(mAdapter);
        float offsetPx = getResources().getDimension(R.dimen.bottom_offset_dp);
        Utils.BottomOffsetDecoration bottomOffsetDecoration = new Utils.BottomOffsetDecoration((int) offsetPx);
        recyclerView.addItemDecoration(bottomOffsetDecoration);

        BroadcastReceiver br = new MedActivity.MedBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cliambrown.broadcast.DB_CLEARED");
        filter.addAction("com.cliambrown.broadcast.MED_EDITED");
        filter.addAction("com.cliambrown.broadcast.DOSES_ADDED");
        filter.addAction("com.cliambrown.broadcast.DOSES_REMOVED");
        filter.addAction("com.cliambrown.broadcast.DOSE_ADDED");
        filter.addAction("com.cliambrown.broadcast.DOSE_EDITED");
        filter.addAction("com.cliambrown.broadcast.DOSE_MOVED");
        filter.addAction("com.cliambrown.broadcast.DOSE_REMOVED");

        // As of Android 14, registerReceiver now requires a flag (RECEIVER_EXPORTED or _NOT_).
        // No need to receive broadcasts from other apps, so this should be RECEIVER_NOT_EXPORTED.
        // HOWEVER, the BR does not receive broadcasts unless RECEIVER_EXPORTED is used (??).
        // Although not ideal, using RECEIVER_EXPORTED here does not present any obvious risks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(br, filter, RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(br, filter);
        }

        btn_med_add_dose.setOnClickListener(view -> {
            Intent intent1 = new Intent(MedActivity.this, EditDoseActivity.class);
            intent1.putExtra("medID", medID);
            startActivity(intent1);
        });
    }

    public class MedBroadcastReceiver extends BroadcastReceiver {
        @SuppressWarnings({"unchecked", "UnnecessaryReturnStatement"})
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action == null) return;
            if (action.equals("com.cliambrown.broadcast.DB_CLEARED")) {
                MedActivity.this.finish();
                return;
            }
            int intentMedID = intent.getIntExtra("medID", -1);
            if (intentMedID != medID) return;
            if (action.equals("com.cliambrown.broadcast.MED_EDITED")) {
                updateInfo();
                updateTimes();
                return;
            }

            if (mAdapter == null) return;

            if (action.equals("com.cliambrown.broadcast.DOSE_MOVED")) {
                int fromPosition = intent.getIntExtra("fromPosition", -1);
                int toPosition = intent.getIntExtra("toPosition", -1);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                mAdapter.notifyItemChanged(toPosition);
                return;
            }

            List<Dose> doses = med.getDoses();

            if (action.equals("com.cliambrown.broadcast.DOSES_ADDED")) {
                try {
                    List<Integer> doseIDs = (List<Integer>) intent.getSerializableExtra("doseIDs");
                    for (int doseID : Objects.requireNonNull(doseIDs)) {
                        for (int i=0; i<doses.size(); ++i) {
                            if (doses.get(i).getId() == doseID) {
                                mAdapter.notifyItemInserted(i);
                                break;
                            }
                        }
                    }
                    updateTimes();
                    onUpdateDoses();
                } catch (Exception e) {
                    Log.w(MedActivity.class.getName(), e);
                }
                return;
            }

            if (action.equals("com.cliambrown.broadcast.DOSES_REMOVED")) {
                updateTimes();
                onUpdateDoses();
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
                    updateTimes();
                    onUpdateDoses();
                    return;
                case "com.cliambrown.broadcast.DOSE_EDITED":
                    for (int i = 0; i < doses.size(); ++i) {
                        if (doses.get(i).getId() == doseID) {
                            mAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    updateTimes();
                    return;
                case "com.cliambrown.broadcast.DOSE_REMOVED":
                    for (int i = 0; i < doses.size(); ++i) {
                        if (doses.get(i).getId() == doseID) {
                            mAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    updateTimes();
                    onUpdateDoses();
                    return;
            }
        }
    }

    public void onUpdateDoses() {
        if (ll_med_no_doses == null) return;
        if (!med.getDoses().isEmpty()) {
            ll_med_no_doses.setVisibility(View.GONE);
            if (mAdapter != null) {
                mAdapter.notifyItemChanged(0, "update_show_more_btn");
            }
        } else {
            ll_med_no_doses.setVisibility(View.VISIBLE);
        }
    }

    // Update Med values that change only when modified
    public void updateInfo() {
        if (med == null) return;
        tv_med_name.setText(med.getName());
        String colorName = med.getColor();
        int attrResourceID = Utils.getResourceIdentifier(MedActivity.this, colorName + "Text", "attr");
        int textColor = ThemeHelper.getThemeAttr(attrResourceID, MedActivity.this);
        tv_med_name.setTextColor(textColor);
        tv_med_maxDoseInfo.setText(med.getMaxDoseInfoStr());
    }

    // Update Med values that change over time
    public void updateTimes() {
        if (med == null) return;
        med.updateTimes();
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
        for (int i=0; i<med.getDoses().size(); ++i) {
            mAdapter.notifyItemChanged(i, "update_times");
        }
    }

    private void startUpdateTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(MedActivity.this::updateTimes);
            }
        };
        timer.schedule(doAsynchronousTask, 60000, 60000); // once every minute
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateTimes();
        onUpdateDoses();
        startUpdateTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.med_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            MedActivity.this.finish();
            return true;
        }
        if (itemID == R.id.mi_med_edit) {
            intent = new Intent(MedActivity.this, EditMedActivity.class);
            intent.putExtra("id", medID);
            startActivity(intent);
            return true;
        }
        if (itemID == R.id.mi_med_settings) {
            intent = new Intent(MedActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}