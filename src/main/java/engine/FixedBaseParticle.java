package engine;

public class FixedBaseParticle extends Particle{

    public FixedBaseParticle(double x, double y) {
        super(x, y, 0, 10000);
    }

    public void updatePos(double y) {
        this.y = y;
    }

    @Override
    public void updateSpeed(double[] newSpeed) {
        this.speedy =  newSpeed[1];
    }
}
