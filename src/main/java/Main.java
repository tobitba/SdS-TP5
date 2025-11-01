import engine.Beeman;
import engine.Silo;
import engine.Time;
import tools.ParticleGenerator;
import tools.PostProcessor;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final String W = "W";
    private static final String D = "D";
    private static final String OUT = "out";
    private static final double SMOOTHING_FACTOR = 10;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        double w = Double.parseDouble(System.getProperty(W));
        double d = Double.parseDouble(System.getProperty(D));
        String output_file = System.getProperty(OUT);
        double dt = 0.0001;
        double height = 0.7;
        double width = 0.2;
        double mass = 0.001;
        double neighborRadius = 0.001;
        double maxParRadius = 0.011;
        AtomicInteger i = new AtomicInteger(0);
        Silo silo = new Silo(
                width, height, d, w, 0.0015, dt, 250, neighborRadius, maxParRadius
        );
        ParticleGenerator.generate(
                200, silo::addParticle, height, width, 0.009, maxParRadius
        );
        Beeman integrator = new Beeman(dt, 1000, silo, mass);
        Iterator<Time> timeIt = integrator.beemanEstimation();
        try (PostProcessor postProcessor = new PostProcessor(output_file)) {
            timeIt.forEachRemaining(time -> {
                if (i.getAndIncrement() % (1 / (SMOOTHING_FACTOR * dt)) == 0) {
                    postProcessor.processSystem(time);
                    System.out.println(time.time());
                }
            });
        }

    }
}
