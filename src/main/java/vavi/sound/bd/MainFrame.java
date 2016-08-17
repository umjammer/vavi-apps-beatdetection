/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;


class MainFrame extends JFrame {

    protected JPanel statusBar;

    protected JToolBar toolBar;

    protected JPanel reBar;

    protected JPanel dlgBar;

    protected MainFrame() {
        toolBar = new JToolBar(); // IDR_MAINFRAME
        dlgBar = new JPanel(); // AFX_IDW_DIALOGBAR
        reBar = new JPanel();
        reBar.add(toolBar);
        reBar.add(dlgBar);

        statusBar = new JPanel();
//        statusBar.SetIndicators(indicators, sizeof(indicators) / sizeof(BeatDetectView.UINT));
    }

    public JPanel getDialogBar() {
        return dlgBar;
    }

    public JPanel getStatusBar() {
        return statusBar;
    }
}
