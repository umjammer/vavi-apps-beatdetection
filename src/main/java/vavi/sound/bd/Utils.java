/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


/**
 * BeatDetect Parameters
 */
class ParamsType {
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

    float varSamplerStartPeriod;

    float varSamplerMaxErrorTime;

    float expectationStdDevSamples;

    float expectationDeviancePercent;

    float varSamplerGainProp, varSamplerGainDiff;

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
    float flCSNAlpha;

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

public class Utils {
    static ParamsType params;
    static final int S_OK = 1;
    static final int S_FALSE = 0;
    static final int E_FAIL = 0;
    static boolean FAILED(int status) {
        return status == S_FALSE;
    }
    static boolean SUCCEEDED(int status) {
        return status == S_OK;
    }
    static final float BOUND(float x, float l, float u) {
        return Math.min(Math.max(x, l), u);
    }
}