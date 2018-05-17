package universum.ui;

import java.util.*;

import universum.bi.*;
import universum.engine.*;
import universum.util.*;

public class HeadlessController implements GameOwner {
    private Universe universe;
    private GameInfo gi;
    private int port;
    private Object waiter = new Object();
    private volatile boolean done;

    public HeadlessController(GameInfo gameInfo, int port) {
        this.port = port;
        this.gi = gameInfo;
    }
    
    public void startGame(String[][] beings) {
        done = false;
        gi.turnDelay = 0;
        //gi.out = null;
        universe = new Universe(this, gi);        
        universe.addBeings(beings);
        universe.bigbang();

        long started = System.currentTimeMillis();
        try {
            // and then wait till completion
            synchronized (waiter) {
                while (!done) {
                    waiter.wait(10000);
                    if (System.currentTimeMillis() - started > 1000*60*60) {
                        universe.stop();
                    }
                }
            }
        } catch (InterruptedException ie) {}
        
        System.exit(0);
    }

    public void redraw() {
    }    

    public void notifyAboutCompletion(GameResult gr) {
        universe.setPaused(true);
        System.out.println("Game "+gr.kind()+" finished in "+gr.numTurns()+" turns");
        System.out.println("Player\t\tScore\t\tEnergy");
        System.out.println("---------------------------------------");
        System.out.print(gr.toString());
        System.out.println("---------------------------------------");        
        
        synchronized (waiter) {
            done = true;
            waiter.notify();
        }
    }


    public void  setListenPort(int port) {
        this.port = port;
    }

    public int  getListenPort() {
        return port;
    }

    public void notifyOnTurnEnd() {}
}
