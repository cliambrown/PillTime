package com.cliambrown.pilltime.meds;

import android.Manifest;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.ParcelableSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.utilities.SimpleMenuActivity;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class EditMedActivity extends SimpleMenuActivity {

    ConstraintLayout cl_editMed_parent;
    EditText et_editMed_name;
    NumberPicker np_editMed_maxDose;
    NumberPicker np_editMed_doseHoursDays;
    AppCompatSpinner sp_editMed_daysHours;
    NumberPicker np_editMed_defaultDoseCount;
    SwitchCompat switch_editMed_trackInventory;
    SwitchCompat switch_editMed_showDayDoseCount;
    SwitchCompat switch_editMed_overrideGlobalNotifyDefaults;
    SwitchCompat switch_editMed_notifyDefault;
    TextView tv_editMed_notificationsDisabled;
    SwitchCompat switch_editMed_notifySoundDefault;
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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                onNotifyDefaultChanged(switch_editMed_overrideGlobalNotifyDefaults.isChecked() && switch_editMed_notifyDefault.isChecked());
            });

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
        boolean isArchived;

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

        switch_editMed_overrideGlobalNotifyDefaults = findViewById(R.id.switch_editMed_overrideGlobalNotifyDefaults);
        switch_editMed_notifyDefault = findViewById(R.id.switch_editMed_notifyDefault);
        tv_editMed_notificationsDisabled = findViewById(R.id.tv_editMed_notificationsDisabled);
        switch_editMed_notifySoundDefault = findViewById(R.id.switch_editMed_notifySoundDefault);

        switch_editMed_overrideGlobalNotifyDefaults.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            switch_editMed_notifyDefault.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            switch_editMed_notifySoundDefault.setVisibility((isChecked && switch_editMed_notifyDefault.isChecked()) ? View.VISIBLE : View.GONE);
            onNotifyDefaultChanged(isChecked && switch_editMed_notifyDefault.isChecked());
        });
        switch_editMed_notifyDefault.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            requestNotificationPermission(isChecked);
            mApp.requestExactAlarmPermission(this);
            onNotifyDefaultChanged(isChecked && switch_editMed_overrideGlobalNotifyDefaults.isChecked());
            switch_editMed_notifySoundDefault.setVisibility((isChecked && switch_editMed_overrideGlobalNotifyDefaults.isChecked()) ? View.VISIBLE : View.GONE);
            if (isChecked) mApp.recordEnabledNotifications();
        });

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

            switch_editMed_overrideGlobalNotifyDefaults.setChecked(med.getOverrideGlobalNotifyDefaults());
            switch_editMed_notifyDefault.setChecked(med.getNotifyDefault());
            switch_editMed_notifyDefault.setVisibility(med.getOverrideGlobalNotifyDefaults() ? View.VISIBLE : View.GONE);
            onNotifyDefaultChanged(switch_editMed_overrideGlobalNotifyDefaults.isChecked() && switch_editMed_notifyDefault.isChecked());
            switch_editMed_notifySoundDefault.setChecked(med.getNotifySoundDefault());
            switch_editMed_notifySoundDefault.setVisibility(
                    (med.getOverrideGlobalNotifyDefaults()
                    && med.getNotifyDefault())
                            ? View.VISIBLE
                            : View.GONE
            );

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
            setTitle(getString(R.string.edit_med_title, "\"" + med.getName() + "\""));
            selectedColor = med.getColor();
            isArchived = med.isArchived();
        } else {
            isArchived = false;
            ll_editMed_trackInventory.setVisibility(View.GONE);
            tv_editMed_inventoryReportedAt.setVisibility(View.GONE);
            int rnd = new Random().nextInt(colors.length);
            selectedColor = colors[rnd];
            setTitle(getString(R.string.new_med));
        }

        btn_editMed_save.setBackgroundColor(Utils.getTextColor(selectedColor, EditMedActivity.this));

        requestNotificationPermission(switch_editMed_notifyDefault.isChecked());

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
            onColorBtnSelectedChanged(imageButton, selectedColor.equals(colorName));
            drawableID = Utils.getRoundButton(colorName);
            imageButton.setBackgroundResource(drawableID);
            imageButton.setFocusable(true);
            imageButton.setContentDescription(
                    getString(R.string.set_med_color) + " "
                    + Utils.getColorString(colorName)
            );
            colorButtons.add(imageButton);

            imageButton.setOnClickListener(view -> {
                for (ImageButton listButton : colorButtons) {
                    onColorBtnSelectedChanged(listButton, false);
                }
                onColorBtnSelectedChanged((ImageButton) view, true);
                selectedColor = colorName;
                btn_editMed_save.setBackgroundColor(Utils.getTextColor(selectedColor, EditMedActivity.this));
            });

            cl_editMed_parent.addView(imageButton);
            flow_editMed_colors.addView(imageButton);
        }

        btn_editMed_save.setOnClickListener(view -> {

            Med med1;

            try {
                String medName = et_editMed_name.getText().toString();
                int maxDose = np_editMed_maxDose.getValue();
                int doseHours = np_editMed_doseHoursDays.getValue();
                boolean isDays = sp_editMed_daysHours.getSelectedItemPosition() == 1;
                if (isDays)doseHours *= 24;
                int defaultDoseCount = np_editMed_defaultDoseCount.getValue();
                boolean showDayDoseCount = switch_editMed_showDayDoseCount.isChecked();
                boolean overrideGlobalNotifyDefaults = switch_editMed_overrideGlobalNotifyDefaults.isChecked();
                boolean notifyDefault = switch_editMed_notifyDefault.isChecked();
                boolean notifySoundDefault = switch_editMed_notifySoundDefault.isChecked();
                boolean isInventoryTracked = switch_editMed_trackInventory.isChecked();
                double reportedInventory = -1d;
                long inventoryReportedAt = -1L;
                if (isInventoryTracked) {
                    reportedInventory = np_editMed_currentInventory.getValue();
                    Calendar calendar = Calendar.getInstance();
                    inventoryReportedAt = calendar.getTimeInMillis() / 1000;
                }
                med1 = new Med(medID, medName, maxDose, doseHours, selectedColor, isInventoryTracked,
                        reportedInventory, inventoryReportedAt, defaultDoseCount, showDayDoseCount,
                        overrideGlobalNotifyDefaults, notifyDefault, notifySoundDefault, isArchived,
                        EditMedActivity.this);
            } catch (Exception e) {
                mApp.handleMedSaveError(getString(R.string.invalid_data));
                return;
            }

            if (medID > -1) {
                if (mApp.setMed(med1)) EditMedActivity.this.finish();
            } else {
                med1.setHasLoadedAllDoses(true);
                if (mApp.saveNewMed(med1)) EditMedActivity.this.finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        onNotifyDefaultChanged(switch_editMed_overrideGlobalNotifyDefaults.isChecked() && switch_editMed_notifyDefault.isChecked());
    }

    private void requestNotificationPermission(boolean notifyDefault) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && notifyDefault
                && mApp.areNotificationsDisabled()
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void onNotifyDefaultChanged(boolean notifyDefault) {
        if (notifyDefault && mApp.areNotificationsDisabled()) {
            tv_editMed_notificationsDisabled.setVisibility(View.VISIBLE);
        } else {
            tv_editMed_notificationsDisabled.setVisibility(View.GONE);
        }
    }

    private void onColorBtnSelectedChanged(ImageButton btn, boolean isSelected) {
        if (isSelected) {
            btn.setImageResource(R.drawable.ic_baseline_check_24);
        } else {
            btn.setImageResource(0);
        }
    }
}