package universum.bi;

import java.util.List;
import universum.engine.*;

/**
 * The  BeingInterface defines everything being knows about reality it lives in.
 * For random numbers and other utility functions see universum.util.Util 
 * (don't use java.util.random() or custom implementation for reproducablity 
 * reasons).
 *
 * @author nike
 * @see universum.util.Util
 */
public interface BeingInterface extends EntityInterface {
    /**
     * Provides owner's handle, for simple FoF detection
     * @return id
     */
    public Object          getOwner(Being me, Integer id);

    /**
     * Provides rounded up mass of being described by id
     * @return rounded mass
     */
    public float           getMass(Being me, Integer id);

    /**
     * Every being is characterized by energy, this function returns
     * current energy of the being
     * @return energy
     */
    public float           getEnergy(Being me);

    /**
     * Provide information about point where being is
     * @return point description
     */
    public PointInfo       getPointInfo(Being me);

    /**
     * Information about points in the neighbourhood
     * @return list of neighbour points
     */
    public List<PointInfo> getNeighbourInfo(Being me);

    /**
     * Get the locations we can go to in one turn (depends on speed)
     * @return available locations
     */
    public List<Location>  getReachableLocations(Being me);

    /**
     * Compute step towards a location. Note that there may be several 
     * paths leading to the target, this function returns framgment
     * of one of such paths.
     *
     * @return reachable location where being can go toward a target
     */
    public Location stepToward(Being me, Location to);

    /**
     * Logging facility
     */
    public void            log(Being me, String s);
}
