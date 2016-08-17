/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import static vavi.sound.bd.Utils.params;


class Node {

    public boolean selected;

    public float selectedTime;

    public int beatReEvaluations;

    public float avgPeriod;

    public float predictionError;

    public int selectedBeats;

    protected NodeTimingNet net;

    protected NodeCSN csn;

    protected NodeVarSampler varSampler;

    protected float m_flPeriod;

    protected float previousBeatStrength;

    public Node() {
        selected = false;
        previousBeatStrength = 0;
    }

    public int initialize(float period) {
        int hr = Utils.S_OK;

        ///////////////////////
        // Create subcomponents
        net = new NodeTimingNet(this);

        csn = new NodeCSN(this);

        varSampler = new NodeVarSampler(this);

        /////////////////////////
        // Calculate initial loop length and sampling rate
        int loopLen = (int) (period / params.varSamplerStartPeriod);
        float samplerPeriod = period / loopLen;
        // Set the loop period based on initial stats
        m_flPeriod = loopLen * samplerPeriod;
        // Set ideal period too
        setIdealPeriod(m_flPeriod);

        ///////////////////////////
        // Initialize Subcomponents
        hr = net.initialize(loopLen);
        if (Utils.FAILED(hr))
            return hr;

        hr = varSampler.Initialize(samplerPeriod);
        if (Utils.FAILED(hr))
            return hr;

        hr = csn.Initialize();
        if (Utils.FAILED(hr))
            return hr;

        ////////////////////////////
        // Track Performance
        selectedTime = 0;
        beatReEvaluations = 0;
        avgPeriod = m_flPeriod;
        predictionError = 0;
        selectedBeats = 0;

        return hr;
    }

    public int executeStep(float[] inputBuffer) {
        int hr = Utils.S_OK;

        ///////
        // Pass input to variable sampler
        boolean[] complete = new boolean[1];
        float[] sample = new float[1];

        hr = varSampler.processInput(inputBuffer, complete, sample);
        if (Utils.FAILED(hr))
            return hr;

        ///////
        // Sample is complete and now flSample is valid, process rest of circuitry
        if (complete[0]) {
            // Generate next beat output before we modify the loop in any way
            net.generateBeatOutput();

            // Pass sample to TimingNet
            hr = net.executeStep(sample);

            // Update CSN
            hr = csn.updateCSN(net.netEnergy());
        }

        if (selected && params.trackPerformance)
            selectedTime++;

        return hr;
    }

    public int commitStep() {
        return csn.commitCSN();
    }

    public void setIdealPeriod(float idealPeriod) {
        m_flIdealPeriod = 0.5f * m_flIdealPeriod + 0.5f * idealPeriod;
    }

    public int adjustPeriod() {

        float newPeriod;

        //////////////////////////////////////////////////////////////////////
        // PD Period Variation from Ideal Period
        if (params.enableVarSampler)
            // New period is that which is dictated by variable sampler
            newPeriod = varSampler().idealSamplePeriod() * net.loopLength();
        else
            // No VS, use the IOI period as the new period
            newPeriod = m_flIdealPeriod;

        // Adjustment strength is inversely proportional to the weighting from the VS expectation window
        float adjustStrength = 0.5f * (1 - varSampler().idealPeriodWeight());
        // Adjust new ideal period by AdjustStrength
        float idealPeriodDiff = (newPeriod - m_flIdealPeriod) * adjustStrength;

        newPeriod = m_flIdealPeriod + idealPeriodDiff;
        //////////////////////////////////////////////////////////////////////

        // Set the new period
        m_flPeriod = newPeriod;

        float newSamplingPeriod = m_flPeriod / net.loopLength();

        // Set new sampling rate
        varSampler.period = newSamplingPeriod;

        // Track Performance
        avgPeriod = 0.9f * avgPeriod + 0.1f * m_flPeriod;

        return Utils.S_OK;
    }

    // Accessors per sae
    public float period() {
        return m_flPeriod;
    }

    public float csnOutput() {
        return csn().csnOutput();
    }

    public float beatOutput() {
        return timingNet().beatOutputPrediction();
    }

    public boolean loopComplete() {
        return timingNet().loopComplete();
    }

    // Semi-Public
    public NodeTimingNet timingNet() {
        return net;
    }

    public NodeCSN csn() {
        return csn;
    }

    public NodeVarSampler varSampler() {
        return varSampler;
    }

    public float m_flIdealPeriod;

    // Performance Measures
    public int calculatePerformanceMeasures() {
        predictionError /= selectedBeats;
        selectedTime /= params.onsetSamplingRate;

        return Utils.S_OK;
    }
}
