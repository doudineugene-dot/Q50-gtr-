package android.graphics;

/** Заглушка Android-API для офлайн-рендера. В APK не попадает. */
public class Shader {
    public enum TileMode { CLAMP, REPEAT, MIRROR }
    public java.awt.Paint awt() { return null; }
}
