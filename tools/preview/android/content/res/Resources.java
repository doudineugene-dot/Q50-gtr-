package android.content.res;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Подмена Resources: getIdentifier ищет файл в res/drawable-nodpi на диске,
 * как aapt нашёл бы его в APK.
 */
public final class Resources {
    private static final List<File> FILES = new ArrayList<File>();

    public int getIdentifier(String name, String type, String pkg) {
        File f = new File("res/drawable-nodpi/" + name + ".png");
        if (!f.isFile()) {
            return 0;
        }
        FILES.add(f);
        return FILES.size();
    }

    public File fileFor(int id) {
        return id >= 1 && id <= FILES.size() ? FILES.get(id - 1) : null;
    }
}
