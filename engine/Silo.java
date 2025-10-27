package engine;

import java.util.List;

public class Silo {
    private final double width;
    private final double height;
    private final double opening;
    private double ys;

    private final List<Particle> grains;
    private final int grainCount;
    private final double mass;
    private final double amplitude;
    private final double frequency;
    private double currentTime;
    private final double dt;
    private final double kn;
    private final double ky;

    public Silo(double width, double height, double opening, List<Particle> grains, double frequency, double amplitude, double dt, double mass, double kn) {
        this.width = width;
        this.height = height;
        this.opening = opening;
        this.grains = grains;
        this.grainCount = grains.size();
        this.frequency = frequency;
        this.dt = dt;
        ys = 0;
        currentTime = 0;
        this.amplitude = amplitude;
        this.mass = mass;
        this.kn = kn;
        this.ky = 2*kn;
    }

    public void updateBase() {
        currentTime += dt;
        ys = amplitude * Math.sin(currentTime*frequency);
    }

    public int grainCount() {
        return grainCount;
    }

    public List<Particle> grains() {
        return grains;
    }

    public double[][] getForceMatrix() {
        return null;
    }
}
