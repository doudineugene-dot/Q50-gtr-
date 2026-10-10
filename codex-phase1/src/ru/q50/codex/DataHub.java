package ru.q50.codex;

import java.util.HashMap;
import java.util.Map;

/** All timestamps use monotonic elapsed milliseconds, never wall clock. */
public final class DataHub {
    public enum Quality { VALID, STALE, UNAVAILABLE, ERROR }
    public enum Source { INTOUCH, ECUTEK, AIRLIFT, DEMO, NONE }
    public static final long FRESH_MS = 2000, EXPIRE_MS = 10000;
    public static final class Sample {
        public final double value;
        public final long timestamp;
        public final Source source;
        public final Quality quality;
        public Sample(double v, long t, Source s, Quality q) {
            value=v; timestamp=t; source=s; quality=q;
        }
        public boolean readable() { return quality==Quality.VALID || quality==Quality.STALE; }
    }
    private final boolean demo;
    private final Map<String, Sample> samples = new HashMap<String, Sample>();
    public DataHub(boolean isDemo) { demo=isDemo; }
    public synchronized void publish(String channel, Sample sample) {
        if (sample.source==Source.NONE || (sample.source==Source.DEMO)!=demo)
            throw new IllegalArgumentException("Source cannot cross DEMO/LIVE boundary");
        if (sample.quality==Quality.VALID && (Double.isNaN(sample.value) || Double.isInfinite(sample.value)))
            throw new IllegalArgumentException("Nonfinite telemetry");
        String key=channel+":"+sample.source;
        Sample previous=samples.get(key);
        if (previous==null || sample.timestamp>=previous.timestamp) samples.put(key,sample);
    }
    private Sample aged(Sample sample, long now) {
        if (sample==null) return new Sample(Double.NaN,0,Source.NONE,Quality.UNAVAILABLE);
        if (!sample.readable()) return sample;
        long age=now-sample.timestamp;
        Quality q=age<0 || age>EXPIRE_MS ? Quality.UNAVAILABLE :
            age>FRESH_MS ? Quality.STALE : sample.quality;
        return new Sample(sample.value,sample.timestamp,sample.source,q);
    }
    public synchronized Sample read(String channel,long now) {
        Source[] order=demo ? new Source[]{Source.DEMO} :
            channel.startsWith("air_") ? new Source[]{Source.AIRLIFT} :
            channel.equals("rpm") || channel.equals("speed") ? new Source[]{Source.ECUTEK,Source.INTOUCH} :
            channel.equals("knock") ? new Source[]{Source.ECUTEK} : new Source[]{Source.ECUTEK,Source.INTOUCH};
        Sample best=new Sample(Double.NaN,0,Source.NONE,Quality.UNAVAILABLE);
        for (int i=0;i<order.length;i++) {
            Sample s=aged(samples.get(channel+":"+order[i]),now);
            if (s.quality==Quality.VALID) return s;
            if (best.source==Source.NONE && s.source!=Source.NONE) best=s;
            if (s.quality==Quality.STALE && best.quality!=Quality.STALE) best=s;
            else if (s.quality==Quality.ERROR && best.quality==Quality.UNAVAILABLE) best=s;
        }
        return best;
    }
}
