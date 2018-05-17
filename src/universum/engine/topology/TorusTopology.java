 package universum.engine.topology;

import java.util.*;
import universum.bi.Location;
import universum.util.*;
import universum.engine.LocationUtil;
import universum.engine.Topology;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default topology of two dimensional torus
 *
 * @author nike
 */
public @SuppressWarnings("deprecation") 
    class TorusTopology implements Topology {

    private Location[][] space;
    private int width, height, maxDist;
    private Map<Location, List<Location>> locCache;
    private List<Integer> diameters; 
    
    public TorusTopology() {       
	// just dummies, to allow class preloading
	new Sphere(LocationUtil.createLocation(0, 0), 0);
        new ImmutableList<Location>(new Location[]{});
    }
    
    public void init(Object ... args) {
        if (args.length != 2 || 
            !(args[0] instanceof Integer) ||
            !(args[1] instanceof Integer)) {
            throw new IllegalArgumentException("must be 2 ints");
        }

        this.width = (Integer)args[0];
        this.height = (Integer)args[1];

        this.maxDist = Math.max(width, height);
        space = new Location[width][height];
        locCache = new ConcurrentHashMap<Location, List<Location>>();
        diameters = new ArrayList<Integer>();
        diameters.add(width); diameters.add(height); 

        for (int i=0; i<width; i++) {
            for (int j=0; j<height; j++) {
                space[i][j] = new Location(i, j);
            }
        }
    }
    
    public List<Location> getNeighbours(Location loc) {
        List<Location> rv = locCache.get(loc);
        if (rv != null) {
            return rv;
        }
        
        int x = loc.getX(); int y = loc.getY();
        int xn = normal(x - 1, width); int yn = normal(y - 1, height);
        int xp = normal(x + 1, width); int yp = normal(y + 1, height);
        Location[] locations = new Location[] {
            space[xn][yn], space[x][yn], space[xp][yn],
            space[xn][y],                space[xp][y],
            space[xn][yp], space[x][yp], space[xp][yp]
        };
        rv = new ImmutableList<Location>(locations);
        locCache.put(loc, rv);

        return rv;
    }
    
    public List<Location> getNeighbours(Location loc, float radius) {
        int r = (int)radius;
        if (r == 1) {
            // optimize the common case: nearest neighbour locations are cached
            return getNeighbours(loc);
        }
        return new Sphere(loc, r);
    }
    
    public void walkSpace(Walker<Location> walker) {
        for (int i=0; i<width; i++) {
            for (int j=0; j<height; j++) {
                walker.walk(space[i][j]);
            }
        }
    }

    public Location getRandomLocation() {
        return space[Util.rnd(width)][Util.rnd(height)];
    }
    
    public List<Integer> diameters() {
        return diameters;
    }
     
    public float distance(Location one, Location other) {
        float dx = Math.abs(one.getX() - other.getX());
        dx = Math.min(dx, width - dx);
        float dy = Math.abs(one.getY() - other.getY());
        dy = Math.min(dy, height - dy);
        return Math.max(dx, dy);
    }

    public boolean contains(Location l) {       
        int x = l.getX(), y = l.getY();
        return (x >= 0 && x < width && y >= 0 && y < height);
    }

    private class Sphere extends AbstractList<Location>
        implements List<Location>
    {
        private int x0;
        private int y0;
        private int diameter;
        private int count;

        public Sphere(Location loc, int radius) {
            x0 = loc.getX() - radius;
            y0 = loc.getY() - radius;
            diameter = 2 * radius + 1;
            count = diameter * diameter - 1;
        }

        public int size() {
            return count;
        }

        public Location get(int index) {
            if (index < 0 || index >= count) {
                throw new IndexOutOfBoundsException();
            }
            if (index >= count / 2) {
                // the center of the sphere is omitted
                index++;
            }
            int y = y0 + index / diameter;
            int x = x0 + index % diameter;
            return space[normal(x, width)][normal(y, height)];
        }
    }

    public Location createLocation(Object ... args) {
        if (args.length != 2 || 
            !(args[0] instanceof Integer) ||
            !(args[1] instanceof Integer)) {
            throw new IllegalArgumentException("must be 2 ints");
        }
        int x = (Integer)args[0];
        int y = (Integer)args[1];
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("must be on the field");
        }        
        return space[x][y];
    }

    public Location stepToward(Location from, Location to, float speed) {
        float d = distance(from, to);
        
        if (d <= speed) {
            return to;
        }

        int x1 = from.getX(), y1 = from.getY();
        int x2 = to.getX(),   y2 = to.getY();
      
        int xd = getDelta( x1, x2, (int)speed, width );
        int yd = getDelta( y1, y2, (int)speed, height );

        return space[normal(x1+xd,width)][normal(y1+yd, height)];
    }
        
    private int getDelta( int x1, int x2, int speed, int size) {
        // X distance
        int d = Math.abs(x2 - x1);
        d = Math.min(d, size - d);
        
        int sign = (x1 == x2) ? 0: ( normal(x1+d, size) == x2 )? 1: -1;
        
        return Math.min( d, speed  ) * sign;
    } 

   private int normal(int x, int max) {
       return (x + max) % max;
   }
}
