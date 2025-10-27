package tools;


import engine.Particle;
import engine.Time;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;

public class PostProcessor implements Closeable {
    private static final String OUTPUT_FILE_NAME = "output.txt";
    private static final BufferedWriter writer;

    static {
        try {
            writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_NAME));
        } catch (IOException e) {
            throw new RuntimeException("Error opening file", e);
        }
    }

    private static void processParticle(Particle particle) {
        try {
            writer.write(particle.csvString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error writing on output file");
        }
    }

    public static void processSystem(Time t) {
        try {
            writer.write("%.4f - %d".formatted(t.time(),t.totalFlow()));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error writing on output file");
        }
        for(Particle p : t.grains()){
            processParticle(p);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
