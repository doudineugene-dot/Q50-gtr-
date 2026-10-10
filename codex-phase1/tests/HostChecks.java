import ru.q50.codex.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class HostChecks {
    private static int checks=0;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static DataHub.Sample sample(double v,long t,DataHub.Source s){return new DataHub.Sample(v,t,s,DataHub.Quality.VALID);}
    public static void main(String[] args)throws Exception {
        DataHub live=new DataHub(false),demo=new DataHub(true);
        check(live.read("rpm",100).quality==DataHub.Quality.UNAVAILABLE,"LIVE starts empty");
        try{new DemoSource().update(live,100);throw new AssertionError("Demo leaked");}catch(IllegalArgumentException expected){checks++;}
        try{demo.publish("rpm",sample(10,100,DataHub.Source.INTOUCH));throw new AssertionError("Live leaked");}catch(IllegalArgumentException expected){checks++;}
        live.publish("rpm",sample(1000,1000,DataHub.Source.INTOUCH));
        live.publish("rpm",sample(2000,500,DataHub.Source.ECUTEK));
        check(live.read("rpm",1000).value==2000,"EcuTek priority");
        check(live.read("rpm",2600).value==1000,"Valid stock fallback beats stale preferred");
        check(live.read("rpm",4000).quality==DataHub.Quality.STALE,"Stale marked");
        check(live.read("rpm",12000).quality==DataHub.Quality.UNAVAILABLE,"Expired cleared");
        live.publish("rpm",sample(9999,0,DataHub.Source.INTOUCH));
        check(live.read("rpm",1000).value==2000,"Out-of-order ignored");
        live.publish("air_fl",sample(8,1000,DataHub.Source.ECUTEK));
        check(!live.read("air_fl",1000).readable(),"Air pressure restricted to AirLift");
        live.publish("knock",sample(8,1000,DataHub.Source.INTOUCH));
        check(!live.read("knock",1000).readable(),"Knock EcuTek only");
        live.publish("afr",new DataHub.Sample(Double.NaN,1000,DataHub.Source.ECUTEK,DataHub.Quality.ERROR));
        check(live.read("afr",1000).quality==DataHub.Quality.ERROR,"Error state");
        check(live.read("rpm",1).quality==DataHub.Quality.UNAVAILABLE,"Future sample invalid");
        try{live.publish("rpm",sample(Double.NaN,2000,DataHub.Source.INTOUCH));throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){checks++;}
        new DemoSource().update(demo,5000);
        check(!demo.read("lpfp",5000).readable(),"LPFP remains unavailable in DEMO");
        check(new DataHub(false).read("boost",5000).quality==DataHub.Quality.UNAVAILABLE,"Switching to LIVE clears demo display");
        int[][] sizes={{840,480},{640,480},{1280,720},{1024,600}};
        File out=new File(args[0]);out.mkdirs();
        for(int[] size:sizes)for(int mode=0;mode<2;mode++)for(int page=0;page<3;page++) {
            DashboardRenderer scene=new DashboardRenderer();scene.page=page;scene.demo=mode==1;
            Java2DSurface surface=new Java2DSurface(size[0],size[1]);
            scene.draw(surface,size[0],size[1],scene.demo?demo:new DataHub(false),5000,
                "HOST RENDER / "+size[0]+"x"+size[1]+" / FPS: N/A");
            ImageIO.write(surface.image,"png",new File(out,(scene.demo?"demo":"live")+"-"+page+"-"+size[0]+"x"+size[1]+".png"));
            float s=DashboardRenderer.scaleFor(size[0],size[1]),W=size[0]/s,H=size[1]/s;
            check(scene.hit((W-120)*s,26*s,size[0],size[1])==4,"Mode touch");
            check(scene.hit((W-35)*s,26*s,size[0],size[1])==5,"Diag touch");
            for(int tab=0;tab<3;tab++)check(scene.hit((12+(W-24)/3*(tab+.5f))*s,(H-28)*s,size[0],size[1])==tab+1,"Tab touch");
            check(scene.hit(5*s,100*s,size[0],size[1])==0,"Content does not toggle controls");
            check(surface.circles>= (page==2?0:8),"Gauges drawn as circles");
            scene.diagnostics=false;scene.draw(surface,size[0],size[1],scene.demo?demo:new DataHub(false),5000,"");
        }
        System.out.println("PASS: "+checks+" checks; 24 captures of production scene (Java2D host, not DCU screenshots)");
    }
    static final class Java2DSurface implements Surface {
        final BufferedImage image; final Graphics2D g; int circles=0;
        Java2DSurface(int w,int h){image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
        void color(int c){g.setColor(new Color(c,true));}
        public void rect(float x,float y,float w,float h,int c){color(c);g.fill(new Rectangle2D.Float(x,y,w,h));}
        public void roundRect(float x,float y,float w,float h,float r,int c){color(c);g.fill(new RoundRectangle2D.Float(x,y,w,h,r*2,r*2));}
        public void circle(float x,float y,float r,int c,float st){circles++;color(c);Shape sh=new Ellipse2D.Float(x-r,y-r,r*2,r*2);if(st==0)g.fill(sh);else{g.setStroke(new BasicStroke(st));g.draw(sh);}}
        public void line(float x,float y,float xx,float yy,int c,float st){color(c);g.setStroke(new BasicStroke(st));g.draw(new Line2D.Float(x,y,xx,yy));}
        public void text(String t,float x,float y,float z,int c,boolean center){color(c);g.setFont(new Font("SansSerif",Font.PLAIN,Math.round(z)));g.drawString(t,center?x-g.getFontMetrics().stringWidth(t)/2f:x,y);}
    }
}
