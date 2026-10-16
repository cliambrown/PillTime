package com.cliambrown.pilltime;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.help.HelpActivity;
import com.cliambrown.pilltime.meds.EditMedActivity;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.meds.MedsRecycleViewAdapter;
import com.cliambrown.pilltime.settings.SettingsActivity;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedsRecycleViewAdapter mAdapter;
    LinearLayout ll_main_no_meds;
    LinearLayout ll_main_no_archived_meds;
    ExtendedFloatingActionButton btn_main_add_med;

    PillTimeApplication mApp;
    Timer timer;
    static SharedPreferences prefs;

    boolean isArchives = false;
    OnBackPressedCallback onBackPressedCallback;

    List<Med> meds = new ArrayList<>();
    private Menu optionsMenu;

    protected void getMeds() {
        meds.clear();
        for (Med med: mApp.getMeds()) {
            if (med.isArchived() == isArchives) meds.add(med);
        }
        toggleNoMedsMessages();
    }

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ll_main_no_meds = findViewById(R.id.ll_main_no_meds);
        ll_main_no_archived_meds = findViewById(R.id.ll_main_no_archived_meds);
        btn_main_add_med = findViewById(R.id.btn_main_add_med);

        mApp = (PillTimeApplication) this.getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        getMeds();

        recyclerView = findViewById(R.id.rv_main_meds);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MedsRecycleViewAdapter(meds, this, mApp, isArchives);
        recyclerView.setAdapter(mAdapter);
        float offsetPx = getResources().getDimension(R.dimen.bottom_offset_dp);
        Utils.BottomOffsetDecoration bottomOffsetDecoration = new Utils.BottomOffsetDecoration((int) offsetPx);
        recyclerView.addItemDecoration(bottomOffsetDecoration);

        BroadcastReceiver br = new MainBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cliambrown.broadcast.DB_CLEARED");
        filter.addAction("com.cliambrown.broadcast.DATA_IMPORTED");
        filter.addAction("com.cliambrown.broadcast.MED_ADDED");
        filter.addAction("com.cliambrown.broadcast.MED_EDITED");
        filter.addAction("com.cliambrown.broadcast.MED_MOVED");
        filter.addAction("com.cliambrown.broadcast.MED_ARCHIVED_OR_UNARCHIVED");
        filter.addAction("com.cliambrown.broadcast.MED_REMOVED");
        filter.addAction("com.cliambrown.broadcast.DOSE_ADDED");
        filter.addAction("com.cliambrown.broadcast.DOSE_EDITED");
        filter.addAction("com.cliambrown.broadcast.DOSE_REMOVED");
        filter.addAction("com.cliambrown.broadcast.DOSES_REMOVED");

        // As of Android 14, registerReceiver now requires a flag (RECEIVER_EXPORTED or _NOT_).
        // No need to receive broadcasts from other apps, so this should be RECEIVER_NOT_EXPORTED.
        // HOWEVER, the BR does not receive broadcasts unless RECEIVER_EXPORTED is used (??).
        // Although not ideal, using RECEIVER_EXPORTED here does not present any obvious risks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(br, filter, RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(br, filter);
        }

        btn_main_add_med.setOnClickListener(view -> {
            Intent addMedIntent = new Intent(MainActivity.this, EditMedActivity.class);
            startActivity(addMedIntent);
        });

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                toggleArchives(false);
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    protected int getMedPosition(int medID) {
        for (int i = 0; i < meds.size(); ++i) {
            if (meds.get(i).getId() == medID) {
                return i;
            }
        }
        return -1;
    }

    public class MainBroadcastReceiver extends BroadcastReceiver {

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter == null) return;
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case "com.cliambrown.broadcast.DB_CLEARED":
                case "com.cliambrown.broadcast.DATA_IMPORTED":
                    getMeds();
                    mAdapter.notifyDataSetChanged();
                    return;
            }

            int medID = intent.getIntExtra("medID", -1);
            int fromPosition = getMedPosition(medID);

            // PillTimeApplication.java sends dose change broadcasts first, then sorts meds
            // and sends second broadcast if med has moved
            switch (action) {
                case "com.cliambrown.broadcast.DOSE_ADDED":
                case "com.cliambrown.broadcast.DOSE_EDITED":
                case "com.cliambrown.broadcast.DOSE_REMOVED":
                case "com.cliambrown.broadcast.DOSES_REMOVED":
                    if (fromPosition >= 0) {
                        mAdapter.notifyItemChanged(fromPosition, "doses_edited");
                    }
                    return;
            }

            getMeds();
            int toPosition = getMedPosition(medID);

            if (fromPosition >= 0 && toPosition == -1) {
                mAdapter.notifyItemRemoved(fromPosition);
            } else if (fromPosition == -1 && toPosition >= 0) {
                mAdapter.notifyItemInserted(toPosition);
                recyclerView.scrollToPosition(toPosition);
            } else if (fromPosition != toPosition) {
                mAdapter.notifyItemChanged(fromPosition);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                recyclerView.scrollToPosition(toPosition);
            }

            if (toPosition >= 0 && action.equals("com.cliambrown.broadcast.MED_EDITED")) {
                mAdapter.notifyItemChanged(toPosition, "med_edited");
                recyclerView.scrollToPosition(toPosition);
            }
        }
    }

    public void toggleNoMedsMessages() {
        if (ll_main_no_meds == null) return;
        if (meds.isEmpty()) {
            if (isArchives) {
                ll_main_no_archived_meds.setVisibility(View.VISIBLE);
                ll_main_no_meds.setVisibility(View.GONE);
            } else {
                ll_main_no_meds.setVisibility(View.VISIBLE);
                ll_main_no_archived_meds.setVisibility(View.GONE);
            }
        } else {
            ll_main_no_meds.setVisibility(View.GONE);
            ll_main_no_archived_meds.setVisibility(View.GONE);
        }
    }

    public void updateTimes() {
        if (mAdapter == null) return;
        for (int i=0; i<meds.size(); ++i) {
            mAdapter.notifyItemChanged(i, "update_times");
        }
    }

    private void startUpdateTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> updateTimes());
            }
        };
        timer.schedule(doAsynchronousTask, 60000, 60000); // once every minute
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    public static class welcomeDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.welcome_dialog_title)
                    .setMessage(R.string.welcome_dialog_content)
                    .setNegativeButton(R.string.close, (dialog, id) -> {
                        dialog.cancel();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("show_v22_dialog", false);
                        editor.apply();
                    });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setPositiveButton(R.string.check_permissions, (dialog, id) -> {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    });
            }
            return builder.create();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateTimes();
        mAdapter.notifyDataSetChanged();
        startUpdateTimer();

        boolean showWelcomeDialog = prefs.getBoolean("show_v22_dialog", true);
        if (showWelcomeDialog) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            new welcomeDialogFragment().show(fragmentManager, "WELCOME_DIALOG");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        optionsMenu = menu;
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void toggleArchives(boolean newIsArchives) {
        isArchives = newIsArchives;
        optionsMenu.findItem(R.id.mi_main_archives).setVisible(!isArchives);
        if (isArchives) {
            setTitle(R.string.archives);
            btn_main_add_med.setVisibility(View.GONE);
            onBackPressedCallback.setEnabled(true);
        } else {
            setTitle(R.string.app_name);
            btn_main_add_med.setVisibility(View.VISIBLE);
            onBackPressedCallback.setEnabled(false);
        }
        // Show back button on archives page
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(isArchives);
        getMeds();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            if (isArchives) {
                toggleArchives(false);
            } else {
                MainActivity.this.finish();
            }
            return true;
        }
        if (itemID == R.id.mi_main_archives) {
            toggleArchives(true);
            return true;
        }
        if (itemID == R.id.mi_main_help) {
            intent = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(intent);
        }
        if (itemID == R.id.mi_main_settings) {
            intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}