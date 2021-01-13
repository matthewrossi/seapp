package com.example.showcaseapp;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionUtility {

    private Context ctx;

    private String[] perms;

    public PermissionUtility(Context context, String... permissions) {
        this.ctx = context;
        this.perms = permissions;
    }

    public boolean arePermissionsEnabled(){
        for(String permission : perms){
            if(ActivityCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    public void requestMultiplePermissions(){
        List<String> missing_perms = new ArrayList<>();
        for (String permission : perms) {
            if (ActivityCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                missing_perms.add(permission);
            }
        }
        ActivityCompat.requestPermissions((Activity) ctx, missing_perms.toArray(new String[missing_perms.size()]), 101);
    }

    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == 101){
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale((Activity) ctx, permissions[i])){
                        requestMultiplePermissions();
                    }
                    return false;
                }
            }
        }
        return true;
    }
}