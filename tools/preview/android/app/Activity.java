package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/** Подмена Activity для офлайн-сборки. */
public class Activity extends Context {
    private final Window window = new Window();

    protected void onCreate(Bundle b) { }
    protected void onResume() { }
    protected void onPause() { }
    protected void onSaveInstanceState(Bundle b) { }
    public boolean requestWindowFeature(int f) { return true; }
    public Window getWindow() { return window; }
    public WindowManager getWindowManager() { return new WindowManager(); }
    public void setContentView(android.view.View v) { }
}
