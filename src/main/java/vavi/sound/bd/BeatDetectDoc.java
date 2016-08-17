/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.io.IOException;
import java.util.Arrays;


class BeatDetectDoc {

    BeatDetect.View view;

    protected BeatDetectDoc(BeatDetect.View view) {
        this.view = view;
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

    public boolean onOpenDocument(String pathName) {
        try {
            view.showWaiting();

            //
            // Beat Processing...
            //

            int hr = input.loadFromWaveFile(pathName);
            if (Utils.FAILED(hr))
                return hr == Utils.S_OK;
            hr = input.normalize();
            if (Utils.FAILED(hr))
                return hr == Utils.S_OK;

            assert input.getSampleRate() == 44100;

            // Stage 1: Onset
            OnsetStage stage1 = new OnsetStage();
            hr = stage1.createOnsetStream(input, onsetOutput, onsetInternal);
            if (Utils.FAILED(hr))
                return hr == Utils.S_OK;

            // Stage 2: RealTime Stage
            RealTimeStage stage2 = new RealTimeStage();
            hr = stage2.createBeatStream(onsetOutput, beatOutput, beatTempo, beatPeriod, beatInfo);
            if (Utils.FAILED(hr))
                return hr == Utils.S_OK;

            //
            // .. End Beat Processing
            //

            view.hideWaiting();

            // Automation - Save and Exit
            if (BeatDetect.theApp.automate) {
                // Save output
                String[] saveName = pathName.split(".");
                onSaveDocument(saveName[0] + "_Output" + saveName[1]);
            }

            return hr == Utils.S_OK;
        } catch (IOException e) {
            view.showError(e);
            return false;
        }
    }

    public boolean onSaveDocument(String pathName) {
        try {
            // Temporary Data Saver
            //m_ASOnsetLPF.SaveToWaveFile( lpszPathName );

            AudioStream click = new AudioStream(), rendered = new AudioStream();

            int hr = renderClickTrack(beatOutput, click);
            if (Utils.FAILED(hr))
                return false;

            hr = DSP.mix(click, 1, input, 0.75f, rendered);
            if (Utils.FAILED(hr))
                return false;

            hr = rendered.deNormalize(2 * 8);
            if (Utils.FAILED(hr))
                return false;

            hr = rendered.saveToWaveFile(pathName);
            if (Utils.FAILED(hr))
                return false;
        } catch (IOException e) {
            view.showError(e);
        }
        return true;
    }

    // Generated message map functions
    protected int renderClickTrack(DataStream in, AudioStream out) throws IOException {
        int hr = Utils.S_OK;

        double decFactor = 44100 / in.getSampleRate();

        // Create click track at 44100Hz, 16bits, with correct number of samples
        hr = out.createData(4 * 8, 44100, (int) (in.getNumSamples() * decFactor), true);
        if (Utils.FAILED(hr))
            return hr;

        // Load click sound
        AudioStream click = new AudioStream();
        hr = click.loadFromWaveFile("click.wav");
        if (Utils.FAILED(hr))
            return hr;
        hr = click.normalize();
        if (Utils.FAILED(hr))
            return hr;

        float[] dataOut = out.getFloatData();
        float[] dataIn = in.getFloatData();
        float[] dataClick = click.getFloatData();

        double outSam = 0;
        boolean clicked = false;
        for (int inSam = 0; inSam < in.getNumSamples(); inSam++) {
            if ((dataIn[inSam] > 0) && !clicked) {
                // Found a click, render it
                int len = Math.min(click.getNumSamples(), out.getNumSamples() - (int) outSam - 1);
                System.arraycopy(dataClick, 0, dataOut, (int) outSam, 4 * len);
                clicked = true;
            } else {
                // No click here, fill with zeros
                Arrays.fill(dataOut, (int) outSam, (int) outSam + 4 * ((int) (outSam + decFactor) - (int) outSam), 0);
                if (dataIn[inSam] == 0)
                    clicked = false;
            }
            outSam += decFactor;
        }

        return hr;
    }

    protected void onSaveOnsets() {
        try {
            // Temporary Data Saver
            //onsetLPF.saveToWaveFile(pathName);

            String newName = view.chooseFile();

            AudioStream click = new AudioStream(), rendered = new AudioStream();

            int hr = renderClickTrack(onsetOutput, click);
            if (Utils.FAILED(hr))
                return;

//        if (true) {
            hr = DSP.mix(click, 1, input, 0.75f, rendered);
            if (Utils.FAILED(hr))
                return;

            hr = rendered.deNormalize(2 * 8);
            if (Utils.FAILED(hr))
                return;

            hr = rendered.saveToWaveFile(newName);
            if (Utils.FAILED(hr))
                return;
//        } else {
//            hr = click.deNormalize(2 * 8);
//            if (Utils.FAILED(hr))
//                return;
//
//            hr = click.saveToWaveFile(newName);
//            if (Utils.FAILED(hr))
//                return;
//        }
        } catch (IOException e) {
            view.showError(e);
        }
    }
}
