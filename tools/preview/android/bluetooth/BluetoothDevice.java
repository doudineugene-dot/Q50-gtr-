package android.bluetooth;

import java.util.UUID;

public class BluetoothDevice {
    public static final String ACTION_FOUND = "android.bluetooth.device.action.FOUND";
    public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
    public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";
    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;

    public String getName() { return null; }
    public String getAddress() { return null; }
    public int getBondState() { return BOND_NONE; }
    public BluetoothClass getBluetoothClass() { return null; }
    public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) { return null; }
}
