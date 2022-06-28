/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.util.ArrayList;
import java.util.List;
import static vavi.sound.bd.Utils.params;


class IOIStatCollector {

    static class IOIPeriodEntry {
        float period;

        int refCount;

        float highestEnergy;
    }

    static class IOIStats {
        static final int IOISTATS_HISTLEN = 2000;

        float[] ioiHists = new float[IOISTATS_HISTLEN];

        List<IOIPeriodEntry> dominantIOIs = new ArrayList<>();
    }

    static final int IOISTATS_PARZEN_HALF_WINDOW_SIZE = 5;

    static final int IOISTATS_PARZEN_WINDOW_SIZE = (IOISTATS_PARZEN_HALF_WINDOW_SIZE * 2 + 1);

    protected List<Integer> onsets = new ArrayList<>();

    protected int lastOnsetDelay;

    protected int maxIOI;

    protected float[] parzenWindow = new float[IOISTATS_PARZEN_WINDOW_SIZE];

    public IOIStatCollector() {
        maxIOI = 0;
    }

    void initialize(IOIStats stats) {
        // Reset onset list
        onsets.clear();

        // Clear IOIStats Histogram and Dominant IOI List
        stats.dominantIOIs.clear();

        lastOnsetDelay = 0;

        // Create Parzen Window
        float var = (float) (IOISTATS_PARZEN_HALF_WINDOW_SIZE * IOISTATS_PARZEN_HALF_WINDOW_SIZE) / 5;
        for (int i = 0; i < IOISTATS_PARZEN_WINDOW_SIZE; i++) {
            float x = i - IOISTATS_PARZEN_HALF_WINDOW_SIZE;

            parzenWindow[i] = (float) Math.exp(-x * x / var);
        }
    }

    void executeStep(float sample, IOIStats stats) {
        lastOnsetDelay += 1;

        if (sample > 0) {
            /////
            // We have received an onset!

            maxIOI = (int) (params.ioiMaxOnsetTime * params.onsetSamplingRate);
            assert maxIOI < IOIStats.IOISTATS_HISTLEN;

            // Increment onset times by newest IOI
            onsets.replaceAll(i -> i + lastOnsetDelay);

            // Add newest IOI to queue
            onsets.add(0, lastOnsetDelay);

            // Look through list for IOIs that are greater than MaxIOI and remove
            while (onsets.get(onsets.size() - 1) > maxIOI) {
                onsets.remove(onsets.size() - 1);
            }

            // Update the IOIStats Histogram
            // Decay Histogram - by amount proportional to passed time
            // lastOnsetDelay * period equals time passed since last onset
            float decay = (float) Math.pow(0.5, ((float) lastOnsetDelay / params.onsetSamplingRate) / params.ioiHistHalflife);
            for (int i = 0; i < IOIStats.IOISTATS_HISTLEN; i++) {
                stats.ioiHists[i] *= decay;
            }

            // Grow Histogram - inversely proportional to histogram half-life
            // 0.693 = ln2 : FROM SEPPANNEN
//            float grow = 0.693f / params.ioiHistHalflife;
            for (int i : onsets) {
                // Use parzen window growth
                for (int parzen = Math
                        .max(0, i - IOISTATS_PARZEN_HALF_WINDOW_SIZE); parzen < i
                                                                                + IOISTATS_PARZEN_HALF_WINDOW_SIZE; parzen++) {
                    stats.ioiHists[parzen] += parzenWindow[parzen - (i - IOISTATS_PARZEN_HALF_WINDOW_SIZE)];
                }
            }

            // Find Peak IOIs
            findDominantIOIs(1f / params.onsetSamplingRate, stats);

            // Reset last delay...
            lastOnsetDelay = 0;
        }
    }

    protected void findDominantIOIs(float period, IOIStats stats) {
        // Empty the list and regenerate
        stats.dominantIOIs.clear();

        // Collect Stats (Mean, Max)
        float mean = 0, max = 0;
        for (int i = 0; i < maxIOI; i++) {
            mean += stats.ioiHists[i];
            if (stats.ioiHists[i] > max)
                max = stats.ioiHists[i];
        }
        mean /= maxIOI;

        // Threshold as ratio between max and mean
        float thresh = max * params.ioiDomThreshRatio + mean * (1 - params.ioiDomThreshRatio);

        // Add IOIs to list that are over threshold
        // Start search at minimum allowed period since should never be that low and simplifies code
        int startIOI = (int) (0.24f * params.onsetSamplingRate);
        for (int i = startIOI; i < maxIOI; i++) {
            if (stats.ioiHists[i] > thresh) {
                // Sufficient intensity, look for peak
                boolean peak = (stats.ioiHists[i] > stats.ioiHists[i - 1]) && (stats.ioiHists[i] > stats.ioiHists[i - 2])
                               && (stats.ioiHists[i] > stats.ioiHists[i + 1]) && (stats.ioiHists[i] > stats.ioiHists[i + 2]);

                // Add this dominant IOI to list
                if (peak) {
                    // Use Newton Interpolation (pg 104 of notes)
                    // Three point interpolation, finding maximum (peak)
                    IOIPeriodEntry entry = new IOIPeriodEntry();
                    entry.period = (i - 0.5f) - ((stats.ioiHists[i] - stats.ioiHists[i - 1])
                                                 / (stats.ioiHists[i + 1] - 2 * stats.ioiHists[i] + stats.ioiHists[i - 1]));
                    entry.period *= period;
                    entry.refCount = 0;
                    entry.highestEnergy = 0;
                    stats.dominantIOIs.add(entry);
                }
            }
        }
    }
}
