package engine;

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
    private final int grainCount;
    private final double amplitude;
    private final double frequency;
    private double currentTime;
    private final double dt;
    private final double kn;
    private final double ky;
    private final static int X = 0;
    private final static int Y = 1;


    public Silo(double width, double height, double opening, List<Particle> grains, double frequency, double amplitude, double dt, double kn) {
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
        this.kn = kn;
        this.ky = 2*kn;
        this.leftBoundaryParticle = new FixedBaseParticle((width - opening)/2, 0 );
        this.rightBoundaryParticle = new FixedBaseParticle(width-(width-opening)/2, 0);
    }

    public void updateBase() {
        currentTime += dt;
        ys = amplitude * Math.sin(currentTime*frequency);
        leftBoundaryParticle.updatePos(ys);
        leftBoundaryParticle.updatePos(ys);
        for(Particle g : grains) {
            if(g.y - ys <= -height/10) {
                g.y = new Random(System.currentTimeMillis()).nextDouble() * 0.3 + 0.4;
                totalFlow++;
            }
        }
    }

    public int grainCount() {
        return grainCount;
    }

    public long totalFlow() {
        return totalFlow;
    }

    public List<Particle> grains() {
        return grains;
    }

    private double dotProduct(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1];
    }

    public double[][] getForceMatrix() {
        double[][] forceMatrix = new double[grainCount][Particle.DIMENSION];
        double leftFloor = (width-opening)/2;
        double rightFloor = width-(width-opening)/2;
        for(Particle p : grains) {
            double[] forceArray = {0,-9.8/1000};
            //TODO: Hacer que esto use el cellIndexMethod
            //TODO: Paralelizar esto
            //TODO: Ver de optimizar esto con simetria
            for(Particle p2 : grains) {
                if(p != p2){
                    double dx = p2.x - p.x;
                    double dy = p2.y - p.y;
                    double dr = Math.sqrt(dx*dx + dy*dy);
                    double xi = p.radius + p2.radius - dr;
                    if(xi>0) {
                        double enx = dx / dr;
                        double eny = dy / dr;
                        double[] en = {enx, eny};
                        double[] et = {-eny, enx};
                        double dvx = p2.speedx - p.speedx;
                        double dvy = p2.speedy - p.speedy;
                        double[] dv = {dvx, dvy};
                        double fnCoeff = -kn * xi - dotProduct(dv, en) * gamma;
                        double[] fn = Arrays.stream(en).map(E -> fnCoeff * E).toArray();
                        double fnAbs = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1]);
                        double ftCoeff = -mu * fnAbs * Math.signum(dotProduct(et, dv));
                        double[] ft = Arrays.stream(et).map(E -> ftCoeff * E).toArray();
                        double[] fnet = {fn[0] + ft[0], fn[1] + ft[1]};
                        for (int i = 0; i < 2; i++) {
                            forceArray[i] += fnet[i];
                        }
                    }
                }
            }
            if(p.x - p.radius < 0) {
                //LEFT WALL
                double[] en = WallVersor.LEFT.getEn();
                double[] et = WallVersor.LEFT.getEt();
                double vn = p.speedx * en[X] + p.speedy * en[Y];
                double vt = p.speedx * et[X] + p.speedy * et[Y];
                double xi =  p.radius - p.x;  // xi = R - |distancia pared|
                double dxi = - vn;

                double fnCoeff = -kn * xi - dxi * gamma;
                double[] fn = Arrays.stream(en).map(E -> fnCoeff * E).toArray();
                double fnAbs = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1]);
                double ftCoeff = -mu * fnAbs * Math.signum( vt);
                double[] ft = Arrays.stream(et).map(E -> ftCoeff * E).toArray();
                double[] fnet = {fn[0] + ft[0], fn[1] + ft[1]};
                for (int i = 0; i < 2; i++) {
                    forceArray[i] += fnet[i];
                }

            } else if(p.x + p.radius > width) {
                //RIGHT WALL
                double[] en = WallVersor.RIGHT.getEn();
                double[] et = WallVersor.RIGHT.getEt();
                double vn = p.speedx * en[X] + p.speedy * en[Y];
                double vt = p.speedx * et[X] + p.speedy * et[Y];
                double xi =  p.radius - (p.x - width);  // xi = R - |distancia pared|
                double dxi = - vn;

                double fnCoeff = -kn * xi - dxi * gamma;
                double[] fn = Arrays.stream(en).map(E -> fnCoeff * E).toArray();
                double fnAbs = Math.sqrt(fn[0] * fn[0] + fn[1] * fn[1]);
                double ftCoeff = -mu * fnAbs * Math.signum( vt);
                double[] ft = Arrays.stream(et).map(E -> ftCoeff * E).toArray();
                double[] fnet = {fn[0] + ft[0], fn[1] + ft[1]};
                for (int i = 0; i < 2; i++) {
                    forceArray[i] += fnet[i];
                }
            }
            if(p.y - p.radius < ys && p.y + p.radius > ys && p.x - p.radius < leftFloor  && p.x + p.radius < rightFloor) {
                //LE FLOOR
                //TODO: Como mierda hacemos los golpes de costado????
            }
            forceMatrix[p.getId()] = forceArray;
        }
        return forceMatrix;
    }
}
