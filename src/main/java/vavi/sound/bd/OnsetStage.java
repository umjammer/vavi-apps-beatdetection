/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import static vavi.sound.bd.Utils.params;


class OnsetStage {
    static final int NUM_BANDS = 8; //9
    //////////////////////////////////////////////////////////////////////
    //Filters
    //////////////////////////////////////////////////////////////////////

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //LIMITING BANDS TO 4 - CHECK THROUGH THIS CODE IF YOU WANT TO CHANGE
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    static final FILTER_6TH_COEFF[] IIR_BAND_FILTERS = {
        // LP: 110
        new FILTER_6TH_COEFF(new double[] {
            1, -5.939446833066, 14.69906478643, -19.40175578925, 14.40534742567, -5.704452460387, 0.9412428706195
        }, new double[] {
            2.246293429042e-013, 1.347776057425e-012, 3.369440143564e-012, 4.492586858085e-012, 3.369440143564e-012,
            1.347776057425e-012, 2.246293429042e-013
        }, 301),
        // BP: 110-220
        new FILTER_6TH_COEFF(new double[] {
            1, -5.967189626739, 14.83793472029, -19.67980659146, 14.68370868926, -5.843788343797, 0.9691411525664
        }, new double[] {
            4.737344671204e-007, 0, -1.421203401361e-006, 0, 1.421203401361e-006, 0, -4.737344671204e-007
        }, 302),
        // BP: 220-440
        new FILTER_6TH_COEFF(new double[] {
            1, -5.931480316493, 14.66543773155, -19.34664873533, 14.36213855651, -5.688679994152, 0.9392327652624
        }, new double[] {
            3.731600537578e-006, 0, -1.119480161273e-005, 0, 1.119480161273e-005, 0, -3.731600537578e-006
        }, 151),
        // BP: 440-880
        new FILTER_6TH_COEFF(new double[] {
            1, -5.85156216959, 14.29071167394, -18.64466322718, 13.705592882, -5.382223290893, 0.8821445873275
        }, new double[] {
            2.895266535932e-005, 0, -8.685799607795e-005, 0, 8.685799607795e-005, 0, -2.895266535932e-005
        }, 75),
        // BP: 880-1760
        new FILTER_6TH_COEFF(new double[] {
            1, -5.659143272881, 13.43390065645, -17.12108832999, 12.35540348684, -4.787127980137, 0.7780827445559
        }, new double[] {
            0.0002181740519493, 0, -0.0006545221558478, 0, 0.0006545221558478, 0, -0.0002181740519493
        }, 38),
        // BP: 1760-3520
        new FILTER_6TH_COEFF(new double[] {
            1, -5.155728149138, 11.39021960408, -13.78148945915, 9.629218366135, -3.685501984199, 0.6048042415924
        }, new double[] {
            0.001557156728436, 0, -0.004671470185309, 0, 0.004671470185309, 0, -0.001557156728436
        }, 19),
        // BP: 3520-7040
        new FILTER_6TH_COEFF(new double[] {
            1, -3.773089717521, 6.812932868527, -7.272198696119, 4.854306593905, -1.912618474691, 0.3626510747757
        }, new double[] {
            0.0101224047336, 0, -0.03036721420079, 0, 0.03036721420079, 0, -0.0101224047336
        }, 9),
        // BP: 7040-14080
        new FILTER_6TH_COEFF(new double[] {
            1, -0.3056501360981, 1.080961146038, -0.2542770672661, 0.6428131702076, -0.07427855753132, 0.1187222370285
        }, new double[] {
            0.05757480091287, 0, -0.1727244027386, 0, 0.1727244027386, 0, -0.05757480091287
        }, 5),
            // HP: 14080+
//        new FILTER_6TH_COEFF(new double[] {
//              1, 1.64639815544, 1.7936603173, 1.081750428817, 0.4245502003134, 0.09188386110321, 0.008968391946871
//        }, new double[] {
//              0.006361663503134, -0.03816998101881, 0.09542495254701, -0.1272332700627, 0.09542495254701, -0.03816998101881, 0.006361663503134
//        }, 4)
    };

    protected AudioStream[] bandInputs = new AudioStream[NUM_BANDS];

    protected DataStream[] onsets = new DataStream[NUM_BANDS];

    public OnsetStage() {
    }

    public int createOnsetStream(AudioStream in, DataStream out, DataStream internal) {
        if (!in.isValid())
            throw new IllegalArgumentException("pStrmIn");

        int hr = Utils.E_FAIL;

        ////////////////////////////////////
        // BandPass Filter the input audio and detect onsets

        OnsetDetect onsetDetect = new OnsetDetect();

        for (int i = NUM_BANDS - 1; i >= 0; i--) {
            hr = DSP.DF2_Filter6(in, bandInputs[i], IIR_BAND_FILTERS[i], true);
            if (Utils.FAILED(hr))
                break;
            hr = onsetDetect.createOnsetStream(bandInputs[i], onsets[i], internal);
            if (Utils.FAILED(hr))
                break;
        }
        if (Utils.FAILED(hr))
            return hr;

        // Reassemble onset streams into one conglomerate stream
        hr = reassembleOnsets(out);
        if (Utils.FAILED(hr))
            return hr;

        return hr;
    }

    // Split the input signal into its separate frequency bands
    protected int bandSplitInput(AudioStream in) {
        return Utils.S_OK;
    }

    // Reassemble onset streams into one output stream
    protected int reassembleOnsets(DataStream out) {
        int hr = Utils.S_OK;

        // Stream out is same format as all onset streams
        out.createData(onsets[0]);

        float[] dataOut = out.getFloatData();

        // Band Weights - Weight higher bands more (Duxbury)
        //float[] bandWeights = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        float[] bandWeights = {
            1, 1, 1, 1, 1, 1, 1, 1,
        };

        //////
        // Create Union of all Onsets
        for (int i = 0; i < out.getNumSamples(); i++) {
            // Union onsets from all bands
            float total = 0;
            for (int j = 0; j < NUM_BANDS; j++)
                total += onsets[j].getData().asFloatBuffer().get(i) * bandWeights[j];

            dataOut[i] = total;
        }

        int minOnsetDist = (int) (params.onsetCombineMinDist * out.getSampleRate());

        //////
        // Filter union of onsets to remove weaker/too close onsets
        // Option 1: Simple - onset found, use strongest onset in next minOnsetDist samples
//        for (int i = 0; i < out.getNumSamples(); i++) {
//            // Found an onset?
//            if (dataOut[i] > 0) {
//                // **** HURTING ****
//                float maxOnset = 0;
//                int samMax = 0;
//                // Search through the next m_nSamMinOnsetDist samples
//                int start = i;
//                for (; i < start + minOnsetDist; i++) {
//                    if (dataOut[i] > maxOnset) {
//                        samMax = i;
//                        maxOnset = dataOut[i];
//                    }
//                    // Clear this range of all onsets
//                    dataOut[i] = 0;
//                }
//                // Set one offset at best location found
//                dataOut[samMax] = maxOnset;
//            }
//        }

        ////////
        // Filter union of onsets to remove weaker/too close onsets
        // Option 3: Sparse - always start search from strongest onset nMinOnsetDist forward
        // restarting search if a stronger onset is found
        for (int i = 0; i < out.getNumSamples(); i++) {
            // Found an onset?
            if (dataOut[i] > 0) {
                float maxOnset = dataOut[i];
                int samMax = i;

                // Search through the next m_nSamMinOnsetDist samples
                int start = i;
                for (; (i < start + minOnsetDist) && (i < out.getNumSamples()); i++) {
                    if (dataOut[i] > maxOnset) {
                        samMax = i; // Found stronger, remember its location
                        start = i; // Restart search here (on now largest onset)
                        maxOnset = dataOut[i];
                    }
                    // Clear this range of all onsets
                    dataOut[i] = 0;
                }
                // Set one offset at best location found
//                if (maxOnset >= params.onsetCombineMinOnset)
//                    dataOut[samMax] = 1;//maxOnset;
//                else
//                    dataOut[samMax] = 0.5f;
                dataOut[samMax] = maxOnset;
            }
        }
        return hr;
    }
}
