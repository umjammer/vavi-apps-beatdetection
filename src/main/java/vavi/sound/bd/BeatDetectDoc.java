/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;


class BeatDetectDoc {

    BeatDetectDoc() {
        input = new AudioStream();
        IntStream.range(0, inputBands.length).forEach(i -> inputBands[i] = new AudioStream());
        onsetOutput = new DataStream();
        onsetInternal = new DataStream();
        beatOutput = new DataStream();
        beatTempo = new DataStream();
        beatPeriod = new DataStream();
        beatInfo = new DataStream();
    }

    public AudioStream input;

    public AudioStream[] inputBands = new AudioStream[6];

    // Onset Detect Streams
    public DataStream onsetOutput;

    public DataStream onsetInternal;

    // Beat Detect Streams
    public DataStream beatOutput;

    public DataStream beatTempo;

    public DataStream beatPeriod;

    public DataStream beatInfo;

    public boolean onNewDocument() {

        // Release all streams
        input.releaseData();
        for (int i = 0; i < OnsetStage.NUM_BANDS; i++)
            inputBands[i].releaseData();
        beatOutput.releaseData();
        beatPeriod.releaseData();
        beatTempo.releaseData();
        beatInfo.releaseData();
        onsetOutput.releaseData();
        onsetInternal.releaseData();
        //
        for (int x = 0; x < 6; x++)
            inputBands[x].releaseData();

        return true;
    }

    public void onOpenDocument(String pathName) throws IOException {
        //
        // Beat Processing...
        //

        input.loadFromWaveFile(pathName);
        input.normalize();

        assert input.getSampleRate() == 44100;

        // Stage 1: Onset
        OnsetStage stage1 = new OnsetStage();
        stage1.createOnsetStream(input, onsetOutput, onsetInternal);

        // Stage 2: RealTime Stage
        RealTimeStage stage2 = new RealTimeStage();
        stage2.createBeatStream(onsetOutput, beatOutput, beatTempo, beatPeriod, beatInfo);

        //
        // .. End Beat Processing
        //

        // Automation - Save and Exit
        if (BeatDetect.theApp.automate) {
            // Save output
            String[] saveName = pathName.split("\\.");
            onSaveDocument(saveName[0] + "_Output" + saveName[1]);
        }
    }

    /** @throws IllegalStateException creation failed */
    public void onSaveDocument(String pathName) throws IOException {
        // Temporary Data Saver
        //onsetLPF.saveToWaveFile(pathName);

        AudioStream click = new AudioStream(), rendered = new AudioStream();

        renderClickTrack(beatOutput, click);

        DSP.mix(click, 1, input, 0.75f, rendered);

        rendered.denormalize(2 * 8);

        rendered.saveToWaveFile(pathName);
    }

    /**
     * Generated message map functions
     * @throws IllegalStateException creation failed
     */
    protected void renderClickTrack(DataStream in, AudioStream out) throws IOException {
        double decFactor = 44100d / in.getSampleRate();

        // Create click track at 44100Hz, 16bits, with correct number of samples
        out.createData(4 * 8, 44100, (int) (in.getNumSamples() * decFactor), true);

        // Load click sound
        AudioStream click = new AudioStream();
        click.loadFromWaveFile("click.wav");
        click.normalize();

        float[] dataOut = out.getFloatData();
        float[] dataIn = in.getFloatData();
        float[] dataClick = click.getFloatData();

        double op = 0;
        boolean clicked = false;
        for (int ip = 0; ip < in.getNumSamples(); ip++) {
            if ((dataIn[ip] > 0) && !clicked) {
                // Found a click, render it
                int len = Math.min(click.getNumSamples(), out.getNumSamples() - (int) op - 1);
                System.arraycopy(dataClick, 0, dataOut, (int) op, 4 * len);
                clicked = true;
            } else {
                // No click here, fill with zeros
                Arrays.fill(dataOut, (int) op, (int) op + 4 * ((int) (op + decFactor) - (int) op), 0);
                if (dataIn[ip] == 0)
                    clicked = false;
            }
            op += decFactor;
        }
    }

    /** @throws IllegalStateException creation failed */
    protected void onSaveOnsets(String newName) throws IOException {
        // Temporary Data Saver
        //onsetLPF.saveToWaveFile(pathName);

        AudioStream click = new AudioStream(), rendered = new AudioStream();

        renderClickTrack(onsetOutput, click);

//        if (true) {
        DSP.mix(click, 1, input, 0.75f, rendered);

        rendered.denormalize(2 * 8);

        rendered.saveToWaveFile(newName);
//        } else {
//            hr = click.deNormalize(2 * 8);
//            if (Utils.FAILED(hr))
//                return;
//
//            hr = click.saveToWaveFile(newName);
//            if (Utils.FAILED(hr))
//                return;
//        }
    }
}
