package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends MainMenuActivity {

    FloatingActionButton btn_main_add;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    PillTimeApplication mApp;

    List<Med> meds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApp = (PillTimeApplication) this.getApplication();
        meds = mApp.getMeds();

        btn_main_add = findViewById(R.id.btn_main_add);

        recyclerView = findViewById(R.id.rv_main_meds);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MedsRecycleViewAdapter(meds, this, mApp);
        recyclerView.setAdapter(mAdapter);

        btn_main_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditMedActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // TODO replace with some kind of queue system? maybe in PillTimeApplication
        mAdapter.notifyDataSetChanged();
    }
}