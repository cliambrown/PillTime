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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MedActivity extends AppCompatActivity {

    TextView textViewDoseInfo;
    FloatingActionButton addButton;
    Menu menu;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    DbHelper dbHelper;
    PillTimeApplication pillTimeApplication = (PillTimeApplication) this.getApplication();
    int medID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        textViewDoseInfo = findViewById(R.id.textViewDoseInfo);
        addButton = findViewById(R.id.addButton);

        dbHelper = new DbHelper(MedActivity.this);
        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        Med med = PillTimeApplication.getMed(medID);

        if (med != null) {
            setTitle(med.getName());
            textViewDoseInfo.setText(med.getDoseInfo());
        } else {
            MedActivity.this.finish();
        }

        recyclerView = findViewById(R.id.dosesList);
        recyclerView.setHasFixedSize(true);
        registerForContextMenu(recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new DosesRecycleViewAdapter(med.getDoses(), this);
        recyclerView.setAdapter(mAdapter);

        addButton.setOnClickListener(new View.OnClickListener() {
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
            case R.id.menuEdit:
                intent = new Intent(MedActivity.this, EditMedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
            case R.id.menuDelete:
                AlertDialog.Builder builder = new AlertDialog.Builder(MedActivity.this);
                builder.setMessage(R.string.dialog_delete_med)
                        .setTitle(R.string.delete)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                boolean deleted = dbHelper.deleteMedById(medID);
                                if (deleted) {
                                    PillTimeApplication pillTimeApplication = (PillTimeApplication) MedActivity.this.getApplication();
                                    pillTimeApplication.removeMedById(medID);
                                    Intent intent = new Intent(MedActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    MedActivity.this.finish();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
        }
        return super.onOptionsItemSelected(item);
    }
}