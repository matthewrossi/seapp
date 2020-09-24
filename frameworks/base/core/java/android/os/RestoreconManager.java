package android.os;

import android.Manifest;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Slog;

import java.io.File;
import java.io.IOException;

/**
 * Manager class
 * The public methods of this class will be part of the new system API
 */
@SystemService(Context.RESTORECON_SERVICE)
public class RestoreconManager {
    private static final String TAG = "RestoreconManager";
    private static final String DATA_DATA_PREFIX = "/data/data/";

    private final Context mContext;
    private final IRestoreconService mService;

    /**
     * SystemServiceRegistry will build this manager object and provide
     * the remote service stub as parameter
     *
     * @param ctx
     * @param service
     * {@hide}
     */
    public RestoreconManager(Context ctx, IRestoreconService service) {
        mContext = ctx;
        mService = service;
    }

    private boolean outOfPackageScope(String pathname) {
        // assumes pathname already resolved
        // get package data directory
        int uid = Process.myUid();
        String pkgName = mContext.getPackageManager().getNameForUid(uid);
        String pkgPrefix = DATA_DATA_PREFIX + pkgName;

        return !pathname.startsWith(pkgPrefix);
    }

    /**
     * Restores a file to its default SELinux security context.
     * If the system is not compiled with SELinux, then {@code true}
     * is automatically returned.
     * If SELinux is compiled in, but disabled, then {@code true} is
     * returned.
     *
     * @param file The file to be relabeled.
     * @return a boolean indicating whether the relabeling succeeded.
     */
    @RequiresPermission(android.Manifest.permission.RESTORECON)
    public boolean restoreFileContext(File file) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.RESTORECON)
                == PackageManager.PERMISSION_DENIED) {
            Slog.e(TAG, "UID " + Process.myUid() + " / PID " + Process.myPid() +
                    " lacks permission " + android.Manifest.permission.RESTORECON);
            return false;
        }

        // resolve pathname
        String pathname;
        try {
            pathname = file.getCanonicalPath();
        } catch (IOException e) {
            Slog.e(TAG, "Error getting canonical path. Restorecon failed for " +
                    file.getPath(), e);
            return false;
        }

        if (outOfPackageScope(pathname)) {
            Slog.e(TAG, "Restorecon outside package scope");
            return false;
        }

        try{
            mService.restoreFileContext(pathname);
        } catch (RemoteException ex){
            Slog.e("restoreFileContext", "Unable to contact the remote RestoreconService");
            return false;
        }
        return true;
    }

    /**
     * Restores a file to its default SELinux security context.
     * If the system is not compiled with SELinux, then {@code true}
     * is automatically returned.
     * If SELinux is compiled in, but disabled, then {@code true} is
     * returned.
     *
     * @param pathname The pathname of the file to be relabeled.
     * @return a boolean indicating whether the relabeling succeeded.
     */
    @RequiresPermission(android.Manifest.permission.RESTORECON)
    public boolean restoreFileContext(String pathname) {
        return restoreFileContext(new File(pathname));
    }

    /**
     * Recursively restores all files under the given path to their default
     * SELinux security context. If the system is not compiled with SELinux,
     * then {@code true} is automatically returned. If SELinux is compiled in,
     * but disabled, then {@code true} is returned.
     *
     * @param file The pathname of the file to be relabeled.
     * @return a boolean indicating whether the relabeling succeeded.
     */
    @RequiresPermission(android.Manifest.permission.RESTORECON)
    public boolean restoreFileContextRecursive(File file) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.RESTORECON)
                == PackageManager.PERMISSION_DENIED) {
            Slog.e(TAG, "UID " + Process.myUid() + " / PID " + Process.myPid() +
                    "  lacks permission" + android.Manifest.permission.RESTORECON);
            return false;
        }

        // resolve pathname
        String pathname;
        try {
            pathname = file.getCanonicalPath();
        } catch (IOException e) {
            Slog.e(TAG, "Error getting canonical path. Restorecon failed for " +
                    file.getPath(), e);
            return false;
        }

        if (outOfPackageScope(pathname)) {
            Slog.e(TAG, "Restorecon outside package scope");
            return false;
        }

        try{
            mService.restoreFileContextRecursive(pathname);
        } catch (RemoteException ex){
            Slog.e("restoreFileContextRecursive", "Unable to contact the remote RestoreconService");
            return false;
        }
        return true;
    }

    /**
     * Recursively restores all files under the given path to their default
     * SELinux security context. If the system is not compiled with SELinux,
     * then {@code true} is automatically returned. If SELinux is compiled in,
     * but disabled, then {@code true} is returned.
     *
     * @param pathname The pathname of the file to be relabeled.
     * @return a boolean indicating whether the relabeling succeeded.
     */
    @RequiresPermission(android.Manifest.permission.RESTORECON)
    public boolean restoreFileContextRecursive(String pathname) {
        return restoreFileContextRecursive(new File(pathname));
    }

}
