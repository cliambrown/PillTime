package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditDoseActivity extends AppCompatActivity {

    Button saveButton;
    EditText editTextCount, editTextTakenAtTime, editTextTakenAtDate;
    DbHelper dbHelper;
    PillTimeApplication pillTimeApplication = (PillTimeApplication) this.getApplication();
    int medID, doseID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dose);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        saveButton = findViewById(R.id.saveButton);
        editTextCount = findViewById(R.id.editTextCount);
        editTextTakenAtTime = findViewById(R.id.editTextTakenAtTime);
        editTextTakenAtDate = findViewById(R.id.editTextTakenAtDate);

        dbHelper = new DbHelper(EditDoseActivity.this);
        Intent intent = getIntent();
        medID = intent.getIntExtra("med_id", -1);
        doseID = intent.getIntExtra("dose_id", -1);
        Med med = PillTimeApplication.getMed(medID);
        Dose dose = null;

        if (med == null) {
            // TODO send back
        }

        if (dose == null) {
            int now = (int) (System.currentTimeMillis() / 1000L);
            dose = new Dose(doseID, medID, med.getMaxDose(), now, EditDoseActivity.this);
            setTitle(getString(R.string.new_dose) + " — " + med.getName());
        } else {
            // TODO get dose
            setTitle(getString(R.string.edit) + " " + getString(R.string.dose));
        }

        editTextCount.setText(String.valueOf(dose.getCount()));
        editTextTakenAtTime.setText(dose.getTimeString());
        editTextTakenAtDate.setText(dose.getDateString());

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                float count;
                String takenAtTime;
                String takenAtDate;
                Calendar cal;

                try {
                    count = Float.parseFloat(editTextCount.getText().toString());
                    takenAtTime = editTextTakenAtTime.getText().toString();
                    takenAtDate = editTextTakenAtDate.getText().toString();
                    cal = Calendar.getInstance();
                } catch (Exception e) {
                    Toast.makeText(EditDoseActivity.this, "error saving dose - invalid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd kk:mm", Locale.ENGLISH);
                    cal.setTime(dateFormat.parse(takenAtDate + " " + takenAtTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                int unixTime = (int) (cal.getTimeInMillis() / 1000L);
                Dose dose = new Dose(doseID, medID, count, unixTime, EditDoseActivity.this);

                if (doseID > -1) {
//                    boolean update = dbHelper.updateMed(med);
//                    if (update) {
//                        PillTimeApplication.setMed(med);
//                        Toast.makeText(EditMedActivity.this, meds.toString(), Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(EditMedActivity.this, "error saving med", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
                } else {
                    int insertID = dbHelper.addDose(dose);
                    if (insertID >= 0) {
                        dose.setId(insertID);
                        med.addDose(dose);
                    } else {
                        Toast.makeText(EditDoseActivity.this, "error saving dose", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Toast.makeText(EditDoseActivity.this, "dose saved", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(EditDoseActivity.this, MedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
                EditDoseActivity.this.finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent;
                intent = new Intent(EditDoseActivity.this, EditMedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}