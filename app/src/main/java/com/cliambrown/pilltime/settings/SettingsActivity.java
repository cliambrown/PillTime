package com.cliambrown.pilltime.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.help.HelpActivity;
import com.cliambrown.pilltime.utilities.DbHelper;
import com.cliambrown.pilltime.utilities.ThemeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    PillTimeApplication mApp;
    static ActivityResultLauncher<Intent> exportLauncher;
    static ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        mApp = (PillTimeApplication) this.getApplication();

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) return;
                    Intent intent = result.getData();
                    if (intent == null) return;
                    Uri uri = intent.getData();
                    if (uri == null) return;
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                        assert pfd != null;
                        FileOutputStream fileOutputStream =
                                new FileOutputStream(pfd.getFileDescriptor());
                        DbHelper dbHelper = new DbHelper(SettingsActivity.this);
                        JSONObject jsonObject = dbHelper.getExportedDb();
                        if (jsonObject == null) {
                            mApp.handleDbError(R.string.error_exporting_database, R.string.null_json_object);
                        } else {
                            fileOutputStream.write((jsonObject.toString()).getBytes());
                            mApp.showToast(R.string.data_exported);
                        }
                        fileOutputStream.close();
                        pfd.close();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        mApp.handleDbError(R.string.error_exporting_database, R.string.null_pointer_exception);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        mApp.handleDbError(R.string.error_exporting_database, R.string.file_not_found);
                    } catch (IOException e) {
                        e.printStackTrace();
                        mApp.handleDbError(R.string.error_exporting_database, R.string.io_exception);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mApp.handleDbError(R.string.error_exporting_database, R.string.json_exception);
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) return;
                    Intent intent = result.getData();
                    if (intent == null) return;
                    Uri uri = intent.getData();
                    PillTimeApplication mApp = (PillTimeApplication) SettingsActivity.this.getApplication();
                    mApp.importFromUri(uri);
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
        if (key == null) return;
        if (key.equals("theme")) {
            AppCompatDelegate.setDefaultNightMode(ThemeHelper.getThemeFromPrefs(SettingsActivity.this));
        } else if (key.equals("med_sort")) {
            PillTimeApplication mApp = (PillTimeApplication) SettingsActivity.this.getApplication();
            mApp.sortMeds();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        PillTimeApplication mApp;
        SharedPreferences prefs;
        Context context;

        Preference pref_notificationsDisabled;
        boolean notifyDefaultVal;
        Preference pref_exactAlarmsDenied;

        @Override
        public void onAttach(@NonNull Context thisContext) {
            super.onAttach(thisContext);
            context = thisContext;
            mApp = (PillTimeApplication) requireActivity().getApplication();
            prefs = PreferenceManager.getDefaultSharedPreferences(thisContext);
        }

        private final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    toggleNotificationsDisabledNotice();
                });

        private void requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && mApp.areNotificationsDisabled()
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            notifyDefaultVal = prefs.getBoolean("notify_default", false);
            toggleNotificationsDisabledNotice();
            toggleExactAlarmsDenied();
        }

        private void toggleNotificationsDisabledNotice() {
            pref_notificationsDisabled.setVisible(
                    mApp.hasEnabledNotificationsOnce()
                    && mApp.areNotificationsDisabled()
            );
        }

        private void toggleExactAlarmsDenied() {
            pref_exactAlarmsDenied.setVisible(
                    mApp.hasEnabledNotificationsOnce()
                    && !mApp.hasExactAlarmPermission()
            );
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            notifyDefaultVal = prefs.getBoolean("notify_default", false);
            if (notifyDefaultVal) requestNotificationPermission();

            PreferenceManager preferenceManager = getPreferenceManager();

            Preference pref_notifyDefault = preferenceManager.findPreference("pref_notifyDefault");
            Preference pref_notifySoundDefault = preferenceManager.findPreference("pref_notifySoundDefault");
            pref_exactAlarmsDenied = preferenceManager.findPreference("pref_exactAlarmsDenied");
            pref_notificationsDisabled = preferenceManager.findPreference("pref_notificationsDisabled");

            if (pref_notifyDefault != null && pref_notifySoundDefault != null && pref_exactAlarmsDenied != null) {
                toggleNotificationsDisabledNotice();
                pref_notifySoundDefault.setVisible(notifyDefaultVal);
                toggleExactAlarmsDenied();
                pref_notifyDefault.setOnPreferenceChangeListener((preference, isChecked) -> {
                    notifyDefaultVal = (Boolean) isChecked;
                    if (notifyDefaultVal) {
                        mApp.recordEnabledNotifications();
                        requestNotificationPermission();
                        mApp.requestExactAlarmPermission(context);
                    }
                    toggleNotificationsDisabledNotice();
                    pref_notifySoundDefault.setVisible(notifyDefaultVal);
                    return true;
                });
                pref_exactAlarmsDenied.setOnPreferenceClickListener((preference) -> {
                    mApp.requestExactAlarmPermission(context);
                    return true;
                });
            }

            Preference pref_export = preferenceManager.findPreference("pref_export");
            if (pref_export != null) {
                pref_export.setOnPreferenceClickListener(preference -> {
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
                });
            }

            Preference pref_import = preferenceManager.findPreference("pref_import");
            if (pref_import != null) {
                pref_import.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    importLauncher.launch(intent);
                    return true;
                });
            }

            Preference pref_clearDb = preferenceManager.findPreference("pref_clearDb");
            if (pref_clearDb != null) {
                pref_clearDb.setOnPreferenceClickListener(preference -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.dialog_clear_db)
                            .setTitle(R.string.clear_db)
                            .setPositiveButton(R.string.yes, (dialog, id) -> {
                                SettingsActivity activity = (SettingsActivity) getActivity();
                                if (activity == null) return;
                                PillTimeApplication mApp = (PillTimeApplication) activity.getApplication();
                                mApp.clearMeds();
                            })
                            .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    builder.show();
                    return true;
                });
            }

            Preference pref_repairDb = preferenceManager.findPreference("pref_repairDb");
            if (pref_repairDb != null) {
                pref_repairDb.setOnPreferenceClickListener(preference -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.dialog_repair_db)
                            .setTitle(R.string.repair_db)
                            .setPositiveButton(R.string.repair, (dialog, id) -> {
                                SettingsActivity activity = (SettingsActivity) getActivity();
                                if (activity == null) return;
                                PillTimeApplication mApp = (PillTimeApplication) activity.getApplication();
                                mApp.repairDb();
                            })
                            .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    builder.show();
                    return true;
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            this.finish();
            return true;
        }
        if (itemID == R.id.mi_settings_help) {
            Intent intent = new Intent(SettingsActivity.this, HelpActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}