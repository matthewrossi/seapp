package com.example.sepolicytestapp;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.io.File;

public class UnclassifiedActivity extends AppCompatActivity {

    private TextView contextView;
    private TextView unclassifiedView;
    private TextView top_secretView;

    private File unclassified;
    private File top_secret;

    private void updateView() {

        contextView.setText(getString(R.string.context, "unclassified"));

        if (unclassified.canRead())
            unclassifiedView.setText(getString(R.string.allowed, unclassified));
        else
            unclassifiedView.setText(getString(R.string.disallowed, unclassified));
        if (top_secret.canRead())
            top_secretView.setText(getString(R.string.allowed, top_secret));
        else
            top_secretView.setText(getString(R.string.disallowed, top_secret));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unclassified);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
        }

        contextView = findViewById(R.id.context);
        unclassifiedView = findViewById(R.id.unclussified);
        top_secretView = findViewById(R.id.top_secret);

        unclassified = new File(getFilesDir(), "unclassified");
        top_secret = new File(getFilesDir(), "top-secret");

        updateView();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
