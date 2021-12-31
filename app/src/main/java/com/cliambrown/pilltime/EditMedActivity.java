package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditMedActivity extends AppCompatActivity {

    Button saveButton;
    EditText editTextMedName, editTextMaxDose, editTextDoseHours;
    DbHelper dbHelper;
    PillTimeApplication pillTimeApplication = (PillTimeApplication) this.getApplication();
    int medID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        saveButton = findViewById(R.id.saveButton);
        editTextMedName = findViewById(R.id.editTextMedName);
        editTextMaxDose = findViewById(R.id.editTextMaxDose);
        editTextDoseHours = findViewById(R.id.editTextDoseHours);

        dbHelper = new DbHelper(EditMedActivity.this);
        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        Med med = PillTimeApplication.getMed(medID);

        if (med != null) {
            editTextMedName.setText(med.getName());
            editTextMaxDose.setText(String.valueOf(med.getMaxDose()));
            editTextDoseHours.setText(String.valueOf(med.getDoseHours()));
            setTitle(getString(R.string.edit) + " " + med.getName());
        } else {
            setTitle(getString(R.string.new_med));
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Med med;
                DbHelper dbHelper = new DbHelper(EditMedActivity.this);
                String medName;
                int maxDose;
                int doseHours;

                try {
                    medName = editTextMedName.getText().toString();
                    maxDose = Integer.parseInt(editTextMaxDose.getText().toString());
                    doseHours = Integer.parseInt(editTextDoseHours.getText().toString());
                    med = new Med(medID, medName, maxDose, doseHours, EditMedActivity.this);
                } catch (Exception e) {
                    Toast.makeText(EditMedActivity.this, "error saving med - invalid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (medID > -1) {
                    boolean update = dbHelper.updateMed(med);
                    if (update) {
                        PillTimeApplication.setMed(med);
                    } else {
                        Toast.makeText(EditMedActivity.this, "error saving med", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    int insertID = dbHelper.insertMed(med);
                    if (insertID >= 0) {
                        med.setId(insertID);
                        Log.d("clb-debug", insertID + "");
                        Log.d("clb-debug", med.getId() + "");
                        PillTimeApplication.addMed(med);
                    } else {
                        Toast.makeText(EditMedActivity.this, "error saving med", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Toast.makeText(EditMedActivity.this, "med saved", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(EditMedActivity.this, MedActivity.class);
                intent.putExtra("id", med.getId());
                startActivity(intent);
                EditMedActivity.this.finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent;
                if (medID >= 0) {
                    intent = new Intent(EditMedActivity.this, MedActivity.class);
                    intent.putExtra("id", medID);
                } else {
                    intent = new Intent(EditMedActivity.this, MainActivity.class);
                }
                startActivity(intent);
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}