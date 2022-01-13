package com.cliambrown.pilltime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    static ActivityResultLauncher<Intent> exportLauncher;
    static ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThemePillTime);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != Activity.RESULT_OK) return;
                        Intent intent = result.getData();
                        if (intent == null) return;
                        Uri uri = intent.getData();
                        try {
                            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                            FileOutputStream fileOutputStream =
                                    new FileOutputStream(pfd.getFileDescriptor());
                            DbHelper dbHelper = new DbHelper(SettingsActivity.this);
                            JSONObject jsonObject = dbHelper.getExportedDb();
                            if (jsonObject == null) {
                                Toast.makeText(SettingsActivity.this, "Error exporting database (null JSON object)", Toast.LENGTH_SHORT).show();
                            } else {
                                fileOutputStream.write((jsonObject.toString()).getBytes());
                            }
                            fileOutputStream.close();
                            pfd.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(SettingsActivity.this, "Error exporting database (file not found)", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(SettingsActivity.this, "Error exporting database (IO exception)", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(SettingsActivity.this, "Error exporting database (JSON exception)", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != Activity.RESULT_OK) return;
                        Intent intent = result.getData();
                        if (intent == null) return;
                        Uri uri = intent.getData();
                        PillTimeApplication mApp = (PillTimeApplication) SettingsActivity.this.getApplication();
                        mApp.importFromUri(uri);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme".equals(key)) {
            AppCompatDelegate.setDefaultNightMode(ThemeProvider.getThemeFromPrefs(SettingsActivity.this));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference export = getPreferenceManager().findPreference("export");
            if (export != null) {
                export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("text/plain");
                        Calendar c = Calendar.getInstance();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String dateStr = sdf.format(c.getTime());
                        String filename = "pilltime_export_" + dateStr;
                        intent.putExtra(Intent.EXTRA_TITLE, filename);
                        exportLauncher.launch(intent);
                        return true;
                    }
                });
            }

            Preference importPref = getPreferenceManager().findPreference("import");
            if (importPref != null) {
                importPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("text/plain");
                        importLauncher.launch(intent);
                        return true;
                    }
                });
            }

            Preference clearDB = getPreferenceManager().findPreference("clearDb");
            if (clearDB != null) {
                clearDB.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.dialog_clear_db)
                                .setTitle(R.string.clear_db)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        SettingsActivity activity = (SettingsActivity) getActivity();
                                        if (activity == null) return;
                                        PillTimeApplication mApp = (PillTimeApplication) activity.getApplication();
                                        mApp.clearMeds();
                                        Toast.makeText(getActivity(), "DB cleared", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}