package com.example.showcaseapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int APP_PERMISSION_REQUEST_LOCATION = 1;
    private Class[] classes = {
            UseCase1Activity.class, UseCase2Activity.class, UseCase3Activity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasLocationPermission())
            askLocationPermission();
    }

    public void goToUseCase(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        int idx = Character.getNumericValue(text.charAt(text.indexOf("#") + 1)) - 1;
        if (idx == 1)
            if (!hasLocationPermission()) {
                showAdvice();
                idx = -1;
            }
        if (idx >= 0) {
            Intent toUseCase = new Intent(MainActivity.this, classes[idx]);
            startActivity(toUseCase);
        }
    }

    public boolean hasLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    private void showAdvice(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setCancelable(true);
        alertBuilder.setMessage("Location permission is required to demonstrate UC#2");
        alertBuilder.show();
    }

    public void askLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                APP_PERMISSION_REQUEST_LOCATION);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case APP_PERMISSION_REQUEST_LOCATION: {
                // if request is cancelled, the result arrays are empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        // if something else has to be done with the position ...
                    }

                } else {
                    // permission denied ...
                }
                return;
            }

        }
    }
}