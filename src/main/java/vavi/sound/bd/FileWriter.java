/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;


class FileWriter {

    protected OutputStream file;

    public void open(String Filename, boolean create) throws IOException {
        if (create) {
            file = Files.newOutputStream(Paths.get(Filename));
        } else {
            file = Files.newOutputStream(Paths.get(Filename), StandardOpenOption.APPEND);
        }
    }

    public void close() throws IOException {
        file.close();
    }

    public void writeFloatArray(String name, float[] data, int len) throws IOException {
        String buffer = String.format("%s = [", name);
        file.write(buffer.getBytes());

        for (int i = 0; i < len; i++) {
            if (data[i] == Float.POSITIVE_INFINITY)
                buffer = String.format("%f, ", 0f); // NAN, INF
            else
                buffer = String.format("%f, ", data[i]);
            file.write(buffer.getBytes());
        }

        file.write("];\n\n".getBytes());
    }

    public void writeFloatList(String name, List<Float> values) throws IOException {
        String buffer;
        buffer = String.format("%s = [", name);
        file.write(buffer.getBytes());

        for (float value : values) {
            buffer = String.format("%f, ", value);
            file.write(buffer.getBytes());
        }

        file.write("];\n\n".getBytes());
    }

    public void writeTimingLoops(List<Node> nodes) throws IOException {
        String buffer;
        String buffer2;

        for (Node node : nodes) {
            buffer = String.format("Net_%d", (int) (node.period() * 1000));
            writeFloatList(buffer, node.timingNet().net());
            // Matlab command - debug
            buffer2 = String.format("%s_Mean = mean(%s.^2)\n", buffer, buffer);
            file.write(buffer2.getBytes());
        }
    }
}
