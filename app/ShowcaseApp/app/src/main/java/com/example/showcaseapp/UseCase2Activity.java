package com.example.showcaseapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// example of ads library
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

public class UseCase2Activity extends AppCompatActivity {

    private int ad_reward;
    private final Unity_ads_listener unity_listener = new Unity_ads_listener();
    private final String TAG = "showcaseapp:ads_d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_case2);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // set instance variables
        ad_reward = 0;

        // set ads library init button
        Button launch_ads_button = (Button) findViewById(R.id.launch_ads_button);
        launch_ads_button.setText("Init AD library");
        launch_ads_button.setOnClickListener(this::launch_ad_onClick);

        // set initial textview info
        TextView ad_status_textview = (TextView) findViewById(R.id.ad_status_textview);
        TextView location_perm_textview = (TextView) findViewById(R.id.location_perm_textview);
        TextView gps_latitude_textview = (TextView) findViewById(R.id.gps_latitude_textview);
        TextView gps_longitude = (TextView) findViewById(R.id.gps_longitude_textview);
        ad_status_textview.setText("Ads Library not initialized");
        gps_latitude_textview.setText("GPS latitude: unknown");
        gps_longitude.setText("GPS longitude: unknown");
        if (hasLocationPermission())
            location_perm_textview.setText("App has location permission granted");
        else
            location_perm_textview.setText("App hasn't location permission granted");

    }

    private boolean hasLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    void captureLocation(){

        final TextView latitude = (TextView) findViewById(R.id.gps_latitude_textview);
        final TextView longitude = (TextView) findViewById(R.id.gps_longitude_textview);

        Log.d(TAG, "Capturing location - setup location listener");
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                location = location;
                Log.d("Location Changes", location.toString());
                latitude.setText("GPS Latitude: " + String.valueOf(location.getLatitude()));
                longitude.setText("GPS longitude: " + String.valueOf(location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Status Changed", String.valueOf(status));
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("Provider Enabled", provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("Provider Disabled", provider);
            }
        };

        // create some criteria (for example to save battery...)
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

        // trying to get a location manager handle (avc:  denied  { find } when SEApp policy is used)
        Log.d(TAG, "Capturing location - trying to get a location manager handle");
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "locationManager obj reference: " + locationManager.toString());

        // to avoid battery drain
        final Looper looper = null;

        // one-time location fetch after the the button is clicked
        if (hasLocationPermission()) {
            //todo: substitute with getCurrentLocation when porting to API v30

            // trying to hook a listener to request a single location update (avc:  denied  { find } when SEApp policy is used)
            // -> ActivityTaskManager forces finishing the current
            Log.d(TAG, "Capturing location - trying to hook a listener to request a single location update");
            locationManager.requestSingleUpdate(criteria, locationListener, looper);
        }
    }

    public void launch_ad_onClick(View v){
        Button ad_button = (Button) findViewById(R.id.launch_ads_button);
        if (UnityAds.isReady())
            UnityAds.show(this);
        else {
            ad_button.setText("Initializing");
            UnityAds.initialize(this, "3967049", unity_listener);
        }

    }

    public void reward_ad(){
        // recognize the user his reward
        TextView out_textview = (TextView) findViewById(R.id.ad_status_textview);
        ad_reward += 1;
        out_textview.setText("Advertisement reward gained: " + new Integer(ad_reward).toString());
    }

    private class  Unity_ads_listener implements IUnityAdsListener{

        @Override
        public void onUnityAdsReady(String s) {
            Button ad_button = (Button) findViewById(R.id.launch_ads_button);
            ad_button.setText("Show advertisement");
            TextView ad_status_textview = (TextView) findViewById(R.id.ad_status_textview);
            ad_status_textview.setText("Ad library initialized");
        }

        @Override
        public void onUnityAdsStart(String s) {

        }

        @Override
        public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
            if (finishState != UnityAds.FinishState.SKIPPED)
                reward_ad();
            captureLocation();
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

        }
    }

}