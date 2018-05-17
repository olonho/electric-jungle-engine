package universum.engine;

import java.io.PrintStream;

import universum.bi.Constants;
import universum.bi.GameKind;
import universum.util.ConfigFile;

// pure data class defining config of game
public final class GameInfo {    
    private GameKind gameKind;
    public int maxTurns;
    public int turnDelay;
    public int waitBefore, waitAfter;
    public String recordGameFilePath, sqlResultFilePath;
    public int fieldWidth;
    public int fieldHeight;
    public int numRegular;
    public int numGolden;
    public float maxRegular;
    public float maxGolden;
    public float growRegular;
    public float growGolden;
    public PrintStream out;
    public boolean autostart;
    public boolean permanent;
    public String topologyClass;
    public String resourceClass;
    public int randomSeed;

    public GameInfo() {
        setKind(Constants.getDebug() ? GameKind.DEBUG : GameKind.JUNGLE);
        recordGameFilePath = Constants.getRecordFile();
        fieldWidth = Constants.getWidth();
        fieldHeight = Constants.getHeight();
        numRegular = Constants.NUM_REGULAR;
        numGolden = Constants.NUM_GOLDEN;
        maxRegular = Constants.MAX_REGULAR;
        maxGolden = Constants.MAX_GOLDEN;
        growRegular = Constants.GROW_REGULAR;
        growGolden = Constants.GROW_GOLDEN;
        turnDelay = Constants.getTurnDelay();
        out = System.out;
        waitBefore = 0;
        waitAfter = 5 * 60 * 1000;
        autostart = false;
        permanent = false;
        topologyClass = "universum.engine.topology.TorusTopology";
        resourceClass = "universum.engine.resource.FairResourceControl";
        randomSeed = 0;
    }
    
    private GameKind parseKind(String k) {
        try {
            return GameKind.valueOf(k);
        } catch (IllegalArgumentException iae) {
            System.out.println("Unknown game: " +k+
                               "; known game kinds are:");
            for (GameKind gk : GameKind.values()) {
                System.out.println(" "+gk);
            }
            throw iae;
        }
    }

    public void setKind(String k) {
        setKind(parseKind(k));
    }

    public void setKind(GameKind k) {
        switch (k) {
        case SINGLE:
            maxTurns = 200;
            break;
        case DUEL:
            maxTurns = 1000;
            break;
        case JUNGLE:
            maxTurns = 2000;
            break;
        }
        gameKind = k;
    }

    public GameKind getKind() {
        return gameKind;
    }

    public void update(ConfigFile f) {
        setKind(parseKind(f.getProperty("kind", getKind().toString())));
        fieldWidth = f.getInteger("fieldWidth", fieldWidth);
        fieldHeight = f.getInteger("fieldHeight", fieldHeight);
        maxTurns = f.getInteger("maxTurns", maxTurns);
        turnDelay = f.getInteger("turnDelay", turnDelay);            
        autostart = f.getBoolean("autostart", autostart);
        numRegular = f.getInteger("numRegular", numRegular);
        numGolden = f.getInteger("numGolden", numGolden);
        maxRegular = f.getFloat("maxRegular", maxRegular);
        maxGolden = f.getFloat("maxGolden", maxGolden);
        growRegular = f.getFloat("growRegular", growRegular);
        growGolden = f.getFloat("growGolden", growGolden);
        waitBefore = f.getInteger("waitBefore", waitBefore);
        waitAfter = f.getInteger("waitAfter", waitAfter);    
        topologyClass = f.getProperty("topologyClass",  topologyClass);
        resourceClass = f.getProperty("resourceClass",  resourceClass);
        recordGameFilePath = f.getProperty("recordGameFilePath", recordGameFilePath);
        sqlResultFilePath = f.getProperty("sqlResultFilePath", sqlResultFilePath);
        // note that same random seeds gives only guarantees on resource
        // configuration. Guarantees on same position given only in 
        // non-UI games
        randomSeed = f.getInteger("randomSeed", randomSeed);
    }

}
