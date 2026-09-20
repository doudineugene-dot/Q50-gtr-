package android.hardware;

import java.util.ArrayList;
import java.util.List;

/**
 * Подмена SensorManager: вне головного устройства автомобильных сенсоров нет,
 * поэтому список пуст. Это ровно тот же путь, по которому пойдёт приложение
 * на обычном Android, и он должен вести к UNAVAILABLE, а не к падению.
 */
public class SensorManager {
    public List<Sensor> getSensorList(int type) { return new ArrayList<Sensor>(); }
    public boolean registerListener(SensorEventListener l, Sensor s, int rateUs) { return false; }
    public void unregisterListener(SensorEventListener l) { }
}
