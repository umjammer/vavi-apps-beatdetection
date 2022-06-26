/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;


class MainFrame extends JFrame {

    protected JPanel statusBar;

    protected JToolBar toolBar;

    protected JPanel reBar;

    protected JPanel dialogBar;

    protected MainFrame() {
        toolBar = new JToolBar();
        toolBar.setPreferredSize(new Dimension(40, 400));
        dialogBar = new JPanel();
        dialogBar.setPreferredSize(new Dimension(40, 400));
        reBar = new JPanel();
        dialogBar.setPreferredSize(new Dimension(80, 400));
        reBar.add(toolBar);
        reBar.add(dialogBar);

        statusBar = new JPanel();
        statusBar.setPreferredSize(new Dimension(20, 400));
//        statusBar.SetIndicators(indicators, sizeof(indicators) / sizeof(BeatDetectView.UINT));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(reBar, BorderLayout.NORTH);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        setTitle("Beat Detection");
        setSize(640, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public JPanel getDialogBar() {
        return dialogBar;
    }

    public JPanel getStatusBar() {
        return statusBar;
    }
}
