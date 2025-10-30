import engine.*;
import tools.ParticleGenerator;
import tools.PostProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final String W = "W";
    private static final String D = "D";
    private static final double SMOOTHING_FACTOR = 10;

    public static void main(String[] args) {
        double w = Double.parseDouble(System.getProperty(W));
        double d = Double.parseDouble(System.getProperty(D));
        double dt = 0.0001;
        double height = 0.7;
        double width = 0.2;
        double mass = 0.001;
        AtomicInteger i = new AtomicInteger(0);
        ArrayList<Particle> grains = new ArrayList<>();
        ParticleGenerator.generate(200,grains::add,height,width);
        Silo silo = new Silo(width,height,d,grains,w,0.0015,dt, 250);
        Beeman integrator = new Beeman(dt,1000,silo,mass);
        Iterator<Time> timeIt = integrator.beemanEstimation();
        timeIt.forEachRemaining(time -> {
            if(i.getAndIncrement() % (1/(SMOOTHING_FACTOR*dt)) == 0)
                PostProcessor.processSystem(time);
        });
    }
}
