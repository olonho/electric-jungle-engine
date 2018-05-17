package universum.engine;

import universum.bi.Location;
import universum.util.Walker;
/**
 *
 * @author nike
 */
public interface ResourceControl {
    public static final int RESOURCE_POWER = 0;
    public static final int RESOURCE_MAX = 1;
    
    /**
     * initialize resource controller with given topology
     */
    public void init(Topology topo, Object ... args);
    /**
     * returns amount of energy at some point
     */
    public float getCount(Location loc);
    /**
     * returns growth rate
     */
    public float getGrowthRate(Location loc);
    /**
     * the maximum amount of energy at the point
     */
    public float getMaxCount(Location loc);
    /**
     * atomically tries to consume some amount of resource
     * returns actually consumed amount
     */
    public float consume(Location loc, float count);
    /**
     * walker over all resource locations
     */
    public void forAllResourceLocations(Walker<Location> walker);
    /**
     * adds energy source at the point
     */
    void addSource(Location loc, float count, float rate, float max);
    /**
     * remove energy source at this point
     */
    void removeSource(Location loc);
    /**
     * make a turn
     */
    void makeTurn();
}
