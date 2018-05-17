package universum.util;

import java.util.*;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import universum.bi.Constants;
import universum.bi.GameKind;

/**
 *
 * @author nike
 */
public class Util {
    private static Random gen;
    private static PrintStream logger = System.out;
    
    static Random gen() {
	return gen;
    }

    public static int rnd() {
        return gen().nextInt();
    }
    
    // return int in interval [0, max)
    public static int rnd(int max) {
        return gen().nextInt(max);
    }
    
    // return int in interval [min, max)    
    public static int rnd(int min, int max) {
        return min + rnd(max-min);
    }
    
    // returns float in interval [0, 1)
    public static float frnd() {
        return gen().nextFloat();
    }
    
    // returns float in interval [0, max)
    public static float frnd(float max) {
        return frnd()*max;
    }
    
    // returns float random in interval [min, max)
    public static float frnd(float min, float max) {
        return min + frnd(max-min);
    }
    // iterates over collection and invoke walker's walk() upone every element
    // Note that collection's lock should be held by owner
    public static <E> void walkList(Collection<E> list, Walker<E> walker) {
        for (E e : list) {
            walker.walk(e);
        }
    }

    public static void sleep(long millis) {
	try {
	    Thread.sleep(millis); 
	} catch (Exception e) {}
    }

    public static InputStream findResourceAsStream(String name) {
        Constants.checkPerms();
        InputStream is = Util.class.getResourceAsStream("/resources/"+name);
        if (is == null) {
            is = Util.class.getResourceAsStream("/resources/resources/"+name);
        }
        return is;
    }

    public static URL findResource(String name) {
        Constants.checkPerms();
        URL u = Util.class.getResource("/resources/"+name);
         if (u == null) {
            u = Util.class.getResource("/resources/resources/"+name);
        }
        return u;
    }

    public static ResourceBundle findBundle(String name) {
        Constants.checkPerms();
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle("resources/"+name);
        } catch (MissingResourceException mre) {
            rb = ResourceBundle.getBundle("resources/resources/"+name);
        }
        return rb;
    }

    public static void setLogger(PrintStream ps) {
        Constants.checkPerms();
        logger = ps;
    }

    public static void log(String s) {
        if (logger != null) {
            logger.println(s);
        }
    }
    
    public static PrintStream getLogger() {
        return logger;
    }
    
    public static PrintStream getSystemLogger() {
        return logger != null ? logger : System.out;
    }

    public static int kind2Int(GameKind kind) {
        switch (kind) {
        case SINGLE:
            return 1;
        case DUEL:
            return 2;
        case PEACE_DUEL:
            return 3;
        case JUNGLE:
        default:
            return 4;
        case DEBUG:            
            return 5;
        }
    }

    public static GameKind int2Kind(int val) {
        switch (val) {
        case 1:
            return GameKind.SINGLE;
        case 2:
            return GameKind.DUEL;
        case 3:
            return GameKind.PEACE_DUEL;
        case 4:
            return GameKind.JUNGLE;
        case 5:
            return GameKind.DEBUG;
        default:
            return GameKind.JUNGLE;
        }
    }

    public static void setSeed(int seed) {
        Constants.checkPerms();
        // make it somewhat deterministic for debug    
        if (Constants.getDebug() && seed == 0) {
            seed = 10;
        }        
        gen = (seed == 0) ? new Random() : new Random(seed);
    }
}
