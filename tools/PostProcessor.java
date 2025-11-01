package tools;


import engine.Particle;
import engine.Time;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class PostProcessor implements Closeable {
    private static final String OUTPUT_FILE_NAME = "output.txt";
    private final BufferedWriter writer;


    public PostProcessor(String outputName) {
        Locale.setDefault(Locale.US);
        try {
            if (outputName == null)
                outputName = OUTPUT_FILE_NAME;
            writer = new BufferedWriter(new FileWriter(outputName));
        } catch (IOException e) {
            throw new RuntimeException("Error opening file");
        }
    }


    private void processParticle(Particle particle) {
        try {
            writer.write(particle.csvString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error writing on output file");
        }
    }

    public void processSystem(Time t) {
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
