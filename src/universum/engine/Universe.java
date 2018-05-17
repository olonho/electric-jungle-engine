package universum.engine;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.io.InputStream;
import java.io.IOException;


import universum.bi.*;
import universum.util.*;
import universum.ui.GameOwner;
import universum.ui.GameResult;

import static universum.bi.Constants.*;
import static universum.engine.RemoteProto.*;
/**
 *
 * @author nike
 */
public class Universe 
    implements Runnable, EventListener, EventFilter, RenderingInfo 
{
    private GameOwner controller;
    private Topology topo;
    private ResourceControl rc;
    private EventQueue eq;
    private List<Entity> entities;
    private Map<String,ScoreData> scores;
    private Map<EventFilter,List<EventListener>> listeners;
    private Map<Entity,EntityInfo> info;
    private Map<Location,List<PointInfo>> neighbours;
    private Map<Location,PointInfo> points;
    private Map<Integer, EntityInfo> mapById;
    private Map<PointInfo, Integer> marks;
    private Map<String, EntityData> shop;
    private Map<Integer, TypeInfo>  types;
    private boolean seventhSeal;
    private boolean paused = false, started = false, stopWhenOneLeft;
    private Object pauser;
    private AccessControlContext creatureContext;
    private BeingInterfaceImpl iface;
    private RemoteRenderer broadcaster;
    private TurnService turnMaker;
    private TurnRecorder recorder;
    private int status;
    private GameInfo gameInfo;
    private boolean singleStep;
    private DefaultEntityFactory toyFactory;

    public Universe(GameOwner controller, GameInfo gameInfo) {
        setStatus(STATUS_UNKNOWN);
        Util.setSeed(gameInfo.randomSeed);
       
        this.topo = createTopology(gameInfo);
        this.rc = createResourceControl(gameInfo);

        this.controller = controller;
        this.maxTurns = gameInfo.maxTurns;
        this.recording = gameInfo.recordGameFilePath != null;
        // we keep reference to receive updated values
        this.gameInfo = gameInfo;

        seventhSeal = false;
        
        eq = new EventQueue();
        entities = new ArrayList<Entity>();
        proc = new Processor();
        listeners = new HashMap<EventFilter,List<EventListener>>();
        info = new ConcurrentHashMap<Entity,EntityInfo>();
        points = new HashMap<Location,PointInfo>();
        neighbours = new HashMap<Location,List<PointInfo>>();
        scores = new HashMap<String,ScoreData>();
        mapById = new ConcurrentHashMap<Integer, EntityInfo>();
        marks = new HashMap<PointInfo, Integer>();
        shop = Collections.synchronizedMap(new HashMap<String, EntityData>());
        types = Collections.synchronizedMap(new HashMap<Integer, TypeInfo>());
        pauser = new Object();
        toyFactory = new DefaultEntityFactory();
        registerToys();
        
        ScoreData.reset();

        PermissionCollection perms = makePerms();
        creatureContext = getAccessControlContext(perms);
                
        turnMaker = new TurnService(this);
        
        recorder = recording ? 
            new TurnRecorder(gameInfo.recordGameFilePath) : null;
        
        // make sure we'll get all events we care about
        // as for now we don't need to monitor events - remove ourselves
        // from the list
        // registerListener(this, this);               

        iface = new BeingInterfaceImpl();

        int port = controller.getListenPort();
        if (port >= 0 || recording) {
            broadcaster = new RemoteRenderer(this, controller);            
        }
        setStatus(STATUS_NOT_STARTED);
        // do some preloading
        System.out.flush();
        Util.setLogger(gameInfo.out);
        if (gameInfo.out != null) {            
            gameInfo.out.flush();
        }
    }

    private void registerToy(String id, float cost) {
        if (toyFactory.registerThing(id)) {
            EntityData ed = new EntityData(this, id, toyFactory, cost);
            shop.put(id, ed);
        }
    }
    
    // here we'll register all available entities
    private void registerToys() {
        //registerToy("Trap", 1f);
    }    
    
    private void registerType(Integer type, String jar) {
        TypeInfo ti = types.get(type);
        if (ti != null) {
            return;
        }
        ti = new TypeInfo(type, jar);
        types.put(type, ti);        
    }

    private Topology createTopology(GameInfo gi) {
        Topology t = null;
        try {
            Class tc = Class.forName(gi.topologyClass);
            t = (Topology)tc.newInstance();
            // not so flexible for now
            t.init(gi.fieldWidth, gi.fieldHeight);
        } catch (Exception e) {
            e.printStackTrace(Util.getSystemLogger());
        }
        return t;
    }

    private ResourceControl createResourceControl(GameInfo gi) {
        ResourceControl r = null;
        try {
            Class rc = Class.forName(gi.resourceClass);
            r = (ResourceControl)rc.newInstance();
            // not so flexible for now
            r.init(getTopo(), gi.numRegular, gi.numGolden);
        } catch (Exception e) {
            e.printStackTrace(Util.getSystemLogger());
        }
        return r;
    }

    private PermissionCollection makePerms() {       
        return null;     
    }
   
    public void setSingleStep(boolean single) {
        this.singleStep = single;
    }

    private AccessControlContext getAccessControlContext(PermissionCollection perms) {       
        ProtectionDomain domain =
            new ProtectionDomain(new CodeSource(null,
                                                (Certificate[]) null), perms);        
        AccessControlContext acc =
            new AccessControlContext(new ProtectionDomain[] { domain });
        
        return acc;
    }
   
    
    public void setController(GameOwner controller) {
        this.controller = controller;
    }
    
    private boolean addZombie(Entity e) {
        synchronized (zombies) {
            EntityInfo ei = getInfo(e);
            if (ei != null) {
                ei.clearPendingEvent();
            }
            return zombies.add(e);
        }
    }

    private boolean isZombie(Entity e) {
        synchronized (zombies) {
            return zombies.contains(e);
        }
    }

    private void death(EntityInfo ei) {
        Being b = ei.isBeing ? (Being)ei.entity : null;
        if (b == null) return;

        // if already dead - do nothing
        if (!addZombie(b)) {
            return;
        }
	if (getDebug()) {
	    Util.log(ei.name+" of "+ei.player+
                     "["+ei.id+"] just died");
	}
        sendEvent(new Event(EventKind.BEING_DEAD, ei.id, null), null);        
    }
    private Set<Entity> zombies = new HashSet<Entity>();
   
    // RenderingInfo implementation
    public  Integer getId(Entity e) {
        return iface.getId(e);
    }

    public Location getLocation(Integer id) {
	EntityInfo ei = getInfoById(id);
	if (ei == null) {
	    return null;
	}
        return ei.location();
    }
    
    public float getEnergy(Integer id) {
	EntityInfo ei = getInfoById(id);
	if (ei == null) {
	    return 0f;
	}
        return ei.energy;
    }

    public String getOwner(Integer id) {
        EntityInfo ei = getInfoById(id);
        if (ei == null) {
	    return null;
	}	
        return ei.player;
    }

    public int getType(Integer eid) {
        EntityInfo ei = getInfoById(eid);
        if (ei == null) {
	    return -1;
	}
        return ei.typeId;      
    }
    
    public float getMass(Integer id) {
        EntityInfo ei = getInfoById(id);
        if (ei == null) {
            return 0f;
        }
        
        return ei.params.M;
    }
    
    public float getResourceCount(Location loc) {
        return getResourceControl().getCount(loc);
    }
    
    public List<Integer> diameters() {
        return getTopo().diameters();
    }

    public int numTurns() {
        return this.numTurns;
    }

    public int maxTurns() {
        return this.maxTurns;
    }   
    
    public void forAllE(final Walker<Integer> what) {
	forAllEntities(new Walker<Entity>() {
                           public void walk(Entity e) {
                               Integer id = getId(e);
                               what.walk(id);
                           }
                       });
    }

    public void forAllR(Walker<Location> what) {
        getResourceControl().forAllResourceLocations(what);
    }

    public synchronized int getStatus() {
        return this.status;
    }

    public Map<String,ScoreData> scores() {
        synchronized (scores) {
            return this.scores;
        }
    }

    public String nameById(int num) {
        synchronized (scores) {
            if (num < currentResults.size()) {
                return currentResults.get(num).player;
            } else {
                return null;
            }
        }
    }

    public byte[] getIconData(Integer type) {                
        TypeInfo ti = types.get(type);
        if (ti == null) {
            return null;
	}
        return ti.getIconData();
    }

    public GameKind getGameKind() {
        return gameInfo.getKind();
    }

    //// end of RenderingInfo impl

    private void recordException(Entity e, Throwable t) {
        EntityInfo ei = getInfo(e);
        if (ei == null || ei.player == null) {
            return;
        }
        synchronized (scores) {
            final ScoreData score = scores.get(ei.player);
            if (score != null) {
                score.storeException(t);
            }
        }
    }
    
    public synchronized void setStatus(int status) {
        if (this.status >= STATUS_COMPLETED && status < this.status) {
            return;
        }
        this.status = status;
    }

    class BeingInterfaceImpl implements BeingInterface {
        BeingInterfaceImpl() {}
        
        // of EntityInterface
        public Location getLocation(Entity b) {
            EntityInfo ei = getInfo(b);
            if (ei == null) {
                return null;
            }
            return  ei.location();
        }
        // of BeingInterface
        public Integer getId(Entity e) {
            EntityInfo ei = getInfo(e);
            if (ei == null) {
                return null;
            }
            return ei.id;
        }
    
        public float distance(Location one, Location another) {
            return getTopo().distance(one, another);
        }
    
        public synchronized void log(Being b, String s) {
            logDo(b, s);
        }

        public int getTurnsCount() {
            return numTurns();
        }

        public int timeTillKilled(Entity me) {
            return proc.timeTillKilled(me);
        }

        public Location createLocation(Object ... args) {
            return getTopo().createLocation(args);
        }

        public float getEnergy(Being b) {
            EntityInfo ei = getInfo(b);
            if (ei == null) {
                return 0f;
            }
            assert ei.isBeing;
            return  ei.energy();
        }
            
        public PointInfo getPointInfo(Being b) {
            EntityInfo ei = getInfo(b);
            if (ei == null) {
                return null;
            }
            assert ei.isBeing;
            return getPointInfo(ei.location());
        }

        
        public float entityCost(Entity who, String what) {
            EntityData ed = shop.get(what); 
            return (ed == null) ? -1f : ed.cost();
        }
    
        private PointInfo getPointInfo(final Location loc) {
            synchronized (points) {
                // we lazily allocate PointInfos
                PointInfo pi = points.get(loc);
                if (pi == null) {
                    pi = AccessController.doPrivileged(new PrivilegedAction<PointInfo>() {
                                                           public PointInfo run() {
                                                               return new PointInfo(Universe.this, loc);
                                                           }
                                                       });
                    points.put(loc, pi);
                }
                return pi;
            }
        }
    
        public List<PointInfo> getNeighbourInfo(Being b) {
            EntityInfo ei = getInfo(b);
            if (ei == null) {
                return null;
            }
            assert ei.isBeing;
            Location loc = ei.location();
            List<PointInfo> rv;
            synchronized (neighbours) {
                rv = neighbours.get(loc);
                if (rv == null) {
                    List<Location> locations = topo.getNeighbours(loc);
                    int lc = locations.size();
                    PointInfo[] points = new PointInfo[lc];
                    for (int i = 0; i < lc; i++) {
                        points[i] = getPointInfo(locations.get(i));
                    }
                    rv = new ImmutableList<PointInfo>(points);
                    neighbours.put(loc, rv);
                }
            }
            return rv;
        }

        public List<Location> getReachableLocations(Being b) {
            final EntityInfo ei = getInfo(b);
            if (ei == null) {
                return null;
            }
            assert ei.isBeing;
            
            return topo.getNeighbours(ei.location(), ei.params.S);
        }

        public Location stepToward(Being b, Location to) {
            final EntityInfo ei = getInfo(b);
            if (ei == null) {
                return null;
            }
            assert ei.isBeing;
            
            return topo.stepToward(ei.location(), to, ei.params.S);
        }
    
        public Object getOwner(Being me, Integer id) {
            if (me == null || id == null) return 0f;            
            EntityInfo ei = getInfoById(id);
            if (ei == null) return null;
        
            return ei.playerHandle;
        }

        public float getMass(Being me, Integer id) {
            if (me == null || id == null) return 0f;
            EntityInfo ei = getInfoById(id);
            if (ei == null) return 0f;
        
            return sameCell(ei, getInfo(me)) ? 
                (float)Math.ceil(ei.params.M) : 0f;
        }
    }


    abstract class UserTask<T> implements  PrivilegedAction<T>,
					   Comparable<UserTask> {	
	long started;
	Entity e;
	UserTask(Entity e) {
	    this.e = e;
	}

	abstract protected T callUser();
	
	public T run() {
	    started = System.currentTimeMillis();
            return callUser();
        }
	
	public int compareTo(UserTask other) {
	    long thisVal = this.started;
	    long otherVal = other.started;
	    return (thisVal < otherVal ? -1 : (thisVal == otherVal ? 0 : 1));
	}
    }

    class  TurnTask extends UserTask<Event> {
	TurnTask(Entity e) {
	    super(e);
	}
	// this code is called from user context
	protected Event callUser() {
	    return e.makeTurn(iface);
	}
		
    }        

    // we use here Callable, not Runnable, to avoid name clash with PrivilegedAction's
    // run(), otherwise Runnable would be enough
    // as side effect - we may know when event was processed
    class  EventHandleTask extends UserTask<Object> implements Callable<Object> {
	Event ev;
	EventHandleTask(Entity dest, Event ev) {
	    super(dest);
	    this.ev = ev;
	}
	// this code is called from user context
	protected Object callUser() {
	    e.processEvent(iface, ev);
	    return null;
	}

	// this code is called from worker thread in engine context
	public Object call() {
	    proc.addCurrent(this);
	    try {
		invokeInSecureContext(this);
	    } catch (Throwable t) {
                if (t instanceof ThreadDeath) {
                    Util.log("worker interrputed (event)");   
                } else {
                    t.printStackTrace(Util.getSystemLogger());
                }
                recordException(this.e, t);
                addZombie(e);
            } finally {
		proc.removeCurrent(this);
	    }
	    return null;
	}
    }
    

    class Processor implements Walker<Entity> {
        class BornRequest {
            float       energy;
            BeingParams bp;
            EntityInfo  parent;
            Class       klazz;
            Location    location;
            BornRequest(EntityInfo parent, Location location, 
                        BeingParams bp, float energy) {
                this.energy = energy;
                this.bp = bp;
                this.parent = parent;
                this.location = location;
            }
        }

        class ItemRequest {
            Location location;
            EntityData ed;
            EntityInfo owner;
            ItemRequest(Location location, EntityData ed, EntityInfo owner) {
                this.location = location;
                this.ed = ed;
                this.owner = owner;
            }
        }

	// somewhat hard to read code, but idea is to feed tasks to
	// multiple threads. idx just works to reference to next
	// to be executed task
        class EntityFeeder implements Feeder<Entity>, Runnable {
            private int idx, size;
            private Walker<Entity> walker;
            private boolean suspended;

            EntityFeeder(Walker<Entity> walker) {
                this.walker = walker;
            }

            void reset() {
                idx = 0; 
                size = entities.size();
                suspended = false;
            }
            
            void suspend() {
                suspended = true;
            }

            public synchronized Entity next() {
		if (suspended || seventhSeal) {
		    return null;
		}
                assert size == entities.size();
                if (idx < size) {
                    return entities.get(idx++);
                }
                return null;
            }
            
            public void run() {                
                Entity e;
                while ((e = next()) != null) {
                    walker.walk(e);
                }
            }
        }        

        private List<BornRequest> babies = new LinkedList<BornRequest>();
        private List<ItemRequest> items  = new LinkedList<ItemRequest>();
        
        private EntityFeeder turnFeeder = new EntityFeeder(Processor.this);
        private HashMap<UserTask,Thread> current = new HashMap<UserTask,Thread>();
      
        void addBaby(EntityInfo ei, Location where, 
                     BeingParams newbp, float babyE) {
            synchronized (babies) {
                babies.add(new BornRequest(ei, where, newbp, babyE));
            }
        }

        void addNewThing(Location where, EntityData ed, EntityInfo owner) {
            synchronized (items) {
                items.add(new ItemRequest(where, ed, owner));
            }
        }
      

        void proceedTurn() {           
            // delegate to TurnService
            turnFeeder.reset();
            // schedule calls to makeTurn() of all creatures
            // by invoking Processor.walk() from several worker threads
            if (turnMaker != null) {
                turnMaker.executeMulti(turnFeeder);
            }
        }

        void proceedEvent(Entity dest, Event e) {
            if (turnMaker != null) {
                // we use TurnService to request execute of user code
                turnMaker.executeSingle(new EventHandleTask(dest, e));
            }
        }

        private void addCurrent(UserTask wt) {
            synchronized (current) {
                current.put(wt, Thread.currentThread());
            }
        }

        private void removeCurrent(UserTask wt) {
            synchronized (current) {
                current.remove(wt);
            }
        }

        @SuppressWarnings("deprecation") void killCurrent(boolean all) {
            synchronized (current) {
		long now = System.currentTimeMillis();
		// removal from hashtable while iterating not exactly a good idea
		Vector<UserTask> zut = new Vector<UserTask>();
                for (UserTask wt : current.keySet()) {
		    if (all || now - wt.started > K_turnduration) {
			Thread t = current.get(wt);
			assert t != Thread.currentThread();
			t.stop();
			// it will be added to zombies by interrupted thread 
			// but do it here just in case
			addZombie(wt.e);
			zut.add(wt);
		    }
                }
		
		for (UserTask z : zut) {
		    removeCurrent(z);
		}
            }
        }
       
        void killAllRunning() {
            turnFeeder.suspend();
            killCurrent(true);
        }

        int timeTillKilled(Entity e) {
            synchronized (current) {
                for (UserTask wt : current.keySet()) {
                    if (wt.e == e) {
                        int left = (int)(K_turnduration - 
                                         (System.currentTimeMillis() - wt.started));
                        return left > 0 ? left : 0;
                    }
                }
            }
            
            return -1;
        }       

        // be careful - this routine is invoked by multiple threads
        public void walk(final Entity e) {            
            EntityInfo ei = getInfo(e);
            if (ei == null) return;

            // let it make turn and send event if it made one and able to
            Event ev;
	    TurnTask tt = new TurnTask(e);
            addCurrent(tt);
            try {        
                ev = invokeInSecureContext(tt);
            } catch (Throwable t) {                
                if (t instanceof ThreadDeath) {
                    Util.log("worker interrputed (turn)");
                } else {
                    t.printStackTrace(Util.getSystemLogger());
                }
                recordException(e, t);
                addZombie(e);
                return;
            } finally {
                removeCurrent(tt);
            }
            // now schedule processed event to be executed when everyone
            // finishes his turn
            ei.setPendingEvent(ev);
        }


        void handleEvent(Entity dest, Event e) {
	    EventHandleTask eht = new EventHandleTask(dest, e);
            addCurrent(eht);
	    try {
		invokeInSecureContext(eht);
	    } catch (Throwable t) {
                if (t instanceof ThreadDeath) {
                    Util.log("worker interrputed (event)");   
                } else {
                    t.printStackTrace(Util.getSystemLogger());
                }
                addZombie(dest);
            } finally {
		removeCurrent(eht);
	    }
        }

        private void updateEntity(EntityInfo ei, Event ev) {
            if (!ei.isBeing) {
                return;
            }
            float dE = eventCost(ev, ei);
            if (dE >= 0f) {
                // if creature can do and got enough energy to do what it planned - let it do it
                if (ev != null && dE < ei.energy()) {
                    if (getDebug()) {
                        debug(ei.entity, ev, true);
                    }
                    sendEvent(ev, ei.id);
                }
            } else {
                dE = 0f;
            }
            
            // now add living cost and update energy level
            dE += ei.eMass * K_masscost;                        
            
            ei.energyDelta(-dE);
        }
        
        Walker<Entity> pendingProcessor = new Walker<Entity>() {
            public void walk(Entity e) {
                EntityInfo ei = getInfo(e);
                if (ei != null) {
                    ei.processPendingEvent();
                }
            }
        };                                                             

        final void postiterate() {
            // commit all pending turns
            forAllEntities(pendingProcessor);

            // and then drain event queue, delivering all messages 
            // to their targets
            drainQueue();

            // at this point no other mutators of zombies, but just in case
            synchronized (zombies) {
                for (Entity e : zombies) {
                    removeEntity(e, true);
                }
                zombies.clear();
            }
            
            // now give birth to everyone
            synchronized (babies) {
                for (BornRequest br : babies) {
                    addBeing(br.parent, br.bp, br.energy, br.location);
                }
                babies.clear();
            }
            
            synchronized (items) {
                for (ItemRequest ir: items) {
                    addEntity(ir.ed, ir.location, ir.owner);
                }
                items.clear();
            }
            // and let them cry for the first time
            drainQueue();
        }      

        private float eventCost(Event ev, EntityInfo ei) {
            if (ev == null) {
                return 0f;
            }
            // handle all known cases, ignore others
            switch (ev.kind()) {
            case ACTION_MOVE_TO:
                {
                    Object o = ev.param();
                    if ((o == null ) || !(o instanceof Location)) {
                        return -1f;
                    }
                    return maybeMove(ei, (Location)o);
                }
                
            case ACTION_BORN:
                {
                    Object o = ev.param();
                    if ((o == null ) || !(o instanceof BeingParams)) {
                        return -1f;
                    }
                    BeingParams newbp = ((BeingParams)o).clone();
                    return maybeBorn(ei, ei.params, newbp);
                }
                
            case ACTION_PRODUCE:
                {
                    Object o = ev.param();
                    if ((o == null ) || !(o instanceof String)) {
                        return -1f;
                    }
                    return maybeProduce(ei, (String)o);
                }

            case ACTION_ATTACK:
                {
                    Object o = ev.param();
                    if ((gameInfo.getKind() == GameKind.PEACE_DUEL)  || 
                        (o == null ) || !(o instanceof Integer)) {
                        return -1f;
                    }
                    return maybeAttack(ei, (Integer)o);
                }
                
            case ACTION_MOVE_ATTACK:
                {
                    Object o = ev.param();
                    if ((gameInfo.getKind() == GameKind.PEACE_DUEL) ||
                        (o == null ) || !(o instanceof Integer)) {
                        return -1f;
                    }
                    return maybeMoveAttack(ei, (Integer)o);
                }
                
            case ACTION_EAT: 
		{
		    Object o = ev.param();
                    if ((o == null ) || !(o instanceof Float)) {
                        return -1f;
                    }
                    return maybeEat(ei, (Float)o);
		}
		
            case ACTION_GIVE:
		{
		    Integer target = ev.target();
		    Object o = ev.param();
                    if ((o == null ) || !(o instanceof Float)) {
                        return -1f;
                    }
                    return maybeGive(ei, target, (Float)o);
		}

		/*
                  case ACTION_MARK: 
                  {
                  Object o = ev.param();
                  if (o == null) {
                  return -1f;
                  }
                  return maybeMark(ei, o);
                  } */

            default:
                {
                    return -1f;
                }                
            }
        }                          
    }

    synchronized void logDo(Entity b, String s) {
        if (gameInfo.out != null) {
            EntityInfo ei = b != null ? getInfo(b) : null;
            gameInfo.out.println((ei != null ? ei.player + "[" +ei.id+"]: " : "" )+s);
        }
    }

    private synchronized void debug(Entity e, Event ev, boolean out) {
        logDo(e, (out ? ">>>" : "<<<")+ev);
    }

    // synchronized to avoid potential race on consumption
    private synchronized float consumeEnergy(EntityInfo ei, float count) {
        assert ei != null && ei.isBeing;

        // be sane
        if (count <= 0f) return 0f;
       
        // limit single bite
        if (count > ei.params.M * K_bite) {
            count = ei.params.M * K_bite;
        }
        
        float can = getResourceControl().consume(ei.location(), count);
        float did = ei.energyDelta(can);
        assert did <= can;        
        // give back something we cannot in fact eat up
        getResourceControl().consume(ei.location(), did-can);

        return did;
    }        
    
    private float giveEnergyTo(final EntityInfo ei, final Integer other, 
                               float count) {
        assert ei != null && ei.isBeing;
        // trying to cheat - attack by giving
        if (count <= 0f) {
            return 0f;
        }
        
        if (count > ei.energy()) {
            count = ei.energy();
        }
        EntityInfo acceptor = getInfoById(other);
        // if trying to give to non-being, 
        //    or someone we're not on the same cell
        //    or to other kind
        if (acceptor == null || 
            !acceptor.isBeing || 
            !sameCell(acceptor, ei) ||
            ei.playerHandle != acceptor.playerHandle) {
            return 0f;
        }
        // cannot give to already dead ones
        if (isZombie(acceptor.entity)) {
            return 0f;
        }
        
        float f = acceptor.energyDelta(count);
        ei.energyDelta(-f);
        
        sendEvent(new Event(EventKind.BEING_ENERGY_GIVEN, other, new Float(f)),
                  ei.id);
        
        return count;
    }

    private float totalMass(Location l) {
        PointInfo pi = iface.getPointInfo(l);
        return pi != null ? pi.totalMass(null) : 0f;
    }


    private boolean sameCell(EntityInfo one, EntityInfo other) {
        return one != null && other != null && 
            one.location().equals(other.location());
    }
    
    public boolean closeEnough(Entity one, Location loc) {
        if (one == null || loc == null) return false;
        Location bloc = iface.getLocation(one);
        return (getTopo().distance(bloc, loc) <= 1f);
    }
    
    private EntityInfo getInfo(Entity e) {
        return info.get(e);        
    }

    private void removeInfo(Entity e) {
        info.remove(e);
    }
    
    private EntityInfo getInfoById(Integer id) {        
        return  mapById.get(id);
    }

    private void drainQueue() {
        Event e;
        while ((e = eq.getEvent()) != null) {
            processEvent(e);
        }
        // wait for completion of event processing
        waitCompletion();
    }
        
    private Processor proc;    
    
    public void bigbang() {
        new Thread(this).start();
        numTurns = 0;        
    }
    
    private void waitCompletion() {
        if (turnMaker != null) {
            turnMaker.waitCompletion();
        }
    }

    public void run() {
        started = true;
        setStatus(STATUS_RUNNING);
        recordState();        
        controller.redraw();

        // have to be after state recording, so that clients can see state 
        // before start
        Util.sleep(gameInfo.waitBefore);

        while (true)  {
            // needed, to allow INIT events to be dispatched, before event processing
            drainQueue();            

            // let beings perform their actions
            if (!paused()) {
                synchronized (entities) {
                    proc.proceedTurn();
                }
                // now wait for completion, not under lock
                
                waitCompletion();

                // commit all turns into reality
                proc.postiterate();                                
                
                numTurns++;
            }
                       
            // update resource counters
            getResourceControl().makeTurn();
            
            // compute player's score
            updateScores();
            
            // update marks
            updateMarks();

            if (turnEnd()) {
                break;
            }
        }

        setStatus(STATUS_FINISHED);
        
        // make an official apocalypse end
        seventhSeal = false;
    }    
    
    class ScoreCounter implements Walker<Entity> {
        public void walk(Entity e) {
            if (!(e instanceof Being)) return; // who cares about stones
            Being b = (Being)e;
            EntityInfo ei = getInfo(b);           
            if (ei != null) {
                String player = ei.player;                
                ScoreData sd = scores.get(player);
                assert sd != null;
                sd.score += ei.params.M;
                sd.energy += ei.energy();
                sd.alive = true;
            }
        }
    }
    ScoreCounter scoreCounter = new ScoreCounter();
    List<ScoreData> currentResults = new ArrayList<ScoreData>();
    private void updateScores() {
        synchronized (scores) {
            // preclean
            for (ScoreData sd : scores.values()) {
                sd.score = 0; sd.energy = 0; sd.alive = false;
            }
            forAllEntities(scoreCounter);

            currentResults.clear();
            currentResults.addAll(scores.values());
            Collections.sort(currentResults);
        }
    }
   
    private void updateMarks() {
        /*
          for (Iterator<PointInfo> i = marks.keySet().iterator(); i.hasNext();) {
          PointInfo pi = i.next();
          Integer ttl = marks.get(pi);
          if (ttl.intValue() < 1) {
          pi.putMark(null);
          i.remove();
          } else {
          marks.put(pi, ttl-1);
          }
          }
        */
    }

    /****************** UI *********************/
    public boolean started() {
        return this.started;
    }
    
    public boolean paused() {
        return this.paused;
    }
    
    public void setPaused(boolean paused) {
        synchronized (pauser) {            
            this.paused = paused;
            pauser.notifyAll();
        }
    }
   
    /******************************************/
    
    public synchronized void registerListener(EventListener listener, EventFilter filter) {
        synchronized (listeners) {
            List<EventListener> list = listeners.get(filter);
            if (list == null) {
                list = new ArrayList<EventListener>();
                listeners.put(filter, list);
            }
            // we don't use sets to keep ordering of listeners (XXX?)
            if (!list.contains(listener)) {
                list.add(listener);
            }
        }
    }
    
    public synchronized void removeListener(EventListener listener, EventFilter filter) {
        synchronized (listeners) {
            List<EventListener> list = listeners.get(filter);
            if (list == null) {
                return;
            }
            list.remove(listener);
        }
    }
    
    public void sendEvent(Event e, Integer source) {
        if (e != null) {
            if (source != null) {
                ((BasicEvent)e).setSender(source);
            }
            eq.putEvent(e);
        }
    }
    
    private void killemall() {
        synchronized(entities) {
            entities.clear();
        }
    }
    
    private void removeEntity(Entity e, boolean natural) {
        synchronized(entities) {
            EntityInfo ei = getInfo(e);
            // already removed by other mean
            if (ei == null) {
                return;
            }
            // put dead corpse on the ground
            if (natural && ei.isBeing && ei.energy() > 0f) {
                ResourceControl rc = getResourceControl();
                rc.addSource(ei.location, ei.energy(), 0f, -1f);
            }
            entities.remove(e);
            // we use entities object as lock for operations
            // on global lists
            removeInfo(e);
        
            // remove it from the map
            PointInfo pi = iface.getPointInfo(ei.location);
            pi.removeEntity(ei.id);
    
            mapById.remove(ei.id);
            
            removeListener(ei, ei);
        }
    }     

    public void stop() {
        seventhSeal = true;
        synchronized (pauser) {            
            pauser.notifyAll();
        }
    }
    
    public void apocalypse() {
	seventhSeal = true;
        if (broadcaster != null) {
            broadcaster.stop();
        }
	if (recorder != null && recording) {
	    recorder.finishRecording();
	}
	turnMaker.stop();	
        //killemall();
        controller.redraw();        
        started = false;

        turnMaker = null;
        Runtime.getRuntime().runFinalization();
    }
    
    // of EventListener
    public void eventFired(Event e) {
        // we ignore messages from beings
        // we know about all events, but doing nothing for now
    }
        
    // of EventFilter
    public boolean match(BasicEvent e) {
        // for now we'll look at all events
        return true;
    }

    private float maybeAttack(EntityInfo ei, Integer other) {
        assert ei != null && ei.isBeing;
        
        EntityInfo oi = getInfoById(other);
        if (!sameCell(oi, ei)) {
            return 0f;
        }
        
        // shouldn't attack dead ones
        if (isZombie(oi.entity)) {
            return 0f;
        }
        

        float cost = ei.params.M * K_fightcost;
        if (cost > ei.energy) {
            return -1f;
        }

        float dE = oi.energyDelta(-ei.params.M * K_fight);       
        sendEvent(new Event(EventKind.BEING_ATTACKED, other, new Float(-dE)), ei.id);
        

        return cost + K_retaliate * (oi.params != null ? oi.params.M : 0);
    }

    private float maybeMove(EntityInfo ei, Location newLoc) {
        float distance = getTopo().distance(ei.location, newLoc);
        float speed = ei.params.S;
        if (distance > speed) {
            Util.log("speed is not enough: "+speed+"<"+distance);
            return -1f;
        }
        if (K_maxmassperpoint > 0f) {
            float m = totalMass(newLoc);
            if (m + ei.params.M > K_maxmassperpoint) {
                Util.log("cannot move in: too crowded");
                return -1f;
            }
        }
        ei.setLocation(newLoc);
        return speed * K_movecost;
    }
    
    private float maybeMoveAttack(EntityInfo ei, Integer other) {
        assert ei != null && ei.isBeing;
        
        EntityInfo oi =  getInfoById(other);
        
        if (oi == null) return -1f;     
        
        // if out of reach
        if (getTopo().distance(oi.location(), ei.location()) > ei.params.S) {
            return -1f;
        }
                        
               
        float cost = ei.params.M * K_fightcost + ei.params.S*K_movecost;
        if (cost > ei.energy) {
            return -1f;
        }

        Location newLoc = oi.location();
        float m = totalMass(newLoc);
        if (K_maxmassperpoint < 0f || m + ei.params.M <= K_maxmassperpoint) {
            ei.setLocation(newLoc);
        }
        
        float dE = oi.energyDelta(-ei.params.M * K_fight * K_fightmovepenalty);                        
        sendEvent(new Event(EventKind.BEING_ATTACKED, other, new Float(-dE)), ei.id);
        
        return cost + K_retaliate * (oi.params != null ? oi.params.M : 0);
    }
    

    private float maybeEat(EntityInfo ei, Float amount) {
	consumeEnergy(ei, amount);
	// eating has no cost
	return 0f;
    }

    private float maybeGive(EntityInfo ei, Integer who, Float amount) {
	giveEnergyTo(ei, who, amount);
	// giving has no cost
	return 0f;
    }
    
    private static boolean checkBorn(float o, float n) {
        return 
            (n >= K_minbornvariation * o) && (n <= K_maxbornvariation * o);
    }  

    private float maybeBorn(EntityInfo ei, BeingParams mybp, 
                            BeingParams newbp) {
        // if requested params are too different
        if (!checkBorn(mybp.M, newbp.M) || !checkBorn(mybp.S, newbp.S)) {
            return -1f;
        }
        float energy = ei.energy();
        
        if (energy < K_toborn * mybp.M) {
            return -1f;
        }
        
        Location where = ei.location();
        if (K_maxmassperpoint > 0f) {                            
            if (totalMass(where) + newbp.M > K_maxmassperpoint) {
                return -1f;
            }
        }
        
        float babyE = energy/2f; // baby gets half of parent's energy

        proc.addBaby(ei, where, newbp, babyE);
          
        return babyE + mybp.M * K_borncost;
    }

    private float maybeProduce(EntityInfo ei, String what) {
        EntityData ed = shop.get(what); 
        if (ed == null) {
            return -1f;
        }
        float cost = ed.cost();
        if (cost > ei.energy) {
            return -1f;
        }        
        
        Location where = ei.location();
        proc.addNewThing(where, ed, ei);

        return cost;
    }

    /*
      private float maybeMark(EntityInfo ei, Object mark) {
      PointInfo pi = iface.getPointInfo(ei.location());
      if (pi.getMark() != null) {
      return -1f;
      }
      pi.putMark(mark);
      marks.put(pi, new Integer(K_markttl));
      return K_markcost;
      }*/
    
    public void forAllEntities(Walker<Entity> walker) {
        synchronized (entities) {
            Util.walkList(entities, walker);
        }
    }
    
    public void processEvent(Event e) {
        List<EventListener> matched = new ArrayList<EventListener>(1);
        synchronized (listeners) {
            for (EventFilter f : listeners.keySet()) {
                if (f.match(e)) {
                    matched.addAll(listeners.get(f));
                }
            }
        }
        
        // whole trick it to avoid event processing under lock
        // thus temporary array        
        try {
            for (EventListener l : matched) {
                l.eventFired(e);
            }
            // if someone throws an exception - all other listeners
            // will miss the notification
        } catch (Exception iwe) {
            iwe.printStackTrace(Util.getSystemLogger());
        }
    }
    
    private int numTurns = 0;
    private int maxTurns = 0;    
    private boolean recording = false;   

    void recordState() {
        if (broadcaster != null && !broadcaster.stopped) {
            broadcaster.makeSnapshot(recording);
            if (recording) {
                recorder.record(numTurns,
                                broadcaster.getSnappedDataLen(),
                                broadcaster.getSnappedData());
            }
        }
    }

    static final boolean consistencyCheck = false;
    boolean turnEnd() {        
        if (consistencyCheck) {
            checkConsistency();
        }
        
        if (singleStep) {            
            controller.notifyOnTurnEnd();
        }
        if (paused()) {
            try {
                synchronized (pauser) {
                    pauser.wait();
                    setStatus(paused ? STATUS_PAUSED : STATUS_RUNNING);
                    recordState();
                }
            } catch (InterruptedException e) {
            }
        } else {
            if (numTurns % Constants.getRefreshTurns() == 0) {
                controller.redraw();
            }
            recordState();
        }
        
        if (getTurnDelay() > 0) {
            Util.sleep(getTurnDelay());
        }

        if (gameOver()) {
            GameResult result = new GameResult(scores(), numTurns, 
                                               gameInfo.getKind());
            if (broadcaster != null) {
                broadcaster.setResult(result);
            }
            
            setPaused(true);
            setStatus(STATUS_COMPLETED);
            
            recordState();
            if (recording && recorder != null) {
                recorder.finishRecording();
                recorder = null;
            }

            if (gameInfo.sqlResultFilePath != null) {
                try {
                    result.storeAsSql(gameInfo.sqlResultFilePath, gameInfo);
                } catch (IOException ioe) {
                    ioe.printStackTrace(Util.getSystemLogger());
                }
            }

            controller.redraw();
            controller.notifyAboutCompletion(result);
            return true;
        }
        return false;
    }

    boolean gameOver() {
        if (numTurns >= maxTurns || !started || seventhSeal) {
            return true;
        }

        if (paused) {
            return false;
        }

        int numAlive = 0;
        synchronized (scores) {
            for (ScoreData sd : scores.values()) {
                if (sd.alive) {
                    numAlive++;
                }
            }
        }

        if (gameInfo.getKind() == GameKind.SINGLE) {
            return numAlive == 0;
        }

        if (gameInfo.getKind() == GameKind.DEBUG) {
            return numAlive == 0;
        }
        
        return numAlive < 2;
    }
    
    class ConsistencyChecker implements Walker<Entity> {
        public void walk(Entity e) {
            Location loc = iface.getLocation(e);
            PointInfo pi = iface.getPointInfo(loc);
            EntityInfo ei = getInfo(e);
            
            Integer entities[] = new Integer[1];
            entities = pi.getEntities(e,  entities);
            
            boolean found = false;
            for (Integer o : entities) {
                if (o.intValue() == ei.id.intValue()) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                throw new RuntimeException("Inconsistent: "+e+" in "+loc+
                                           " not in pi");
            } 
        }
    }
    ConsistencyChecker checker;
    void checkConsistency() {
        if (checker == null) {
            checker = new ConsistencyChecker();
        }
        forAllEntities(checker);
    }
       
    static class JailedInfo {
        String player, asString, name;
        BeingParams bp;
        JailedInfo(BeingParams bp) {
            this.bp = bp;
        }
    }

    private Being addBeing(EntityInfo parent,
                           BeingParams bp, float energy, 
                           Location loc) {
        return addBeing(parent.entity.getClass(),
                        parent.player,
                        parent.asString,
                        parent.name,
                        parent.jarFile,
                        bp, energy, loc);
                        
    }

    private Being addBeing(final Class klazz,
                           final String owner,
                           final String asString,
                           final String name,
                           final String jarFile,
                           BeingParams bp, float energy, 
                           Location loc) {
        final JailedInfo ji = new JailedInfo(bp);
        
        Being b = invokeInSecureContext(new PrivilegedAction<Being>() {
                                            public Being run() {
                                                Being b;
                                                try {                    
                                                    b = (Being)klazz.newInstance();

                                                    // not yet known - ask creature
                                                    if (owner == null) {
                                                        UserGameInfo ugi = new UserGameInfo();
                                                        ugi.kind = gameInfo.getKind();
                                                        ugi.maxTurns = maxTurns;
                                                        // call reinit before everything else
                                                        b.reinit(ugi);
                                                        ji.player = b.getOwnerName();
                                                        ji.asString = b.toString();
                                                        ji.name = b.getName();
                                                    } else {
                                                        ji.player = owner;
                                                        ji.asString = asString;
                                                        ji.name = name;
                                                    }

                                                    if (ji.bp == null) {
                                                        ji.bp = b.getParams().clone();
                                                    }

                                                    return b;
                                                } catch (Throwable t) {
                                                    t.printStackTrace(Util.getSystemLogger());
                                                    return null;
                                                }
                                            }});

        if (b == null) {
            return null;
        }       
        
        if (!checkParams(ji.bp)) {
            Util.log("Bad params of the being, not adding");
            return null;
        }                
        
        if (owner == null) {
            synchronized (scores) {
                ScoreData sd = scores.get(ji.player); 
                int i = 1;
                String player = ji.player;
                while (sd != null) {
                    ji.player = player+"-"+(i++);
                    sd = scores.get(ji.player); 
                }
                scores.put(ji.player, new ScoreData(ji.player, jarFile));
            }
        }

        // create EntityInfo, it'll add itself into global hash automatically
        new EntityInfo(b, ji.bp, ji.player, ji.asString, 
                       ji.name, loc, energy, jarFile);

        return b;
    }

    private Entity addEntity(EntityData ed, Location where, EntityInfo ei) {     
        Entity e = ed.produce(ei.playerHandle);
        if (e != null) {
            new EntityInfo(e, where);
        }
        return e;
    }
    
    @SuppressWarnings("unchecked") 
        public Being addBeing(final String klazzName, final String jarFile,
                              final Location where) {
        final BeingLoader loader = new BeingLoader(this);
        Class klazz = 
            invokeInSecureContext(
                                  new PrivilegedAction<Class>() {
                                      public Class run() {
                                          try {
                                              return loader.loadBeing(klazzName, jarFile);
                                          } catch (Throwable t) {
                                              t.printStackTrace(Util.getSystemLogger());
                                          }
                                          return null;
                                      }
                                  });
        


        if (klazz == null) {
            return null;
        }
        
        // now check for potentially dangerous overrides
        try {
            Method hc = klazz.getMethod("hashCode");
            if (!java.lang.Object.class.getMethod("hashCode").equals(hc)) {
                throw new IllegalArgumentException("It's disallowed to override hashCode()!");
            }
            Method eq = klazz.getMethod("equals", Object.class);
            if (!java.lang.Object.class.getMethod("equals", Object.class).equals(eq)) {
                throw new IllegalArgumentException("It's disallowed to override equals()!");
            }
            Method gc = klazz.getMethod("getClass");
            if (!java.lang.Object.class.getMethod("getClass").equals(gc)) {
                throw new IllegalArgumentException("It's disallowed to override getClass()!");
            }
            
        } catch (Throwable e) {
            e.printStackTrace(Util.getSystemLogger());
            return null;
        }
        Being b = addBeing(klazz, 
                           null /* owner */, 
                           null /* asString */, 
                           null /* name */,
                           jarFile /* jarFile */,
                           null /* params */, 
                           -1f /* energy */, 
                           where /* loc */);        

        // give bonus
        if (b != null) {
            rc.addSource(iface.getLocation(b), 
                         Constants.BORN_BONUS, 
                         Constants.BORN_BONUS_GROWTH,
                         Constants.BORN_BONUS);
        }

        // to dispatch pending events
        drainQueue();

        updateScores();

        return b;
    }


    public Being[] addBeings(String[][] beings) {
        int len = beings.length;
        Being rv[] = new Being[len];
        Location locs[] = new Location[len];
        // we do it in two steps to allow use of unperturbed by user code
        // random number generator
        for (int i = 0; i<len; i++) {
            locs[i] = getTopo().getRandomLocation();
        }
        for (int i = 0; i<len; i++) {
            rv[i] = addBeing(beings[i][0], beings[i][1], locs[i]);
        }
        return rv;
    }


    public String getOwnerOf(Being b) {
        EntityInfo ei = getInfo(b);
        if (ei == null) {
            return null;
        }
        return ei.player;
    }
    
    private boolean checkParams(BeingParams p) {
        if (p.M < K_minmass || p.M > K_maxmass) {
            return false;
            
        }
        
        if (p.S < K_minspeed || p.S > K_maxspeed) {
            return false;
        }
        
        return true;
    }
    
    public Topology getTopo() {
        return topo;
    }
       
    public ResourceControl getResourceControl() {
        return rc;
    }
    
    public GameOwner getController() {
        return controller;
    }

    private int eid;    
    private HashMap<String, Object> handles =  new HashMap<String, Object>();
    private Object getPlayerHandle(String name) {
        synchronized (handles) {
            Object rv = handles.get(name);
            if (rv == null) {
                rv = new Object();
                handles.put(name, rv);
            }
            return rv;
        }
    }

    class EntityInfo implements EventListener, EventFilter {
        final Entity      entity;
        final String      player;
        final String      asString;
        final String      name;
        private volatile Location  location;
        private final String jarFile;
        final Integer     id;
        // in fact most entities expected to be beings, if not -
        // split out to separate class
        final boolean     isBeing;
        // params
        final BeingParams params;
        private float     energy;
        private float     eMass;
        final Object      playerHandle;    
        private Event     pendingEvent;
        private final int typeId;

        EntityInfo(Being b, BeingParams bp,
                   String player, String asString, String name,
                   Location loc, float energy, String jarFile) {
            this.entity = b;
            this.isBeing = true;
            this.player = player;
            this.asString = asString;
            this.name = name;
            // it's OK to use rnd(), as for good random generator 
            // (like in Java) it not gonna loop for very long time
            this.jarFile = jarFile;
            this.params  = bp.clone();            
            this.energy = energy < 0f ? params.M : energy;
            this.eMass = (float)Math.pow(params.M, 0.5 );
            this.playerHandle = getPlayerHandle(player);
            this.id = getDebug() ? new Integer(++eid) : Util.rnd();            
            this.typeId = scores.get(player).id;

            init(loc);
        }
        
        
        EntityInfo(Entity entity, Location loc) {
            this.entity = entity;
            this.isBeing = false;
            this.player = null;
            this.asString = null;
            this.name = null;
            this.jarFile = null;
            this.params = null;
            this.playerHandle = null;
            this.id = getDebug() ? new Integer(++eid) : Util.rnd();
            this.typeId = -2; // XXX: must be different for various entities

            init(loc);
        }

        private void init(Location loc) {
            registerType(typeId, jarFile);
            registerListener(this, this);            
            setLocation(loc != null ? loc : getTopo().getRandomLocation());
            // being starts fully loaded, unless otherwise specified
            info.put(entity, this);
            mapById.put(id, this);

            synchronized (entities) {
                entities.add(entity);
            }
            
            // must be the last call, when everythings else is already inited
            // note that we pass clone, not params, for better isolation
            sendEvent(new Event(EventKind.BEING_BORN, id, 
                                params != null ? params.clone() : null), null);
        }               

        synchronized void setLocation(Location newLoc) {
            Location oldLoc = location();
            // ignore subspace jumps
            if (!getTopo().contains(newLoc)) return;

            if (location != null) {
                PointInfo piOld = iface.getPointInfo(oldLoc);
                piOld.removeEntity(id);
            }
            PointInfo piNew = iface.getPointInfo(newLoc);
            piNew.addEntity(id);
            this.location = newLoc;
        }
        
        // in theory this one should be synchronized, but public interfaces in
        // PointInfo use this method, and making it synchronized will
        // create a deadlock like:
        /*
          T1:
          at universum.engine.Universe$EntityInfo.location(Universe.java:1556)
          - waiting to lock <0xa9b48180> (a universum.engine.Universe$EntityInfo)         at universum.engine.Universe$BeingInterfaceImpl.getLocation(Universe.java:263)
          at universum.engine.Universe.checkDistance(Universe.java:811)
          at universum.bi.PointInfo.getEntities(PointInfo.java:53)
          - locked <0xa9bb7f10> (a universum.bi.PointInfo)
          T2:
          at universum.bi.PointInfo.removeEntity(PointInfo.java:86)
          - waiting to lock <0xa9bb7f10> (a universum.bi.PointInfo)
          at universum.engine.Universe$EntityInfo.setLocation(Universe.java:1548)
          - locked <0xa9b48180> (a universum.engine.Universe$EntityInfo)
          at universum.engine.Universe.maybeMove(Universe.java:1103)
        */

        Location location() { 
            return this.location; 
        }
        
        synchronized void setPendingEvent(Event e) {
            assert pendingEvent == null;
            this.pendingEvent = e;
        }

        synchronized void clearPendingEvent() {
            this.pendingEvent = null;
        }


        synchronized void processPendingEvent() {
            try {
                proc.updateEntity(this, pendingEvent);
            } catch (Throwable t) {
                t.printStackTrace(Util.getSystemLogger());
            }
            this.pendingEvent = null;
        }

        synchronized float energyDelta(float delta) {
            if (energy <= 0f) return 0f;

            float newEnergy = energy+delta;
            if (newEnergy < 0f) newEnergy = 0f;
            if (newEnergy > params.M) newEnergy = params.M;
            float rv = newEnergy - energy;


            // to handle float rounding errors
            if (delta > 0f && rv > delta) {
                rv = delta;
            }            

            energy = newEnergy;

            if (isBeing && newEnergy <=  K_emin *params.M) {
                death(this);                
            }

            return rv;
        }       
              
        synchronized float energy() {
            return this.energy;
        }
        
        public void eventFired(Event e) {
            if (getDebug()) {
                debug(entity, e, false);
            }
            proc.proceedEvent(entity, e);          
        }
            
        public boolean match(BasicEvent e) {
            Integer target = e.target();
            return target == null ? false : id.equals(target);
        }
    }

    <T> T invokeInSecureContext(PrivilegedAction<T> what) {
        if (JungleSecurity.getCheckSecurity()) {
            return AccessController.doPrivileged(what, creatureContext);
        } else {
            return what.run();
        }
    }

    void checkHanging() {
        Util.log("Hang?");

        // kill only while not debugging
        if (!getDebug()) {
            proc.killCurrent(false);
        }
    }

    void killAllRunning() {
        proc.killAllRunning();
    }
        

    int getTurnDelay() {
        return gameInfo.turnDelay; 
    }

}

class TypeInfo {        
    TypeInfo(Integer type, String jarFile) {
        this.type = type;
        this.jarFile = jarFile;            
    }
        
    private Integer type;
    private String jarFile;
    private byte[] iconData;
    private boolean noIcon = false;
        
    synchronized byte[] getIconData() {            
        if (iconData != null) {
            return iconData;
        }
        if  (jarFile == null || noIcon) {
            return null;
        }
        try {
            JarFile jf = new JarFile(jarFile);
            ZipEntry iconEntry = jf.getEntry("icon.png");
            if (iconEntry == null) {
                noIcon = true;
                return null;
            }
            InputStream is = jf.getInputStream(iconEntry);
            int len = (int)iconEntry.getSize();
            iconData = new byte[len];
            is.read(iconData, 0, len);
            return iconData;
        } catch (Exception e) {
            noIcon = true;
            e.printStackTrace(Util.getSystemLogger());
            return null;
        }
    }
}
