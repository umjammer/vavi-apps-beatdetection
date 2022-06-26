/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


/**
 * note: this code assumes the data is normalized - floating point
 */
class DSP {

    static class FILTER_6TH_COEFF {
        double[] dA = new double[7];
        double[] dB = new double[7];
        int nSamplesDelay;

        public FILTER_6TH_COEFF(double[] a, double[] b, int c) {
            dA = a;
            dB = b;
            nSamplesDelay = c;
        }
    }

    static final FILTER_6TH_COEFF LPF30Hz = new FILTER_6TH_COEFF(new double[] {
            1, -2.98860194684, 2.977268758941, -0.9886666280522, 0, 0, 0
    }, new double[] {
            2.300606208316e-008, 6.901818624949e-008, 6.901818624949e-008, 2.300606208316e-008, 0, 0, 0
    }, 0);

    /**
     * Direct Form II - Up to 6th Order Filter
     * IIR Filter with A,B coefficients
     * @throws java.lang.IllegalStateException creation failed
     */
    public static void DF2_Filter6(DataStream in,
                                  DataStream out,
                                  FILTER_6TH_COEFF coeff,
                                  boolean reverseGroupDelay /* = false */) {
        if (!in.isNormalized())
            throw new IllegalArgumentException("in");

        // Delay units - allow up to 6-order filters

        double delay[] = new double[] {
            0, 0, 0, 0, 0, 0
        };
        double middle = 0;
        double x, y;

        //                               middle
        //  X ---. Sum ------ 1/a(0) ----.|--------- b(0) ----. Sum ---. Y
        //          ^                     |                      ^
        //          |                     v                      |
        //         Sum <----- -a(1) ----- D(0) ------ b(1) ----. Sum
        //          ^                     |                      ^
        //          |                     v                      |
        //         Sum <----- -a(2) ----- D(1) ------ b(2) ----. Sum
        //          .                     .                      .
        //          .                     .                      .
        //          .                     .                      .

        // Make the outstream the same params as the instream
        out.createData(in);

        float[] dataIn = in.getFloatData();
        float[] dataOut = out.getFloatData();

        double[] dA = coeff.dA;
        double[] dB = coeff.dB;

        int inSamples = in.getNumSamples();
        int inSamplesPlusDelay = inSamples + coeff.nSamplesDelay;

        if (!reverseGroupDelay)
            throw new IllegalStateException();

//        boolean fDone = false;
        int i = 0;
        int c = 0;
        while (i < inSamplesPlusDelay) {
            // Input value from input stream or zero if past end of input stream
            x = 0;
            if (i < inSamples)
                x = dataIn[c++];

            // Calculate
            middle = (x - delay[0] * dA[1] - delay[1] * dA[2] - delay[2] * dA[3] - delay[3] * dA[4] - delay[4] * dA[5]
                       - delay[5] * dA[6])
                      / dA[0];

            y = middle * dB[0] + delay[0] * dB[1] + delay[1] * dB[2] + delay[2] * dB[3] + delay[3] * dB[4]
                 + delay[4] * dB[5] + delay[5] * dB[6];

            // Set output value to stream if no reverse group delay, or only start setting to
            // stream after group delay has passed
            if (i >= coeff.nSamplesDelay)
                dataOut[c++] = (float) y;

            // Update Delays
            delay[5] = delay[4];
            delay[4] = delay[3];
            delay[3] = delay[2];
            delay[2] = delay[1];
            delay[1] = delay[0];
            delay[0] = middle;

            i++;

            // Done if through all samples and no reverse group delay, or if through all samples
            // plus group delay samples
        }
    }

    // Root-Mean-Square Decimate signal
    public static void decimateRms(DataStream in, DataStream out, int dec) {
        if (!in.isNormalized())
            throw new IllegalArgumentException("in is not normalized");

        // Create working stream
        DataStream temp = new DataStream();

        // Create output stream as decimated input
        out.createData(in.getBitsPerSample(),
                            in.getSampleRate() / dec,
                            (int) Math.ceil((double) in.getNumSamples() / dec),
                            in.isNormalized());

        float[] dataIn = in.getFloatData();
        float[] dataOut = out.getFloatData();

        // Square the input signal
        for (int i = 0; i < in.getNumSamples(); i++) {
            dataIn[i] = dataIn[i] * dataIn[i];
        }

        // LPF input signal
        DF2_Filter6(in, temp, LPF30Hz, true);

        float[] dataTemp = temp.getFloatData();

        // Root and Decimate the signal
        for (int i = 0; i < out.getNumSamples(); i++) {
            float result = 0;
            int rightLimit = Math.min((i + 1) * dec, temp.getNumSamples());

            for (int j = i * dec; j < rightLimit; j++) {
                result += (float) Math.sqrt(Math.max(dataTemp[j], 0));
            }

            dataOut[i] = result / dec;
        }

        //

//        for (int iSam = 0; iSam < out.GetNumSamples(); iSam++) {
//            float flResult = 0;
//            int iRightLimit = Math.min((iSam + 1) * dec, pStrmIn.GetNumSamples());
//            for (int ii = iSam * dec; ii < iRightLimit; ii++) {
//                flResult += dataIn[ii] * dataIn[ii];
//            }
//            dataOut[iSam] = (float) Math.sqrt(flResult) / dec;
//        }
    }

    // Convolution by a variable length kernel
    public static void convolve(DataStream in, DataStream out, final float[] kernel, final int kernelLen) {
        if (!in.isNormalized())
            throw new IllegalArgumentException("in is not normalized");

        // Create output stream same as input but longer by kernel length - 1
        out.createData(in.getBitsPerSample(),
                        in.getSampleRate(),
                        in.getNumSamples() + kernelLen - 1,
                        in.isNormalized());

        //
        // NOTE: THIS CODE ASSUMES THE DATA IS 16-BITS PER SAMPLE!!
        //
        float[] dataIn = in.getFloatData();
        float[] pflDataOut = out.getFloatData();

        for (int i = 0; i < out.getNumSamples(); i++) {
            float output = 0;
            int leftLimit = Math.max(i - kernelLen + 1, 0);
            int rightLimit = Math.min(i + 1, in.getNumSamples());
            for (int k = leftLimit; k < rightLimit; k++) {
                output += dataIn[k] * kernel[i - k];
            }
            pflDataOut[i] = output;
        }
    }

    /** @throws IllegalStateException creation failed */
    public static void mix(DataStream in1, float vol1, DataStream in2, float vol2, DataStream out) {
        if (!in1.isNormalized() || !in2.isNormalized())
            throw new IllegalArgumentException("in1 or in2 is not normalized");
        if (in1.getSampleRate() != in2.getSampleRate())
            throw new IllegalArgumentException("in1, in2 sampleRate is differ");

        int samples = Math.min(in1.getNumSamples(), in2.getNumSamples());

        out.createData(in1.getBitsPerSample(), in1.getSampleRate(), samples, true);

        float[] dataIn1 = in1.getFloatData();
        float[] dataIn2 = in2.getFloatData();
        float[] dataOut = out.getFloatData();

        for (int i = 0; i < samples; i++) {
            dataOut[i] = dataIn1[i] * vol1 + dataIn2[i] * vol2;
        }
    }
}