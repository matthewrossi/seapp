package com.example.showcaseapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class UseCase3Activity  extends AppCompatActivity {

    private StringBuilder sb;
    private int [] some_numbers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_case3);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // generating some random numbers process by the .so library
        some_numbers = new int[10];
        Random rand = new Random();
        for (int i=0; i<10; i++)
            some_numbers[i] = rand.nextInt();

        // create the sb to build output
        sb = new StringBuilder(100);

        // hook method to run the UC
        Button launch_dotso_button = (Button) findViewById(R.id.lauch_dotso_button);
        launch_dotso_button.setText("run shared library");
        launch_dotso_button.setOnClickListener(this::launchSharedLibrary);

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void launchSharedLibrary(View v){
        TextView dotso_textview = (TextView) findViewById(R.id.dotso_textview);

        // show that the .so can access legitimate services
        if (hasCameraPermission()){
            sb.append("App has camera permission granted\n");
            final CameraManager cameraManager = (CameraManager) getHandleFromJNI("CAMERA_SERVICE");
            sb.append("The .so library can access the CameraManager via:\n" + cameraManager.toString() + "\n");
            dotso_textview.setText(sb.toString());
        }

        // show that the .so library can do some computation
        sb.append("\n\nThe .so can perform some linear algebra, result: " + doSomeAlgebraInJNI(some_numbers) + "\n\n");
        dotso_textview.setText(sb.toString());

        // show the confinement when policy is active
        if (hasConnectivityPermission()) {
            sb.append("App has connectivity permission granted\n");
            final ConnectivityManager connectivityManager = (ConnectivityManager) getHandleFromJNI("CONNECTIVITY_SERVICE");
            sb.append("The .so library can access the ConnectivityManager via:\n" + connectivityManager.toString() + "\n");
            dotso_textview.setText(sb.toString());

            // trying to bind the current process to network
            boolean bound_to_network = false;
            for (Network netw : connectivityManager.getAllNetworks()) {
                bound_to_network = connectivityManager.bindProcessToNetwork(netw);
                if (bound_to_network)
                    break;
            }
            if (bound_to_network)
                sb.append("Try bindProcessToNetwork: " + "SUCCESS!");
            else
                sb.append("Try bindProcessToNetwork: " + "FAILURE!\n(avc: denied {create} for media_d on udp_socket)");
            dotso_textview.setText(sb.toString());
            // clean sb
            sb = new StringBuilder(100);
        }
    }

    private boolean hasCameraPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    private boolean hasConnectivityPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE)
                == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    public native Object getHandleFromJNI(String service_name);

    public native int doSomeAlgebraInJNI(int[] factors);

    static {
        System.loadLibrary("vulnerablemedia");
    }

}