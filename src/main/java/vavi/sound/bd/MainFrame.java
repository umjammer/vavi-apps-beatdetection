/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;


class MainFrame extends JFrame {

    protected JPanel statusBar;

    protected JPanel toolBar;

    protected JToolBar reBar;

    protected JPanel dialogBar;

    protected MainFrame(BeatDetectView view) {
        toolBar = new JPanel();

        toolBar.setPreferredSize(new Dimension(640, 40));
        toolBar.add(view.IDC_VIEW_INPUT_CHECK);
        toolBar.add(view.IDC_VIEW_ONSET_CHECK);
        toolBar.add(view.IDC_VIEW_ONSET2_CHECK);
        toolBar.add(view.IDC_VIEW_BEATOUT_CHECK);
        toolBar.add(view.IDC_VIEW_TEMPOOUT_CHECK);
        toolBar.add(view.IDC_VIEW_PERIODOUT_CHECK);
        toolBar.add(view.IDC_VIEW_INFOOUT_CHECK);

        dialogBar = new JPanel();
        dialogBar.setPreferredSize(new Dimension(640, 40));
        dialogBar.setBackground(Color.magenta);

        reBar = new JToolBar();
        reBar.setPreferredSize(new Dimension(640, 80));
        reBar.setLayout(new BorderLayout());
        reBar.add(toolBar, BorderLayout.NORTH);
        reBar.add(dialogBar, BorderLayout.SOUTH);

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(view.ID_FILE_OPEN);
        fileMenu.add(view.ID_FILE_NEW);
        fileMenu.addSeparator();
        fileMenu.add(view.ID_APP_ABOUT);

        JMenu zoomMenu = new JMenu("Zoom");
        zoomMenu.add(view.ID_VIEW_ZOOMIN);
        zoomMenu.add(view.ID_VIEW_ZOOMOUT);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(zoomMenu);
        this.setJMenuBar(menuBar);

        statusBar = new JPanel();
        statusBar.setPreferredSize(new Dimension(640, 20));
        statusBar.setBackground(Color.green.brighter().brighter());
//        statusBar.setIndicators(indicators, sizeof(indicators) / sizeof(BeatDetectView.UINT));

        JPanel mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension(640, 480));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(reBar, BorderLayout.NORTH);
        mainPanel.add(view, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        setTitle("Beat Detection");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 480);
        pack();
    }

    public JPanel getDialogBar() {
        return dialogBar;
    }

    public JPanel getStatusBar() {
        return statusBar;
    }
}
