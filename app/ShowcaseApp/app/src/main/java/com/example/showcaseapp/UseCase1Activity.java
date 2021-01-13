package com.example.showcaseapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class UseCase1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_case1);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(UseCase1Activity.this).create();
        alertDialog.setTitle(getString(R.string.err_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> {
                    dialog.dismiss();   // prolly not needed
                    finish();
                });

        Intent intent = getIntent();
        String path = intent.getStringExtra("com.example.showcaseapp.intent.extra.PATH");
        File data = new File (getFilesDir().getPath() + "/user/" + path);

        StringBuilder stringBuilder = new StringBuilder();
        try (
            BufferedReader is = new BufferedReader(new FileReader(data))
        ){
            String line;
            while ((line = is.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        } catch (IOException e) {
            alertDialog.setMessage(getString(R.string.err_reading));
            alertDialog.show();
        }

        TextView fileView = findViewById(R.id.file);
        fileView.setText(stringBuilder);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}