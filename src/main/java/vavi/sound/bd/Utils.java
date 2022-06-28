/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


public class Utils {

    /**
     * BeatDetect Parameters
     */
    static class ParamsType {
        // Onset Detection:
        //   Onset detection minimum onset distance
        //   Onset detection threshold top and bottom levels for hysteresis
        float onsetDetectResetTime = 0.1f; // 100 ms;

        float onsetDetectThreshLow = -0.035f; // From Sepannen
        float onsetDetectThreshHigh = 0.06f; // 0.06 is From Sepannen

        // Onset Detection and Assembly:
        //   Min onset distance
        //   Minimum threshold for onset output
        float onsetCombineMinDist = 0.05f; // 50 ms

        float onsetCombineMinOnset = 0.1f;

        // Onset Stream Output Sampling Rate
        final int onsetSamplingRate = 441;

        // Maximum difference in node periods that consitutes an identical node
        float nodeMaxDiff;

        // Variable Sampler
        //   Starting Sample Period
        //   Ratio of previous error vs current error for input to PD Controller
        //   Proportional and Differential Gains for the PD Controller
        boolean enableVarSampler = true;

        float samplerStartPeriod = 0.02f; // 50 Hz

        float samplerMaxErrorTime = 0.06f; // DETERMINE EXPERIMENTALLY: Max jitter offset = ~3 samples

        float expectationStdDevSamples = 2; // From Cemgil et al

        float expectationDeviancePercent = 0.08f; // From Krumhansl, 2000

        float samplerGainProp = 1.0f; // Proportional Gain
        float samplerGainDiff = 1.0f; // Differential Gain

        //   Fuzzy onset triangle distribution width
        float fuzzyOnsetWidth = samplerStartPeriod;

        // Timing Nets:
        //   Loop initial/min value
        //   Loop max value
        float loopInitValue = (float)Math.pow(2, -5);;

        float loopMaxValue = 1 - loopInitValue;

        // CSN:
        //   Alpha and Beta constants for the CSN linkage parameters
        //   CSN decay rate
        //   Minimum and maximum activation allowed
        //   Threshold multiplier for determining the top output from the CSN - Option 1
        //   Threshold in seconds for how long a loop must have max CSN output to be selected - Option 2
        float csnAlpha = 5.0f;

        float csnMinLink = -0.03f;
        float csnMaxLink = 0.04f;

        float csnInputLink = 0.2f;

        float csnDecay = 0.8f;;

        float csnMinAct = -1;
        float csnMaxAct = 1;

        float csnOutputTimeThresh = 1.0f; // One second as best required to be selected

        // Beat Detection Logic
        //   Threshold percentage between mean and max loop value to output beats (Option 1)
        float beatOutputMinThresh  = 0.0f; //

        // IOI Statistics Collector:
        //   max onset tracking time, histogram halflife
        //   dominant IOI list threshold low and high
        float ioiMaxOnsetTime = 1.2f;
        float ioiHistHalflife = 2.0f;

        float ioiDomThreshRatio = 0.25f;

        // Performance Measures
        boolean trackPerformance = false;

        float trackBeginOffset = 8.0f;

        int trackChangeNodeCount = 0;
    }

    static ParamsType params = new ParamsType();
    static float bound(float x, float l, float u) {
        return Math.min(Math.max(x, l), u);
    }
}