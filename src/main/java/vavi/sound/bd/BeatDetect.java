/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


/**
 * the beat detect application
 */
class BeatDetect {

    static BeatDetect theApp;

    public BeatDetect() {
        doc = new BeatDetectDoc();
        view = new BeatDetectView(doc);
        mainFrame = new MainFrame();
    }

    public boolean automate;

    MainFrame mainFrame;
    BeatDetectView view;
    BeatDetectDoc doc;

    public static void main(String[] args) {

        theApp = new BeatDetect();
        // Called from command line, automate entire process
        if (args.length == 1)
            theApp.automate = true;

        theApp.mainFrame.setVisible(true);
    }
}
