package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.cliambrown.pilltime.meds.EditMedActivity;
import com.cliambrown.pilltime.settings.SettingsActivity;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.meds.MedsRecycleViewAdapter;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedsRecycleViewAdapter mAdapter;
    LinearLayout ll_main_no_meds;
    Button btn_main_no_meds;

    PillTimeApplication mApp;
    Timer timer;
    static SharedPreferences prefs;

    List<Med> meds;

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ll_main_no_meds = findViewById(R.id.ll_main_no_meds);
        btn_main_no_meds = findViewById(R.id.btn_main_no_meds);

        mApp = (PillTimeApplication) this.getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        meds = mApp.getMeds();
        onUpdateMeds();

        recyclerView = findViewById(R.id.rv_main_meds);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MedsRecycleViewAdapter(meds, this, mApp);
        recyclerView.setAdapter(mAdapter);

        SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.swiperefresh_main);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onRefresh() {
                mApp.loadMeds();
                mAdapter.notifyDataSetChanged();
                onUpdateMeds();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        BroadcastReceiver br = new MainBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cliambrown.broadcast.DB_CLEARED");
        filter.addAction("com.cliambrown.broadcast.MED_ADDED");
        filter.addAction("com.cliambrown.broadcast.MED_EDITED");
        filter.addAction("com.cliambrown.broadcast.MED_REMOVED");
        filter.addAction("com.cliambrown.broadcast.MED_MOVED");
        filter.addAction("com.cliambrown.broadcast.DOSE_ADDED");
        filter.addAction("com.cliambrown.broadcast.DOSE_EDITED");
        filter.addAction("com.cliambrown.broadcast.DOSE_REMOVED");
        filter.addAction("com.cliambrown.broadcast.DOSES_REMOVED");

        // As of Android 14, registerReceiver now requires a flag (RECEIVER_EXPORTED or _NOT_).
        // No need to receive broadcasts from other apps, so this should be RECEIVER_NOT_EXPORTED.
        // HOWEVER the BR does not receive broadcasts unless RECEIVER_EXPORTED is used (??).
        // Although not ideal, using RECEIVER_EXPORTED here does not present any obvious risks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(br, filter, RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(br, filter);
        }

        btn_main_no_meds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditMedActivity.class);
                startActivity(intent);
            }
        });
    }

    public class MainBroadcastReceiver extends BroadcastReceiver {
        @SuppressWarnings("UnnecessaryReturnStatement")
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter == null) return;
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals("com.cliambrown.broadcast.DB_CLEARED")) {
                mAdapter.notifyDataSetChanged();
                onUpdateMeds();
            }
            int medID = intent.getIntExtra("medID", -1);
            if (action.equals("com.cliambrown.broadcast.MED_ADDED")) {
                for (int i=0; i<meds.size(); ++i) {
                    if (meds.get(i).getId() == medID) {
                        mAdapter.notifyItemInserted(i);
                        recyclerView.scrollToPosition(i);
                        onUpdateMeds();
                        return;
                    }
                }
                return;
            }
            if (action.equals("com.cliambrown.broadcast.MED_EDITED")) {
                for (int i=0; i<meds.size(); ++i) {
                    if (meds.get(i).getId() == medID) {
                        mAdapter.notifyItemChanged(i, "update_info");
                        return;
                    }
                }
                return;
            }
            if (action.equals("com.cliambrown.broadcast.MED_REMOVED")) {
                for (int i=0; i<meds.size(); ++i) {
                    if (meds.get(i).getId() == medID) {
                        mAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
                onUpdateMeds();
                return;
            }
            if (action.equals("com.cliambrown.broadcast.MED_MOVED")) {
                int fromPosition = intent.getIntExtra("fromPosition", -1);
                int toPosition = intent.getIntExtra("toPosition", -1);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                return;
            }
            if (action.equals("com.cliambrown.broadcast.DOSE_ADDED") ||
                    action.equals("com.cliambrown.broadcast.DOSES_ADDED") ||
                    action.equals("com.cliambrown.broadcast.DOSE_EDITED") ||
                    action.equals("com.cliambrown.broadcast.DOSE_REMOVED") ||
                    action.equals("com.cliambrown.broadcast.DOSES_REMOVED")
            ) {
                for (int i=0; i<meds.size(); ++i) {
                    if (meds.get(i).getId() == medID) {
                        mAdapter.notifyItemChanged(i, "update_times");
                        return;
                    }
                }
                return;
            }
        }
    }

    public void onUpdateMeds() {
        if (ll_main_no_meds == null) return;
        if (!meds.isEmpty()) {
            ll_main_no_meds.setVisibility(View.GONE);
        } else {
            ll_main_no_meds.setVisibility(View.VISIBLE);
        }
    }

    public void updateTimes() {
        if (mAdapter == null) return;
        for (int i=0; i<meds.size(); ++i) {
            mAdapter.notifyItemChanged(i, "update_times");
        }
    }

    private void startUpdateTimer() {
        final Handler handler = new Handler();
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        updateTimes();
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 60000, 60000); // once every minute
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    public static class DevDecreeDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dev_decree_title)
                    .setMessage(R.string.dev_decree_notice)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("show_dev_decree_dialog", false);
                            editor.apply();
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent browserIntent = new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://f-droid.org/en/2025/09/29/google-developer-registration-decree.html")
                            );
                            startActivity(browserIntent);
                        }
                    });
            return builder.create();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateTimes();
        startUpdateTimer();
        boolean showDevDecreeDialog = prefs.getBoolean("show_dev_decree_dialog", true);
        if (showDevDecreeDialog) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            new DevDecreeDialogFragment().show(fragmentManager, "DEV_DECREE_DIALOG");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            MainActivity.this.finish();
            return true;
        }
        if (itemID == R.id.mi_main_add) {
            intent = new Intent(MainActivity.this, EditMedActivity.class);
            startActivity(intent);
            return true;
        }
        if (itemID == R.id.mi_main_settings) {
            intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}