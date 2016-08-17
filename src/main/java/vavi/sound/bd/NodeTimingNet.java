/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.util.List;


class NodeTimingNet {

    // Timing Net
    protected List<Float> netList;

    protected int loopLen;

    // Integrator
    protected float integrator;

    // Beat Output   
    protected int loopLoc;

    protected float beatThresh;

    protected float loopMean;

    protected float loopEnergy;

    protected float loopMax;

    protected float beatOutputPrediction;

    // Beat Output Version 2
    protected int beatLoc;

    protected int candidateLoc;

    protected int beatLife;

    protected Node node;

    public NodeTimingNet(Node node) {
        this.node = node;
        loopLen = 0;
        integrator = 0;
        loopLoc = 0;
        loopMax = 0;
        loopEnergy = 0;
        loopMean = 0;
        beatThresh = Integer.MAX_VALUE;
        beatOutputPrediction = 0;
        beatLoc = -1;
        candidateLoc = -1;
        beatLife = 0;
        beatStrength = 0;
    }

    public int initialize(int loopLen) {
        //////
        // Timing Net
        // Create loop of specified length
        netList.clear();
        this.loopLen = loopLen;
        for (int i = 0; i < loopLen; i++) {
            netList.add(0, Utils.params.loopInitValue);
        }

        //////
        // BeatOutput
        loopLoc = loopLength();

        return Utils.S_OK;
    }

    // Timing Net Execution
    public int executeStep(float[] input) {
        // Translate onset value to Sigmoid input value (may need fuzzy stuff here later)
        if (input[0] < 0.05f) {
            input[0] = ((0.05f - input[0]) / 0.05f) * -0.5f;
        } else {
            input[0] = input[0] - 0.05f;
            input[0] = Math.min(input[0], 1.0f);
        }

        ////////////////////////////////
        // Sigmoid Growth/Decay function
        ////////////////////////////////
        float newValue;
        float oldValue = netList.get(0);

        newValue = oldValue + input[0] * oldValue * (1 - oldValue);
        newValue = Math.max(newValue, Utils.params.loopInitValue);
        newValue = Math.min(newValue, Utils.params.loopMaxValue);

        netList.remove(0);
        netList.add(newValue);

        ////////////////////////////////////
        // Update Loop stats
        // Decrement loop count
        loopLoc--;
        if (loopLoc == 0)
            loopLoc = loopLength();

        updateLoopStats();

        //beatThresh = loopMean * (1 - params.flBeatOutputThresh) + loopMax * params.flBeatOutputThresh;    
        //beatThresh = Math.max(beatThresh, params.flBeatOutputMinThresh);
        ////////////////////////////////////

        //////
        // Update leaky integrator
        // Int = alpha * Int + beta * input
        //integrator = m_flIntDecay * integrator + m_flIntGrow * flNewValue;

        //integrator = loopEnergy;
        //integrator = 0.9 * integrator + 0.1 * (loopMean*loopMean) * 50;
        integrator = 0.9f * integrator + 0.1f * loopEnergy * 10;

        return Utils.S_OK;
    }

    // Beat Output Methods
    public float beatOutputPrediction() {
        return beatOutputPrediction;
    }

    public boolean loopComplete() {
        return loopLoc == loopLength();
    }

    public float netEnergy() {
        return integrator;
    }

    public int loopLength() {
        return loopLen;
    }

    public List<Float> net() {
        return netList;
    }

    public void generateBeatOutput() {
        beatOutputPrediction = 0;

        int beat = 0;
        ++beat;

        // Beat Threshold Level Option
        // Front is the corresponding location to the current set of input samples
        // 2nd to front is the next beat output - the one we want to predict
        // (remember this is often called just before finishing the processing for a group of samples)
        // Check 2nd from front against its neighbours to ensure it is bigger (ie a peak exists)
//        int other = 0;
//        boolean peak = (netList.get(beat) > netList.get(other));
//        other++;
//        other++;
//        peak = peak && (beat > other);
        // A peak is defined as a loop neuron with activation that is large that of both
        // neurons on either side (ahead and behind)
//        if (peak) {
//            if (netList.get(beat) >= beatThresh)
//                beatOutputPrediction = netList.get(beat);
//        }

        // One Beat Output Option
        if (beatLoc == -1) {
            // No beat location exists, assign maximum
            if ((netList.get(beat) == loopMax) && (netList.get(beat) > Utils.params.loopInitValue)) {
                beatLoc = loopLength();
                beatOutputPrediction = netList.get(beat);
                beatStrength = netList.get(beat);
                beatLife = 2;
            }
        } else {
            // Elapsed beat location?  Output beat
            beatLoc -= 1;

            if (beatLoc == 0) {
                beatLoc = loopLength();
                beatOutputPrediction = netList.get(beat);
                beatStrength = netList.get(beat);
                // Is this the max?  Yes, reset life, no decrement life
                if (netList.get(beat) < loopMax) {
                    beatLife -= 1;
                    if (beatLife <= 0) {
                        beatLoc = -1;
                        // Track Performance - Count number of revaluations of beat location
                        if (node.selected)
                            node.beatReEvaluations++;
                    }
                } else {
                    beatLife = 2;
                }
            }
        }

        //beatOutputPrediction = beat;
    }

    public float beatStrength;

    protected void updateLoopStats() {
        float mean = 0, energy = 0, max = 0;

        for (float net : netList) {
            mean += net;
            energy += net;
            if (net > max)
                max = net;
        }
        loopMean = mean / loopLength();
        loopEnergy = (float) (Math.sqrt(energy) / loopLength());
        loopMax = max;
    }
}
