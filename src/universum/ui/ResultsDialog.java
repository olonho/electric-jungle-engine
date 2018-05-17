package universum.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

import universum.bi.Constants;
import universum.bi.GameKind;
import universum.engine.GameInfo;

class ResultsDialog extends JDialog implements ActionListener {
    private JButton     ok, verbose;
    private String      verboseMsg;

    ResultsDialog(JFrame parent, String title, String body, 
                  String verboseMsg) {
        super(parent, title, true);

        this.verboseMsg = verboseMsg;

        final int width = 200, height = 140;
        setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(d.width/2 - width/2, d.height/2 - height/2);
        
        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(ok = new MyButton("OK", this));
        buttons.add(verbose = new MyButton("Full Results", this));


        setLayout(new BorderLayout());
        add(new JLabel(body), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        validate();
    }

    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();
        boolean needRestart = false;        
        if (source == ok) {
            setVisible(false);
        } if (source == verbose) {
            JOptionPane.showMessageDialog(this,
                                          verboseMsg, "Full results",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
