/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import static vavi.sound.bd.Utils.params;


class NodeVarSampler {
    static final int ERROR_HIST_LENGTH = 5;

    static final float ERROR_HIST_ALPHA = 0.8f;

    protected float period;

    // Sampling and Fuzzy Onsets
    protected float endSam;

    protected float beginSam;

    protected int curSam;

    protected int lastOnset;

    protected float lastOnsetEnergy;

    protected float energyRemainder;

    // Sampling Rate Adjustment Variables
    protected float recentBeat, recentOnset;

    protected boolean foundBeat;

    protected float recentBeatEnergy;

    protected float lastBeatTime;

    protected float idealPeriodWeight;

    protected float lastExpWeight;

    // Ideal period according to the variable sampler
    protected float idealPeriod;

    protected Node node;

    public NodeVarSampler(Node node) {
        this.node = node;
    }

    public int Initialize(float samplerPeriod) {
        int hr = Utils.S_OK;

        // Sampler Init
        period = samplerPeriod;
        idealPeriod = samplerPeriod;

        beginSam = 0;
        endSam = period * params.onsetSamplingRate;
        curSam = 0;

        energyRemainder = 0;
        lastOnset = Integer.MIN_VALUE;
        lastOnsetEnergy = 0;
        lastBeatTime = 0;

        idealPeriodWeight = 0;
        lastExpWeight = 1;

        // Variable Sampler Init
        foundBeat = false;
        offset = 0;

        return hr;
    }

    public int processInput(float[] inputBuffer, boolean[] sampleComplete, float[] sample) {
        int hr = Utils.S_OK;

        // Onset?
        if (inputBuffer[0] > 0) {
            lastOnset = curSam;
            lastOnsetEnergy = inputBuffer[0];
        }

        // Increment current sample
        curSam += 1;

        // Complete this sample period?
        if (curSam > endSam) {
            sample[0] = 0;

            /////////////////////////////
            // Calculate fuzzy onset energy

            float fuzzyWidth = params.fuzzyOnsetWidth * params.onsetSamplingRate / 2;

            if (lastOnset > beginSam - fuzzyWidth) {
                // Which case is it?  Or none = no onset!
                //              |___________|
                //  |..W.....W..|..W.....W..|..W...
                //             A B    C    D E
                // Case A: Previous sampling interval within fuzzy width (W) of edge
                if (lastOnset < beginSam) {
                    // Remaining energy variable should be accurate, use it...
                    sample[0] = energyRemainder;
                }
                // Case B: This interval, but within fuzzy width (W) of the beginning
                else if (lastOnset < beginSam + fuzzyWidth) {
                    // Remaining energy variable not necessarily valid, calculate in full
                    float flDist = beginSam - (lastOnset - fuzzyWidth);
                    sample[0] = lastOnsetEnergy - (lastOnsetEnergy / (2 * fuzzyWidth * fuzzyWidth)) * flDist * flDist;
                }
                // Case C: This interval and not near an edge, so no fuzziness
                else if (lastOnset < endSam - fuzzyWidth) {
                    // No fuzzy energy sharing
                    sample[0] = lastOnsetEnergy;
                }
                // Case D: Withing fuzzy width of end of sampling interval
                else {
                    // Calculate energy and place remainder in remaining energy variable
                    float flDist = (lastOnset + fuzzyWidth) - endSam;
                    energyRemainder = (lastOnsetEnergy / (2 * fuzzyWidth * fuzzyWidth)) * flDist * flDist;
                    sample[0] = lastOnsetEnergy - energyRemainder;
                }
            } else {
                // Check Case E: Search beginning of next sampling interval in buffer for onset
                for (int searchSam = 1; searchSam < fuzzyWidth; searchSam++) {
                    if (inputBuffer[searchSam] > 0) {
                        // Calculate energy, don't bother with remaining energy since we'll always recalculate
                        float dist = (curSam - 1) + searchSam - endSam;
                        sample[0] = (inputBuffer[searchSam] / (2 * fuzzyWidth * fuzzyWidth)) * dist * dist;
                        break;
                    }
                }
            }

            /////////////////////////////
            // Variable Sampler Execution
            int hrTest = adjustSamplingRate();
            if (hrTest == Utils.S_OK)
                node.adjustPeriod();

            /////////////////////////////
            // Update variables, continue
            beginSam = endSam;
            endSam += period * params.onsetSamplingRate;
            sampleComplete[0] = true;
        } else {
            // Not yet complete
            sampleComplete[0] = false;
        }

        return hr;
    }

    public float samplePeriod() {
        return period;
    }

    public float idealSamplePeriod() {
        return idealPeriod;
    }

    public float idealPeriodWeight() {
        return idealPeriodWeight;
    }

    public float offset;

    // Testing
    public float error;

    public float differentialError;

    protected int adjustSamplingRate() {
        int hr = Utils.S_FALSE;

        // Get the current beat output (remember, we have not yet sent this sample out the door
        // so the beat output given here corresponds to the current range of samples)
        float beat = node.timingNet().beatOutputPrediction();

        // If a beat occurred, set it's location to be dead centre of sampling interval
        if (beat > 0) {
            // Record recent beat location and recent onset location
            foundBeat = true;
            recentBeat = beginSam + period / 2;
            recentBeatEnergy = beat;
            recentOnset = lastOnset;
            // But wait, what if a beat and onset come at the same time, well then we know that "last
            // onset" *MUST* be the closest onset by definition, so just go with it
        }

        if (foundBeat) {
            // We've found the beat, now we're looking after the beat for the possibility of finding
            // the nearest onset. But ONLY search as far as the minimum of the distance to the
            // most recent onset (because after that we know the previous onset is closer) and the 
            // maximum search distance (can't search forever)

            boolean adjust = false;
            float searchDist = endSam - recentBeat;
            float offset = 0;

            if (recentOnset != lastOnset) {
                // Last onset is not the only onset we've found - we're set to adjust!
                adjust = true;

                float dist1 = recentOnset - recentBeat;
                float dist2 = lastOnset - recentBeat;

                offset = Math.abs(dist1) < Math.abs(dist2) ? dist1 : dist2;
            } else if ((searchDist > (recentOnset - recentBeat))
                       || (searchDist > (params.varSamplerMaxErrorTime * params.onsetSamplingRate))) {
                // Searched too long, time to adjust
                adjust = true;

                offset = recentOnset - recentBeat;
            }

            //////////////////
            // Time to adjust?
            if (adjust && Math.abs(offset / params.onsetSamplingRate) < params.varSamplerMaxErrorTime) {
                // Track Performance
                if (node.selected) {
                    node.predictionError += offset * offset;
                    node.selectedBeats++;
                }

//                float sqrBeatEnergy = (float) Math.sqrt(recentBeatEnergy);

                // Anything 0.5 samples or smaller is the smallest error possible here, so treat
                // it as no error by setting the offset to zero
                if (Math.abs(offset) <= 0.5)
                    offset = 0;

                // Calculate expectancy weight            
                // StdDev proportional to loop length
                float stdDev = (node.period() * params.expectationDeviancePercent) * params.onsetSamplingRate;
                stdDev = Math.min(stdDev, params.expectationStdDevSamples * period * params.onsetSamplingRate);
                float weight = (float) Math.exp(-((offset * offset) / (stdDev * stdDev)));

                // Convert offset sample distance into time - use sampling rate of input
                // signal since the # of sample error is at this sampling rate
                offset /= params.onsetSamplingRate;

                // PD Controller Error Input
                float lastOffset = this.offset;
                this.offset = offset;

                ///////////////////////////////////////////////////////////////////////
                // PD Controller
                // Error in seconds error per sample of the loop
                error = this.offset / node.timingNet().loopLength();
                // Differential Error in seconds error change per sample of the loop
                differentialError = (this.offset - lastOffset) / (recentBeat - lastBeatTime);

                idealPeriod = (float) (period + weight * params.varSamplerGainProp * error
                                       + Math.sqrt(weight * lastExpWeight) * params.varSamplerGainDiff * differentialError);

                // Calculate expectation weighting for this period calculation
                //idealPeriodWeight = Math.sqrt(weight * lastExpWeight);
                lastExpWeight = weight;

                hr = Utils.S_OK;
                //////////////////////////////////////////////////////////////////////

                // Remember time of this beat for next time
                lastBeatTime = recentBeat;
                // We're done here so reset and start over
                foundBeat = false;
            }
        }

        return hr;
    }
}
