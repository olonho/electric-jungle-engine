package universum.bi;

import java.util.List;
import java.util.LinkedList;
import universum.engine.*;

/**
 * Information on particular point of the universe. Unlike Location 
 * keeps dynamical information about the world.
 *
 * @author nike
 */
public final class PointInfo {
    private Location loc;
    private Universe u;
    private List<Integer> ids;
    private Object mark;
    
    /**
     * Engine only constructor.
     */
    public PointInfo(Universe u, Location loc) {
        Constants.checkPerms();
        this.loc = loc;
        this.u = u;
        ids = new LinkedList<Integer>();
    }
    
    /**
     * Topological location of this point.
     */
    public Location getLocation() {
        return loc;
    }
    
    /**
     * Amount of energy at this point.
     */
    public float getCount(Being me) {
        // only known if close
        if (!u.closeEnough(me, getLocation())) {
            return -1f;
        }
        return u.getResourceControl().getCount(loc);
    }
    
    /**
     * How fast energy increases here.
     *
     * @return increase per turn
     */
    public float getGrowthRate(Being me) {
        // always known 
        return u.getResourceControl().getGrowthRate(loc);
    }
    
    /**
     * The maximum amount of energy at the point.
     */
    public float getMaxCount(Being me) {
        // always known 
        return u.getResourceControl().getMaxCount(loc);
    }
    
    /**
     * Returns ids of all entities at given point. Can be used
     * as parameter for events. Parameter data is an array, if of enough
     * length to be used as output paramter, otherwise new array allocated. 
     * If its length bigger than number of entities, array is null-terminated.
     * 
     * @return array of entities
     */
    public synchronized Integer[] getEntities(Entity me, Integer[] data) {
        // only known if close
        if (!u.closeEnough(me, getLocation())) {
            return null;
        }
        int ilen = ids.size();
        if (data == null) {
            data = new Integer[ilen];
        }
        int dlen = data.length;
        data = ids.toArray(data);
        // null terminate, if needed
        if (dlen > ilen) {
            data[ilen] = null;
        }
        return data;
    }
   
    /*
    synchronized public void putMark(Object mark) {
        Constants.checkPerms();
        this.mark = mark;
    }

    synchronized public Object getMark() {
        return mark;
	} */

    /**
     * Engine-only API.
     */
    synchronized public void addEntity(Integer id) {
        Constants.checkPerms();
        ids.add(id);
    }
    
    /**
     * Engine-only API.
     */
    synchronized public void removeEntity(Integer id) {
        Constants.checkPerms();
        ids.remove(id);
    }

    /**
     * Total mass of beings at the point. 
     *
     * @return sum of masses of all beings at the point, or -1 if we cannot 
     */
    synchronized public float totalMass(Being me) {
	if (me == null) {
	    // API only for the engine 
	    Constants.checkPerms();
	} else {
	    if (!u.closeEnough(me, getLocation())) {
		return -1f;
	    }
	}
	float rv = 0f;
	for (Integer i : ids) {
	    rv += u.getMass(i);
	}
	return rv;
    }
}
