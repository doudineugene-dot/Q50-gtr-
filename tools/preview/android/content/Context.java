package android.content;

public class Context { 
    private final android.content.res.Resources res = new android.content.res.Resources();

    public android.content.res.Resources getResources() { return res; }

    public String getPackageName() { return "com.q50gtr.plus"; }
}
