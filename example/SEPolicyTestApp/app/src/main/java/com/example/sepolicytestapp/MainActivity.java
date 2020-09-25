package com.example.sepolicytestapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.File;
import android.os.RestoreconManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView contextView;
    private TextView unclassifiedView;
    private TextView top_secretView;

    private AlertDialog alertDialog;

    private File unclassified;
    private File top_secret;

    private void updateView() {

        contextView.setText(getString(R.string.context, "init"));

        if (unclassified.canRead())
            unclassifiedView.setText(getString(R.string.allowed, unclassified));
        else
            unclassifiedView.setText(getString(R.string.disallowed, unclassified));
        if (top_secret.canRead())
            top_secretView.setText(getString(R.string.allowed, top_secret));
        else
            top_secretView.setText(getString(R.string.disallowed, top_secret));
    }

    public void chgActivity(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        if (getString(R.string.unclassifed_button).equals(text)) {
            Intent toUnclassified = new Intent(MainActivity.this, UnclassifiedActivity.class);
            startActivity(toUnclassified);
        } else {
            Intent toTopSecret = new Intent(MainActivity.this, TopSecretActivity.class);
            startActivity(toTopSecret);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contextView = findViewById(R.id.context);
        unclassifiedView = findViewById(R.id.unclussified);
        top_secretView = findViewById(R.id.top_secret);

        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(getString(R.string.err_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();   // prolly not needed
                        finish();
                    }
                });

        unclassified = new File(getFilesDir().getPath(), "unclassified");
        top_secret = new File(getFilesDir().getPath(), "top-secret");
        try {
            unclassified.createNewFile();
            top_secret.createNewFile();
        } catch(IOException ioe) {
            alertDialog.setMessage(getString(R.string.err_files));
            alertDialog.show();
        }

        /*File random = new File("/data/data/com.example.sepolicytestapp/files/random/try/of/mkdirs");
        random.mkdirs();*/

        /*RestoreconManager restoreconManager = (RestoreconManager) getSystemService(RESTORECON_SERVICE);
        if (!restoreconManager.restoreFileContextRecursive(getFilesDir())) {
            alertDialog.setMessage(getString(R.string.err_restorecon));
            alertDialog.show();
        }*/

        updateView();

    }
}
