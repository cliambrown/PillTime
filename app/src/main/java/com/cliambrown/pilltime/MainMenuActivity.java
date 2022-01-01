package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
//                intent = new Intent(MainMenuActivity.this, MainActivity.class);
//                startActivity(intent);
                MainMenuActivity.this.finish();
                return true;
            case R.id.mi_main_settings:
                intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.mi_main_clearDb:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
                builder.setMessage(R.string.dialog_clear_db)
                        .setTitle(R.string.clear_db)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                PillTimeApplication mApp = (PillTimeApplication) MainMenuActivity.this.getApplication();
                                mApp.clearMeds();
                                Intent intent = new Intent(MainMenuActivity.this, MainMenuActivity.class);
                                startActivity(intent);
                                MainMenuActivity.this.finish();
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
        return super.onOptionsItemSelected(item);
    }
}