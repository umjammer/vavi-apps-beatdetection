/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import static vavi.sound.bd.Utils.params;


/**
 * interface for the CBDOnsetDetect class.
 */
class OnsetDetect {
    // Half-Hanning (Raised Cosine) window
    // 100ms duration at 441Hz sampling rate
    static final float[] aflHalfHanning100ms = {
        0.9997f, 0.9972f, 0.9922f, 0.9848f, 0.9750f, 0.9628f, 0.9483f, 0.9315f, 0.9126f, 0.8917f, 0.8688f, 0.8441f, 0.8176f,
        0.7896f, 0.7601f, 0.7294f, 0.6974f, 0.6645f, 0.6308f, 0.5965f, 0.5616f, 0.5265f, 0.4912f, 0.4559f, 0.4209f, 0.3863f,
        0.3522f, 0.3189f, 0.2865f, 0.2551f, 0.2250f, 0.1962f, 0.1689f, 0.1433f, 0.1195f, 0.0976f, 0.0776f, 0.0598f, 0.0442f,
        0.0308f, 0.0198f, 0.0112f, 0.0050f, 0.0012f
    };

    static final int halfHamming100ms = 44;

    // Number of steps of slope to calculate and normalization factor
    // Normalization is Sum(1/i), i=1 to Steps
    static final float ENV_NORMALIZE = 2.9290f;

    static final int ENV_STEPS = 10;

    // Thresholds must be set with some degree of intelligence
    public OnsetDetect() {
    }

    public void createOnsetStream(AudioStream in, DataStream out, DataStream internal) {
        internal.releaseData();

        DataStream decimated = new DataStream(), processed = new DataStream(); // TODO

        //
        DSP.decimateRms(in, decimated, in.getSampleRate() / params.onsetSamplingRate);

        //
        DSP.convolve(decimated, processed, aflHalfHanning100ms, halfHamming100ms);

        //
        processEnvelope(processed, internal);

        //
        thresholdStream(internal, processed, out);
    }

    /** @throws IllegalStateException creation failed */
    protected void processEnvelope(DataStream in, DataStream out) {
        // Calculate onset detection threshold function
        out.createData(in);

        float[] dataOut = out.getFloatData();
        float[] dataIn = in.getFloatData();
        for (int i = 0; i < out.getNumSamples(); i++) {
            // Seppanen/Klapuri
//            if (dataIn[i] + dataIn[i - 1] > 0)
//                dataOut[i] = (dataIn[i] - dataIn[i - 1]) /
//                        (dataIn[i] + dataIn[i - 1]);
//            else
//                dataOut[i] = 0;

            // Scheirer - LOUSY
            //dataOut[i] = 50 * (dataIn[i] - dataIn[i-1]);

            // Duxbury
//            double result = dataIn[i];
//            int leftLimit = max(i - 30, 0);
//            for (int ii = leftLimit; ii < i; ii++) {
//                result -= (double) dataIn[ii] / (i - ii);
//            }
//            dataOut[i] = (short) (10 * result);

            // Duxbury/Klapuri
            float result = 0;
            int leftLimit = Math.max(i - ENV_STEPS, 0);
            for (int j = leftLimit; j < i; j++) {
                float temp = (dataIn[i] - dataIn[j]);
                if (temp != 0) // Don't want to divide by zero!
                    result += temp / ((i - j) * (dataIn[i] + dataIn[j]));
            }
            dataOut[i] = result / ENV_NORMALIZE;

            // Second Difference - LOUSY
            //dataOut[i] = 50 * ((dataIn[i] - dataIn[i - 1]) - (dataIn[i - 1] - dataIn[i - 2]));
        }
    }

    enum EState {
        ThreshFound,
        ThreshLooking
    }

    /** @throws java.lang.IllegalStateException creation failed */
    protected void thresholdStream(DataStream in, DataStream env, DataStream out) {
        EState state = EState.ThreshLooking;

        // Calculate onset detection threshold function
        out.createData(in);

        float[] dataOut = out.getFloatData();
        float[] dataIn = in.getFloatData();
        float[] dataEnv = env.getFloatData();

        // Calculate minimum distance to pass before another sample may be detected
        int samMinDist = (int) (params.onsetDetectResetTime * in.getSampleRate());

        int lastFound = 0;
        for (int i = 0; i < out.getNumSamples(); i++) {
            if ((state == EState.ThreshLooking) && (dataIn[i] > params.onsetDetectThreshHigh)) {
                // Found onset, update state
                state = EState.ThreshFound;
                lastFound = i;

                // Determine the intensity of this onset - search for maximum level of envelope
                // Intensity = env(max) - env(begin)
                float envMax = 0;

                while ((dataIn[i] > 0) && (i - lastFound < samMinDist)) {
                    if (dataEnv[i] > envMax)
                        envMax = dataEnv[i];
                    i++;
                    dataOut[i] = 0; // Zero out these subsequent searched samples' onset sample
                }

                dataOut[lastFound] = envMax - dataEnv[lastFound];
            } else if ((state == EState.ThreshFound)
                       && ((dataIn[i] < params.onsetDetectThreshLow) || (i > lastFound + samMinDist))) {
                //
                state = EState.ThreshLooking;
                dataOut[i] = 0;
            } else {
                //
                dataOut[i] = 0;
            }
        }
    }
}
