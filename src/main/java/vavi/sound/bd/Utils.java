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
        float onsetDetectResetTime;

        float onsetDetectThreshLow, onsetDetectThreshHigh;

        // Onset Detection and Assembly:
        //   Min onset distance
        //   Minimum threshold for onset output
        float onsetCombineMinDist;

        float onsetCombineMinOnset;

        // Onset Stream Output Sampling Rate
        int onsetSamplingRate;

        // Maximum difference in node periods that consitutes an identical node
        float nodeMaxDiff;

        // Variable Sampler
        //   Starting Sample Period
        //   Ratio of previous error vs current error for input to PD Controller
        //   Proportional and Differential Gains for the PD Controller
        boolean enableVarSampler;

        float samplerStartPeriod;

        float samplerMaxErrorTime;

        float expectationStdDevSamples;

        float expectationDeviancePercent;

        float samplerGainProp, samplerGainDiff;

        //   Fuzzy onset triangle distribution width
        float fuzzyOnsetWidth;

        // Timing Nets:
        //   Loop initial/min value
        //   Loop max value
        float loopInitValue;

        float loopMaxValue;

        // CSN:
        //   Alpha and Beta constants for the CSN linkage parameters
        //   CSN decay rate
        //   Minimum and maximum activation allowed
        //   Threshold multiplier for determining the top output from the CSN - Option 1
        //   Threshold in seconds for how long a loop must have max CSN output to be selected - Option 2
        float csnAlpha;

        float csnMinLink, csnMaxLink;

        float csnInputLink;

        float csnDecay;

        float csnMinAct, csnMaxAct;

        float csnOutputTimeThresh; // Option 2

        // Beat Detection Logic
        //   Threshold percentage between mean and max loop value to output beats (Option 1)
        float beatOutputMinThresh;

        // IOI Statistics Collector:
        //   max onset tracking time, histogram halflife
        //   dominant IOI list threshold low and high
        float ioiMaxOnsetTime, ioiHistHalflife;

        float ioiDomThreshRatio;

        // Performance Measures
        boolean trackPerformance;

        float trackBeginOffset;

        int trackChangeNodeCount;
    }

    static ParamsType params;
    static float bound(float x, float l, float u) {
        return Math.min(Math.max(x, l), u);
    }
}