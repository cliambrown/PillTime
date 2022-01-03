package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    PillTimeApplication mApp;

    List<Med> meds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApp = (PillTimeApplication) this.getApplication();
        meds = mApp.getMeds();

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
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // TODO replace with some kind of queue system? maybe in PillTimeApplication
        mAdapter.notifyDataSetChanged();
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