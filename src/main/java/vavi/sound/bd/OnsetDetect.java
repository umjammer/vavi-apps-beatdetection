/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import static vavi.sound.bd.Utils.params;


// OnsetDetect.h: interface for the CBDOnsetDetect class.

class OnsetDetect {
    // Half-Hanning (Raised Cosine) window
    // 100ms duration at 441Hz sampling rate
    static final float[] aflHalfHanning100ms = {
        0.9997f, 0.9972f, 0.9922f, 0.9848f, 0.9750f, 0.9628f, 0.9483f, 0.9315f, 0.9126f, 0.8917f, 0.8688f, 0.8441f, 0.8176f,
        0.7896f, 0.7601f, 0.7294f, 0.6974f, 0.6645f, 0.6308f, 0.5965f, 0.5616f, 0.5265f, 0.4912f, 0.4559f, 0.4209f, 0.3863f,
        0.3522f, 0.3189f, 0.2865f, 0.2551f, 0.2250f, 0.1962f, 0.1689f, 0.1433f, 0.1195f, 0.0976f, 0.0776f, 0.0598f, 0.0442f,
        0.0308f, 0.0198f, 0.0112f, 0.0050f, 0.0012f
    };

    static final int nHalfHanning100ms = 44;

    // Number of steps of slope to calculate and normalization factor
    // Normalization is Sum(1/i), i=1 to Steps
    static final float ENV_NORMALIZE = 2.9290f;

    static final int ENV_STEPS = 10;

 // ***** Thresholds must be set with some degree of intelligence *****
    public OnsetDetect() {
    }

    public int createOnsetStream(AudioStream pStrmIn, DataStream pStrmOut, DataStream pStrmInternal) {
        int hr = Utils.S_OK;

        pStrmInternal.releaseData();

        DataStream StrmDecimated = null, StrmProcessed = null; // TODO

        //////
        hr = DSP.RMSDecimate(pStrmIn, StrmDecimated, pStrmIn.getSampleRate() / params.onsetSamplingRate);
        if (Utils.FAILED(hr))
            return hr;

        //////
        hr = DSP.Convolve(StrmDecimated, StrmProcessed, aflHalfHanning100ms, nHalfHanning100ms);
        if (Utils.FAILED(hr))
            return hr;

        //////
        hr = ProcessEnvelope(StrmProcessed, pStrmInternal);
        if (Utils.FAILED(hr))
            return hr;

        //////
        hr = ThresholdStream(pStrmInternal, StrmProcessed, pStrmOut);
        if (Utils.FAILED(hr))
            return hr;

        return hr;
    }

    protected int ProcessEnvelope(DataStream pStrmIn, DataStream pStrmOut) {
        int hr = Utils.S_OK;

        //////
        // Calculate onset detection threshold function
        hr = pStrmOut.createData(pStrmIn);
        if (Utils.FAILED(hr))
            return hr;

        float[] pflDataOut = pStrmOut.getFloatData();
        float[] pflDataIn = pStrmIn.getFloatData();
        for (int iSam = 0; iSam < pStrmOut.getNumSamples(); iSam++) {
            // Seppanen/Klapuri
            /* if( pflDataIn[iSam]+pflDataIn[iSam-1] > 0 )
             * pflDataOut[iSam] = (pflDataIn[iSam]-pflDataIn[iSam-1]) /
             * (pflDataIn[iSam]+pflDataIn[iSam-1]);
             * else
             * pflDataOut[iSam] = 0; */

            // Scheirer - LOUSY
            //psDataOut[iSam] = 50*(psDataIn[iSam]-psDataIn[iSam-1]);

            // Duxbury
            /* double dResult = psDataIn[iSam];
             * int iLeftLimit = max(iSam-30,0);
             * for( int ii=iLeftLimit; ii<iSam; ii++ )
             * {
             * dResult -= (double)psDataIn[ii]/(iSam-ii);
             * }
             * psDataOut[iSam] = (signed short)(10*dResult); */

            // Duxbury/Klapuri
            float flResult = 0;
            int iLeftLimit = Math.max(iSam - ENV_STEPS, 0);
            for (int ii = iLeftLimit; ii < iSam; ii++) {
                float flTemp = (pflDataIn[iSam] - pflDataIn[ii]);
                if (flTemp != 0) // Don't want to divide by zero!
                    flResult += flTemp / ((iSam - ii) * (pflDataIn[iSam] + pflDataIn[ii]));
            }
            pflDataOut[iSam] = flResult / ENV_NORMALIZE;

            // Second Difference - LOUSY
            //psDataOut[iSam] = 50*((psDataIn[iSam]-psDataIn[iSam-1])-(psDataIn[iSam-1]-psDataIn[iSam-2]));
        }

        return hr;
    }

    enum EState {
        ThreshFound,
        ThreshLooking
    }

    protected int ThresholdStream(DataStream pStrmIn, DataStream pStrmEnv, DataStream pStrmOut) {
        int hr = Utils.S_OK;

        EState eState = EState.ThreshLooking;

        //////
        // Calculate onset detection threshold function
        hr = pStrmOut.createData(pStrmIn);
        if (Utils.FAILED(hr))
            return hr;

        float[] pflDataOut = pStrmOut.getFloatData();
        float[] pflDataIn = pStrmIn.getFloatData();
        float[] pflEnv = pStrmEnv.getFloatData();

        // Calculate minimum distance to pass before another sample may be detected
        int nSamMinDist = (int) (params.onsetDetectResetTime * pStrmIn.getSampleRate());

        int iLastFound = 0;
        for (int iSam = 0; iSam < pStrmOut.getNumSamples(); iSam++) {
            if ((eState == EState.ThreshLooking) && (pflDataIn[iSam] > params.onsetDetectThreshHigh)) {
                //////
                // Found onset, update state
                eState = EState.ThreshFound;
                iLastFound = iSam;

                // Determine the intensity of this onset - search for maximum level of envelope
                // Intensity = env(max) - env(begin)
                float flEnvMax = 0;

                while ((pflDataIn[iSam] > 0) && (iSam - iLastFound < nSamMinDist)) {
                    if (pflEnv[iSam] > flEnvMax)
                        flEnvMax = pflEnv[iSam];
                    iSam++;
                    pflDataOut[iSam] = 0; // Zero out these subsequent searched samples' onset sample
                }

                pflDataOut[iLastFound] = flEnvMax - pflEnv[iLastFound];
            } else if ((eState == EState.ThreshFound)
                       && ((pflDataIn[iSam] < params.onsetDetectThreshLow) || (iSam > iLastFound + nSamMinDist))) {
                //////
                eState = EState.ThreshLooking;
                pflDataOut[iSam] = 0;
            } else {
                //////
                pflDataOut[iSam] = 0;
            }
        }

        return hr;
    }
}
