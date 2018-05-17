package universum.engine;

import java.util.List;
import universum.bi.Location;
import universum.util.Walker;

/**
 *
 * @author nike
 */
public interface Topology {
    /**
     * Initialize topology, arguments are topology specific
     *
     * @throws IllegalArgumentException if arguments make no sense
     */
    public void init(Object ... args);
    
    /**
     * Visit all locations of this world
     */
    public void walkSpace(Walker<Location> walker);
    /**
     * Get random location in this world.
     */
    public Location getRandomLocation();
    /**
     * Get all points being direct neighbours of given location.
     * Usually same as <code>getNeighbours(Location loc, 1f)</code>
     */
    public List<Location> getNeighbours(Location loc);
    /**
     * Get all points being in some radius of given location.
     */
    public List<Location> getNeighbours(Location loc, float radius);
    
    // those one adds metrical properties, not really belonging to the topology
    // but we need a way to represent it on 2D screen in 3D Euclidian space (what a mess!)
    /**
     * Sizes of projection of this world on 2D Euclidian space,
     * to be rendered on the screen.
     */
    public List<Integer> diameters();

    /**
     * Computes distance between two locations.
     */
    public float distance(Location one, Location other);

    /**
     * If ths location belogins to this world.
     */
    public boolean contains(Location x);

    /**
     * Create new location, with topology specific arguments.
     * Extremly not recommended to use.
     */
    public Location createLocation(Object ... points);

    /**
     * Find best location in direction of the given location
     */
    public Location stepToward(Location from, Location to, float speed);
}
