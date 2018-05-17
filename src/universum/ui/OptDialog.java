package universum.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

import universum.bi.Constants;
import universum.bi.GameKind;
import universum.engine.GameInfo;

class OptDialog extends JDialog implements ActionListener, ItemListener {
    private JButton     ok, cancel;
    private JSlider     speedSLider, turnsSlider;
    private JSpinner    wField, hField, maxTurns; 
    private JCheckBox   idsBox, gridBox;
    private Controller  ctr;
    private GameInfo gi;
    private JRadioButton gameKinds[];
    private ButtonGroup  gameKindBG =  new ButtonGroup();
    private static final GameKind kindTable[] = 
        new GameKind[] { 
        GameKind.SINGLE,       
        GameKind.JUNGLE,
        GameKind.DUEL,
        GameKind.DEBUG
    };

    OptDialog(Controller ctr, GameInfo gi) {
        super(ctr.frame, "Configure", false);
        this.ctr = ctr;
        this.gi = gi;

        final int width = 220, height = 240;
        setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(d.width/2 - width/2, d.height/2 - height/2);
        
        setLayout(new BorderLayout());
        
        addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                setVisible(false);
            }
        });
        
        GridBagConstraints c = new GridBagConstraints();
        GridBagPanel opts = new GridBagPanel(c);

        wField = new JSpinner(new SpinnerNumberModel(gi.fieldWidth, 5, 1000, 5));
        hField = new JSpinner(new SpinnerNumberModel(gi.fieldHeight, 5, 1000, 5));
        maxTurns = new JSpinner(new SpinnerNumberModel(gi.maxTurns, 0, 10000000, 200));
        gridBox = new JCheckBox("Show grid", Constants.getDrawGrid());
        gridBox.addItemListener(this);
        idsBox = new JCheckBox("Show being ids", Constants.getDrawNumber());
        idsBox.addItemListener(this);
        
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        opts.add(new JLabel("Field size:"));
        c.weightx = 0.5;
        opts.add(wField);
        c.weightx = 0.0;
        opts.add(new JLabel("x"));
        c.weightx = 0.5;
        c.gridwidth = GridBagConstraints.REMAINDER;
        opts.add(hField);

        c.insets.bottom = 10;
        c.weightx = 0.0;
        c.gridwidth = 1;
        opts.add(new JLabel("Max turns:"));
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        opts.add(maxTurns);

        JPanel boxes = new JPanel();
        boxes.setLayout(new BoxLayout(boxes, BoxLayout.Y_AXIS));
        boxes.add(gridBox);
        boxes.add(idsBox);

        JPanel checkBoxes = new JPanel(new GridLayout(2, 3));
        gameKinds = new JRadioButton[4];       
        gameKinds[0] = new JRadioButton("Single", 
                                        gi.getKind() == GameKind.SINGLE);
        gameKinds[1] = new JRadioButton("Jungle", 
                                        gi.getKind() == GameKind.JUNGLE);
        gameKinds[2] = new JRadioButton("Duel", 
                                        gi.getKind() == GameKind.DUEL);
        gameKinds[3] = new JRadioButton("Debug", 
                                        gi.getKind() == GameKind.DEBUG);
        for (int i = 0; i < gameKinds.length; i++ ) {
            gameKindBG.add(gameKinds[i]);
            checkBoxes.add(gameKinds[i]);
        }
        opts.add(checkBoxes);

        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(ok = new MyButton("OK", this));
        buttons.add(cancel = new MyButton("Cancel", this));
        
        add(opts, BorderLayout.NORTH);
        add(boxes, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        
        validate();
    }
    
    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();
        boolean needRestart = false;        
        if (source == ok) {
            setVisible(false);
            int newWidth = (Integer)wField.getValue();
            int newHeight = (Integer)hField.getValue();
            if (newWidth != gi.fieldWidth || newHeight !=  gi.fieldHeight) {
                gi.fieldWidth = newWidth;
                gi.fieldHeight = newHeight;
                needRestart = true;
            }
            
            GameKind newKind = GameKind.JUNGLE;
            for ( int i = 0; i < gameKinds.length; i++ ) {
                if (gameKinds[i].isSelected()) {
                    newKind = kindTable[i];                    
                    break;
                }
            }            
            needRestart |= (newKind != gi.getKind());
            if (newKind != gi.getKind()) {
                needRestart = true;
                gi.setKind(newKind);
            } else {
                gi.maxTurns = (Integer)maxTurns.getValue();
            }
            ctr.redraw();
        } else if (source == cancel) {
            setVisible(false);
            wField.setValue(gi.fieldWidth);
            hField.setValue(gi.fieldHeight);
            maxTurns.setValue(gi.maxTurns);
        }

        if (needRestart) {
             ctr.stopWorld();
             ctr.startWorld(true);
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object obj = e.getSource();        
        if (obj == idsBox) {
            Constants.setDrawNumber(idsBox.isSelected());
        } else if (obj == gridBox) {
            Constants.setDrawGrid(gridBox.isSelected());
        }
        ctr.redraw();
    }
}
