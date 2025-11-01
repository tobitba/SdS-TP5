package tools;

import engine.Particle;

import java.util.*;
import java.util.function.Consumer;

public class ParticleGenerator {

    public static void generate(int particleNumber, Consumer<Particle> consumer,
                                double height, double width,
                                double rMin, double rMax) {

        Random random = new Random(System.currentTimeMillis());
        final double cellSize = 2 * rMax;
        GeneratedGrid grid = new GeneratedGrid();

        int placed = 0;
        int failures = 0;
        final int maxAttempts = 2000; // intentos aleatorios por partícula antes de fallback

        for (int i = 0; i < particleNumber; i++) {
            double radius;
            boolean placedThis = false;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                radius = rMin + random.nextDouble() * (rMax - rMin);
                double x = random.nextDouble() * (width - 2 * radius) + radius;
                double y = random.nextDouble() * (height - 2 * radius) + radius;

                Cell cell = Cell.fromPos(x, y, cellSize);
                if (!grid.checkCollision(x, y, radius, cell)) {
                    Particle p = new Particle(x, y, radius);
                    grid.addParticle(p, cell);
                    consumer.accept(p);
                    placedThis = true;
                    placed++;
                    break;
                }
            }

            if (!placedThis) {
                failures++;
                System.err.println("Warning: no se pudo colocar la partícula " + i + " (espacio agotado).");
            }
        }

        System.out.printf("Generación terminada: pedidos=%d, colocadas=%d, fallidas=%d%n",
                particleNumber, placed, failures);
    }


    private static class GeneratedGrid {
        private final Map<Cell, List<Particle>> grid;
        // chequear 8 vecinos + celda actual
        private final static int[][] directions = {
                {0, 0}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
        };

        public GeneratedGrid() {
            this.grid = new HashMap<>();
        }

        public void addParticle(Particle p, Cell cell) {
            grid.computeIfAbsent(cell, a -> new ArrayList<>()).add(p);
        }


        public boolean checkCollision(double x, double y, double rNew, Cell cell) {
            for (int[] d : directions) {
                Cell n = new Cell(cell.i + d[0], cell.j + d[1]);
                List<Particle> list = grid.get(n);
                if (list == null) continue;
                for (Particle other : list) {
                    double[] pos = other.getPos();
                    double dx = x - pos[0];
                    double dy = y - pos[1];
                    double dist2 = dx * dx + dy * dy;
                    double minDist = other.getRadius() + rNew;
                    if (dist2 < minDist * minDist) return true; // hay solapamiento
                }
            }
            return false;
        }
    }


    private record Cell(int i, int j) {

        @Override
            public boolean equals(Object o) {
                return o instanceof Cell cell && i == cell.i && j == cell.j;
            }


        public static Cell fromPos(double x, double y, double cellSize) {
                int ci = (int) Math.floor(x / cellSize);
                int cj = (int) Math.floor(y / cellSize);
                return new Cell(ci, cj);
            }
        }
}
