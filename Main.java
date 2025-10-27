import engine.*;
import tools.ParticleGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final String W = "W";
    private static final String D = "D";
    private static final double SMOOTHING_FACTOR = 10;

    public static void main(String[] args) {
        double w = Double.parseDouble(W);
        double d = Double.parseDouble(D);
        double dt = 0.0001;
        double height = 70;
        double width = 20;
        double mass = 1;
        AtomicInteger i = new AtomicInteger(0);
        ArrayList<Particle> grains = new ArrayList<>();
        ParticleGenerator.generate(200,grains::add,height,width);
        //250N/m = 2,5N/cm
        Silo silo = new Silo(width,height,d,grains,w,0.15,dt, mass, 2.5);
        Beeman integrator = new Beeman(dt,1000,silo,mass);
        Iterator<Time> timeIt = integrator.beemanEstimation();
        timeIt.forEachRemaining(time -> {
            if(i.getAndIncrement() % (1/(SMOOTHING_FACTOR*dt)) == 0)
                //PostPocessor.processSystem(time);
                System.out.println(time);
        });
    }
}
