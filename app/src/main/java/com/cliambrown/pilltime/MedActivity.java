package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class MedActivity extends AppCompatActivity {

    TextView tv_med_doseInfo;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    PillTimeApplication mApp;
    int medID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tv_med_doseInfo = findViewById(R.id.tv_med_doseInfo);

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);

        if (med != null) {
            setTitle(getString(R.string.history) + ": " + med.getName());
            tv_med_doseInfo.setText(med.getMaxDoseInfo());
        } else {
            MedActivity.this.finish();
        }

        recyclerView = findViewById(R.id.rv_med_doses);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DosesRecycleViewAdapter(med, med.getDoses(), this, mApp);
        recyclerView.setAdapter(mAdapter);
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
        inflater.inflate(R.menu.med_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                MedActivity.this.finish();
                return true;
            case R.id.mi_med_edit:
                intent = new Intent(MedActivity.this, EditMedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
                return true;
            case R.id.mi_med_add:
                intent = new Intent(MedActivity.this, EditDoseActivity.class);
                intent.putExtra("med_id", medID);
                startActivity(intent);
                return true;
            case R.id.mi_med_settings:
                intent = new Intent(MedActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}