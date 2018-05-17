package universum.ui;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.io.*;
import java.util.*;

import universum.engine.*;
import universum.bi.*;
import universum.util.*;

import static universum.engine.RemoteProto.*;

public abstract class RemoteInfo implements RenderingInfo {
    ByteBuffer buf;

    void init() {
        buf = ByteBuffer.allocate(BUF_SIZE);
        status = STATUS_UNKNOWN;
    }

    abstract boolean refresh() throws IOException;

    private String getString() {
        int len = buf.getInt();
        char[] data = new char[len];
        for (int i=0; i<len; i++) {
            data[i] = buf.getChar();
        }
        return new String(data);
    }

    private ScoreData parsePlayer(int id, boolean hasErrorData) {
        float score = buf.getFloat();
        float energy = buf.getFloat();
        String player = getString();
        ScoreData sd = new ScoreData(player, null);
        if (hasErrorData) {
            sd.storeException(getString());
        }
        sd.energy = energy;
        sd.score = score;
        sd.id = id;
        return sd;
    }

    static class SimpleInfo {
        Integer id, owner;
        float e;
        Location loc;
        SimpleInfo(Integer id, Integer owner, float e, Location loc) {
            this.id = id;
            this.owner = owner;
            this.e = e;
            this.loc = loc;
        }
    }

    protected Map<String,ScoreData> scores;    
    protected Map<Integer,SimpleInfo> info;
    protected Map<Integer,String> playerNames;
    protected Map<Location,Float> resources;
    protected List<Integer> diameters;
    protected int maxTurns;
    protected int numTurns;
    protected int players;
    protected int status;
    protected GameKind kind;

    protected synchronized boolean parseBuf() {
        scores = new HashMap<String,ScoreData>();
        info = new HashMap<Integer,SimpleInfo>();
        playerNames = new HashMap<Integer,String>();
        resources = new HashMap<Location,Float>();
        diameters = new ArrayList<Integer>(2);

        // status
        status = buf.getInt();

        // kind
        kind = Util.int2Kind(buf.get());

        // playfield size
        diameters.add(buf.getInt());
        diameters.add(buf.getInt());

        // current turn
        maxTurns = buf.getInt();
        numTurns = buf.getInt();

        // players data
        players = 0;
        for (;;) {
            int id = buf.getInt();
            if (id == -1) break;
            if (id >= players) {
                players = id + 1;
            }
            ScoreData sd = parsePlayer(id, status >= STATUS_COMPLETED);
            scores.put(sd.player, sd);
            playerNames.put(id, sd.player);
        }

        // entities
        for (;;) {
            int id = buf.getInt();
            if (id == -1) break;
            int owner = buf.getInt();
            Location loc = LocationUtil.fromBuffer(buf);
            float e = buf.getFloat();
            info.put(id, new SimpleInfo(id, owner, e, loc));
        }
        // resources
        for (;;) {
            float c = buf.getFloat();
            if (c < 0) break;
            Location loc = LocationUtil.fromBuffer(buf);
            resources.put(loc, c);
        }

        return status < STATUS_COMPLETED;
    }

    protected void doChannelRead(ReadableByteChannel sc) throws IOException {
        buf.clear();
        buf.limit(8);
        int r = sc.read(buf); 
        if (r < 8) {
            throw new IOException("short read: "+r);
        }
        buf.flip();
        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("inconsistent magic: "+magic);
        }
        int len = buf.getInt();
        if (len < 8 || len > BUF_SIZE) {
            throw new IOException("bad frame: "+len);
        }
        buf.flip();
        //System.out.println("len="+len);
        buf.limit(len-8);
        int read = 8;
        do {
            r = sc.read(buf);
            if (r < 0) {
                throw new IOException("cut frame");
            }
            read += r;
        } while (read < len);

        buf.flip();
    }

    // of RenderingInfo  
    synchronized public String getOwner(Integer id) {
        SimpleInfo si = info.get(id);        
        return playerNames.get(si.owner);
    }

    synchronized public int             getType(Integer id) {
        SimpleInfo si = info.get(id);        
        return si.owner;
    }

    synchronized public Location        getLocation(Integer id) {
        SimpleInfo si = info.get(id);
        return si.loc;
    }

    synchronized public float           getEnergy(Integer id) {
        SimpleInfo si = info.get(id);
        return si.e;
    }
    
    synchronized public float           getResourceCount(Location loc) {
        return resources.get(loc);
    }

    synchronized public List<Integer> diameters() {
        return diameters;
    }

    synchronized public int numTurns() {
        return numTurns;
    }

    synchronized public int maxTurns() {
        return maxTurns;
    }

    synchronized public void forAllE(Walker<Integer> what) {
        for (Integer id : info.keySet()) {
            what.walk(id);
        }
    }

    synchronized public void forAllR(Walker<Location> what) {
        for (Location l : resources.keySet()) {
            what.walk(l);
        }
    }

    public int getStatus() {
        return status;
    }

    public synchronized Map<String,ScoreData> scores() {
        return this.scores;
    }

    public synchronized String nameById(int num) {
        return playerNames.get(num);
    }

    public byte[] getIconData(Integer type) {
        return null;
    }

    public GameKind getGameKind() {
        return kind;
    }
}
