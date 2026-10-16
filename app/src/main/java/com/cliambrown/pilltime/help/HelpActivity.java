package com.cliambrown.pilltime.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.utilities.SimpleMenuActivity;

import java.util.Objects;

public class HelpActivity extends SimpleMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setTitle(R.string.help);
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
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private void openAppSettings() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.help_preferences, rootKey);
            PreferenceManager preferenceManager = getPreferenceManager();

            Preference pref_tipDelayedNotifications2 = preferenceManager.findPreference("pref_tipDelayedNotifications2");
            if (pref_tipDelayedNotifications2 != null) {
                pref_tipDelayedNotifications2.setOnPreferenceClickListener(preference -> {
                    openAppSettings();
                    return true;
                });
            }

            Preference pref_tipDelayedNotifications3 = preferenceManager.findPreference("pref_tipDelayedNotifications3");
            if (pref_tipDelayedNotifications3 != null) {
                pref_tipDelayedNotifications3.setOnPreferenceClickListener(preference -> {
                    openAppSettings();
                    return true;
                });
            }

            Preference pref_tipDelayedNotifications4 = preferenceManager.findPreference("pref_tipDelayedNotifications4");
            if (pref_tipDelayedNotifications4 != null) {
                pref_tipDelayedNotifications4.setOnPreferenceClickListener(preference -> {
                    openAppSettings();
                    return true;
                });
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Preference pref_tipDelayedNotifications5 = preferenceManager.findPreference("pref_tipDelayedNotifications5");
                if (pref_tipDelayedNotifications5 != null) {
                    pref_tipDelayedNotifications5.setOnPreferenceClickListener(preference -> {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    });
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}