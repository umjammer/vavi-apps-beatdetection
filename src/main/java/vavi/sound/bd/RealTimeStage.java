/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.IOException;

import javax.swing.JOptionPane;

import static vavi.sound.bd.Utils.params;


class RealTimeStage {

    static final boolean OUTPUT_ONSETS = false;

    static final boolean OUTPUT_ACTUALBEATS = false;

    static final boolean FIND_BEATS = true;

    /**
     * @param in Onset stream, constant tempo
     * @param out Beat stream, constant tempo
     * @param tempo Sampling period, constant tempo
     * @param beatPeriod Winning loop period, constant tempo
     * @param beatInfo Extra Info
     * @throws IllegalStateException creation failed
     */
    public void createBeatStream(DataStream in,
                                DataStream out,
                                DataStream tempo,
                                DataStream beatPeriod,
                                DataStream beatInfo) throws IOException {
        // Debug
        FileWriter writer = new FileWriter();
        if (OUTPUT_ONSETS) {
            writer.open("Onsets.m", true);
        } else if (OUTPUT_ACTUALBEATS) {
            writer.open("ActualBeats.m", true);
        }

        // Create streams with same info as input stream
        out.createData(in);
        tempo.createData(in);
        beatPeriod.createData(in);
        beatInfo.createData(in);

        // Setup Param specifying onset input sampling rate
        assert params.onsetSamplingRate == in.getSampleRate();

        if (FIND_BEATS) {
            // Components Setup
            IOIStatCollector.IOIStats ioiStats = new IOIStatCollector.IOIStats();

            IOIStatCollector ioiCollector = new IOIStatCollector();
            ioiCollector.initialize(ioiStats);

            NodeControl nodeControl = new NodeControl();
            nodeControl.initialize();

            // Execute

            // Create sample "buffer" - points to actual onset stream data but could theoretically be
            // a finite length buffer (10 ms maybe) used by the fuzzy onset creation for look ahead
            float[] sampleBuffer = in.getFloatData();
            // Current input sample #
            int curSam = 0;
            Node bestNode = null;

            while (curSam < in.getNumSamples()) {
                // Realtime Step

                // Track Performance
                if (!params.trackPerformance && ((float) curSam / params.onsetSamplingRate > params.trackBeginOffset))
                    params.trackPerformance = true;

                // IOI Stats Collector - pass in only current sample
                ioiCollector.executeStep(sampleBuffer[0], ioiStats);

                // Node Control
                nodeControl.executeStep(sampleBuffer, ioiStats);
                // Find best node
                bestNode = nodeControl.bestNode();

                // Update and Output
                if (null != bestNode) {
                    // Output Samples
                    out.getData().asFloatBuffer().put(curSam, bestNode.beatOutput());
                    tempo.getData()
                            .asFloatBuffer()
                            .put(curSam, (bestNode.varSampler().samplePeriod() - Utils.params.samplerStartPeriod) * 1000);//(bestNode.varSampler().samplePeriod() - params.samplerStartPeriod) * 1000;//
                    beatPeriod.getData().asFloatBuffer().put(curSam, bestNode.period());//bestNode.varSampler().flE * 1000;//bestNode.varSampler().m_flOffset * 100;//
                    beatInfo.getData().asFloatBuffer().put(curSam, bestNode.loopComplete() ? 10 : 0);//bestNode.varSampler().fldE * 1000;//(bestNode.varSampler().IdealSamplePeriod() - params.samplerStartPeriod) * 1000;//bestNode.csnOutput();//bestNode.varSampler().idealSamplePeriod() - bestNode.varSampler().samplePeriod()) * 1000;//
                } else {
                    out.getData().asFloatBuffer().put(curSam, -1);
                    tempo.getData().asFloatBuffer().put(curSam, 0);
                    beatPeriod.getData().asFloatBuffer().put(curSam, 0);
                    beatInfo.getData().asFloatBuffer().put(curSam, 0);
                }

//              if (curSam % 500 == 0) {
//                  String label = String.format("IOIHist_%d", curSam);
//                  writer.writeFloatArray(label, IOIStats.ioiHists, 800);
//              }

                sampleBuffer[0]++;
                curSam++;
            }

            // Calculate Performance Measures
            if (null != bestNode) {
                Node longestNode = bestNode;
                for (Node node : nodeControl.nodes) {
                    if (node.selectedTime > longestNode.selectedTime) {
                        longestNode = node;
                    }
                }
                longestNode.calculatePerformanceMeasures();

                if (!BeatDetect.theApp.automate) {
                    float bpm = 60 / longestNode.avgPeriod;
                    float percentTime = (float) (longestNode.selectedTime / (in.getDuration() - params.trackBeginOffset));
                    String message = String.format("%% Time = %.2f\n%.2f BPM\n%.2f Error\n%d Beat Re-eval\n%d Node Changes",
                                           percentTime * 100,
                                           bpm,
                                           Math.sqrt(longestNode.predictionError),
                                           longestNode.beatReEvaluations,
                                           params.trackChangeNodeCount);
                    JOptionPane.showConfirmDialog(null, message);
                }
            }

            //

            FileWriter writer2 = new FileWriter();
            writer2.open("Beats.m", true);
            //writer2.writeFloatArray("SamplePeriod", tempo.getFloatData(), tempo.getNumSamples());
            //writer2.writeFloatArray("MaxLoopPeriod", beatPeriod.getFloatData(), beatPeriod.getNumSamples());
            writer2.writeFloatArray("BeatOutput", out.getFloatData(), out.getNumSamples());
            //writer2.writeFloatArray("BeatInfo", beatInfo.getFloatData(), beatInfo.getNumSamples());
            //writer2.writeTimingLoops(NodeControl.nodes);
            writer2.close();
        }

        if (OUTPUT_ONSETS) {
            //writer.writeFloatArray("IOIHist_End", ioiStats.aflIOIHist, 800);
            //writer.writeFloatList("IOIDom", ioiStats.lstDominantIOI);
            writer.writeFloatArray("Onsets", in.getFloatData(), in.getNumSamples());
            writer.close();
        } else if (OUTPUT_ACTUALBEATS) {
            writer.writeFloatArray("ActualBeats", in.getFloatData(), in.getNumSamples());
            writer.close();
        }

        //WriteFloatArrayToMFile("OnsetStream.m", "OnsetStream", in.getFloatData(), in.getNumSamples());
    }
}
