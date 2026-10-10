package ru.q50.codex;

/** Drawing primitives: Android Canvas on-device; Java2D for exact host render captures. */
public interface Surface {
    void rect(float x,float y,float w,float h,int color);
    void roundRect(float x,float y,float w,float h,float r,int color);
    void circle(float x,float y,float r,int color,float stroke);
    void line(float x1,float y1,float x2,float y2,int color,float stroke);
    void text(String text,float x,float y,float size,int color,boolean centered);
}
