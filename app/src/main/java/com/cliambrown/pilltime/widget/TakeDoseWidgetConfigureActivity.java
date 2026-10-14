package com.cliambrown.pilltime.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.Med;

import java.util.ArrayList;
import java.util.List;

public class TakeDoseWidgetConfigureActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_take_dose_widget_configure);

        Intent launchIntent = getIntent();
        if (launchIntent != null && launchIntent.getExtras() != null) {
            appWidgetId = launchIntent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        Spinner spinner = findViewById(R.id.widget_med_spinner);
        Button saveButton = findViewById(R.id.widget_save_button);
        TextView hint = findViewById(R.id.widget_configure_hint);

        List<Med> meds = ((PillTimeApplication) getApplication()).getMeds();
        List<String> medNames = new ArrayList<>();
        List<Integer> medIds = new ArrayList<>();
        for (Med med : meds) {
            medNames.add(med.getName());
            medIds.add(med.getId());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                medNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (medIds.isEmpty()) {
            hint.setText(R.string.widget_no_meds);
            spinner.setVisibility(View.GONE);
            saveButton.setEnabled(false);
            return;
        }

        saveButton.setOnClickListener(view -> {
            int selectedPosition = spinner.getSelectedItemPosition();
            if (selectedPosition < 0 || selectedPosition >= medIds.size()) return;

            TakeDoseWidgetProvider.saveMedId(this, appWidgetId, medIds.get(selectedPosition));
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            TakeDoseWidgetProvider.updateWidget(this, manager, appWidgetId);

            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
