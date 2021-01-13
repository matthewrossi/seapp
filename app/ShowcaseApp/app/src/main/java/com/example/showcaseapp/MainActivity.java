package com.example.showcaseapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private String[] perms = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private PermissionUtility permsUtil;

    private Class[] classes = {
            UseCase1Activity.class, UseCase2Activity.class, UseCase3Activity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permsUtil = new PermissionUtility(this, perms);
        if(!permsUtil.arePermissionsEnabled()){
            permsUtil.requestMultiplePermissions();
        }

    }

    public void goToUseCase(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        int idx = Character.getNumericValue(text.charAt(text.indexOf("#") + 1)) - 1;
        if (idx == 1 || idx == 2)
            if (!permsUtil.arePermissionsEnabled()) {
                showAdvice();
                idx = -1;
            }
        if (idx >= 0) {
            Intent toUseCase = new Intent(MainActivity.this, classes[idx]);
            startActivity(toUseCase);
        }
    }

    private void showAdvice(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setCancelable(true);
        alertBuilder.setMessage("Permissions are required to demonstrate UC#2 and UC#3");
        alertBuilder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(permsUtil.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            Log.d("Showcaseapp: ", "Permission granted");
        }
    }
}