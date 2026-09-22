package android.bluetooth;

import java.util.Collections;
import java.util.Set;

/** Подмена: вне ГУ Bluetooth-адаптера нет, путь обязан вести к DISABLED. */
public class BluetoothAdapter {
    public static final String ACTION_DISCOVERY_FINISHED =
            "android.bluetooth.adapter.action.DISCOVERY_FINISHED";

    public static BluetoothAdapter getDefaultAdapter() { return null; }
    public String getName() { return null; }
    public String getAddress() { return null; }
    public boolean isEnabled() { return false; }
    public boolean isDiscovering() { return false; }
    public boolean startDiscovery() { return false; }
    public boolean cancelDiscovery() { return false; }
    public Set<BluetoothDevice> getBondedDevices() { return Collections.emptySet(); }
    public BluetoothDevice getRemoteDevice(String address) { return null; }
}
