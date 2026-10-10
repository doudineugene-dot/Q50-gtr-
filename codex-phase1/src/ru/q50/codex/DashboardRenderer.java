package ru.q50.codex;

import java.util.Locale;

/** Original vector scene; no legacy application source or raster dashboard assets. */
public final class DashboardRenderer {
    private static final int BG=0xff080b12, PANEL=0xff141a25, SILVER=0xffc3ccdb,
        MUTED=0xff8591a6, BLUE=0xff819aff, VIOLET=0xffac91f2, WHITE=0xffeef3ff;
    public int page=0;
    public boolean demo=false, diagnostics=true;
    private float scale=1, width=840, height=480;
    public static float scaleFor(int w,int h) { return Math.min(w/840f,h/480f); }
    public int hit(float px,float py,int w,int h) {
        float s=scaleFor(w,h), x=px/s,y=py/s,W=w/s,H=h/s;
        if (y>=H-58 && y<H-10 && x>=12 && x<W-12) return 1+(int)((x-12)/((W-24)/3));
        if (y>=8 && y<=44 && x>=W-172 && x<=W-68) return 4;
        if (y>=8 && y<=44 && x>=W-58 && x<=W-12) return 5;
        return 0;
    }
    public void draw(Surface out,int w,int h,DataHub hub,long now,String dimensions) {
        scale=scaleFor(w,h); width=w/scale; height=h/scale;
        Surface g=new Scaled(out,scale);
        g.rect(0,0,width,height,BG);
        g.rect(0,0,width,59,0xff101623);
        g.line(12,59,width-12,59,0xff303c55,1);
        g.text("Q50 GTR+",16,31,25,WHITE,false);
        g.text("CODEX / ФАЗА 1",184,30,12,MUTED,false);
        g.roundRect(width-172,8,104,36,7,demo?0xff53402a:0xff203f46);
        g.text(demo?"DEMO":"LIVE",width-120,32,18,demo?0xffffc979:0xff9cd9db,true);
        g.roundRect(width-58,8,46,36,7,diagnostics?0xff394265:PANEL);
        g.text("DIAG",width-35,31,12,WHITE,true);
        g.text(demo?"DEMO • СИНТЕТИЧЕСКИЕ ДАННЫЕ • НЕ ТЕЛЕМЕТРИЯ АВТОМОБИЛЯ":
            "LIVE • ИСТОЧНИКИ НЕ ПОДКЛЮЧЕНЫ • НЕТ ДАННЫХ",16,51,12,demo?0xffffc979:MUTED,false);
        float tileY=height-(diagnostics?184:132);
        float radius=Math.min((width-76)/4,(tileY-80)/2);
        float cy=68+radius;
        if (page==0) {
            gauge(g,width*.25f,cy,radius,"ОБОРОТЫ","rpm","об/мин",0,8000,8,hub,now);
            gauge(g,width*.75f,cy,radius,"НАДДУВ","boost","bar",-1,2,6,hub,now);
            tile(g,12,tileY,(width-60)/4,70,"АНТИФРИЗ","coolant","°C",hub,now);
            tile(g,24+(width-60)/4,tileY,(width-60)/4,70,"ТЕМП. МАСЛА","oil_temp","°C",hub,now);
            tile(g,36+(width-60)/2,tileY,(width-60)/4,70,"ДАВЛ. МАСЛА","oil_pressure","bar",hub,now);
            tile(g,48+3*(width-60)/4,tileY,(width-60)/4,70,"ВПУСК","iat","°C",hub,now);
        } else if (page==1) {
            gauge(g,width*.25f,cy,radius,"LPFP","lpfp","bar",0,10,5,hub,now);
            gauge(g,width*.75f,cy,radius,"HPFP","hpfp","bar",0,250,5,hub,now);
            float tw=(width-60)/4;
            tile(g,12,tileY,tw,70,"КОРРЕКЦИИ ST / LT","trims","%",hub,now);
            tile(g,24+tw,tileY,tw,70,"AFR","afr","",hub,now);
            tile(g,36+2*tw,tileY,tw,70,"ЦЕЛЬ НАДДУВА","boost_target","bar",hub,now);
            tile(g,48+3*tw,tileY,tw,70,"НАДДУВ ФАКТ","boost","bar",hub,now);
        } else {
            float tw=Math.min(210,width*.26f), gap=16;
            tile(g,gap,83,tw,78,"ПЕРЕД ЛЕВО / FL","air_fl","bar",hub,now);
            tile(g,width-tw-gap,83,tw,78,"ПЕРЕД ПРАВО / FR","air_fr","bar",hub,now);
            tile(g,gap,tileY-82,tw,78,"ЗАД ЛЕВО / RL","air_rl","bar",hub,now);
            tile(g,width-tw-gap,tileY-82,tw,78,"ЗАД ПРАВО / RR","air_rr","bar",hub,now);
            car(g,width/2,78,Math.min(170,tileY-90));
            tile(g,width/2-110,tileY,220,70,"РЕСИВЕР","air_tank","bar",hub,now);
            g.text(demo?"Air Lift: DEMO":"Air Lift: НЕ ПОДКЛЮЧЕН",width/2,tileY-8,13,MUTED,true);
        }
        if (diagnostics) {
            float dy=height-108;
            g.roundRect(12,dy,width-24,42,5,0xff1d2535);
            g.text("ДИАГНОСТИКА • "+dimensions,22,dy+16,12,SILVER,false);
            g.text("РЕЖИМ: "+(demo?"DEMO":"LIVE")+"  |  "+(demo?"DEMO source":"InTouch / EcuTek / Air Lift: UNAVAILABLE"),22,dy+33,12,MUTED,false);
        }
        String[] tabs={"ДВИГАТЕЛЬ","ТОПЛИВО","ШАССИ"};
        float tabW=(width-24)/3;
        for (int i=0;i<3;i++) {
            float x=12+i*tabW;
            g.roundRect(x,height-58,tabW-4,48,6,i==page?0xff283650:PANEL);
            if (i==page) g.rect(x+10,height-58,tabW-24,2,BLUE);
            g.text(tabs[i],x+(tabW-4)/2,height-28,18,i==page?WHITE:MUTED,true);
        }
    }
    private void gauge(Surface g,float cx,float cy,float r,String label,String key,String unit,
                       double min,double max,int steps,DataHub hub,long now) {
        DataHub.Sample sample=hub.read(key,now);
        g.circle(cx,cy,r,0xff3f485b,0); g.circle(cx,cy,r-2,SILVER,2);
        g.circle(cx,cy,r-7,0xff101520,0); g.circle(cx,cy,r-9,0xff586782,1);
        for (int i=0;i<=steps*5;i++) {
            double a=Math.toRadians(135+i*270.0/(steps*5));
            boolean major=i%5==0; float outer=r-16,inner=outer-(major?11:5);
            int color=i>steps*4?VIOLET:(major?SILVER:MUTED);
            g.line(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,
                cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,color,major?2:1);
            if (major) {
                double value=min+(max-min)*i/(steps*5);
                String tick=key.equals("rpm")?format(value/1000,0):format(value, key.equals("boost")?1:0);
                g.text(tick,cx+(float)Math.cos(a)*(r-36),cy+(float)Math.sin(a)*(r-36)+4,12,SILVER,true);
            }
        }
        if (sample.readable()) {
            double fraction=Math.max(0,Math.min(1,(sample.value-min)/(max-min)));
            double a=Math.toRadians(135+270*fraction);
            g.line(cx,cy,cx+(float)Math.cos(a)*(r-46),cy+(float)Math.sin(a)*(r-46),
                sample.quality==DataHub.Quality.STALE?MUTED:BLUE,3);
            g.circle(cx,cy,5,BLUE,0);
        }
        g.roundRect(cx-r*.52f,cy+18,r*1.04f,38,5,0xff080c14);
        g.text(sample.readable()?format(sample.value,key.equals("rpm")||key.equals("hpfp")?0:1):"—",
            cx,cy+46,29,sample.quality==DataHub.Quality.STALE?MUTED:WHITE,true);
        g.text(label+" / "+unit,cx,cy-8,12,SILVER,true);
        g.text(state(sample,now),cx,cy+72,11,MUTED,true);
    }
    private void tile(Surface g,float x,float y,float w,float h,String title,String key,String unit,DataHub hub,long now) {
        DataHub.Sample s=hub.read(key.equals("trims")?"stft":key,now);
        DataHub.Sample lt=hub.read("ltft",now);
        if (key.equals("trims") && !lt.readable()) s=lt;
        g.roundRect(x,y,w,h,7,PANEL); g.rect(x+8,y+8,2,h-16,s.readable()?BLUE:0xff455064);
        g.text(title,x+17,y+18,12,SILVER,false);
        String value=!s.readable()?"—":key.equals("trims")?format(s.value,1)+" / "+format(lt.value,1):
            format(s.value,key.equals("coolant")||key.equals("oil_temp")||key.equals("iat")?0:1);
        g.text(value+(s.readable()?" "+unit:""),x+17,y+43,key.equals("trims")?20:24,WHITE,false);
        g.text(state(s,now),x+17,y+h-8,10,MUTED,false);
    }
    private String state(DataHub.Sample s,long now) {
        if (s.quality==DataHub.Quality.UNAVAILABLE) return "НЕТ ДАННЫХ / UNAVAILABLE";
        if (s.quality==DataHub.Quality.ERROR) return "ОШИБКА / ERROR";
        if (s.quality==DataHub.Quality.STALE) return "УСТАРЕЛО / STALE "+((now-s.timestamp)/1000)+"s";
        return (s.source==DataHub.Source.DEMO?"DEMO":s.source.toString())+" / VALID";
    }
    private String format(double n,int decimals) { return String.format(Locale.US,decimals==0?"%.0f":"%.1f",n); }
    private void car(Surface g,float cx,float y,float h) {
        float k=h/190, w=62*k;
        g.roundRect(cx-w,y,2*w,h,26*k,0xff384558);
        g.roundRect(cx-w+4*k,y+4*k,2*w-8*k,h-8*k,22*k,0xff1c2738);
        g.roundRect(cx-44*k,y+32*k,88*k,40*k,12*k,0xff7194b1);
        g.roundRect(cx-42*k,y+112*k,84*k,29*k,8*k,0xff506e87);
        g.roundRect(cx-45*k,y+76*k,90*k,32*k,7*k,0xff303e51);
        g.line(cx-w+12*k,y+19*k,cx+w-12*k,y+19*k,SILVER,2);
        g.line(cx-w+12*k,y+h-16*k,cx+w-12*k,y+h-16*k,VIOLET,3);
        for (int i=0;i<2;i++) {
            float ty=y+(i==0?43:137)*k;
            g.roundRect(cx-w-8*k,ty,12*k,28*k,3*k,0xff607087);
            g.roundRect(cx+w-4*k,ty,12*k,28*k,3*k,0xff607087);
        }
    }
    private static final class Scaled implements Surface {
        private final Surface target; private final float s;
        Scaled(Surface t,float value) { target=t;s=value; }
        public void rect(float x,float y,float w,float h,int c){target.rect(x*s,y*s,w*s,h*s,c);}
        public void roundRect(float x,float y,float w,float h,float r,int c){target.roundRect(x*s,y*s,w*s,h*s,r*s,c);}
        public void circle(float x,float y,float r,int c,float st){target.circle(x*s,y*s,r*s,c,st*s);}
        public void line(float x,float y,float xx,float yy,int c,float st){target.line(x*s,y*s,xx*s,yy*s,c,st*s);}
        public void text(String t,float x,float y,float z,int c,boolean center){target.text(t,x*s,y*s,z*s,c,center);}
    }
}
