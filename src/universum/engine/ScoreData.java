package universum.engine;

import java.util.*;
import java.io.PrintWriter;
import java.io.StringWriter;

import universum.util.Util;

/**
 *
 * @author nike
 */
public final class ScoreData implements Comparable<ScoreData> {
    public String  jarFile;
    public String  player;
    public float   score;
    public float   energy;
    public int     id; // unique id of particluar player
    public boolean alive;
    private static int curId = 0;
    private List<Throwable> exceptions; 
    private static final int MAX_EXCEPTIONS = 1; 
    private String exceptionString;

    public ScoreData(String player, String jarFile) {
        this.player  = player;
        this.score   = 0.0f;
        this.energy  = 0.0f;
        this.alive   = true;
        this.id      = curId++;
        this.jarFile = jarFile;
    }

    static void reset() {
        curId = 0;
    }

    public int compareTo(ScoreData other) {
        float thisVal = this.score;
        float otherVal = other.score;
        return (thisVal > otherVal ? -1 : (thisVal == otherVal ? 0 : 1));
    }

    public String getErrors() {
        if (exceptionString != null) {
            return exceptionString;
        }

        if (exceptions != null ) {
            StringBuffer sb = new StringBuffer();
            for (Throwable ex : exceptions) {
                StringWriter sw = new StringWriter( 250 );
                PrintWriter pw = new PrintWriter( sw );
                ex.printStackTrace( pw );
                sb.append( sw.toString() );
                sb.append( "\n");
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public synchronized void storeException(Throwable t) {
        if (exceptions == null ) {
            exceptions = new LinkedList<Throwable>();
        }
        if (exceptions.size() < MAX_EXCEPTIONS ) {
            exceptions.add(t);
        }
    }
    
    public synchronized void storeException(String exceptionString) {
        this.exceptionString = exceptionString;
    }
}
