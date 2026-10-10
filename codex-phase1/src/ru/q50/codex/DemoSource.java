package ru.q50.codex;

/** Synthetic samples are physically confined to a separate DEMO DataHub. */
public final class DemoSource {
    public void update(DataHub hub,long now) {
        double wave=(1+Math.sin(now/1800.0))/2;
        put(hub,"rpm",900+wave*5200,now); put(hub,"boost",-.6+wave*2.1,now);
        put(hub,"coolant",88+wave*5,now); put(hub,"oil_temp",97+wave*8,now);
        put(hub,"oil_pressure",2.5+wave*2,now); put(hub,"iat",31+wave*9,now);
        put(hub,"hpfp",55+wave*150,now); put(hub,"stft",-2.4+wave*4,now);
        put(hub,"ltft",1.8,now); put(hub,"afr",14.7-wave*2.5,now);
        put(hub,"boost_target",.2+wave*1.4,now);
        put(hub,"air_fl",5.2,now); put(hub,"air_fr",5.3,now);
        put(hub,"air_rl",4.6,now); put(hub,"air_rr",4.7,now); put(hub,"air_tank",9.4,now);
        // LPFP remains UNAVAILABLE: no proven pressure channel, including in DEMO.
    }
    private void put(DataHub hub,String key,double value,long now) {
        hub.publish(key,new DataHub.Sample(value,now,DataHub.Source.DEMO,DataHub.Quality.VALID));
    }
}
