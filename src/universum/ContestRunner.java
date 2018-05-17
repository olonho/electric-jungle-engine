package universum;

import java.util.*;
import java.io.File;
import universum.engine.*;
import universum.bi.*;
import universum.ui.*;
import universum.util.*;
import ejungle.web.Contest;
import static ejungle.web.Contest.State.*;

public class ContestRunner extends Contest implements GameOwner {
    private Universe universe;
    private GameInfo gi;
    private GameResult gr;
    private String beings[][];
    private Object cond = new Object();

    public void redraw() {}

    public void notifyAboutCompletion(GameResult result) {
        endTime = new Date();
        synchronized (cond) {
            gr = result;
            cond.notifyAll();
        }	
        updateBeingInfo();
	state = FINISHED;
	// make result available for some time
        if (gi.waitAfter > 0) {
            Util.sleep(gi.waitAfter);
        }
        destroy();
    }

    private int parseInt(String v, int min, int max) throws Exception {
        int value = Integer.valueOf(v);
        if (value < min || value > max) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public void parseArgs(String[] args) {
        gi = new GameInfo();
        int argc = args.length;
        int argp = 0;
        for (; argp < argc; argp++) {
            String arg = args[argp];
            try {
                if ("--game-kind".equals(arg)) {                    
                    gi.setKind(args[++argp]);
                } else if ("--max-turns".equals(arg)) {
                    gi.maxTurns = parseInt(args[++argp], 10, 10000000);
                } else if ("--field-width".equals(arg)) {
                    gi.fieldWidth = parseInt(args[++argp], 10, 1000);
                } else if ("--field-height".equals(arg)) {
                    gi.fieldHeight = parseInt(args[++argp], 10, 1000);
                } else if ("--resources".equals(arg)) {
                    gi.numRegular = parseInt(args[++argp], 0, 10000);
                } else if ("--golden".equals(arg)) {
                    gi.numGolden = parseInt(args[++argp], 0, 10000);
                } else if ("--turn-delay".equals(arg)) {
                    gi.turnDelay = parseInt(args[++argp], 0, 10000);
                } else if ("--ignore-output".equals(arg)) {
                    gi.out = null;
                } else if ("--batch".equals(arg)) {
                    gi.turnDelay = 0;
                    gi.out = null;
                    gi.waitAfter = 0;
                } else if ("--wait-before".equals(arg)) {
                    gi.waitBefore = parseInt(args[++argp], 0, 10000000);
                } else if ("--no-log".equals(arg)) {
                    System.out.println("logging skipped...");
                    gi.out = null;
                } else if ("--record-file".equals(arg)) {
                    gi.recordGameFilePath = args[++argp].replace("<key>", key);
                } else if ("--permanent".equals(arg)) {
                    gi.permanent = true;
                } else {
                    break;
                }
            } catch (Exception e) {
                // skip silently and use default GameInfo values
            }
            
        }

        int beingCount = (argc - argp) / 2;
        beings = new String[beingCount][4];
        for (int i = 0; i < beingCount; i++) {
          beings[i][0] = args[argp++];
          beings[i][1] = args[argp++];
          beings[i][2] = beings[i][0];
          beings[i][3] = "n/a";
        }
    }

    public synchronized void run() {
        if (state != ACTIVE) {
            startTime = new Date();
            universe = new Universe(this, gi);
            Being[] bs = universe.addBeings(beings);
            // now update owner field with info from the being
            for (int i = 0; i < beings.length; i++) {
                if (bs[i] != null) {
                    beings[i][2] = universe.getOwnerOf(bs[i]);
                } 
            }
            updateBeingInfo();
            universe.bigbang();
            state = ACTIVE;
        }
    }

    protected synchronized void stop() {
        if (state == ACTIVE) {
            state = FINISHED;
            endTime = new Date();
	    assert universe != null;
	}
	if (universe != null) {
            universe.apocalypse();
            universe = null;
        }
        if (gi.recordGameFilePath != null && !gi.permanent) {
            new File(gi.recordGameFilePath).delete();
        }
    }

    private String scoreString(float f) {
        return Integer.toString((int)f);
    }
    
    private synchronized void updateBeingInfo() {
        if (universe == null) {
            return;
        }
        Map<String,ScoreData> scores = universe.scores();
        for (String[] info : beings) {
            ScoreData sd = scores.get(info[2]);
            info[3] = sd == null ? "n/a" : scoreString(sd.score);
        }
    }
    
    // we'd better get rid of this function
    public String[][] getBeingInfo() {
        if (state == ACTIVE) {
            updateBeingInfo();
        }
        return beings;
    }

    public boolean waitCompletion() {
        int iter=0;
        try {
            synchronized (cond) {
                while (gr == null) {
                    cond.wait(1000);
                    // 60 minutes
                    if (iter++ > 60 * 60)  {
                         return false;
                    }
                }
            }
            return true;
        } catch (InterruptedException ie) {
            return false;
        }
    }

    public int numTurns() {
        return gr.numTurns();
    }
    
    public String[] results() {
        String[] rv = new String[gr.results().size()*2];
        int i = 0;
        for (ScoreData sd : gr.results()) {
            rv[i++] = sd.jarFile;
            rv[i++] = Float.toString(sd.score);
        }
        return rv;
    }

    public void notifyOnTurnEnd() {}
}
