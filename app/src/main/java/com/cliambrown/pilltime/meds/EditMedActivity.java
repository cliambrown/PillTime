package com.cliambrown.pilltime.meds;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.ParcelableSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.cliambrown.pilltime.MainActivity;
import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.settings.SettingsActivity;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class EditMedActivity extends AppCompatActivity {

    ConstraintLayout cl_editMed_parent;
    EditText et_editMed_name;
    NumberPicker np_editMed_maxDose;
    NumberPicker np_editMed_doseHoursDays;
    AppCompatSpinner sp_editMed_daysHours;
    NumberPicker np_editMed_defaultDoseCount;
    SwitchCompat switch_editMed_trackInventory;
    SwitchCompat switch_editMed_showDayDoseCount;
    LinearLayout ll_editMed_trackInventory;
    NumberPicker np_editMed_currentInventory;
    TextView tv_editMed_inventoryReportedAt;
    Flow flow_editMed_colors;
    ExtendedFloatingActionButton btn_editMed_save;
    PillTimeApplication mApp;
    int medID;
    String[] colors;
    String selectedColor;
    List<ImageButton> colorButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_med);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        medID = intent.getIntExtra("id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);

        cl_editMed_parent = findViewById(R.id.cl_editMed_parent);
        et_editMed_name = findViewById(R.id.et_editMed_name);
        flow_editMed_colors = findViewById(R.id.flow_editMed_colors);
        btn_editMed_save = findViewById(R.id.btn_editMed_save);

        np_editMed_maxDose = findViewById(R.id.np_editMed_maxDose);
        np_editMed_maxDose.setMinValue(1);
        np_editMed_maxDose.setMaxValue(1000);
        np_editMed_maxDose.setWrapSelectorWheel(false);

        np_editMed_defaultDoseCount = findViewById(R.id.np_editMed_defaultDoseCount);
        np_editMed_defaultDoseCount.setMinValue(1);
        np_editMed_defaultDoseCount.setMaxValue(1000);
        np_editMed_defaultDoseCount.setWrapSelectorWheel(false);

        np_editMed_doseHoursDays = findViewById(R.id.np_editMed_doseHoursDays);
        np_editMed_doseHoursDays.setMinValue(1);
        np_editMed_doseHoursDays.setMaxValue(100);
        np_editMed_doseHoursDays.setWrapSelectorWheel(false);

        sp_editMed_daysHours = findViewById(R.id.sp_editMed_daysHours);
        // Create an ArrayAdapter using the string array and a default spinner layout.
        String[] spinnerItems = {getString(R.string.hours), getString(R.string.days)};
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        sp_editMed_daysHours.setAdapter(adapter);

        switch_editMed_showDayDoseCount = findViewById(R.id.switch_editMed_showDayDoseCount);

        np_editMed_currentInventory = findViewById(R.id.np_editMed_currentInventory);
        np_editMed_currentInventory.setMinValue(0);
        np_editMed_currentInventory.setMaxValue(1000);
        np_editMed_currentInventory.setWrapSelectorWheel(false);

        tv_editMed_inventoryReportedAt = findViewById(R.id.tv_editMed_inventoryReportedAt);
        ll_editMed_trackInventory = findViewById(R.id.ll_editMed_trackInventory);
        switch_editMed_trackInventory = findViewById(R.id.switch_editMed_trackInventory);

        switch_editMed_trackInventory.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            ll_editMed_trackInventory.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            tv_editMed_inventoryReportedAt.setVisibility(isChecked && med != null && med.getInventoryReportedAt() > 0 ?
                    View.VISIBLE : View.GONE);
        });

        colors = getResources().getStringArray(R.array.color_options);

        if (med != null) {
            et_editMed_name.setText(med.getName());
            np_editMed_maxDose.setValue(med.getMaxDose());
            if (med.getDoseHours() % 24 == 0) {
                np_editMed_doseHoursDays.setValue(med.getDoseHours() / 24);
                sp_editMed_daysHours.setSelection(1, false);
            } else {
                np_editMed_doseHoursDays.setValue(med.getDoseHours());
            }
            np_editMed_defaultDoseCount.setValue(med.getDefaultDoseCount());
            switch_editMed_showDayDoseCount.setChecked(med.getShowDayDoseCount());
            if (med.getIsInventoryTracked()) {
                switch_editMed_trackInventory.setChecked(true);
                ll_editMed_trackInventory.setVisibility(View.VISIBLE);
                np_editMed_currentInventory.setValue((int) med.getCurrentInventory());
                if (med.getInventoryReportedAt() > 0) {
                    tv_editMed_inventoryReportedAt.setVisibility(View.VISIBLE);
                    List<List<ParcelableSpan>> spansList = new ArrayList<>();
                    List<ParcelableSpan> spans = new ArrayList<>();
                    spans.add(new StyleSpan(Typeface.BOLD));
                    spansList.add(spans);
                    String unformatted = getString(R.string.inventory_reported_at);
                    String timeSpanString = Utils.getRelativeTimeSpanString(this, med.getInventoryReportedAt(), true);
                    tv_editMed_inventoryReportedAt.setText(Utils.styleString(unformatted, spansList, timeSpanString));
                } else {
                    tv_editMed_inventoryReportedAt.setVisibility(View.GONE);
                }
            } else {
                switch_editMed_trackInventory.setChecked(false);
                ll_editMed_trackInventory.setVisibility(View.GONE);
                tv_editMed_inventoryReportedAt.setVisibility(View.GONE);
            }
            setTitle(getString(R.string.edit_med_title, "\"" + med.getName() +"\""));
            selectedColor = med.getColor();
        } else {
            ll_editMed_trackInventory.setVisibility(View.GONE);
            tv_editMed_inventoryReportedAt.setVisibility(View.GONE);
            int rnd = new Random().nextInt(colors.length);
            selectedColor = colors[rnd];
            setTitle(getString(R.string.new_med));
        }

        colorButtons = new ArrayList<>();
        ImageButton imageButton;

        int drawableID;
        int dp48 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48,
                getResources().getDisplayMetrics()
        );
        int btnText = ThemeHelper.getThemeAttr(R.attr.buttonText, this);
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
            imageButton.setContentDescription(R.string.set_med_color + " " + Utils.getResourceIdentifier(this, colorName, "string"));
            colorButtons.add(imageButton);

            imageButton.setOnClickListener(view -> {
                for (ImageButton listButton : colorButtons) {
                    setColorBtnState(listButton, false);
                }
                setColorBtnState((ImageButton) view, true);
                selectedColor = colorName;
            });

            cl_editMed_parent.addView(imageButton);
            flow_editMed_colors.addView(imageButton);
        }

        btn_editMed_save.setOnClickListener(view -> {

            Med med1;
            String medName;
            int maxDose;
            int doseHours;
            int defaultDoseCount;

            try {
                medName = et_editMed_name.getText().toString();
                maxDose = np_editMed_maxDose.getValue();
                doseHours = np_editMed_doseHoursDays.getValue();
                boolean isDays = sp_editMed_daysHours.getSelectedItemPosition() == 1;
                if (isDays) {
                    doseHours *= 24;
                }
                defaultDoseCount = np_editMed_defaultDoseCount.getValue();
                boolean showDayDoseCount = switch_editMed_showDayDoseCount.isChecked();
                boolean isInventoryTracked = switch_editMed_trackInventory.isChecked();
                double reportedInventory = -1d;
                long inventoryReportedAt = -1L;
                if (isInventoryTracked) {
                    reportedInventory = np_editMed_currentInventory.getValue();
                    Calendar calendar = Calendar.getInstance();
                    inventoryReportedAt = calendar.getTimeInMillis() / 1000;
                }
                med1 = new Med(medID, medName, maxDose, doseHours, selectedColor, isInventoryTracked,
                        reportedInventory, inventoryReportedAt, defaultDoseCount, showDayDoseCount, EditMedActivity.this);
            } catch (Exception e) {
                Toast.makeText(EditMedActivity.this, "Error saving med: invalid data", Toast.LENGTH_SHORT).show();
                return;
            }

            if (medID > -1) {
                boolean edited = mApp.setMed(med1);
                if (!edited) return;
            } else {
                med1.setHasLoadedAllDoses(true);
                boolean added = mApp.addMed(med1);
                if (!added) return;
            }

            Toast.makeText(EditMedActivity.this, getString(R.string.toast_med_saved),
                    Toast.LENGTH_SHORT).show();

            EditMedActivity.this.finish();
        });
    }

    private void setColorBtnState(ImageButton btn, boolean isSelected) {
        if (isSelected) {
            btn.setImageResource(R.drawable.ic_baseline_check_24);
        } else {
            btn.setImageResource(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_med_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            EditMedActivity.this.finish();
            return true;
        }
        if (itemID == R.id.mi_edit_med_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_delete_medication)
                    .setTitle(R.string.delete)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        mApp.removeMedById(medID);
                        Intent homeIntent = new Intent(EditMedActivity.this, MainActivity.class);
                        startActivity(homeIntent);
                        EditMedActivity.this.finish();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            builder.show();
            return true;
        }
        if (itemID == R.id.mi_edit_med_settings) {
            intent = new Intent(EditMedActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}