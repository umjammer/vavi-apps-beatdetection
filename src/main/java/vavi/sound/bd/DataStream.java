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

    // Create stream from existing stream or data
    public int createData(int bitsPerSample, int sampleRate, int samples, boolean normalized) {
        if (isValid())
            return Utils.E_FAIL;

        if ((bitsPerSample % 8) != 0)
            throw new IllegalArgumentException("bitsPerSample");

        if (normalized && (bitsPerSample < 32))
            throw new IllegalArgumentException("normalized or bitsPerSample");

        this.data = ByteBuffer.allocate(samples * (bitsPerSample / 8));
        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
        this.samples = samples;
        this.normalized = normalized;
        return Utils.S_OK;
    }

    public int createData(final DataStream dataStreamCopyFrom) {
        if (null != dataStreamCopyFrom)
            return createData(dataStreamCopyFrom.getBitsPerSample(),
                              dataStreamCopyFrom.getSampleRate(),
                              dataStreamCopyFrom.getNumSamples(),
                              dataStreamCopyFrom.isNormalized());
        else
            throw new IllegalArgumentException("dataStreamCopyFrom");
    }

    public void releaseData() {
        if (data != null) {
            data = null;
            samples = 0;
        }
    }

    // Normalize and Denormalize - assumes use of float for normalized version
    public int normalize() {
        int hr = Utils.S_OK;

        if (isNormalized() || !isValid())
            return Utils.E_FAIL;

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
            hr = Utils.S_OK;
        } else {
            hr = Utils.E_FAIL;
        }

        return hr;
    }

    public int deNormalize(int bitsPerSample) {
        int hr = Utils.S_OK;

        if (!isNormalized() || !isValid())
            return Utils.E_FAIL;

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
            hr = Utils.S_OK;
        } else {
            hr = Utils.E_FAIL;
        }

        return hr;
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
    public int reallocate(int samples) {
        // Nothing to do
        if (samples == this.samples)
            return Utils.S_OK;

        int hr = Utils.E_FAIL;

        if (samples < this.samples) {
            // Request for shorter data length, just change the # of samples and be
            // done with it, no need for reallocation
            this.samples = samples;
            hr = Utils.S_OK;
        } else {
            // Copy over data and pad end with zeros - not quite efficient but who cares...
            data = ByteBuffer.allocate(samples * (bitsPerSample / 8));
            hr = Utils.S_OK;
        }

        return hr;
    }
}