package universum.engine;

import universum.bi.Location;

/**
 * File to provide custom operation on locations
 * instead of deprecated versions
 *
 * @author nike
 */
public class LocationUtil {    
    public static @SuppressWarnings("deprecation") 
        Location createLocation(int x, int y) {
        return new Location(x, y);
    }

    private static @SuppressWarnings("deprecation") 
        int getX(Location loc) {
        return loc.getX();
    }

    private static @SuppressWarnings("deprecation") 
        int getY(Location loc) {
        return loc.getY();
    }

    public static void toBuffer(java.nio.ByteBuffer buf, Location loc) {
        int x = getX(loc); 
        int y = getY(loc);
        if (x < 0 || x > 255 || y < 0 || y > 255) {
            throw new RuntimeException("cannot fit to byte");
        }
        buf.put((byte)x); buf.put((byte)y);        
    }

    public static Location fromBuffer(java.nio.ByteBuffer buf) {
        int x = buf.get() & 0xff;
        int y = buf.get() & 0xff;
        return createLocation(x, y);
    }
    
    public static float projectionX(Location loc, int xmax) {
        return (float)getX(loc)/(float)xmax;
    }

    public static float projectionY(Location loc, int ymax) {
        return (float)getY(loc)/(float)ymax;
    }
}
