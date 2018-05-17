package universum.bi;

import universum.engine.JungleSecurity;

/**
 * Class containing basic constants affecting game rules.
 *
 * @author nike
 */

public final class Constants {
    /***************** GLOBAL UNIVERSE CONSTANTS ******************************/
    /* NB: here % mass means M*const, so 0.1f means 10% mass */

    /**
     * minimal amout of energy being can have, if lower - it dies,
     * and leaves remaining amount on the map
     */
    public static final float K_emin = 0.15f;

    /**
     * how much energy (% mass) can be consumed per turn
     */
    public static final float K_bite = 0.1f;
   
    /**
     * how much energy (% of effective mass) being spends per turn.
     * Effective mass is square root of mass.
     */
    public static final float K_masscost = 0.001f;

    /**
     * how much energy (% speed) spent during move
     */
    public static final float K_movecost = 0.01f;
   
    /**
     * how different baby could be from parent (both speed and mass)
     */
    public static final float K_minbornvariation = 0.8f;    
    /**
     * how different baby could be from parent (both speed and mass)
     */
    public static final float K_maxbornvariation = 1.2f;

    /**
     * how much energy (% mass) being should have to give birth to others
     */
    public static final float K_toborn = 0.8f;

    /**
     * how much damage, in % of mass creature can do
     */
    public static final float K_fight = 0.2f;
    
    /** 
     * penalty to attack, if combined with move
     */
    public static final float K_fightmovepenalty = 0.75f;
    
    /**
     * how expensive attack is (% mass)*/
    public static final float K_fightcost = 0.01f;

    /**
     * how expensive giving birth is (% mass)
     */
    public static final float K_borncost = 0.2f;

    /**
     * how expensive is marking  (in absolute units)
     */
    public static final float K_markcost = 1f;

    /**
     * how long mark lives (in turns)
     */
    public static final int K_markttl = 20;

    /**
     * how much damage attacker recieves, % mass of attacked
     */
    public static final float K_retaliate = 0.05f;   

    /**
     * minimal mass allowed
     */
    public static final float K_minmass = 0.1f;
    
    /**
     * maximal mass allowed
     */
    public static final float K_maxmass = 1000f;

    /**
     * maximal biomass at the point, -1f - unlimited
     */
    public static final float K_maxmassperpoint = -1f;

    /** 
     * minimal speed allowed
     */
    public static final float K_minspeed = 1f;
    
    /** 
     * maximal speed allowed
     */
    public static final float K_maxspeed = 10f;

    /** 
     * maximal allowed duration of the turn in milliseconds
     * (typical turn must be much shorter, although)
     */
    public static final int K_turnduration = 1000;

    /** 
     * number of regular resource sources
     */
    public static final int NUM_REGULAR = 130;

    /** 
     * number of golden resource sources
     */
    public static final int NUM_GOLDEN  = 3;

    // max and growth for regular points
    public static final float MAX_REGULAR = 20f;
    public static final float GROW_REGULAR = 0.03f;
    
    // max and growth for golden points
    public static final float MAX_GOLDEN = 500f;
    public static final float GROW_GOLDEN = 1.2f;

    // for "fair" resource allocation
    // how big is total energy bugdet, randomly split between sources
    // we try to behave similar of what we'd have with regular 
    // resource allocation
    public static final float TOTAL_CAPACITY_REGULAR = 
        NUM_REGULAR * MAX_REGULAR * 0.7f;
    public static final float TOTAL_STARTED_REGULAR = 
        TOTAL_CAPACITY_REGULAR / 2;
    public static final float TOTAL_GROWTH_REGULAR =
        NUM_REGULAR * GROW_REGULAR; 
    public static final float TOTAL_CAPACITY_GOLDEN = 
        NUM_GOLDEN * MAX_GOLDEN  * 0.7f;
    public static final float TOTAL_STARTED_GOLDEN = 
        TOTAL_CAPACITY_GOLDEN / 2;
    public static final float TOTAL_GROWTH_GOLDEN =
        NUM_GOLDEN * GROW_GOLDEN; 
    public static final float BORN_BONUS = 100.0f;
    public static final float BORN_BONUS_GROWTH = 1.0f;    

    /**
     * how much turns allowed
     */
    private static int MAX_TURNS = 1000;
    public static int getMaxTurns() {
        return MAX_TURNS;
    }    
    public static void setMaxTurns(int val) {
        checkPerms();
        MAX_TURNS = val;
    }

    /**
     * width of the playfield, may change in the future
     */
    private static int WIDTH  = 140;
    /**
     * height of the playfield, may change in the future
     */
    private static int HEIGHT = 120;
    public static int getWidth() {
        return WIDTH;
    }
    public static int getHeight() {
        return HEIGHT;
    }
    public static void setWidth(int val) {
        checkPerms();
        WIDTH = val;
    }
    public static void setHeight(int val) {
        checkPerms();
        HEIGHT = val;
    }

    /**************************************************************************/
    
    // some UI constants here
    // delay between turns, in ms
    private static int TURN_DELAY = 100; 
    public static int getTurnDelay() {
        return TURN_DELAY;
    }    
    public static void setTurnDelay(int val) {
        checkPerms();
        TURN_DELAY = val;
    }
    
    // how frequently redraw screen, in turns
    private static int REFRESH_TURNS = 1;
    public static int getRefreshTurns() {
        return REFRESH_TURNS;
    }    
    public static void setRefreshTurns(int val) {
        checkPerms();
        REFRESH_TURNS = val;
    }

    private static boolean DRAW_NUMBER = false;
    public static void setDrawNumber(boolean param) {
        checkPerms();
	DRAW_NUMBER = param;
    }
    public static boolean getDrawNumber() {
	return DRAW_NUMBER;
    }

    private static boolean DRAW_GRID = false;
    public static void setDrawGrid(boolean param) {
        checkPerms();
	DRAW_GRID = param;
    }
    public static boolean getDrawGrid() {
	return DRAW_GRID;
    }

    private static boolean DEBUG = false;
    public static void setDebug(boolean param) {
        checkPerms();
	DEBUG = param;
    }
    public static boolean getDebug() {
        //checkPerms();
	return DEBUG;
    }

    private static String RECORD_PATH = null;
    public static void setRecordFile(String path) {
        checkPerms();
	RECORD_PATH = path;
    }
    public static String getRecordFile() {
        checkPerms();
	return RECORD_PATH;
    }
    

    // system constants
    // let remotely watch games over HTTP
    public static final boolean ALLOW_HTTP_WATCHING = true;

    /**
     * engine version
     */
    public static String VERSION = "v1.7.1, 3.03.2007"; 
    
    public static void checkPerms() {
        SecurityManager sm = System.getSecurityManager();
        if (sm instanceof JungleSecurity) {
	    ((JungleSecurity)sm).checkEnginePermission();            
        }
    }

    static {
        JungleSecurity.updateSecurity();
    }
};
