package ru.q50.codex;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

public final class CanvasSurface implements Surface {
    private Canvas canvas;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box=new RectF();
    public void bind(Canvas c) { canvas=c; }
    private void setup(int color,float stroke) {
        paint.setColor(color); paint.setStyle(stroke==0?Paint.Style.FILL:Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
    }
    public void rect(float x,float y,float w,float h,int c){setup(c,0);canvas.drawRect(x,y,x+w,y+h,paint);}
    public void roundRect(float x,float y,float w,float h,float r,int c){setup(c,0);box.set(x,y,x+w,y+h);canvas.drawRoundRect(box,r,r,paint);}
    public void circle(float x,float y,float r,int c,float st){setup(c,st);canvas.drawCircle(x,y,r,paint);}
    public void line(float x,float y,float xx,float yy,int c,float st){setup(c,st);canvas.drawLine(x,y,xx,yy,paint);}
    public void text(String t,float x,float y,float z,int c,boolean center){
        setup(c,0);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(z);
        paint.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);canvas.drawText(t,x,y,paint);
    }
}
