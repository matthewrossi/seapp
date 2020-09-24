package android.os;

import android.app.ActivityThread;
import android.content.Context;

import java.io.IOException;
import java.net.URI;

public class File extends java.io.File{

    private final Context mContext = ActivityThread.currentApplication();

    public File(String pathname) {
        super(pathname);
    }

    public File(String parent, String child) {
        super(parent, child);
    }

    public File(File parent, String child) {
        super(parent, child);
    }

    public File(URI uri) {
        super(uri);
    }

    @Override
    public boolean createNewFile() throws IOException {
        boolean created = super.createNewFile();
        if (created) {
            RestoreconManager restoreconManager =
                    (RestoreconManager) mContext.getSystemService(Context.RESTORECON_SERVICE);
            if (!restoreconManager.restoreFileContext(this)) {
                this.delete();
                throw new IOException("Cannot restorecon file path");
            }
        }
        return created;
    }

    @Override
    public boolean mkdir() {
        boolean created = super.mkdir();
        if (created) {
            RestoreconManager restoreconManager =
                    (RestoreconManager) mContext.getSystemService(Context.RESTORECON_SERVICE);
            if (!restoreconManager.restoreFileContext(this)) {
                this.delete();
                return false;
            }
        }
        return created;
    }

    @Override
    public boolean mkdirs() {
        if (exists()) {
            return false;
        }
        if (mkdir()) {
            return true;
        }
        File canonFile = null;
        try {
            canonFile = new File(getCanonicalPath());
        } catch (IOException e) {
            return false;
        }

        File parent = new File(canonFile.getParentFile().getPath());
        return (parent != null && (parent.mkdirs() || parent.exists()) &&
                canonFile.mkdir());
    }

    public static File createTempFile(String prefix, String suffix,
                                      File directory)
            throws IOException
    {
        java.io.File tmpfile = java.io.File.createTempFile(prefix, suffix, directory);
        Context context = ActivityThread.currentApplication();
        RestoreconManager restoreconManager =
                (RestoreconManager) context.getSystemService(Context.RESTORECON_SERVICE);
        if (!restoreconManager.restoreFileContext(tmpfile.getPath())) {
            tmpfile.delete();
            throw new IOException("Cannot restorecon file path");
        }
        return new File(tmpfile.getPath());
    }

    public static File createTempFile(Context ctx, String prefix, String suffix)
            throws IOException
    {
        return createTempFile(prefix, suffix, null);
    }

}
