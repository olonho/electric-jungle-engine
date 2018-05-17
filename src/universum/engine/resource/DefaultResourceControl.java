package universum.engine.resource;

import java.util.*;
import universum.bi.Location;
import universum.bi.Constants;

import universum.util.Util;
import universum.util.Walker;
import universum.engine.Topology;
import universum.engine.ResourceControl;

/**
 *
 * @author nike
 */
public class DefaultResourceControl implements ResourceControl {
    protected Map<Location,float[]> resources;
    private List<Location> zombies;
    protected int reg, golden;
    
    public DefaultResourceControl() {
        resources = new HashMap<Location,float[]>();
        zombies = new LinkedList<Location>();
    }
    
    public void init(Topology topo, Object ... args) {
        if (args.length != 2 || 
            !(args[0] instanceof Integer) ||
            !(args[1] instanceof Integer)) {
            throw new IllegalArgumentException("must be 2 ints, regular and golden");
        }                

        this.reg = (Integer)args[0];
	this.golden = (Integer)args[1];

	// add regular feeding grounds
        for (int i = 0; i<reg; i++) {
            addNextResource(topo.getRandomLocation(), false);
        }
	
	// and few golden spots
	for (int i = 0; i<golden; i++) {
	    addNextResource(topo.getRandomLocation(), true);
	}
    }
        
    protected void addNextResource(Location loc, boolean golden) {
        addSource(loc, 
                  Util.frnd(golden ? 
                            Constants.MAX_GOLDEN : Constants.MAX_REGULAR), 
                  Util.frnd(golden ? 
                            Constants.GROW_GOLDEN : Constants.GROW_REGULAR), 
                  /* same as count */
                  golden ? Constants.MAX_GOLDEN : -1f);
    }

    private float[] get(Location loc) {
        synchronized (resources) {
           return resources.get(loc); 
        }
    } 
    
    public float getCount(Location loc) {
        float[] res = get(loc);
        return  res == null ? 0f : res[0];
    }
    
    public float getGrowthRate(Location loc) {
        float[] res = get(loc);        
        return  res == null ? 0f : res[1];
    }
    
    public float getMaxCount(Location loc) {
        float[] res = get(loc);        
        return  res == null ? 0f : res[2];
    }
    
    public float consume(Location loc, float count) {
        float[] res = get(loc);
        float c = 0f;
        
        if (res != null) {
            synchronized (res) {
                c = res[0];
                if (c >= count) {
                    res[0] -= count;
                    c = count;
                } else {
                    c = res[0];
                    res[0] = 0f;
                }
            }
        }        
        return c;
    }
    
    public void forAllResourceLocations(Walker<Location> walker) {      
        synchronized (resources) {
            Util.walkList(resources.keySet(), walker);
        }
    }
    
    public void addSource(Location loc, float count, float rate, float max) {
       synchronized (resources) {
            float res[] = resources.get(loc);
            boolean isTransient = false;
	    // normalization
	    if (max < 0f) {
                max = count;
                isTransient = true;
            }
	    if (count > max) {
                count = max;
            }

            if (res != null) {
                res[0] += count;
                res[1] += rate;
                if (!isTransient) {                    
                    res[2] += max;
                    if (res[0] > res[2]) {
                        res[0] = res[2];
                    }
                }
            } else {
               res = new float[] {count, rate, max};
               resources.put(loc, res);
            }
            if (res[0] > res[2] && !isTransient) {
                res[0] = res[2];
            }
        }
    }
    
    public void removeSource(Location loc) {
        synchronized (resources) {
            //resources.put(loc, null);
            resources.remove(loc);
        }
    }
    
    public void makeTurn() {
        zombies.clear();
        synchronized (resources) {
            Set<Location> places = resources.keySet();
            for (Location l : places) {
                float res[] = resources.get(l);
                res[0] += res[1];
                if (res[0] < 0.00001f && res[1] < 0.000001f) {
                    zombies.add(l);
                }
		if (res[0] > res[2]) {
		    res[0] = res[2];
		}
            }
            for (Location z : zombies) {
                resources.remove(z);
            }
        }
    }
}
