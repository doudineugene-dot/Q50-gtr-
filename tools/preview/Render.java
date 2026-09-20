import android.content.Context;
import android.graphics.Canvas;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.ui.DashboardView;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

/**
 * Рендерит все три вкладки в PNG 840x480 тем же кодом, который идёт в APK.
 * Android-классы подменены тонким слоем поверх Java2D, но Theme, Gauges,
 * Icons, Q50Rear и сами страницы используются настоящие.
 */
public final class Render {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : ".";
        DataHub hub = new DataHub();
        hub.start();
        // Прокручиваем демо-данные, чтобы приборы стояли не на нуле.
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 40; i++) {
            hub.poll(t0 + i * 100L);
        }
        VehicleData d = hub.getData();
        System.out.println("RPM=" + d.rpm.text(0) + "  boost=" + d.boostActual.text(2)
                + "  coolant=" + d.coolantTemp.text(0) + "  LPFP=" + d.lpfp.text(1));

        DashboardView view = new DashboardView(new Context(), hub);
        Method onDraw = DashboardView.class.getDeclaredMethod("onDraw", Canvas.class);
        onDraw.setAccessible(true);

        String[] names = {"preview-1-engine.png", "preview-2-fuel.png", "preview-3-chassis.png"};
        for (int page = 0; page < 3; page++) {
            view.setPage(page);
            BufferedImage img = new BufferedImage(840, 480, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            Canvas c = new Canvas(g);
            onDraw.invoke(view, c);
            g.dispose();
            File f = new File(out, names[page]);
            ImageIO.write(img, "png", f);
            System.out.println("написано " + f.getPath());
        }
    }
}
