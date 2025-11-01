package engine;

import java.util.ArrayList;
import java.util.List;

public class Particle {
    public static final int DIMENSION = 2;
    private static int globalId = 0;
    private final int id;
    double x;
    double y;
    double speedx = 0, speedy = 0;
    final double radius;
    final double[] contactForce = {0.0, 0.0};
    final List<Particle> neighbors = new ArrayList<>();

    public Particle(double x, double y, double radius) {
        this(x, y, radius, globalId++);
    }

    public Particle(double x, double y, double radius, int id) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void updatePos(double[] newPos) {
        x = newPos[0];
        y = newPos[1];
    }

    public void updateSpeed(double[] newSpeed) {
        speedx = newSpeed[0];
        speedy = newSpeed[1];
    }

    public double[] getPos(){
        return new double[]{x, y};
    }

    public double[] getSpeed(){
        return new double[]{speedx, speedy};
    }

    public double getDistance(Particle p) {
        return Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2)) - radius - p.radius;
    }

    public void addNeighbor(Particle p) {
        neighbors.add(p);
    }

    public void resetContactForce() {
        neighbors.clear();
        contactForce[0] = 0.0;
        contactForce[1] = 0.0;
    }

    @Override
    public String toString() {
        return "%d: x=%.2f y=%.2f spx=%.2f spy=%.2f".formatted(getId(), x, y, speedx, speedy);
    }

    public String csvString() {
        return "%.8f,%.8f,%.8f,%.8f,%.8f".formatted(x, y, speedx, speedy, radius);
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Particle p && id == p.id;
    }

    public double getRadius() {
        return radius;
    }
}
