package android.hardware;

import java.util.ArrayList;
import java.util.List;

/**
 * Подмена SensorManager: вне головного устройства автомобильных сенсоров нет,
 * поэтому список пуст. Это ровно тот же путь, по которому пойдёт приложение
 * на обычном Android, и он должен вести к UNAVAILABLE, а не к падению.
 */
public class SensorManager {
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_UI = 2;
    public static final int SENSOR_DELAY_NORMAL = 3;

    public List<Sensor> getSensorList(int type) { return new ArrayList<Sensor>(); }
    public boolean registerListener(SensorEventListener l, Sensor s, int rateUs) { return false; }
    public void unregisterListener(SensorEventListener l) { }
}
