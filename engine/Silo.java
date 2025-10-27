package engine;

import java.util.List;

public class Silo {
    private final double width;
    private final double height;
    private final double opening;

    private final List<Particle> grains;
    private final int grainCount;
    private final double amplitude = 0.15;
    private final double frequency;

    private final double kn = 2.5;
    private final double ky = 2*kn;

    public Silo(double width, double height, double opening, List<Particle> grains, double frequency) {
        this.width = width;
        this.height = height;
        this.opening = opening;
        this.grains = grains;
        this.grainCount = grains.size();
        this.frequency = frequency;
    }

    public int grainCount() {
        return grainCount;
    }

    public List<Particle> grains() {
        return grains;
    }

    public double[][] getForceMatrix() {
        return new double[][] {};
    }
}
