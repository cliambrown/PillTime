package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedsRecycleViewAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    LinearLayout ll_main_no_meds;
    Button btn_main_no_meds;

    PillTimeApplication mApp;
    Timer timer;

    List<Med> meds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThemePillTime);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ll_main_no_meds = findViewById(R.id.ll_main_no_meds);
        btn_main_no_meds = findViewById(R.id.btn_main_no_meds);

        mApp = (PillTimeApplication) this.getApplication();
        meds = mApp.getMeds();
        onUpdateMeds();

        recyclerView = findViewById(R.id.rv_main_meds);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MedsRecycleViewAdapter(meds, this, mApp);
        recyclerView.setAdapter(mAdapter);

        SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh_main);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
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
        this.registerReceiver(br, filter);

        btn_main_no_meds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditMedActivity.class);
                startActivity(intent);
            }
        });
    }

    public class MainBroadcastReceiver extends BroadcastReceiver {
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
                    action.equals("com.cliambrown.broadcast.DOSE_REMOVED")
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
        if (meds.size() > 0) {
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

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateTimes();
        startUpdateTimer();
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
        switch (item.getItemId()) {
            case android.R.id.home:
                MainActivity.this.finish();
                return true;
            case R.id.mi_main_add:
                intent = new Intent(MainActivity.this, EditMedActivity.class);
                startActivity(intent);
                return true;
            case R.id.mi_main_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}