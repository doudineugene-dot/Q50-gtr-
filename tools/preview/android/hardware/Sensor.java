package android.hardware;

/** Подмена android.hardware.Sensor для офлайн-сборки. */
public class Sensor {
    public static final int TYPE_ALL = -1;

    private final int type;
    private final String name;
    private final String vendor;

    public Sensor(int type, String name, String vendor) {
        this.type = type; this.name = name; this.vendor = vendor;
    }

    public int getType() { return type; }
    public String getName() { return name; }
    public String getVendor() { return vendor; }
}
