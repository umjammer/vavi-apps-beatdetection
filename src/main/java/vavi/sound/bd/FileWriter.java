/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


class FileWriter {
    public FileWriter() {
    }

    public boolean open(String lpszFilename, boolean fCreate) throws IOException {
        if (fCreate) {
            m_File = new FileOutputStream(lpszFilename);
        } else {
            m_File = new FileOutputStream(lpszFilename, true);
        }

        return true;
    }

    public boolean close() throws IOException {
        m_File.close();
        return true;
    }

    public boolean writeFloatArray(String lpszName, float[] pArray, int nLen) throws IOException {
        String szBuffer;
        szBuffer = String.format("%s = [", lpszName);
        m_File.write(szBuffer.getBytes());

        for (int ii = 0; ii < nLen; ii++) {
            if (pArray[ii] == Float.POSITIVE_INFINITY)
                szBuffer = String.format("%f, ", 0); //NAN, INF
            else
                szBuffer = String.format("%f, ", pArray[ii]);
            m_File.write(szBuffer.getBytes());
        }

        m_File.write("];\n\n".getBytes());

        return true;
    }

    public boolean WriteFloatList(String lpszName, List<Float> List) throws IOException {
        String szBuffer;
        szBuffer = String.format("%s = [", lpszName);
        m_File.write(szBuffer.getBytes());

        for (Float flValue : List) {
            szBuffer = String.format("%f, ", flValue);
            m_File.write(szBuffer.getBytes());
        }

        m_File.write("];\n\n".getBytes());

        return true;
    }

    public boolean WriteTimingLoops(List<Node> List) throws IOException {
        String szBuffer;
        String szBuffer2;

        for (Node node : List) {
            szBuffer = String.format("Net_%d", (int) (node.period() * 1000));
            WriteFloatList(szBuffer, node.timingNet().net());
            // Matlab command - debug
            szBuffer2 = String.format("%s_Mean = mean(%s.^2)\n", szBuffer, szBuffer);
            m_File.write(szBuffer2.getBytes());
        }

        return true;
    }

    protected OutputStream m_File;
}
