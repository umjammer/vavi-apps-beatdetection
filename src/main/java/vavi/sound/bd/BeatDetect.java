/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;


import java.awt.Cursor;
import java.io.IOException;


/**
 * the beat detect application
 */
class BeatDetect {

    static BeatDetect theApp;

    public BeatDetect() {
        doc = new BeatDetectDoc();
        view = new BeatDetectView(doc);
        mainFrame = new MainFrame(view);
    }

    public boolean automate;

    MainFrame mainFrame;
    BeatDetectView view;
    BeatDetectDoc doc;

    public static void main(String[] args) throws Exception {

        theApp = new BeatDetect();
        // Called from command line, automate entire process
        if (args.length == 0)
            theApp.automate = true;

        theApp.mainFrame.setVisible(true);

        if (args.length > 0) {
            theApp.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            theApp.doc.openDocument(args[0]);
            theApp.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            theApp.mainFrame.repaint();
        }
    }
}
