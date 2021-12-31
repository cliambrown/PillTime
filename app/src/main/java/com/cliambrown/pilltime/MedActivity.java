package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MedActivity extends AppCompatActivity {

    TextView tv_med_doseInfo;
    FloatingActionButton btn_med_add;

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
        btn_med_add = findViewById(R.id.btn_med_add);

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);

        if (med != null) {
            setTitle(med.getName());
            tv_med_doseInfo.setText(med.getDoseInfo());
        } else {
            MedActivity.this.finish();
        }

        recyclerView = findViewById(R.id.rv_med_doses);
        recyclerView.setHasFixedSize(true);
        registerForContextMenu(recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DosesRecycleViewAdapter(med.getDoses(), this, mApp);
        recyclerView.setAdapter(mAdapter);

        btn_med_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MedActivity.this, EditDoseActivity.class);
                intent.putExtra("med_id", medID);
                startActivity(intent);
            }
        });
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
                intent = new Intent(MedActivity.this, MainActivity.class);
                startActivity(intent);
                this.finish();
                return true;
            case R.id.mi_med_edit:
                intent = new Intent(MedActivity.this, EditMedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
                return true;
            case R.id.mi_med_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(MedActivity.this);
                builder.setMessage(R.string.dialog_delete_item)
                        .setTitle(R.string.delete)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MedActivity.this.mApp.removeMedById(medID);
                                Intent intent = new Intent(MedActivity.this, MainActivity.class);
                                startActivity(intent);
                                MedActivity.this.finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}