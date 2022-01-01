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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditDoseActivity extends MainMenuActivity {

    Button btn_editDose_save;
    EditText et_editDose_count, et_editDose_takenAtTime, et_editDose_takenAtDate;
    PillTimeApplication mApp;
    int medID, doseID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dose);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        btn_editDose_save = findViewById(R.id.btn_editDose_save);
        et_editDose_count = findViewById(R.id.et_editDose_count);
        et_editDose_takenAtTime = findViewById(R.id.et_editDose_takenAtTime);
        et_editDose_takenAtDate = findViewById(R.id.et_editDose_takenAtDate);

        Intent intent = getIntent();
        medID = intent.getIntExtra("med_id", -1);
        doseID = intent.getIntExtra("dose_id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);
        Dose dose;

        if (med == null) {
            EditDoseActivity.this.finish();
        }

        if (doseID > -1) {
            dose = med.getDoseById(doseID);
            if (dose == null) {
                EditDoseActivity.this.finish();
            }
            setTitle(getString(R.string.edit) + " " + getString(R.string.dose));
        } else {
            long now = System.currentTimeMillis() / 1000L;
            dose = new Dose(doseID, medID, med.getMaxDose(), now, EditDoseActivity.this);
            setTitle(getString(R.string.new_dose) + " — " + med.getName());
        }

        NumberFormat nf = new DecimalFormat("##.###");
        et_editDose_count.setText(nf.format(dose.getCount()));
        et_editDose_takenAtTime.setText(dose.getTimeString());
        et_editDose_takenAtDate.setText(dose.getDateString());

        btn_editDose_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                double count;
                String takenAtTime;
                String takenAtDate;
                Calendar cal = Calendar.getInstance();

                try {
                    count = Double.parseDouble(et_editDose_count.getText().toString());
                    takenAtTime = et_editDose_takenAtTime.getText().toString();
                    takenAtDate = et_editDose_takenAtDate.getText().toString();
                } catch (Exception e) {
                    Toast.makeText(EditDoseActivity.this, "Error saving dose: invalid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd kk:mm", Locale.ENGLISH);
                    cal.setTime(dateFormat.parse(takenAtDate + " " + takenAtTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                long unixTime = cal.getTimeInMillis() / 1000L;
                Dose dose = new Dose(doseID, medID, count, unixTime, EditDoseActivity.this);

                if (doseID > -1) {
                    boolean setted = mApp.setDose(med, dose);
                    if (!setted) return;
                } else {
                    boolean added = mApp.addDose(med, dose);
                    if (!added) return;
                }

                Toast.makeText(EditDoseActivity.this, "Dose saved", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(EditDoseActivity.this, MedActivity.class);
                intent.putExtra("id", medID);
                startActivity(intent);
                EditDoseActivity.this.finish();
            }
        });
    }
}