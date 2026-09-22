package android.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothSocket {
    public void connect() throws IOException { throw new IOException("нет Bluetooth"); }
    public InputStream getInputStream() throws IOException { throw new IOException("нет"); }
    public OutputStream getOutputStream() throws IOException { throw new IOException("нет"); }
    public void close() throws IOException { }
}
