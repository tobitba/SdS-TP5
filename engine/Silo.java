package engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Silo {
    private final double width;
    private final double height;
    private final double opening;
    private double ys;
    private long totalFlow;
    //Gamma puede ser 1 o 0.1
    private final double gamma = 0.1;
    private final double mu = 0.5;

    private final List<Particle> grains;
    private final FixedBaseParticle leftBoundaryParticle;
    private final FixedBaseParticle rightBoundaryParticle;
    private final double amplitude;
    private final double frequency;
    private double currentTime;
    private final double dt;
    private final double kn;
    private final double ky;
    private final static int X = 0;
    private final static int Y = 1;

    private final int M;
    private final int N;
    private final double neighborRadius;
    private final double vCellLength;
    private final double hCellLength;
    private final List<List<Particle>> grid;

    public Silo(double width, double height, double opening, double frequency, double amplitude, double dt, double kn, double neighborRadius, double maxParRadius) {
        this.width = width;
        this.height = height;
        this.opening = opening;
        this.frequency = frequency;
        this.dt = dt;
        ys = 0;
        currentTime = 0;
        this.amplitude = amplitude;
        this.kn = kn;
        this.ky = 2 * kn;
        this.leftBoundaryParticle = new FixedBaseParticle((width - opening) / 2, 0);
        this.rightBoundaryParticle = new FixedBaseParticle(width - (width - opening) / 2, 0);

        this.M = (int) Math.round(Math.ceil(height / (neighborRadius + 2 * maxParRadius) - 1));
        this.N = (int) Math.round(Math.ceil(width / (neighborRadius + 2 * maxParRadius) - 1));
        this.neighborRadius = neighborRadius;
        this.vCellLength = height / M;
        this.hCellLength = width / N;

        this.grains = new ArrayList<>();
        this.grid = new ArrayList<>();
        for (int i = 0; i < M * N; i++) {
            grid.add(new ArrayList<>());
        }
    }

    /**
     * Particles on horizontal cell borders go to the upper cell,
     * and particles on vertical cell borders go to the right cell.
     */
    public void addParticle(Particle particle) {
        addParticleToGrid(particle);
        grains.add(particle);
    }

    private void addParticleToGrid(Particle particle) {
        double parX = particle.x;
        double parY = particle.y;

        if (parX >= width || parX < 0 || parY >= height || parY < 0) {
            // throw new IndexOutOfBoundsException("The particle doesn't fit on the grid");
            // System.out.printf("Particle %d left the grid\n", particle.getId());
            return;
        }
        int i = (int) (parX / hCellLength) + N * (int) (parY / vCellLength);
        grid.get(i).add(particle);
    }

    private void resetGrid() {
        for (List<Particle> cell : grid) {
            cell.clear();
        }
        for (Particle p : grains) {
            addParticleToGrid(p);
        }
    }

    public void updateBase() {
        currentTime += dt;
        ys = amplitude * Math.sin(currentTime * frequency);
        leftBoundaryParticle.updatePos(ys);
        rightBoundaryParticle.updatePos(ys);
        for (Particle g : grains) {
            if (g.y - ys <= -height / 10) {
                g.y = new Random(System.currentTimeMillis()).nextDouble() * 0.3 + 0.4;
                double newX = new Random(System.currentTimeMillis()).nextDouble() * width;
                if(newX<0.011)
                    newX = 0.011;
                else if(newX>width-0.011)
                    newX = width-0.011;
                g.x = newX;
                g.speedx = 0;
                g.speedy = 0;
                totalFlow++;
            }
        }
    }

    public int grainCount() {
        return grains.size();
    }

    public long totalFlow() {
        return totalFlow;
    }

    public List<Particle> grains() {
        return grains;
    }

    private double dotProduct(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    private double[] getFnet(double xi, double[] dv, double[] en, double[] et) {
        double fnCoeff = -kn * xi - dotProduct(dv, en) * gamma;
        double[] fn = Arrays.stream(en).map(E -> fnCoeff * E).toArray();
        double fnAbs = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1]);
        double ftCoeff = -mu * fnAbs * Math.signum(dotProduct(et, dv));
        double[] ft = Arrays.stream(et).map(E -> ftCoeff * E).toArray();
        return new double[]{fn[0] + ft[0], fn[1] + ft[1]};
    }

    private double[] getParticleInteractionForce(Particle p, Particle p2) {
        double dx = p2.x - p.x;
        double dy = p2.y - p.y;
        double dr = Math.sqrt(dx * dx + dy * dy);
        double xi = p.radius + p2.radius - dr;
        double[] fnet = {0.0, 0.0};
        if (xi > 0) {
            double enx = dx / dr;
            double eny = dy / dr;
            double[] en = {enx, eny};
            double[] et = {-eny, enx};
            double dvx = p2.speedx - p.speedx;
            double dvy = p2.speedy - p.speedy;
            double[] dv = {dvx, dvy};
            fnet = getFnet(xi, dv, en, et);
        }
        return fnet;
    }

    private void performCellIndexMethod() {
        double[] fnet;
        for (int i = 0; i < M * N; i++) {
            for (Particle particle : grid.get(i)) {
                List<Particle> neighbors = getAboveAndRightAdjacentParticles(i);
                for (Particle neighbor : neighbors) {
                    if (neighbor.getDistance(particle) <= neighborRadius) {
                        fnet = getParticleInteractionForce(particle, neighbor);
                        particle.contactForce[X] += fnet[X];
                        particle.contactForce[Y] += fnet[Y];

                        neighbor.contactForce[X] -= fnet[X];
                        neighbor.contactForce[Y] -= fnet[Y];
                        // TODO Para debuggear lo de abajo, borrarlo luego
                        particle.addNeighbor(neighbor);
                        neighbor.addNeighbor(particle);
                    }
                }
                for (Particle neighbor : getCurrentCellParticles(i, particle)) {
                    if (neighbor.getDistance(particle) <= neighborRadius) {
                        fnet = getParticleInteractionForce(particle, neighbor);
                        particle.contactForce[X] += fnet[X];
                        particle.contactForce[Y] += fnet[Y];
                        // Para debuggear lo de abajo
                        particle.addNeighbor(neighbor);
                    }
                }
            }
        }
    }

    public double[][] getForceMatrix() {
        double[][] forceMatrix = new double[grains().size()][Particle.DIMENSION];
        double leftFloor = (width - opening) / 2;
        double rightFloor = width - (width - opening) / 2;
        for (Particle p : grains) {
            p.resetContactForce();
        }
        resetGrid();
        performCellIndexMethod();
        //TODO: Paralelizar esto
        for (Particle p : grains) {
            double[] forceArray = {0, -9.8 / 1000};
            // Interaction Between Particles
            forceArray[X] += p.contactForce[X];
            forceArray[Y] += p.contactForce[Y];
            if (p.x - p.radius < 0) {
                //LEFT WALL
                double[] en = WallVersor.LEFT.getEn();
                double[] et = WallVersor.LEFT.getEt();
                double xi = -(p.radius - p.x);  // xi = R - |distancia pared|
                double[] fnet = getFnet(xi, p.getSpeed(), en, et);
                for (int i = 0; i < 2; i++) {
                    forceArray[i] += fnet[i];
                }

            } else if (p.x + p.radius > width) {
                //RIGHT WALL
                double[] en = WallVersor.RIGHT.getEn();
                double[] et = WallVersor.RIGHT.getEt();
                double xi = -(p.radius - (width - p.x));  // xi = R - |distancia pared|
                double[] fnet = getFnet(xi, p.getSpeed(), en, et);
                for (int i = 0; i < 2; i++) {
                    forceArray[i] += fnet[i];
                }
            }
            if (p.y - p.radius < ys && p.y + p.radius > ys) {
                if (p.x < leftFloor || p.x > rightFloor) {
                    //LE FLOOR
                    double[] en = WallVersor.DOWN.getEn();
                    double[] et = WallVersor.DOWN.getEt();
                    double xi = -(p.radius - (p.y - ys));
                    double[] fnet = getFnet(xi, p.getSpeed(), en, et);
                    for (int i = 0; i < 2; i++) {
                        forceArray[i] += fnet[i];
                    }
                } else {
                    // O toco el borde o ya estoy en la apertura
                    double[] fnetRight = getParticleInteractionForce(p, rightBoundaryParticle);
                    double[] fnetLeft = getParticleInteractionForce(p, leftBoundaryParticle);
                    for (int i = 0; i < 2; i++) {
                        forceArray[i] += fnetRight[i] + fnetLeft[i];
                    }
                }

            }
            forceMatrix[p.getId()] = forceArray;
        }
        return forceMatrix;
    }

    private List<Particle> getAboveAndRightAdjacentParticles(int cellIndex) {
        List<Particle> adjacentParticles = new ArrayList<>();

        int row = cellIndex / N;
        int col = cellIndex % N;

        int[][] directions = {
                {1, 0}, {1, 1}, // above, upper right
                {0, 1}, // right
                {-1, 1} // lower right
        };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < M && newCol >= 0 && newCol < N) {
                int neighborCellIndex = newRow * N + newCol;
                adjacentParticles.addAll(grid.get(neighborCellIndex));
            }
        }

        return adjacentParticles;
    }

    private List<Particle> getCurrentCellParticles(int i, Particle p) {
        List<Particle> toReturn = new ArrayList<>(grid.get(i));
        toReturn.remove(p);
        return toReturn;
    }
}
