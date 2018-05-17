package universum.engine.topology;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import universum.bi.Location;
import universum.engine.LocationUtil;
import universum.engine.Topology;
import universum.util.ImmutableList;
import universum.util.Util;
import universum.util.Walker;

/**
 * HoneycombTopology
 *
 * @author Anar Ibragimoff
 * @date 16.01.2007
 * $Revision: 1.3 $
 */
public @SuppressWarnings("deprecation") 
    class HoneycombTopology implements Topology {
    
    private Location[][] space;
    private int radius, diameter, radiusp1, diameterp1; // actually we need only radius, but for eXtra-perfomance :-)
    private Map<Location, List<Location>> locCache;
    private List<Location> allLocations;
    private List<Integer> diameters; 
    
    /**
     * Sphere :-)
     * Nike's good solution to provide reachable locations as memory-cheap list
     */
    private class Sphere extends AbstractList<Location> implements List<Location>{
        private int x0;
        private int y0;
        private int radius;
        private int count;
    
        public Sphere(Location loc, int radius) {
            x0 = loc.getX() - radius;
            y0 = loc.getY() - radius;
            this.radius = radius;
            
            // calculate count of all locations in the sphere
            int multiplicator;
            if(radius % 2 == 0){
                multiplicator = (radius + 1) * (radius >> 1);
            }else{
                multiplicator = (radius + 1) * ((radius - 1) >> 1) + ((radius + 1) >> 1);
            }
            count = 6 * multiplicator;
        }
    
        public int size() {
            return count;
        }
    
        // TODO optimize this piece of ... "nice" code :-(
        public Location get(int index) {
            if (index < 0 || index >= count) {
                throw new IndexOutOfBoundsException();
            }
            int half = count >> 1;
            boolean mirror = false;
            if (index >= half) {
                // the center of the sphere is omitted
                index-=half;
                mirror = true;
            }
            int x = 0, y = 0;
            int l = radius;
            int r = 0;
            while(index - (r+l) > 0){
                l++;
                r += l;
                y++;
            }
            x = index - r;
            if(mirror){
                x = (radius<<1) - x;
                y = (radius<<1) - y;
            }
            return getNormalizedLocation(x0+x, y0+y);
        }
    }
    
    /**
     * 
     */
    public HoneycombTopology(){
        // just dummies, to allow class preloading
        new Sphere(LocationUtil.createLocation(0, 0), 0);
        new ImmutableList<Location>(new Location[]{});        
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#init(universum.engine.Universe)
     */
    public void init(Object ... args){
        // to make compatible with the way engine invokes it 
        if ((args.length < 1) ||
            !(args[0] instanceof Integer)) {
            throw new IllegalArgumentException("must be single int");
        }

        this.radius = (Integer)args[0];
        this.radiusp1 = radius + 1;
        this.diameter = radius << 1;
        this.diameterp1 = diameter + 1;
        space = new Location[diameterp1][diameterp1];
        locCache = new HashMap<Location, List<Location>>();
        diameters = new ArrayList<Integer>();
        diameters.add(diameter); diameters.add(diameter);
        
        allLocations = new ArrayList<Location>();
        for (int y=0; y<diameterp1; y++) {
            for (int x=0; x<diameterp1; x++) {
                // we do init only existing cells
                if(Math.abs(y-x)<=radius){
                    Location location = LocationUtil.createLocation(x, y);
                    space[y][x] = location;
                    allLocations.add(location);
                }else{
                    space[y][x] = null;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#walkSpace(universum.util.Walker)
     */
    public void walkSpace(Walker<Location> walker){
        for(Location location : allLocations){
            walker.walk(location);
        }
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#getRandomLocation()
     */
    public Location getRandomLocation(){
        return allLocations.get(Util.rnd(allLocations.size()));
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#getNeighbours(universum.bi.Location)
     */
    public List<Location> getNeighbours(Location loc){
        List<Location> rv = locCache.get(loc);
        if (rv == null) {
            synchronized (locCache) {
                int x = loc.getX(); int y = loc.getY();
                Location[] locations = new Location[] {
                    getNormalizedLocation(x-1, y-1), getNormalizedLocation(x, y-1),
                    getNormalizedLocation(x-1, y  ),                                getNormalizedLocation(x+1, y),
                                                     getNormalizedLocation(x, y+1), getNormalizedLocation(x+1, y+1)
                };
                rv = new ImmutableList<Location>(locations);
                locCache.put(loc, rv);
            }
        }
        return rv;
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#getNeighbours(universum.bi.Location, float)
     */
    public List<Location> getNeighbours(Location loc, float radius){
        int r = (int)radius;
        if (r == 1) {
            // optimize the common case: nearest neighbour locations are cached
            return getNeighbours(loc);
        }
        return new Sphere(loc, r);
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#diameters()
     */
    public List<Integer> diameters(){
        return diameters;
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#distance(universum.bi.Location, universum.bi.Location)
     */
    public float distance(Location one, Location other){
        int x1 = one.getX();
        int y1 = one.getY();
        int x2 = other.getX();
        int y2 = other.getY();
        int distance = distance(x1, y1, x2, y2);
        if(distance > radius){
            distance = distance(x1, y1, x2-diameterp1, y2-radius); // distance to point in satelite 1 (see notes in getNormalizedLocation())
            if(distance > radius){
                distance = distance(x1, y1, x2-radiusp1, y2-diameterp1); // distance to point in satelite 2
                if(distance > radius){
                    distance = distance(x1, y1, x2+radius, y2-radiusp1); // distance to point in satelite 3
                    if(distance > radius){
                        distance = distance(x1, y1, x2+diameterp1, y2+radius); // distance to point in satelite 4
                        if(distance > radius){
                            distance = distance(x1, y1, x2+radiusp1, y2+diameterp1); // distance to point in satelite 5
                            if(distance > radius){
                                distance = distance(x1, y1, x2-radius, y2+radiusp1); // distance to point in satelite 6
                            }
                        }
                    }
                }
            }
        }
        return distance;
    }
    
    /**
     * Calculates distance between arbitrary points (even with negative coordinates)
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return calculated distance
     */
    private final int distance(int x1, int y1, int x2, int y2){
        int dx = x2 - x1;
        int dy = y2 - y1;
        int abs_dx = Math.abs(dx);
        int abs_dy = Math.abs(dy);
        
        if(dx*dy >= 0){
            return Math.max(abs_dx, abs_dy);
        }else{
            return Math.max(abs_dx, abs_dy) + Math.min(abs_dx, abs_dy);
        }
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#contains(universum.bi.Location)
     */
    public boolean contains(Location l){
        int x = l.getX(), y = l.getY();
        return contains(x, y);
    }

    /**
     * Checks if cell pointed by x and y belongs to the set of initialized cells.
     * @param x
     * @param y
     * @return true if (x, y) belongs to the set of initialized cells 
     */
    private final boolean contains(int x, int y){
        return (x >= 0 && x <= diameter && y >= 0 && y <= diameter) && (Math.abs(y-x)<=radius);
    }

    /**
     * Converts (x, y) to normalized coordinates (which belongs to the set of initialized cells) 
     * and returns Location of this point 
     * @param x
     * @param y
     * @return
     * @see universum.engine.topology.HoneycombTopology#contains(int, int)
     */
    private final Location getNormalizedLocation(int x, int y){
        while(!contains(x, y)){
            if((y < 0) && (x < radius) && (x >= y)){ // satelite 2 direction
                x += radiusp1;
                y += diameterp1;
            }else if((x < 0) && (y <= radius) && (y > x)){ // satelite 1 direction
                x += diameterp1;
                y += radius;
            }else if((x > diameter) && (y >= radius) && (y < x)){ // satelite 4 direction
                x -= diameterp1;
                y -= radius;
            }else if((y > diameter) && (x > radius) && (x <= y)){ // satelite 5 direction
                x -= radiusp1;
                y -= diameterp1;
            }else if((y < radius)){ // satelite 3 direction
                x -= radius;
                y += radiusp1;
            }else{ // satelite 6 direction 
                // it should be "if (y > radius)"... but who knows if my estimations are correct :-)
                x += radius;
                y -= radiusp1;
            }
            /*
             * Note: Satelites locations (satelites - are copies of current space placed right up to the current space):
             *   \ 2 |
             * 1  \__|  3
             * ___|   \____
             *   6 \___| 4
             *     | 5  \
             *     |     \
             *     
             * Note: We have "while" construction in case of specified coordinates point to 
             * somewhere outside of closest satelites. In this case point would move to the
             * center (and it could be undulatory motion). 
             */
        }
        return space[y][x];
    }
    
    /* (non-Javadoc)
     * @see universum.engine.Topology#createLocation(java.lang.Object...)
     */
    public Location createLocation(Object... args){
        if (args.length != 2 || 
                !(args[0] instanceof Integer) ||
                !(args[1] instanceof Integer)) {
                throw new IllegalArgumentException("must be 2 ints");
            }
            int x = (Integer)args[0];
            int y = (Integer)args[1];
            return getNormalizedLocation(x, y);
    }

    /* (non-Javadoc)
     * @see universum.engine.Topology#stepToward(universum.bi.Location, universum.bi.Location, float)
     */
    public Location stepToward(Location from, Location to, float speed) {
        
        if(speed <1){ // just to avoid incorrect calculations
            return from;
        }
        
        int x1 = from.getX();
        int y1 = from.getY();
        int x2 = to.getX();
        int y2 = to.getY();
        int virtualX2 = x2;
        int virtualY2 = y2;
        // step 1: find closest point (even if it's virtual in some satelite)
        int distance = distance(x1, y1, x2, y2);
        if(distance > radius){
            distance = distance(x1, y1, x2-diameterp1, y2-radius); // distance to point in satelite 1 (see notes in getNormalizedLocation())
            if(distance <= radius){
                virtualX2 = x2-diameterp1;
                virtualY2 = y2-radius;
            }else{
                distance = distance(x1, y1, x2-radiusp1, y2-diameterp1); // distance to point in satelite 2
                if(distance <= radius){
                    virtualX2 = x2-radiusp1;
                    virtualY2 = y2-diameterp1;
                }else{
                    distance = distance(x1, y1, x2+radius, y2-radiusp1); // distance to point in satelite 3
                    if(distance <= radius){
                        virtualX2 = x2+radius;
                        virtualY2 = y2-radiusp1;
                    }else{
                        distance = distance(x1, y1, x2+diameterp1, y2+radius); // distance to point in satelite 4
                        if(distance <= radius){
                            virtualX2 = x2+diameterp1;
                            virtualY2 = y2+radius;
                        }else{
                            distance = distance(x1, y1, x2+radiusp1, y2+diameterp1); // distance to point in satelite 5
                            if(distance <= radius){
                                virtualX2 = x2+radiusp1;
                                virtualY2 = y2+diameterp1;
                            }else{
                                distance = distance(x1, y1, x2-radius, y2+radiusp1); // distance to point in satelite 6
                                virtualX2 = x2-radius; // virtual point in satelite 6
                                virtualY2 = y2+radiusp1; 
                            }
                        }
                    }
                }
            }
        }
        
        //step 2: find point on the way to the found virtual point
        if(distance <= speed){ // if found point is reachable with current speed at one step - retrun destinatio point
            return to;
        }else{
            float stepX = (float)(virtualX2-x1)/distance;
            float stepY = (float)(virtualY2-y1)/distance;
            int middleX = (int)(x1 + speed*stepX);
            int middleY = (int)(y1 + speed*stepY);
            //make correction in case of speed is not integer and found location is not reachable
            while(distance(x1, y1, middleX, middleY) > speed){
                middleX -= Math.round(stepX);
                middleY -= Math.round(stepY);
            }
            return getNormalizedLocation(middleX, middleY);
        }
    }
}
