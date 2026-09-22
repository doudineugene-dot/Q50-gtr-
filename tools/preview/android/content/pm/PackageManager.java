package android.content.pm;

public class PackageManager {
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;

    public int checkPermission(String permission, String pkg) { return PERMISSION_DENIED; }

    public boolean hasSystemFeature(String name) { return false; }

    public java.util.List<ApplicationInfo> getInstalledApplications(int flags) {
        return new java.util.ArrayList<ApplicationInfo>();
    }

}
