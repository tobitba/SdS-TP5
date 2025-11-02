package engine;

import java.util.Iterator;

public class Beeman {
    private final Silo silo;
    private final double maxTime;
    private final double dt;
    private final double dts;
    private final double mass;

    public Beeman(double dt, double maxTime, Silo silo, double mass) {
        this.maxTime = maxTime;
        this.dt = dt;
        this.dts = dt*dt;
        this.silo = silo;
        this.mass = mass;
    }

    public Iterator<Time> beemanEstimation() {
        return new BeemanIterator();
    }

    private class BeemanIterator implements Iterator<Time> {
        private double time;
        private final double[][] currentSpeed;
        private double[][] prevForceMatrix;

        public BeemanIterator() {
            time = 0;
            currentSpeed = new double[silo.grainCount()][Particle.DIMENSION];
            prevForceMatrix = silo.getForceMatrix();
        }

        @Override
        public boolean hasNext() {
            return time <= maxTime;
        }

        @Override
        public Time next() {
            silo.updateBase();
            double[][] forceMatrix = silo.getForceMatrix();
            for (Particle p : silo.grains()) {
                double[] forceArray = forceMatrix[p.getId()];
                double[] posArray = p.getPos();
                double[] speedArray = p.getSpeed();
                double[] newSpeed = new double[Particle.DIMENSION];
                double[] newPos = new double[Particle.DIMENSION];
                for (int i = 0; i < Particle.DIMENSION; i++) {
                    double pos = posArray[i];
                    double speed = speedArray[i];
                    currentSpeed[p.getId()][i] = speed * 0.9625;
                    double force = forceArray[i];
                    double prevForce = prevForceMatrix[p.getId()][i];
                    newPos[i] = pos + speed * dt + 2.0 * dts * (force / (3.0 * mass)) - dts * prevForce / (6.0 * mass);
                    newSpeed[i] = speed + 3.0 * dt * force / (2.0 * mass) - dt * prevForce / (2.0 * mass); // Este es el predictedVel
                }
                p.updatePos(newPos);
                p.updateSpeed(newSpeed);
            }
            double[][] nextForceMatrix = silo.getForceMatrix(); // Get the forces using the newPos and newPredSpeed
            for (Particle p : silo.grains()) {
                double[] newSpeed = new double[Particle.DIMENSION];
                double[] nextForceArray = nextForceMatrix[p.getId()];
                double[] forceArray = forceMatrix[p.getId()];
                double[] prevForceArray = prevForceMatrix[p.getId()];
                for (int i = 0; i < Particle.DIMENSION; i++) {
                    double speed = currentSpeed[p.getId()][i];
                    double nextSpeed = speed + dt * nextForceArray[i] / (3.0 * mass) + 5.0 * dt * forceArray[i] / (6.0 * mass) - dt * prevForceArray[i] / (6.0 * mass);
                    newSpeed[i] = nextSpeed;
                }
                p.updateSpeed(newSpeed);
            }
            time += dt;
            prevForceMatrix = forceMatrix;
            return new Time(time, silo.grains(), silo.totalFlow());
        }
    }
}
