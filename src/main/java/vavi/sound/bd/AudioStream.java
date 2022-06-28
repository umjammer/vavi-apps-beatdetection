/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import vavi.util.Debug;
import vavi.util.win32.WAVE;


/**
 * AudioStream. 
 */
class AudioStream extends DataStream {

    public void loadFromWaveFile(String filename) throws IOException {
        // Cannot load wave file if memory already allocated for another wave
        if (null != data)
            throw new IllegalStateException("null data");

        WAVE wave = WAVE.readFrom(Files.newInputStream(Paths.get(filename)), WAVE.class);
        byte[] data = wave.findChildOf(WAVE.data.class).getWave();
        this.data = ByteBuffer.wrap(data);

        WAVE.fmt format = wave.findChildOf(WAVE.fmt.class);
        bitsPerSample = format.getSamplingBits();
        sampleRate = format.getSamplingRate();
        channels = format.getNumberChannels();
        samples = data.length / ((bitsPerSample / 8) * channels);
    }

    public void saveToWaveFile(String filename) throws IOException {
        // If no data, cannot save
        if (null == data)
            throw new IllegalStateException("null data");

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             getSampleRate(),
                                             getBitsPerSample(),
                                             getNumChannels(),
                                             1,
                                             getSampleRate(),
                                             false);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data.array()),
                                                    format,
                                                    data.array().length);
Debug.println("l1: " + data.array().length + ", l2: " +  getNumChannels() * getBitsPerSample() / 8 * getNumSamples());
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, Files.newOutputStream(Paths.get(filename)));
    }
}
