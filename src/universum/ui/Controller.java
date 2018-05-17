package universum.ui;

import javax.swing.*;
import java.util.*;
import java.io.File;

import universum.bi.*;
import universum.engine.*;
import universum.util.*;

/**
 *
 * @author nike
 */
public class Controller extends GameUI implements GameOwner {
    private JDialog optDialog;
    private JDialog beingSelector;
    private Universe universe;
    ConfigFile cfgBeings;
    private boolean beingListChanged;
    private int playerCount;
    JFrame frame; 
    private GameInfo gi;    
    private String[][] beingList;
    
    public Controller(JFrame owner, GameInfo gi, String[][] beingList) {
        super(MODE_ADMINISTRATOR);

        this.frame = owner;
        cfgBeings = new ConfigFile("beings.properties");
        this.gi = gi;

        this.beingList = beingList;
        this.beingListChanged = false;

        // gi can be updated by UI
        optDialog = new OptDialog(this, gi);
        beingSelector = new BeingSelectorDialog(this);

        startWorld(false);                
    }

    public int getPlayerCount() {
        return universe == null ? 0 : universe.scores().size();
    }

    public ScoreData getPlayerScore(int playerId) {
        if (universe == null || universe.scores() == null) {
            return null;
        }
        return universe.scores().get(universe.nameById(playerId));
    }

    protected void speedChanged(int v) {
        if (v > 0) {
            gi.turnDelay = 0;
            Constants.setRefreshTurns(v / 10 + 1);
        } else {
            gi.turnDelay = -v;
            Constants.setRefreshTurns(1);
        }
    }

    
    String getClassJar(String className, boolean canRebuild) {
        if (canRebuild) {
            String props = cfgBeings.getStrings(className, 2);
            if (!"".equals(props)) {
                System.out.println("Making being: "+className);
                universum.Main.makeBeing(props);
            }
        }
        
        String jar = cfgBeings.getStrings(className, 0);
        if (new File(jar).exists()) {
            return jar;
        }
        
        return universum.Main.beingPath(jar);
    }


    void addBeing(String className) {
        addBeing(className, true);
    }

    List<String[]> added = new ArrayList<String[]>();   
    void addBeing(String className, boolean isNew) {
        addBeing(className, getClassJar(className, true), isNew);        
    }

    private void addBeing(String className, String jar, boolean isNew) {
        if (universe == null) {
            return;
        }

        if (isNew) {
            added.add(new String[]{className, jar});
        }
        if (universe.addBeing(className, jar, null) == null) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot load!\n"+
                                          " Class "+className+" from "+jar+"\n"+
                                          "See console for details",
                                          "Cannot load being",
                                          JOptionPane.INFORMATION_MESSAGE);
        
            return;
        }

        int nPlayers = universe.scores().size();
        if (nPlayers != playerCount) {
            beingListChanged = true;
            playerCount = nPlayers;
        }
    }

    void reinit(boolean reload) {
        btnStart.setEnabled(true);
        btnStop.setEnabled(true);
        universe = new Universe(this, gi);
        renderer = new Renderer(universe);
        playerCount = 0;
        if (reload) {
            for (String[] b : added) {
                addBeing(b[0], b[1], false);
            }
        } else {
            added.clear();
        }
    }
    
    void startWorld(boolean reload) {
        reinit(reload);
        setPaused(true);
        waitResult = false;
        // update single step state
        chkSingleStep.setSelected(false);        
        universe.bigbang();

        boolean cannotStart = true;
        if (beingList != null) {
            for (String[] bj : beingList) {
                addBeing(bj[0], bj[1], true);
            }
            beingList = null;
            cannotStart = false;
        }
        if (added.size() > 0 ) {
            cannotStart = false;
        }
        setPaused(!gi.autostart || cannotStart);        
    }
    
    private GameResult gameResult;
    private volatile boolean waitResult;
    private void showResultAndTerminate() {
        setPaused(true);
        if (universe != null) {
            waitResult = true;            
            btnStart.setEnabled(false);
            btnStop.setEnabled(false);
            showResults(RemoteProto.STATUS_COMPLETED, gameResult);
            universe.apocalypse();
            universe = null;
            gameResult = null;
        }   
        
    }       
    void stopWorld() {
        if (universe != null) {
            waitResult = true;
            universe.stop();
            // we'll wait for notification from engine
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ie) {}
            }
            waitResult = false;
            showResultAndTerminate();
        }
    }

    public synchronized void redraw() {
        redraw(beingListChanged);
        beingListChanged = false;
    }   

    public void notifyAboutCompletion(GameResult gr) {        
        this.gameResult = gr;
        if (waitResult) {
            synchronized (this) {
                notify();
            }
        } else {
            showResultAndTerminate();
        }
    }

    public void  setListenPort(int port) {
        // must be same as  asked
        assert port == 8889;
    }

    public int  getListenPort() {
        //return 0;
        return 8889;
    }

    protected void setSingleStep(boolean single) {
        universe.setSingleStep(single);
    }

    public void notifyOnTurnEnd() {
        setPaused(true);
        redraw();
    }

    public void setPaused(boolean paused) {
        universe.setPaused(paused);
        super.setPaused(paused);
        redraw();
    }

    protected void setStopped() {
        super.setStopped();
        stopWorld();
    }

    public void buttonClick(Object source) {
        if (source == btnAdd) {
            beingSelector.setVisible(true);
            beingSelector.repaint();
        } else if (source == btnNew || source == btnNew2) {
            setStopped();
            startWorld(source == btnNew /* reload */);
        } else if (source == btnOptions) {
            optDialog.setVisible(true);
            optDialog.repaint();
        } else {
            super.buttonClick(source);
        }
    }

}
