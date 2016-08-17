/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


class FILTER_6TH_COEFF {
    double[] dA = new double[7];
    double[] dB = new double[7];
    int nSamplesDelay;

    public FILTER_6TH_COEFF(double[] a, double[] b, int c) {
        dA = a;
        dB = b;
        nSamplesDelay = c;
    }
}

/**
 * note: this code assumes the data is normalized - floating point
 */
class DSP {

    static FILTER_6TH_COEFF LPF30Hz = new FILTER_6TH_COEFF(new double[] {
        1, -2.98860194684, 2.977268758941, -0.9886666280522, 0, 0, 0
    }, new double[] {
        2.300606208316e-008, 6.901818624949e-008, 6.901818624949e-008, 2.300606208316e-008, 0, 0, 0
    }, 0);

    public DSP() {
    }

    // Direct Form II - Up to 6th Order Filter
    // IIR Filter with A,B coefficients
    public static int DF2_Filter6(DataStream pStrmIn,
                                  DataStream pStrmOut,
                                  FILTER_6TH_COEFF pCoeff,
                                  boolean fReverseGroupDelay /* = false */) {
        int hr = Utils.S_OK;

        if (!pStrmIn.isNormalized())
            throw new IllegalArgumentException("pStrmIn");

        // Delay units - allow up to 6-order filters

        double dDelay[] = new double[] {
            0, 0, 0, 0, 0, 0
        };
        double dMiddle = 0;
        double dX, dY;

        //                               dMiddle
        //  X ---. Sum ------ 1/a(0) ----.|--------- b(0) ----. Sum ---. Y
        //           ^                      |                       ^
        //           |                      v                       |     
        //          Sum <----- -a(1) ----- D(0) ------ b(1) ----. Sum
        //           ^                      |                       ^
        //           |                      v                       |     
        //          Sum <----- -a(2) ----- D(1) ------ b(2) ----. Sum
        //           .                      .                       .
        //           .                      .                       .
        //           .                      .                       .

        // Make the outstream the same params as the instream
        hr = pStrmOut.createData(pStrmIn);
        if (Utils.FAILED(hr))
            return hr;

        float[] pflDataIn = pStrmIn.getFloatData();
        float[] pflDataOut = pStrmOut.getFloatData();

        double[] dA = pCoeff.dA;
        double[] dB = pCoeff.dB;

        int nInSamples = pStrmIn.getNumSamples();
        int nInSamplesPlusDelay = nInSamples + pCoeff.nSamplesDelay;

        if (!fReverseGroupDelay)
            return Utils.E_FAIL;

//        boolean fDone = false;
        int iSam = 0;
        int c = 0;
        while (iSam < nInSamplesPlusDelay) {
            // Input value from input stream or zero if past end of input stream
            dX = 0;
            if (iSam < nInSamples)
                dX = pflDataIn[c++];

            // Calculate
            dMiddle = (dX - dDelay[0] * dA[1] - dDelay[1] * dA[2] - dDelay[2] * dA[3] - dDelay[3] * dA[4] - dDelay[4] * dA[5]
                       - dDelay[5] * dA[6])
                      / dA[0];

            dY = dMiddle * dB[0] + dDelay[0] * dB[1] + dDelay[1] * dB[2] + dDelay[2] * dB[3] + dDelay[3] * dB[4]
                 + dDelay[4] * dB[5] + dDelay[5] * dB[6];

            // Set output value to stream if no reverse group delay, or only start setting to
            // stream after group delay has passed
            if (iSam >= pCoeff.nSamplesDelay)
                pflDataOut[c++] = (float) dY;

            // Update Delays
            dDelay[5] = dDelay[4];
            dDelay[4] = dDelay[3];
            dDelay[3] = dDelay[2];
            dDelay[2] = dDelay[1];
            dDelay[1] = dDelay[0];
            dDelay[0] = dMiddle;

            iSam++;

            // Done if through all samples and no reverse group delay, or if through all samples
            // plus group delay samples
        }

        return hr;
    }

    // Root-Mean-Square Decimate signal
    public static int RMSDecimate(DataStream pStrmIn, DataStream pStrmOut, int nDec) {
        int hr = Utils.S_OK;

        if (!pStrmIn.isNormalized())
            throw new IllegalArgumentException("pStrmIn");

        // Create working stream
        DataStream strmTemp = new DataStream();

        // Create output stream as decimated input
        pStrmOut.createData(pStrmIn.getBitsPerSample(),
                            pStrmIn.getSampleRate() / nDec,
                            (int) Math.ceil((double) pStrmIn.getNumSamples() / nDec),
                            pStrmIn.isNormalized());

        float[] pflDataIn = pStrmIn.getFloatData();
        float[] pflDataOut = pStrmOut.getFloatData();

        ///////////////////////////////////////////////////////////
        // Sqaure the input signal
        for (int iSam = 0; iSam < pStrmIn.getNumSamples(); iSam++) {
            pflDataIn[iSam] = pflDataIn[iSam] * pflDataIn[iSam];
        }

        // LPF input signal
        hr = DF2_Filter6(pStrmIn, strmTemp, LPF30Hz, true);
        if (Utils.FAILED(hr))
            return hr;

        float[] pflDataTemp = strmTemp.getFloatData();

        // Root and Decimate the signal
        for (int iSam = 0; iSam < pStrmOut.getNumSamples(); iSam++) {
            float flResult = 0;
            int iRightLimit = Math.min((iSam + 1) * nDec, strmTemp.getNumSamples());

            for (int ii = iSam * nDec; ii < iRightLimit; ii++) {
                flResult += (float) Math.sqrt(Math.max(pflDataTemp[ii], 0));
            }

            pflDataOut[iSam] = flResult / nDec;
        }
        ///////////////////////////////////////////////////////////

//        for (int iSam = 0; iSam < pStrmOut.GetNumSamples(); iSam++) {
//            float flResult = 0;
//            int iRightLimit = Math.min((iSam + 1) * nDec, pStrmIn.GetNumSamples());
//            for (int ii = iSam * nDec; ii < iRightLimit; ii++) {
//                flResult += pflDataIn[ii] * pflDataIn[ii];
//            }
//            pflDataOut[iSam] = (float) Math.sqrt(flResult) / nDec;
//        }

        return hr;
    }

    // Convolution by a variable length kernel
    public static int Convolve(DataStream pStrmIn, DataStream pStrmOut, final float aflKernel[], final int nKernelLen) {
        int hr = Utils.S_OK;

        if (!pStrmIn.isNormalized())
            throw new IllegalArgumentException("pStrmIn");

        // Create output stream same as input but longer by kernel length - 1
        pStrmOut.createData(pStrmIn.getBitsPerSample(),
                            pStrmIn.getSampleRate(),
                            pStrmIn.getNumSamples() + nKernelLen - 1,
                            pStrmIn.isNormalized());

        //
        // NOTE: THIS CODE ASSUMES THE DATA IS 16-BITS PER SAMPLE!!
        //
        float[] pflDataIn = pStrmIn.getFloatData();
        float[] pflDataOut = pStrmOut.getFloatData();

        for (int iSam = 0; iSam < pStrmOut.getNumSamples(); iSam++) {
            float flOutput = 0;
            int iLeftLimit = Math.max(iSam - nKernelLen + 1, 0);
            int iRightLimit = Math.min(iSam + 1, pStrmIn.getNumSamples());
            for (int iK = iLeftLimit; iK < iRightLimit; iK++) {
                flOutput += pflDataIn[iK] * aflKernel[iSam - iK];
            }
            pflDataOut[iSam] = flOutput;
        }

        return hr;
    }

    public static int mix(DataStream pStrmIn1, float flVol1, DataStream pStrmIn2, float flVol2, DataStream pStrmOut) {
        if (!pStrmIn1.isNormalized() || !pStrmIn2.isNormalized())
            throw new IllegalArgumentException("pStrmIn1 or pStrmIn2");
        if (pStrmIn1.getSampleRate() != pStrmIn2.getSampleRate())
            throw new IllegalArgumentException("pStrmIn1.GetSampleRate() or pStrmIn2.GetSampleRate()");

        int nSamples = Math.min(pStrmIn1.getNumSamples(), pStrmIn2.getNumSamples());

        int hr;
        hr = pStrmOut.createData(pStrmIn1.getBitsPerSample(), pStrmIn1.getSampleRate(), nSamples, true);
        if (Utils.FAILED(hr))
            return hr;

        float[] pflDataIn1 = pStrmIn1.getFloatData();
        float[] pflDataIn2 = pStrmIn2.getFloatData();
        float[] pflDataOut = pStrmOut.getFloatData();

        for (int iSam = 0; iSam < nSamples; iSam++) {
            pflDataOut[iSam] = pflDataIn1[iSam] * flVol1 + pflDataIn2[iSam] * flVol2;
        }

        return hr;
    }
}