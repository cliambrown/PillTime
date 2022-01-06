package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

public class EditMedActivity extends SimpleMenuActivity {

    ConstraintLayout cl_editMed_parent;
    EditText et_editMed_name;
    NumberPicker np_editMed_maxDose;
    NumberPicker np_editMed_doseHours;
    Flow flow_editMed_colors;
    Button btn_editMed_save;
    PillTimeApplication mApp;
    int medID;
    String[] colors;
    String selectedColor;
    List<ImageButton> colorButtons;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThemePillTime);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        cl_editMed_parent = findViewById(R.id.cl_editMed_parent);
        et_editMed_name = findViewById(R.id.et_editMed_name);
        flow_editMed_colors = findViewById(R.id.flow_editMed_colors);
        btn_editMed_save = findViewById(R.id.btn_editMed_save);

        np_editMed_maxDose = findViewById(R.id.np_editMed_maxDose);
        np_editMed_maxDose.setMinValue(1);
        np_editMed_maxDose.setMaxValue(100);
        np_editMed_maxDose.setWrapSelectorWheel(false);

        np_editMed_doseHours = findViewById(R.id.np_editMed_doseHours);
        np_editMed_doseHours.setMinValue(1);
        np_editMed_doseHours.setMaxValue(100);
        np_editMed_doseHours.setWrapSelectorWheel(false);

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);

        colors = getResources().getStringArray(R.array.color_options);

        if (med != null) {
            et_editMed_name.setText(med.getName());
            np_editMed_maxDose.setValue(med.getMaxDose());
            np_editMed_doseHours.setValue(med.getDoseHours());
            setTitle(getString(R.string.edit) + " " + med.getName());
            selectedColor = med.getColor();
        } else {
            int rnd = new Random().nextInt(colors.length);
            selectedColor = colors[rnd];
            setTitle(getString(R.string.new_med));
        }

        colorButtons = new ArrayList<ImageButton>();
        ImageButton imageButton;

        int drawableID;
        int dp48 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48,
                getResources().getDisplayMetrics()
        );
        int btnText = ThemeProvider.getThemeAttr(R.attr.buttonText, this);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(dp48, dp48);

        for (String colorName : colors) {
            imageButton = new ImageButton(this);
            imageButton.setId(View.generateViewId());
            imageButton.setLayoutParams(params);
            imageButton.setColorFilter(btnText);
            setColorBtnState(imageButton, selectedColor.equals(colorName));
            drawableID = Utils.getResourceIdentifier(this, "round_button_" + colorName, "drawable");
            imageButton.setBackgroundResource(drawableID);
            imageButton.setFocusable(true);
            colorButtons.add(imageButton);

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (ImageButton listButton : colorButtons) {
                        setColorBtnState(listButton, false);
                    }
                    setColorBtnState((ImageButton) view, true);
                    selectedColor = colorName;
                }
            });

            cl_editMed_parent.addView(imageButton);
            flow_editMed_colors.addView(imageButton);
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
                    maxDose = np_editMed_maxDose.getValue();
                    doseHours = np_editMed_doseHours.getValue();
                    med = new Med(medID, medName, maxDose, doseHours, selectedColor, EditMedActivity.this);
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

    private void setColorBtnState(ImageButton btn, boolean isSelected) {
        if (isSelected) {
            btn.setImageResource(R.drawable.ic_baseline_check_24);
        } else {
            btn.setImageResource(0);
        }
    }
}