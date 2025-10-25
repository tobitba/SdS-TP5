package engine;

import java.util.List;

public class Silo {
    private final double width;
    private final double height;
    private final double opening;

    private final List<Particle> grains;
    private final double amplitude = 0.15;
    private final double frequency;

    private double currentTime;
    private final double maxTime;
    private final double dt;

    private final double kn = 2.5;
    private final double ky = 2*kn;
}
