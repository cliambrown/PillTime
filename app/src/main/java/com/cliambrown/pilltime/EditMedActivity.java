package com.cliambrown.pilltime;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditMedActivity extends SimpleMenuActivity {

    Button btn_editMed_save;
    EditText et_editMed_name, et_editMed_maxDose, et_editMed_doseHours;
    PillTimeApplication mApp;
    int medID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        btn_editMed_save = findViewById(R.id.btn_editMed_save);
        et_editMed_name = findViewById(R.id.et_editMed_name);
        et_editMed_maxDose = findViewById(R.id.et_editMed_maxDose);
        et_editMed_doseHours = findViewById(R.id.et_editMed_doseHours);

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);

        if (med != null) {
            et_editMed_name.setText(med.getName());
            et_editMed_maxDose.setText(String.valueOf(med.getMaxDose()));
            et_editMed_doseHours.setText(String.valueOf(med.getDoseHours()));
            setTitle(getString(R.string.edit) + " " + med.getName());
        } else {
            setTitle(getString(R.string.new_med));
        }

        btn_editMed_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Med med;
                String medName;
                int maxDose;
                int doseHours;

                try {
                    medName = et_editMed_name.getText().toString();
                    maxDose = Integer.parseInt(et_editMed_maxDose.getText().toString());
                    doseHours = Integer.parseInt(et_editMed_doseHours.getText().toString());
                    med = new Med(medID, medName, maxDose, doseHours, EditMedActivity.this);
                } catch (Exception e) {
                    Toast.makeText(EditMedActivity.this, "Error saving med: invalid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (medID > -1) {
                    boolean edited = mApp.setMed(med);
                    if (!edited) return;
                } else {
                    boolean added = mApp.addMed(med);
                    if (!added) return;
                }

                Toast.makeText(EditMedActivity.this, "Med saved", Toast.LENGTH_SHORT).show();

                EditMedActivity.this.finish();
            }
        });
    }
}