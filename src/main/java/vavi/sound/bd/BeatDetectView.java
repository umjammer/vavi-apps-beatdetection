/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;


class BeatDetectView extends JPanel {
    static final int RULER_HEIGHT = 20;

    JMenuItem ID_APP_ABOUT = new JMenuItem();
    JMenuItem ID_FILE_NEW =  new JMenuItem();
    JMenuItem ID_FILE_OPEN = new JMenuItem();

    JButton ID_VIEW_ZOOMIN = new JButton();
    JButton ID_VIEW_ZOOMOUT = new JButton();
    JButton IDC_VIEW_INPUT_CHECK = new JButton();
    JButton IDC_VIEW_ONSET_CHECK = new JButton();
    JButton IDC_VIEW_ONSET2_CHECK = new JButton();
    JButton IDC_VIEW_BEATOUT_CHECK = new JButton();
    JButton IDC_VIEW_TEMPOOUT_CHECK = new JButton();
    JButton IDC_VIEW_PERIODOUT_CHECK = new JButton();
    JButton IDC_VIEW_INFOOUT_CHECK = new JButton();

    BeatDetectDoc doc;

    protected BeatDetectView(BeatDetectDoc doc) {
        this.doc = doc;

        zoom = 0.016;

//        JPanel dialogBar = mainFrame.getDialogBar();

        ID_APP_ABOUT.addActionListener(this::onAppAbout);
        ID_FILE_NEW.addActionListener(this::onSaveDocument);
        ID_FILE_OPEN.addActionListener(this::onOpenDocument);

        IDC_VIEW_INPUT_CHECK.setEnabled(true);
        IDC_VIEW_INPUT_CHECK.addActionListener(this::onViewStreamChange);
        IDC_VIEW_ONSET_CHECK.setEnabled(true);
        IDC_VIEW_ONSET_CHECK.addActionListener(this::onViewStreamChange);
        IDC_VIEW_BEATOUT_CHECK.setEnabled(true);
        IDC_VIEW_BEATOUT_CHECK.addActionListener(this::onViewStreamChange);
        IDC_VIEW_TEMPOOUT_CHECK.setEnabled(true);
        IDC_VIEW_TEMPOOUT_CHECK.addActionListener(this::onViewStreamChange);
        IDC_VIEW_PERIODOUT_CHECK.setEnabled(true);
        IDC_VIEW_PERIODOUT_CHECK.addActionListener(this::onViewStreamChange);

        IDC_VIEW_ONSET2_CHECK.addActionListener(this::onViewStreamChange);
        IDC_VIEW_INFOOUT_CHECK.addActionListener(this::onViewStreamChange);

        ID_VIEW_ZOOMIN.addActionListener(this::onViewZoomIn);
        ID_VIEW_ZOOMOUT.addActionListener(this::onViewZoomOut);

        onViewStreamChange(null);
    }

    public void paint(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        Rectangle viewRect = new Rectangle();
        getViewRect(viewRect);

        List<DataStream> streams = new ArrayList<>();
        if (createStreamsList(streams)) {
            // Draw Time Ruler
            Rectangle rulerRect = viewRect;
            rulerRect.y = rulerRect.y;
            rulerRect.height = RULER_HEIGHT;
            viewRect.y = rulerRect.y + rulerRect.height;
            drawTimeRuler(g2d, rulerRect);

            // Draw Data Streams

            // Determine rects for drawing streams
            int streamsCount = streams.size();

            int streamHeight = viewRect.height / streamsCount;
            Rectangle streamRect = viewRect;
            streamRect.height = streamHeight - streamRect.y;

            for (DataStream stream : streams) {
                // Draw
                drawStream(g2d, stream, streamRect);
                // Offset stream rect
                streamRect.y += streamHeight;
            }
        }
    }

    /** Seconds per pixel */
    protected double zoom;

    protected boolean scrollError;

    protected void drawStream(Graphics2D g2d, DataStream stream, Rectangle rect) {
        int centreLine = (rect.y + rect.height) / 2;

        // Draw horizontal line at zero
        g2d.setColor(new Color(0x00, 0xFF, 0x00, 0));
        Rectangle drawRect = rect;
        drawRect.y = centreLine;
        drawRect.width = 1;
        g2d.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

        // Hurt Casting!
        // Assume Float
        float[] data = stream.getFloatData();

        Rectangle clipRect = g2d.getClipBounds();
        clipRect.width += 2;

        // Calculate draw region
        int sampleScale = rect.height / 2;

        // Calculate Sample Offsets
        int startSample = Math.max((int) (clipRect.x * zoom * stream.getSampleRate()), 0);
        int endSample = Math.min((int) ((clipRect.x + clipRect.width) * zoom * stream.getSampleRate()), stream.getNumSamples());

        double samplesPerPixel = zoom * stream.getSampleRate();
        double samCurPixel = startSample;
        double samNextPixel = samCurPixel + samplesPerPixel;

        // Loop through all samples
        int x = clipRect.x;
        float min = 1;
        float max = -1;
        boolean next = false;
        for (int i = startSample; i < endSample; i++) {
            // Draw and go to the next pixel
            while (i > (int) samNextPixel) {
                // Draw this pixel
                drawRect.x = x;
                drawRect.width = 1;
                drawRect.y = centreLine - (int) (max * sampleScale);
                drawRect.width = 1;

                if (max > 1) {
                    g2d.setColor(new Color(0x00, 0x90, 0x00, 0));
                } else {
                    if (scrollError)
                        g2d.setColor(new Color(0x00, 0x00, 0xff, 0));
                    else
                        g2d.setColor(new Color(0xcc, 0x00, 0x00, 0));
                }
                g2d.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

                // Next pixel
                samNextPixel += samplesPerPixel;
                x += 1;
                next = true;
            }
            if (next) {
                // Swap min and max to make continuous plot for next set of samples
                // Only do this after drawing all pixels for current samples because
                // we need to make sure to draw situations where samples/pixel is less
                // than one
                float temp = min;
                min = max;
                max = temp;
                next = false;
            }

            if (data[i] > max)
                max = data[i];
            if (data[i] < min)
                min = data[i];
        }
    }

    void getViewRect(Rectangle rect) {
        Dimension totalSize = getMaximumSize();

        rect.y = 0;
        rect.x = 0;
        rect.height = totalSize.height;
        rect.width = totalSize.width;
    }

    boolean createStreamsList(List<DataStream> list) {
        if (null == list)
            throw new IllegalArgumentException("pList");

//        JPanel dialogBar = mainFrame.getDialogBar();

        // Clear out list
        list.clear();

        if (IDC_VIEW_INPUT_CHECK.isEnabled()) {
            list.add(doc.input);
        }

        if (IDC_VIEW_ONSET_CHECK.isEnabled()) {
            list.add(doc.onsetOutput);
        }

        if (IDC_VIEW_ONSET2_CHECK.isEnabled()) {
            list.add(doc.onsetInternal);
        }

        if (IDC_VIEW_BEATOUT_CHECK.isEnabled()) {
            list.add(doc.beatOutput);
        }

        if (IDC_VIEW_TEMPOOUT_CHECK.isEnabled()) {
            list.add(doc.beatTempo);
        }

        if (IDC_VIEW_PERIODOUT_CHECK.isEnabled()) {
            list.add(doc.beatPeriod);
        }

        if (IDC_VIEW_INFOOUT_CHECK.isEnabled()) {
            list.add(doc.beatInfo);
        }

        return !list.isEmpty();
    }

    void drawTimeRuler(Graphics2D g2d, Rectangle rect) {
        // Draw Background
        g2d.setColor(new Color(0xBB, 0xBB, 0xBB, 0));
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);

        // Draw horizontal marker lines
        g2d.setColor(new Color(0x00, 0x00, 0x00, 0));

        Rectangle clipRect = g2d.getClipBounds();
        int startPixel = clipRect.x;
        int endPixel = clipRect.x + clipRect.width + 2;

        float ticks = (float) (1 / zoom);
        int tickHeight = rect.height - 1;
        while (ticks > 25) {
            float i = (int) (startPixel / ticks) * ticks;
            for (; i < endPixel; i += ticks) {
                Rectangle tickRect = new Rectangle();
                tickRect.height = rect.height;
                tickRect.y = tickRect.y + tickRect.height - tickHeight;
                tickRect.x = (int) i;
                tickRect.width = 1;
                g2d.fillRect(tickRect.x, tickRect.y, tickRect.width, tickRect.height);
            }
            tickHeight = (tickHeight * 2) / 3;
            ticks /= 2;
        }
    }

    void onViewZoomIn(ActionEvent ev) {
        // Messy crap, but this is not a pro app...
        if (zoom > 0.0001) {
            zoom = zoom / 2;
            onViewStreamChange(ev);
        }
    }

    void onViewZoomOut(ActionEvent ev) {
        // Messy crap, but this is not a pro app...
        if (zoom < 0.1) {
            zoom = zoom * 2;
            onViewStreamChange(ev);
        }
    }

    void onViewStreamChange(ActionEvent ev) {
        // Update the view with changes in the streams we are to show
        List<DataStream> streams = new ArrayList<>();

        double duration = 0;

        if (createStreamsList(streams)) {
            for (DataStream stream : streams) {
                if (stream.getDuration() > duration)
                    duration = stream.getDuration();
            }
        }
        // Set View Size
        Rectangle rect = getBounds();

        Dimension size = new Dimension();
        size.width = Math.max((int) (duration / zoom), rect.width);
        size.height = rect.height;
        setPreferredSize(size);

        invalidate();

        // Scroll Error Crap
        // ScrollBars suck and can't go higher than 32767, so warn if this happens
        scrollError = size.width > 32767;
    }

    void onAppAbout(ActionEvent ev) {
        JDialog aboutDialog = new JDialog();
        aboutDialog.setModal(true);
        aboutDialog.setVisible(true);
    }

    enum UINT {
        ID_SEPARATOR, // status line indicator
        ID_INDICATOR_CAPS,
        ID_INDICATOR_NUM,
        ID_INDICATOR_SCRL,
    }

    JPanel working = new JPanel(); // "IDD_WORKING"

    String chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showOpenDialog(null);
        return fileChooser.getSelectedFile().getPath();
    }

    void onOpenDocument(ActionEvent ev) {
        try {
            String filename = chooseFile();
            working.setVisible(true);
            doc.onOpenDocument(filename);
            working.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onSaveDocument(ActionEvent ev) {
        try {
            String filename = chooseFile();
            doc.onSaveDocument(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onSaveOnsets(ActionEvent ev) {
        try {
            String filename = chooseFile();
            doc.onSaveOnsets(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
