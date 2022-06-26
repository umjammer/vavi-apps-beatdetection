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

    protected float period;

    protected float previousBeatStrength;

    public Node() {
        selected = false;
        previousBeatStrength = 0;
    }

    public void initialize(float period) {
        // Create subcomponents
        net = new NodeTimingNet(this);

        csn = new NodeCSN(this);

        varSampler = new NodeVarSampler(this);

        // Calculate initial loop length and sampling rate
        int loopLen = (int) (period / params.samplerStartPeriod);
        float samplerPeriod = period / loopLen;
        // Set the loop period based on initial stats
        this.period = loopLen * samplerPeriod;
        // Set ideal period too
        setIdealPeriod(this.period);

        // Initialize Subcomponents
        net.initialize(loopLen);

        varSampler.initialize(samplerPeriod);

        csn.initialize();

        // Track Performance
        selectedTime = 0;
        beatReEvaluations = 0;
        avgPeriod = this.period;
        predictionError = 0;
        selectedBeats = 0;
    }

    public void executeStep(float[] inputBuffer) {
        // Pass input to variable sampler
        boolean[] complete = new boolean[1];
        float[] sample = new float[1];

        varSampler.processInput(inputBuffer, complete, sample);

        // Sample is complete and now flSample is valid, process rest of circuitry
        if (complete[0]) {
            // Generate next beat output before we modify the loop in any way
            net.generateBeatOutput();

            // Pass sample to TimingNet
            net.executeStep(sample);

            // Update CSN
            csn.updateCSN(net.netEnergy());
        }

        if (selected && params.trackPerformance)
            selectedTime++;
    }

    public void commitStep() {
        csn.commitCSN();
    }

    public void setIdealPeriod(float idealPeriod) {
        this.idealPeriod = 0.5f * this.idealPeriod + 0.5f * idealPeriod;
    }

    public void adjustPeriod() {

        float newPeriod;

        // PD Period Variation from Ideal Period
        if (params.enableVarSampler)
            // New period is that which is dictated by variable sampler
            newPeriod = varSampler().idealSamplePeriod() * net.loopLength();
        else
            // No VS, use the IOI period as the new period
            newPeriod = idealPeriod;

        // Adjustment strength is inversely proportional to the weighting from the VS expectation window
        float adjustStrength = 0.5f * (1 - varSampler().idealPeriodWeight());
        // Adjust new ideal period by AdjustStrength
        float idealPeriodDiff = (newPeriod - idealPeriod) * adjustStrength;

        newPeriod = idealPeriod + idealPeriodDiff;

        //

        // Set the new period
        period = newPeriod;

        // Set new sampling rate
        varSampler.period = period / net.loopLength();

        // Track Performance
        avgPeriod = 0.9f * avgPeriod + 0.1f * period;
    }

    // Accessors per sae
    public float period() {
        return period;
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

    public float idealPeriod;

    // Performance Measures
    public void calculatePerformanceMeasures() {
        predictionError /= selectedBeats;
        selectedTime /= params.onsetSamplingRate;
    }
}
