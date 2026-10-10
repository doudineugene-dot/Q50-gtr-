package ru.q50.codex;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Canvas;

public final class MainActivity extends Activity {
    private DashboardView dashboard;
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dashboard=new DashboardView();
        if (state!=null) {
            dashboard.scene.page=Math.max(0,Math.min(2,state.getInt("page")));
            dashboard.scene.demo=state.getBoolean("demo");
            dashboard.scene.diagnostics=state.getBoolean("diagnostics",true);
        }
        setContentView(dashboard);
    }
    protected void onResume(){super.onResume();dashboard.active=true;dashboard.start();dashboard.invalidate();}
    protected void onPause(){dashboard.active=false;dashboard.stop();super.onPause();}
    protected void onSaveInstanceState(Bundle out){
        out.putInt("page",dashboard.scene.page);out.putBoolean("demo",dashboard.scene.demo);
        out.putBoolean("diagnostics",dashboard.scene.diagnostics);super.onSaveInstanceState(out);
    }
    private final class DashboardView extends View {
        final DashboardRenderer scene=new DashboardRenderer();
        final CanvasSurface surface=new CanvasSurface();
        final DataHub live=new DataHub(false),demo=new DataHub(true);
        final DemoSource generator=new DemoSource();
        final TelemetrySource[] sources={new InTouchVehicleSource(),new EcuTekSource(),new AirLiftSource()};
        boolean active=false;
        long fpsStart=SystemClock.elapsedRealtime(); int frames=0;float fps=0;
        DashboardView(){super(MainActivity.this);setFocusable(true);}
        void start(){fpsStart=SystemClock.elapsedRealtime();frames=0;for(int i=0;i<sources.length;i++)sources[i].start(live);}
        void stop(){for(int i=0;i<sources.length;i++)sources[i].stop();}
        protected void onSizeChanged(int w,int h,int ow,int oh){
            super.onSizeChanged(w,h,ow,oh);Log.i("Q50Codex","view="+w+"x"+h);
        }
        protected void onDraw(Canvas canvas) {
            long now=SystemClock.elapsedRealtime(); frames++;
            if(now-fpsStart>=1000){fps=frames*1000f/(now-fpsStart);frames=0;fpsStart=now;}
            if(scene.demo)generator.update(demo,now);
            DisplayMetrics display=new DisplayMetrics();getWindowManager().getDefaultDisplay().getMetrics(display);
            View window=getWindow().getDecorView();
            String dimensions="screen "+display.widthPixels+"x"+display.heightPixels+
                "  window "+window.getWidth()+"x"+window.getHeight()+
                "  view "+getWidth()+"x"+getHeight()+"  canvas "+canvas.getWidth()+"x"+canvas.getHeight()+
                "  FPS "+(int)(fps+.5f);
            surface.bind(canvas);scene.draw(surface,getWidth(),getHeight(),scene.demo?demo:live,now,dimensions);
            if(active)postInvalidateDelayed(100);
        }
        public boolean onTouchEvent(MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_UP) {
                int hit=scene.hit(event.getX(),event.getY(),getWidth(),getHeight());
                if(hit>=1 && hit<=3)scene.page=hit-1;
                if(hit==4)scene.demo=!scene.demo;
                if(hit==5)scene.diagnostics=!scene.diagnostics;
                Log.i("Q50Codex","page="+scene.page+" mode="+(scene.demo?"DEMO":"LIVE")+" diag="+scene.diagnostics);
                invalidate();
            }
            return true;
        }
    }
}
