/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import vavi.util.win32.WAVE;


/**
 * AudioStream. 
 */
class AudioStream extends DataStream {
    public AudioStream() {
    }

    public int loadFromWaveFile(String filename) throws IOException {
        int hr = Utils.S_OK;

        // Cannot load wave file if memory already allocated for another wave
        if (null != data)
            return Utils.E_FAIL;

        WAVE wave = (WAVE) WAVE.readFrom(new FileInputStream(filename));
        byte[] data = WAVE.data.class.cast(wave.findChildOf(WAVE.data.class)).getWave();
        this.data = ByteBuffer.wrap(data);

        WAVE.fmt format = WAVE.fmt.class.cast(wave.findChildOf(WAVE.fmt.class));
        bitsPerSample = format.getSamplingBits();
        channels = format.getNumberChannels();
        samples = data.length / ((bitsPerSample / 8) * channels);

        return hr;
    }

    public int saveToWaveFile(String filename) throws IOException {
        int hr = Utils.S_OK;

        // If no data, cannot save
        if (null == data)
            return Utils.E_FAIL;

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
System.err.println("l1: " + data.array().length + ", l2: " +  getNumChannels() * getBitsPerSample() / 8 * getNumSamples());
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new FileOutputStream(filename));

        return hr;
    }
}
