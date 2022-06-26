/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


class DataStream {

    protected int channels;

    protected int bitsPerSample;

    protected int sampleRate;

    protected int samples;

    protected boolean normalized;

    protected ByteBuffer data;

    public DataStream() {
        data = null;
        samples = 0;
        channels = 1;
    }

    /**
     * Create stream from existing stream or data
     * @throws IllegalStateException already data created
     */
    public void createData(int bitsPerSample, int sampleRate, int samples, boolean normalized) {
        if (isValid())
            throw new IllegalStateException("already data created");
        if ((bitsPerSample % 8) != 0)
            throw new IllegalArgumentException("bitsPerSample is not multiple of 8");
        if (normalized && (bitsPerSample < 32))
            throw new IllegalArgumentException("normalized bitsPerSample is under 32");

        this.data = ByteBuffer.allocate(samples * (bitsPerSample / 8));
        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.samples = samples;
        this.normalized = normalized;
    }

    /** @throws IllegalArgumentException copyFrom is null */
    public void createData(final DataStream copyFrom) {
        if (null != copyFrom)
            createData(copyFrom.getBitsPerSample(),
                       copyFrom.getSampleRate(),
                       copyFrom.getNumSamples(),
                       copyFrom.isNormalized());
        else
            throw new IllegalArgumentException("copyFrom is null");
    }

    public void releaseData() {
        if (data != null) {
            data = null;
            samples = 0;
        }
    }

    // Normalize and Denormalize - assumes use of float for normalized version
    public void normalize() {
        if (!isValid())
            throw new IllegalStateException("not valid");
        if (isNormalized())
            throw new IllegalStateException("already normalized");

        ByteBuffer bb = ByteBuffer.allocate(4 * getNumSamples());
        FloatBuffer data = bb.asFloatBuffer();
        boolean success = false;
        if (getBitsPerSample() == 16) {
            ShortBuffer sb = getData().asShortBuffer();
            // Copy over data and normalize
            for (int i = 0; i < getNumSamples(); i++)
                data.put(i, (float) sb.get(i) / 32768);

            success = true;
        } else {
            // No support for other bit depths yet
            assert (false);
        }

        if (success) {
            this.data = bb;
            normalized = true;
            bitsPerSample = 4 * 8;
        } else {
        }
    }

    public void denormalize(int bitsPerSample) {
        if (!isValid())
            throw new IllegalStateException("not valid");
        if (!isNormalized())
            throw new IllegalStateException("not normalized");

        ByteBuffer bb = ByteBuffer.allocate(getNumSamples() * (bitsPerSample / 8));
        boolean success = false;
        if (bitsPerSample == 16) {
            ShortBuffer sb = bb.asShortBuffer();
            // Copy over data and denormalize
            for (int i = 0; i < getNumSamples(); i++)
                sb.put(i, (short) (getData().asFloatBuffer().get(i) * 32768));

            success = true;
        } else {
            // No support for other bit depths yet
            assert false;
        }

        if (success) {
            data = bb;
            normalized = false;
            this.bitsPerSample = bitsPerSample;
        } else {
        }
    }

    public int getNumChannels() {
        return channels;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getNumSamples() {
        return samples;
    }

    public double getDuration() {
        return (double) samples / sampleRate;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public ByteBuffer getData() {
        return data;
    }

    public float[] getFloatData() {
        return data.asFloatBuffer().array();
    }

    public boolean isValid() {
        return data != null;
    }

    // Reallocate stream to new length - shorter or longer, pads with zeros
    public void reallocate(int samples) {
        // Nothing to do
        if (samples == this.samples)
            return;

        if (samples < this.samples) {
            // Request for shorter data length, just change the # of samples and be
            // done with it, no need for reallocation
            this.samples = samples;
        } else {
            // Copy over data and pad end with zeros - not quite efficient but who cares...
            data = ByteBuffer.allocate(samples * (bitsPerSample / 8));
        }
    }
}