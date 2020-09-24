package android.os;

/** {@hide} */
interface IRestoreconService {

    /**
     * Restores the file at the given path to its default SELinux security context.
     */
    void restoreFileContext(@utf8InCpp String pathname);

    /**
     * Recursively restores all files under the given path to their default SELinux security
     * context.
     */
    void restoreFileContextRecursive(@utf8InCpp String pathname);
}