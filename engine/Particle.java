package engine;

public class Particle {
    public static final int DIMENSION = 2;
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx = 0, speedy = 0;
    private final double radius;

    public Particle(double x, double y, double radius) {
        this.id = globalId++;
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

    public double getRadius() {
        return radius;
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
}
