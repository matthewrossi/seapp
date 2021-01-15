package com.example.showcaseapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "Showcaseapp.MainActivity";

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

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(getString(R.string.err_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> {
                    dialog.dismiss();   // prolly not needed
                    finish();
                });

        // Create application directory structure
        File internal = null;
        File user = null;
        boolean createdInternals = false;
        boolean createdUser = false;
        try{
            internal = new android.os.File(getFilesDir().getPath(), "internal");
            user = new android.os.File(getFilesDir().getPath(), "user");
            createdInternals = internal.mkdir();
            createdUser = user.mkdir();
        } catch (Exception e){
            Log.d(TAG, "App has no policy module, cannot restore context of internal directories");
            e.printStackTrace();
            Log.d(TAG, "Internal directory structure will be created with java.io.File");
        }

        // Fallback to standard directory creation when there is no policy module
        if (!createdInternals && !createdUser && (!internal.exists() || !user.exists())) {
            internal = new File(getFilesDir().getPath(), "internal");
            user = new File(getFilesDir().getPath(), "user");
            internal.mkdir();
            user.mkdir();
            Log.d(TAG, "Internal directory structure created successfully");
        }

        // Create an example file per application directory
        File appInternalData = new File(internal, "data");
        File userData = new File(user, "data");
        boolean appInternalDataCreated = false;
        boolean userDataCreated = false;
        try {
            appInternalDataCreated = appInternalData.createNewFile();
            userDataCreated = userData.createNewFile();
        } catch (IOException e) {
            alertDialog.setMessage(getString(R.string.err_files));
            alertDialog.show();
        }

        // Initialize example files with some text
        if (appInternalDataCreated) {
            try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(appInternalData))
            ) {
                String string = "EXPLOITED!";
                writer.write(string);
            } catch (IOException e) {
                alertDialog.setMessage(getString(R.string.err_writing));
                alertDialog.show();
            }
        }
        if (userDataCreated) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(userData))) {
                String string = "This Use Case proves the benefits a SEApp has over a normal app " +
                        "when dealing with access to its internal storage.\nTo demonstrate this, " +
                        "we show how the current activity, besides suffering of a path traversal " +
                        "vulnerability, cannot be exploited when the app is associated with a " +
                        "properly configured SEApp policy module.\n\nThe activity given an " +
                        "intent containing a file path displays its content. While this may be " +
                        "\"fine\" when the intent comes only from trusted components (other " +
                        "components within the same app), the activity supports implicit intents " +
                        "coming from untrusted sources too.\nTherefore, by sending a " +
                        "specifically crafted intent, we can exploit the vulnerable activity and " +
                        "see the content of any target file within the application internal " +
                        "storage.\n\nUsing ADB:\nadb shell\nam start " +
                        "-n com.example.showcaseapp/.UseCase1Activity\n" +
                        "-a \"com.example.showcaseapp.intent.action.SHOW\"\n" +
                        "--es \"com.example.showcaseapp.intent.extra.PATH\"\n\"../internal/data\"";
                writer.write(string);
            } catch (IOException e) {
                alertDialog.setMessage(getString(R.string.err_writing));
                alertDialog.show();
            }
        }

        permsUtil = new PermissionUtility(this, perms);
        if(!permsUtil.arePermissionsEnabled()){
            permsUtil.requestMultiplePermissions();
        }
    }

    public void goToUseCase1(View view) {
        Intent toUseCase1 = new Intent(MainActivity.this, UseCase1Activity.class);
        toUseCase1.putExtra("com.example.showcaseapp.intent.extra.PATH", "data");
        startActivity(toUseCase1);
    }

    public void goToUseCase2Or3(View view) {
        Button clickedButton = (Button) view;
        String text = clickedButton.getText().toString();
        int idx = Character.getNumericValue(text.charAt(text.indexOf("#") + 1)) - 1;
        if (!permsUtil.arePermissionsEnabled()) {
            showAdvice();
            return;
        }
        Intent toUseCase = new Intent(MainActivity.this, classes[idx]);
        startActivity(toUseCase);
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