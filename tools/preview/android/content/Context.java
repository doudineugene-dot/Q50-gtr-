package android.content;

public class Context { 
    private final android.content.res.Resources res = new android.content.res.Resources();

    public android.content.res.Resources getResources() { return res; }

    private final android.content.res.AssetManager assets = new android.content.res.AssetManager();

    public android.content.res.AssetManager getAssets() { return assets; }

    public String getPackageName() { return "com.q50gtr.plus"; }

    public static final String SENSOR_SERVICE = "sensor";

    private final android.content.pm.PackageManager pm = new android.content.pm.PackageManager();

    public Object getSystemService(String name) {
        if (SENSOR_SERVICE.equals(name)) {
            return new android.hardware.SensorManager();
        }
        return null;
    }

    public android.content.pm.PackageManager getPackageManager() { return pm; }
}
