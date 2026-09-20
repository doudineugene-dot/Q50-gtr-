package com.q50gtr.plus;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import android.util.Log;

import com.q50gtr.plus.data.AirLiftLiveSource;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.DemoDataProvider;
import com.q50gtr.plus.data.EcuTekLiveSource;
import com.q50gtr.plus.data.InTouchVehicleSource;
import com.q50gtr.plus.data.VehicleProbe;
import com.q50gtr.plus.ui.DashboardView;

/**
 * Q50 GTR+ — a single full-screen instrument panel.
 *
 * No fragments, no support library, no layout XML: the activity owns a data hub
 * and one custom View, which is all a factory head unit should ever be asked
 * to run.
 */
public final class MainActivity extends Activity {

    private static final String STATE_PAGE = "page";
    private static final String TAG = "Q50GTR";

    private DataHub hub;
    private DashboardView dashboard;
    private VehicleProbe probe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // The theme is already fullscreen; all the window needs from us is to
        // stay awake while the car is running.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Сначала зонд: на незнакомой прошивке важно записать факты до того,
        // как что-то подключать. Он только читает.
        probe = new VehicleProbe();
        try {
            probe.run(this);
        } catch (Throwable t) {
            Log.w(TAG, "зонд упал: " + t);
        }

        hub = new DataHub(new InTouchVehicleSource(this), new EcuTekLiveSource(),
                new AirLiftLiveSource(), new DemoDataProvider());
        dashboard = new DashboardView(this, hub);
        dashboard.setProbe(probe);
        setContentView(dashboard);

        if (savedInstanceState != null) {
            dashboard.setPage(savedInstanceState.getInt(STATE_PAGE, 0));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hub.start();
        dashboard.start();
    }

    @Override
    protected void onPause() {
        dashboard.stop();
        hub.stop();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, dashboard.getPage());
    }
}
