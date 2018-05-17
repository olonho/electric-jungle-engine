package universum.bi;

import universum.engine.*;

/**
 * Interface between engine and entity.
 *
 * @author nike
 */
public interface EntityInterface {
    /**
     * Gives unique integer id for entiry, to be used as a handle for it.
     * In debug version (-debug switch) id is sequential, otherwise
     * there are random to prevent cheating.
     *
     * @return id
     */
    public Integer         getId(Entity me);
    
    /**
     * Figure out current position of entity
     * @return location where entity is
     */
    public Location        getLocation(Entity me);
    
    /**
     * Compute distance in some metrics, depending on current topology
     * of space
     * @return distance between 2 locations
     */
    public float           distance(Location one, Location another);
    
    /**
     * @return current turn number
     */
    public int getTurnsCount();

    /**
     * Return approximate amount of time remaining till this entity 
     * will be killed by engine as hang. This method gives you no
     * real warranties, because of garbage collection and multithreaded
     * nature of engine and should be used with great care.
     * Typical creature turn must be much shorter than this, and only on rare
     * occassions of long computations this function should be used.
     * 
     * @return number of milliseconds, -1 if entity not currently executed
     */
    public int timeTillKilled(Entity me);
     
    /**
     * Creates location in specific for the topology way,
     * for example createLocation(2,1)
     * @return newly created location
     * @throws IllegalArgumentException if argument set not suitable 
     *         for current topology
     */
    public Location createLocation(Object ... args);

    /**
     * Return cost of particular entity kind.
     * For contest there's no avaliable entitites, so it's only 
     * for future engine development.
     * 
     * @return entity energy cost, or negative value if not available
     */
    float entityCost(Entity me, String entityKind);
}
