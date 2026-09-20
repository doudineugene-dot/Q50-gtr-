package android.content.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Offline-заглушка: читает файлы из каталога assets/ проекта. */
public class AssetManager {
    public static File root = new File("assets");

    public InputStream open(String path) throws IOException {
        File f = new File(root, path);
        if (!f.isFile()) {
            throw new IOException("asset not found: " + f.getPath());
        }
        return new FileInputStream(f);
    }

    public File file(String path) { return new File(root, path); }
}
