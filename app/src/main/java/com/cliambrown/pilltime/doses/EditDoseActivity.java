package com.cliambrown.pilltime.doses;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.utilities.SimpleMenuActivity;
import com.cliambrown.pilltime.utilities.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Calendar;
import java.util.TimeZone;

public class EditDoseActivity extends SimpleMenuActivity {

    ImageButton btn_editDose_minusCount, btn_editDose_plusCount;
    EditText et_editDose_count;
    TextView tv_editDose_takenAtTime;
    TextView tv_editDose_takenAtDate;
    TextView tv_editDose_timezone;
    SwitchCompat switch_editDose_notify;
    SwitchCompat switch_editDose_notifySound;
    ExtendedFloatingActionButton btn_editDose_save;
    PillTimeApplication mApp;
    int medID, doseID;
    Calendar selectedDatetime;
    boolean hasManuallySelectedTime = false;

    private ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) switch_editDose_notify.setChecked(false);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dose);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        btn_editDose_minusCount = findViewById(R.id.btn_editDose_minusCount);
        btn_editDose_plusCount = findViewById(R.id.btn_editDose_plusCount);
        et_editDose_count = findViewById(R.id.et_editDose_count);
        tv_editDose_takenAtTime = findViewById(R.id.tv_editDose_takenAtTime);
        tv_editDose_takenAtDate = findViewById(R.id.tv_editDose_takenAtDate);
        tv_editDose_timezone = findViewById(R.id.tv_editDose_timezone);
        switch_editDose_notify = findViewById(R.id.switch_editDose_notify);
        switch_editDose_notifySound = findViewById(R.id.switch_editDose_notifySound);
        btn_editDose_save = findViewById(R.id.btn_editDose_save);

        Intent intent = getIntent();
        medID = intent.getIntExtra("medID", -1);
        doseID = intent.getIntExtra("doseID", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);
        Dose dose;

        if (med == null) {
            EditDoseActivity.this.finish();
            return;
        }

        selectedDatetime = Calendar.getInstance();
        hasManuallySelectedTime = false;

        TimeZone tz = selectedDatetime.getTimeZone();
        tv_editDose_timezone.setText(tz.getDisplayName());

        if (doseID > -1) {
            dose = med.getDoseById(doseID);
            if (dose == null) {
                EditDoseActivity.this.finish();
                return;
            }
            setTitle(getString(R.string.edit_dose_title,  "\"" + med.getName() + "\""));
            selectedDatetime.setTimeInMillis(dose.getTakenAt() * 1000L);
        } else {
            long now = System.currentTimeMillis() / 1000L;
            dose = new Dose(doseID, medID, med.getMaxDose(), now, getDefaultNotify(), getDefaultNotifySound(), EditDoseActivity.this);
            setTitle(getString(R.string.new_dose_title, "\"" + med.getName() + "\""));
        }

        switch_editDose_notify.setChecked(dose.getNotify());
        switch_editDose_notifySound.setChecked(dose.getNotifySound());
        switch_editDose_notifySound.setEnabled(dose.getNotify());

        handleNotifyPermissions();

        switch_editDose_notify.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            switch_editDose_notifySound.setEnabled(isChecked);
            handleNotifyPermissions();
        });

        et_editDose_count.setText(Utils.getStrFromDbl(dose.getCount()));
        updateTimeField();
        updateDateField();

        btn_editDose_minusCount.setOnClickListener(view -> incrementCount(-1D));

        btn_editDose_plusCount.setOnClickListener(view -> incrementCount(1D));

        tv_editDose_takenAtTime.setOnClickListener(this::showTimePickerDialog);

        tv_editDose_takenAtDate.setOnClickListener(this::showDatePickerDialog);

        btn_editDose_save.setOnClickListener(view -> {

            double count;
            boolean notify = getDefaultNotify();
            boolean notifySound = getDefaultNotifySound();

            try {
                count = Double.parseDouble(et_editDose_count.getText().toString());
                notify = switch_editDose_notify.isChecked();
                notifySound = switch_editDose_notifySound.isChecked();
            } catch (Exception e) {
                Toast.makeText(EditDoseActivity.this, "Error saving dose: invalid data", Toast.LENGTH_SHORT).show();
                return;
            }

            // If the user has chosen the time, ignore current actual time and round to the minute
            if (hasManuallySelectedTime) {
                selectedDatetime.set(Calendar.SECOND, 0);
                selectedDatetime.set(Calendar.MILLISECOND, 0);
            }

            long unixTime = selectedDatetime.getTimeInMillis() / 1000L;

            Dose dose1 = new Dose(doseID, medID, count, unixTime, notify, notifySound, EditDoseActivity.this);

            if (doseID > -1) {
                boolean edited = mApp.setDose(med, dose1);
                if (!edited) return;
            } else {
                boolean added = mApp.addDose(med, dose1);
                if (!added) return;
            }

            Toast.makeText(EditDoseActivity.this, getString(R.string.toast_dose_saved),
                    Toast.LENGTH_SHORT).show();

            EditDoseActivity.this.finish();
        });
    }

    private void handleNotifyPermissions() {
        if (switch_editDose_notify.isChecked()) {
            boolean notificationsEnabled = mApp.areNotificationsEnabled();
            if (!notificationsEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
    }

    private void incrementCount(double changeBy) {
        double count = 0D;
        try {
            count = Double.parseDouble(et_editDose_count.getText().toString());
        } catch (Exception e) {
            // Do nothing
        }
        count = count + changeBy;
        if (count < 0D) count = 0D;
        et_editDose_count.setText(Utils.getStrFromDbl(count));
    }

    private boolean getDefaultNotify() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(EditDoseActivity.this);
        return prefs.getBoolean("notify_default", false);
    }

    private boolean getDefaultNotifySound() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(EditDoseActivity.this);
        return prefs.getBoolean("notify_sound_default", false);
    }

    private void updateTimeField() {
        long unixTimeMs = selectedDatetime.getTimeInMillis();
        tv_editDose_takenAtTime.setText(DateUtils.formatDateTime(
                this,
                unixTimeMs,
                DateUtils.FORMAT_SHOW_TIME
        ));
    }

    private void updateDateField() {
        long unixTimeMs = selectedDatetime.getTimeInMillis();
        tv_editDose_takenAtDate.setText(DateUtils.formatDateTime(
                this,
                unixTimeMs,
                DateUtils.FORMAT_ABBREV_ALL |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_SHOW_YEAR
        ));
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int hour;
            int minute;
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity != null) {
                hour = activity.selectedDatetime.get(Calendar.HOUR_OF_DAY);
                minute = activity.selectedDatetime.get(Calendar.MINUTE);
            } else {
                // This block may not be necessary (?)
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            }

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity == null) return;
            activity.selectedDatetime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            activity.selectedDatetime.set(Calendar.MINUTE, minute);
            activity.updateTimeField();
            activity.hasManuallySelectedTime = true;
        }
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int year;
            int month;
            int day;
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity != null) {
                year = activity.selectedDatetime.get(Calendar.YEAR);
                month = activity.selectedDatetime.get(Calendar.MONTH);
                day = activity.selectedDatetime.get(Calendar.DAY_OF_MONTH);
            } else {
                // This block may not be necessary (?)
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity == null) return;
            activity.selectedDatetime.set(Calendar.YEAR, year);
            activity.selectedDatetime.set(Calendar.MONTH, month);
            activity.selectedDatetime.set(Calendar.DAY_OF_MONTH, day);
            activity.updateDateField();
            activity.hasManuallySelectedTime = true;
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
}