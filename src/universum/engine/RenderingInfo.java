package universum.engine;

import java.util.List;
import java.util.Map;
import java.awt.Image;

import universum.bi.*;
import universum.util.Walker;

/**
 * This class provides enough information to render universe.
 * May be implemented by networked version, thus no complex data structures used.
 */
public interface RenderingInfo {
    /**
     * Current game status.
     */
    public int getStatus();

    /**
     * Owner's name.
     */
    public String          getOwner(Integer beingId);

    /**
     * Unique type id of this being.
     */
    public int             getType(Integer beingId);
    
    /**
     * Where this being is?
     */
    public Location        getLocation(Integer beingId);    

    /**
     * How much energy this being has?
     */
    public float           getEnergy(Integer id);
    
    /**
     * Current turn number.
     */
    public int             numTurns();
    
    /**
     * Total numer of turns.
     */
    public int             maxTurns();

    /**
     * How much energy this particular location has?
     */
    public float           getResourceCount(Location loc);
    
    /**
     * Playfield dimensions, for representation on the screen.
     */
    public List<Integer>   diameters();

    /**
     * Visit all points where beings are.
     */
    public void forAllE(Walker<Integer> what);
    
    /**
     * Visit all points where energy is.
     */
    public void forAllR(Walker<Location> what);

    /**
     * Returns scores table.
     */
    public Map<String,ScoreData> scores();

    /**
     * Returns unique ID of the player.
     */
    public String nameById(int num);

    /**
     * Returns icon data for particular entity kind.
     */
    byte[] getIconData(Integer type);

    /**
     * Game kind for this game
     */
    GameKind getGameKind();
}
