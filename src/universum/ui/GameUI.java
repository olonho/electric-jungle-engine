package universum.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;

import universum.bi.*;
import universum.engine.*;

import static universum.engine.RemoteProto.*;

/**
 *
 * @author pan
 */
public class GameUI extends JSplitPane 
    implements ActionListener, ChangeListener, ItemListener, RendererHolder {
    static final int MODE_OBSERVER      = 0;
    static final int MODE_ADMINISTRATOR = 1;
    static final int MODE_PLAYER = 2; 

    protected DrawSurface surface;
    protected JButton     btnAdd, btnNew, btnStart, btnOptions, 
        btnAbout, btnNew2, btnStop;
    protected JCheckBox   chkSingleStep;
    protected JLabel      lblTurns;
    protected JSlider     speed;
    protected JTable      table;
    protected Renderer    renderer;
    protected Icon        startIcon, pauseIcon;
    private   boolean     paused;

    public GameUI(int mode) {
        super(HORIZONTAL_SPLIT);
        setLeftComponent(surface = new DrawSurface(this));
        setRightComponent(createToolBox(mode));
        setResizeWeight(1.0);
        startIcon = Images.getImageIcon("btn_start.png");
        pauseIcon = Images.getImageIcon("btn_pause.png");
        paused = true;
    }

    protected void speedChanged(int value) {
        // do nothing by default
    }

    Component createToolBox(int mode) {
        GridBagConstraints c = new GridBagConstraints();
        GridBagPanel toolbox = new GridBagPanel(c);

        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        toolbox.add(lblTurns = new JLabel("Turn: 0"));

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        table = new JTable(new BeingTableModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setRowHeight(20);
        table.setRowSelectionAllowed(false);
        int[] preferredWidth = new int[] {20, 100, 50, 50};
        TableColumnModel tcm = table.getColumnModel();
        int cols = tcm.getColumnCount();
        for (int i = 0; i < cols; i++) {
            tcm.getColumn(i).setPreferredWidth(preferredWidth[i]);
        }
        toolbox.add(new JScrollPane(table));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.0;
        
        if (mode == MODE_ADMINISTRATOR) {                        
            c.gridwidth = 3;
            toolbox.add(btnAdd = new MyButton("Add", this));
            toolbox.add(btnNew = new MyButton("New", this));
            c.gridwidth = GridBagConstraints.REMAINDER;
            toolbox.add(btnNew2 = new MyButton("Clean", this));            
            
        }
        
        c.insets.bottom = 15;
        if (mode == MODE_ADMINISTRATOR || mode == MODE_PLAYER) {
            c.insets.top = 40;
            c.gridx = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            speed = new JSlider(-500, 200, -Constants.getTurnDelay());
            Hashtable<Integer,JComponent> labels = new Hashtable<Integer,JComponent>();
            JLabel lblSlower = new JLabel("slower");
            JLabel lblFaster = new JLabel("faster");
            Font font = lblSlower.getFont();
            font = new Font(font.getName(), 0, font.getSize() - 1);
            lblSlower.setFont(font);
            lblFaster.setFont(font);
            labels.put(new Integer(-490), lblSlower);
            labels.put(new Integer(-150), new JLabel("Game Speed"));
            labels.put(new Integer(190), lblFaster);
            speed.setLabelTable(labels);
            speed.setMajorTickSpacing(50);
            speed.setPaintTicks(true);
            speed.setPaintLabels(true);
            speed.addChangeListener(this);
            toolbox.add(speed);

        } 
        
        if (mode == MODE_ADMINISTRATOR) {
            c.insets.top = 10;
            c.gridx = 0;
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 6.0f;
            toolbox.add(chkSingleStep = new JCheckBox("Single Step", false));
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0f;
            toolbox.add(btnOptions = new MyButton("btn_options.png", 
                                                  "Options", this));
            chkSingleStep.addItemListener(this);            
                        
        }

         if (mode == MODE_ADMINISTRATOR || mode == MODE_PLAYER) {
            c.gridwidth = 4;
            c.weightx = 1.0;
            c.gridx = 0;
            c.insets.top = 45;
            c.insets.left = 5;
            c.insets.right = 5;
            toolbox.add(btnStart = new MyButton("btn_start.png", "Start", this));
            c.gridx = GridBagConstraints.RELATIVE;
            toolbox.add(btnStop = new MyButton("btn_stop.png", "Stop", this));
        }
        c.insets.left = 5;
        c.insets.right = 5;        
        c.gridwidth = GridBagConstraints.REMAINDER;
        toolbox.add(btnAbout = new MyButton("btn_about.png", "About", this));

        toolbox.setMinimumSize(new Dimension(210, 0));
        return toolbox;
    }

    class BeingTableModel extends AbstractTableModel {
        String[] columnName = new String[] {
            "", "Owner", "Mass", "Energy"
        };

        public int getRowCount() {
            return getPlayerCount();
        }

        public int getColumnCount() {
            return columnName.length;
        }

        public String getColumnName(int col) {
            return columnName[col];
        }

        public Class getColumnClass(int col) {
            switch (col) {
            case 0:
                return ImageIcon.class;
            case 1:
                return String.class;
            default:
                return Integer.class;
            }
        }

        public Object getValueAt(int row, int col) {
            ScoreData sd = getPlayerScore(row);
            if (sd == null) {
                return null;
            }
            switch (col) {
            case 1:
                return sd.player;
            case 2:
                return (int)sd.score;
            case 3:
                return (int)sd.energy;
            default:
                return renderer.getPlayerIcon(sd.id);
            }
        }
    }

    public int getPlayerCount() {
        return 0;
    }

    public ScoreData getPlayerScore(int playerId) {
        return null;
    }

    public void setPaused(boolean paused) {
        btnStart.setIcon(paused ? startIcon : pauseIcon);
        btnStart.requestFocusInWindow();
        this.paused = paused;
    }

    public Renderer getRenderer() {
        return renderer;
    }
    
    public void redraw(boolean beingListChanged) {
        surface.redraw();
        if (beingListChanged) {
            table.revalidate();
        }
        table.repaint();
        RenderingInfo ri = renderer.ri;
        lblTurns.setText("Turn: " + ri.numTurns() + " of " + ri.maxTurns());
    }

    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();
        if (source == speed) {
          int v = speed.getValue();
          speedChanged(speed.getValue());          
        }
    }

    private String javaInfo() {
        return 
            System.getProperty("java.vendor")+
            " "+
            System.getProperty("java.version");
    }

    public void showCredits() {
        final String credits = 
            "Java: " + javaInfo() + "\n" +
            "Engine: " + Constants.VERSION + "\n" +
            "Authors:\n" +
            "  Nikolay Igotti\n" + 
            "  Andrei Pangin\n" +
            "  Elena Kalmykova (artwork)\n"+
            "  others (see CREDITS)\n";
        JOptionPane.showMessageDialog(this,
                                      credits,
                                      "Electric Jungle's creators",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public void buttonClick(Object source) {
        if (source == btnStart) {
            setPaused(!paused);
        } else if (source == btnAbout) {
            showCredits();
        } else if (source == btnStop) {
            setStopped();
        }
    }

    public void actionPerformed(ActionEvent e) {
        buttonClick(e.getSource());
    }

    protected void setStopped() {
        btnStop.setEnabled(false);
    }   

    protected void showResults(int status, GameResult gr) {
        switch (status) {
        case STATUS_COMPLETED: {            
            if (false) {
                new ResultsDialog(null, "Done", results(gr), fullResults(gr)).
                    setVisible(true);
            } else {
                Object[] options = {"OK", "Show log"};
                switch (JOptionPane.showOptionDialog(this,
                                                     results(gr),
                                                     "Done",
                                                     JOptionPane.YES_NO_OPTION,
                                                     JOptionPane.QUESTION_MESSAGE,
                                                     null,
                                                     options,
                                                     options[0])) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    
                    JOptionPane.showMessageDialog(this,
                                                  makeScrollPane(fullResults(gr)),
                                                  "Full log",
                                                  JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
            }
            break;
        }
        case STATUS_FINISHED: {
            JOptionPane.showMessageDialog(this,
                                          "Game already finished!",
                                          "Too late",
                                          JOptionPane.INFORMATION_MESSAGE);
            break;
        }
        }
    }
    

    private JComponent makeScrollPane(String text) {
        JTextArea area = new JTextArea(20, 50);
        area.setText(text);
        JScrollPane scroll = new JScrollPane(area);
        area.setEditable(false);
        return scroll;
    }

    protected String results(GameResult gr) {
        String rv = "Game finished!\n";

        if (gr == null) {
            return rv + "Results unknown";
        }
        
        ScoreData winner = gr.winner();        
        if (winner == null) {
            return rv + "Noone survived";
        } else {
            return rv + winner.player + " won with " + (int)winner.score+
                " points in " + gr.numTurns()+" turns"
                ;
        }
    }

    protected String fullResults(GameResult gr) {
        return gr.toString();
    }
    
    public void itemStateChanged(ItemEvent e) {
        Object obj = e.getSource();        
        if (obj == chkSingleStep) {
            if (chkSingleStep.isSelected()) {
                setPaused(true);
                setSingleStep(true);
            } else {
                setSingleStep(false);
            }
        }
    }
    
    protected void setSingleStep(boolean single) {}
}
