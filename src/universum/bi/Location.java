package universum.bi;

/**
 * Location defines the point in the space of game universe.
 * Usually user is not intended to create new locations with constructor
 * but get existing location using methods of BeingInterface.
 * In rare occasions where you do need to create new location
 * and absolutely sure about topology you're running on,
 * use BeingInterface.createLocation()
 *
 * @see BeingInterface
 * @author nike
 */
public final class Location {
    private final int x, y;

    /** 
     * Create new location in 2D discreet topology.
     * Don't use this constuctor unless you have to, as
     * it will eventually be removed. 
     * Use BeingInterface.createLocation(x,y).
     *
     * @see BeingInterface#createLocation(Object...)
     */
    @Deprecated public Location(int x, int y) {     
        //Constants.checkPerms();
        this.x = x;
        this.y = y;
    }
    
    
    /**
     * Return x coordinate of this location, actual semantic of this
     * function depends on topology and may be something not so intuitive.
     */
    public int getX() {
        return x;
    }
    
    /**
     * Return y coordinate of this location, , actual semantic of this
     * function depends on topology and may be something not so intuitive.
     */
    public int getY() {
        return y;
    }
    
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Location)) {
            return false;
        }
        Location loc = (Location)other;
        return loc.getX() == x && loc.getY() == y;
    }
    
    public int hashCode() {
        return x << 16 | y;
    }

    public String toString() {
        return "["+x+","+y+"]";
    }       
}
