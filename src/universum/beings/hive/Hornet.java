package universum.beings.hive;

import universum.bi.*;
import universum.util.Util;

import java.util.*;

public class Hornet implements Being {

    private static final String boss = "kda";
    private static Object bossHandle = null;

    private static boolean canAttack = true;
    
    private static final float n_mass = 10;
    private static final float n_speed = 6;
    
    private static final float t_mass = 5.0f;
    private static final float t_speed = 10;
    
    private static int currentCount;

    private static int maxX;
    private static int maxY;

    private static int DX;
    private static int DY;

    private static Vector<Location> unverified;
    private static HashMap<Location, Integer> occupiedCells;
    //    private static HashMap<Integer, Float> need;
    private static HashSet<Location> resourceLocations;

    private static Hashtable<Integer, Location> movingEnemies;
    private static Hashtable<Integer, Location> staticEnemies;

    
    private static BitSet list;
    private static int lowest;
    
    private static boolean exploring;
    private static int foundLocations;
    private static int verifiedLocations;
    private static boolean fullControl;
    private static boolean controlVerified;

    private static TreeSet<Source> sources;

    private static int turnNumber;

    private static Object resSync = new Object(); 
    private static Object enemySync = new Object();
  
    public void reinit(UserGameInfo info) {
    	currentCount = 0;

        maxX = -1;
        maxY = -1;

        DX = -1;
        DY = -1;
        
        unverified = new Vector<Location>();
        occupiedCells = new HashMap<Location, Integer>();
        //        need = new HashMap<Integer, Float>(); 
        resourceLocations = new HashSet<Location>();

        movingEnemies = new Hashtable<Integer, Location>();
        staticEnemies = new Hashtable<Integer, Location>();
        
        exploring = true;
        foundLocations = 0;
        verifiedLocations = 0;
        fullControl = false;

        sources = new TreeSet<Source>();

        turnNumber = 0;
        
        list = new BitSet(4000);
        lowest = -1;
        
        bossHandle = null;
    }

    private static void findTargetCells(int x, int y) {
    
    	int vx = (x - 1) / 3;
    	int vy = (y - 1) / 3;
    
    	if (vx <= maxX && vy <= maxY) {
            return;
    	}
			
    	if (vx > maxX) {
            for (int i = maxX + 1; i <= vx; i++) {
                for (int j = 0; j <= maxY; j++) {
                    unverified.add(new Location(i * 3 + 1, j * 3 + 1));
                    foundLocations++;
                }
            }
            maxX = vx;
    	}
    	
    	if (vy > maxY) {
            for (int i = 0; i <= maxX; i++) {
                for (int j = maxY + 1; j <= vy; j++) {
                    unverified.add(new Location(i * 3 + 1, j * 3 + 1));
                    foundLocations++;
                }
            }
            maxY = vy;
    	}
    }

    
    private static void checkLimits(List<Location> cells) {

    	if (DX != -1 && DY != -1) {
            return;
    	}

    	synchronized (resSync) {
            int minX = 99999999;
            int maxX = -1;
            int minY = 99999999;
            int maxY = -1;
            for (Location l : cells) {
                int x = l.getX();
                int y = l.getY();
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
			
            findTargetCells(maxX, maxY);
			
            if (minX == 0 && maxX - minX > 25) {
                DX = maxX + 1;
            }
            if (minY == 0 && maxY - minY > 25) {
                DY = maxY + 1;
            }
        }
    }
    
    private static float distance(Location one, Location other) {
        float dx = Math.abs(one.getX() - other.getX());
        if (DX != -1) {
            dx = Math.min(dx, DX - dx);
        }
        float dy = Math.abs(one.getY() - other.getY());
        if (DY != -1) {
            dy = Math.min(dy, DY - dy);
        }
        return Math.max(dx, dy) +
            (dx * dx + dy * dy) / (800 * 800);
    }

    private static Location findNearest(Location target, Iterable<Location> locations) {

    	Location result = null;
    	float dist = 9999999;
    	
    	for (Location l : locations) {
            float r = distance(target, l);
            if (r < dist) {
                dist = r;
                result = l;
            }
    	}
    	return result;
    }

    private static void checkPoint(Location where, float rate) {
        if (rate == 0) {
            return;
        }
        sources.add(new Source(where, rate));
        resourceLocations.add(where);
    }

    private static Location getNewTarget(Location where) {

    	int skipCount = Util.rnd(5);
        Location target = null;

        synchronized (resSync) {

            for (Source x : sources) {
                if (!occupiedCells.containsKey(x.l)) {
                    target = x.l;
                    if (skipCount == 0) {
                        break;
                    }
                    skipCount--;
                }
            }

            if (target != null) {
                return target;
            }

            return findNearest(where, resourceLocations);
        }
    }

    private static boolean isFullControl() {

        synchronized (resSync) {
            if (controlVerified) {
                return fullControl;
            }
            controlVerified = true;
    	
            fullControl = true;
            for (Source x : sources) {
                if (! occupiedCells.containsKey(x.l)) {
                    fullControl = false;
                    break;
                }
            }
            return fullControl;
        }
    }

    private static void changeCount(Location l, int delta) {
    	
    	if (l == null) {
            return;
    	}
    	
        synchronized (resSync) {
            Integer count = occupiedCells.get(l);
            int cnt = count == null ? 0 : count;

            cnt += delta;

            if (cnt == 0) {
                occupiedCells.remove(l);
            } else {
                occupiedCells.put(l, new Integer(cnt));
            }
        }
    }

    private void addEnemy(Location l, Integer enemy) {
    	synchronized (enemySync) {
            if (id > enemy) {
                staticEnemies.put(enemy, l);
            } else {
                movingEnemies.put(enemy, l);
            }
        }
    }
   
    private Integer getEnemy(Location l) {
    	return getEnemy(l, 0.001f);
    }

    static private Vector<Integer> theEnemies = new Vector<Integer>();
    	
    private Integer getEnemy(Location l, float distance) {

    	synchronized (enemySync) {

            if (movingEnemies.size() == 0 && staticEnemies.size() == 0) {
                return null;
            }

            Integer key;
            Enumeration<Integer> e;

            theEnemies.clear();

            if (movingEnemies.size() != 0) {
                e = movingEnemies.keys();
                while (e.hasMoreElements()) {
                    key = e.nextElement();
                    if (key < id) {
                        movingEnemies.remove(key);
                        continue;
                    }
                    if (distance(l, movingEnemies.get(key)) < distance) {
                        theEnemies.add(key);
                    }
                }
            }

            if (staticEnemies.size() != 0) {
                e = staticEnemies.keys();
                while (e.hasMoreElements()) {
                    key = e.nextElement();
                    if (distance(l, staticEnemies.get(key)) < distance) {
                        theEnemies.add(key);
                    }
                }
            }
            if (theEnemies.size() == 0) {
                return null;
            }
            return theEnemies.elementAt(Util.rnd(theEnemies.size()));
        }
    }
    
    // -----------------------------------------------------------------------------------

    
    public Hornet() {
    	neighbors = new Vector<PointInfo>(9);    	
    }

    private Integer id;

    private Location oldLocation;
    private Location targetLocation;
    private Location moveLocation;
    
    private float my_speed;
    private float my_mass;
    
    private Vector <PointInfo> neighbors;
    private Integer[] others;

    
    public Event makeTurn(BeingInterface bi) {

    	if (id == null) {
            return null;
    	}
    	
        if (id == lowest) {
            synchronized (resSync) {
                synchronized (enemySync) {
                    turnNumber++;
                    if (turnNumber % 10 == 0) {
                        System.out.println("Turn " + turnNumber);
                    }
                    movingEnemies = staticEnemies;
                    staticEnemies = new Hashtable<Integer, Location>();
                    controlVerified = false;
                    //		    		System.out.println("*** " + turnNumber + "  " + lowest);
                }
            }
        }
    	
    	Event ev;
        PointInfo pi = bi.getPointInfo(this);
        Location where = pi.getLocation();

        neighbors.clear();
    	neighbors.add(pi);
    	neighbors.addAll(bi.getNeighbourInfo(this));
        
        List<Location> reachable = bi.getReachableLocations(this);
        checkLimits(reachable);
        
    	if (! where.equals(oldLocation)) {
            changeCount(oldLocation, -1);
            changeCount(where, 1);
            oldLocation = where;
    	}

    	if (canAttack) {
            for (PointInfo x : neighbors) {
                others = x.getEntities(this, others); 
                if (others != null) {
                    for (int i = 0;
                         (i < others.length) && (others[i] != null); i++) {
                        if (bossHandle != (bi.getOwner(this, others[i]))) {
                            addEnemy(x.getLocation(), others[i]);
                        }
                        others[i] = null;
                    }                        
                }
            }
    	}
    	
    	//-----------------------------------------------------------

        //		need.remove(id);

    	if ((ev = attack(where)) != null) {
            return ev;
    	}
        
    	if ((ev = born(bi)) != null) {
            //			setNeed(bi, true, 0);
            return ev;
    	}

    	float energy = bi.getEnergy(this);

    	if (pi.getCount(this) > my_mass * 0.05 &&
            energy < my_mass * 0.9) {
            releaseTarget();
            return new Event(EventKind.ACTION_EAT, my_mass);
    	}

    	if ((ev = moveAndAttack(bi)) != null) {
            return ev;
    	}
    	
    	{
            PointInfo bestNeighbor = null;
            float resources = 0;
            for (PointInfo x : neighbors) {
                float r = x.getCount(this);
                if (r > resources) {
                    resources = r;
                    bestNeighbor = x;
                }
            }
            if (bestNeighbor != null &&
                (resources > my_mass * 0.5 ||
                 (resources > my_mass * 0.09 && energy < my_mass * 0.25))) {
                return bestNeighbor.equals(pi) ? null :
                    new Event(EventKind.ACTION_MOVE_TO,
                              bestNeighbor.getLocation());
            }
    	}
    	
        /*    	
              float avail = pi.getCount(this);
              if (avail > my_mass * 0.2) {
              // good place, wait here
              if (exploring) {
              if (targetLocation != null) {
              unverified.add(targetLocation);
              targetLocation = null;
              }

              float extra = (float) (bi.getEnergy(this) - my_mass * 0.16);

              if (extra > 0) {
					
              Integer mostNeedy = null;
              float needs = 0;
					
              others = pi.getEntities(this, others);

              for (int i = 0; i < others.length; i++) {
              if (others[i] == null) {
              break;
              }
              if (id != null
              && id.intValue() < others[i]
              && boss.equals(bi.getOwner(this, others[i]))) {

              Float want = need.get(others[i]);
              if (want == null) {
              continue;
              }

              float t = want.floatValue();
              if (t > needs) {
              needs = t;
              mostNeedy = others[i];
              continue;
              }
              }
              }

              if (mostNeedy != null) { 
              if (extra > needs) {
              extra = needs;
              need.remove(mostNeedy);
              } else {
              need.put(mostNeedy,
              new Float(needs - extra));
              }
              setNeed(bi, false, extra);
              System.out.println("Transferring " + extra + " from " + id + " to " + mostNeedy);
              return new Event(EventKind.ACTION_GIVE, mostNeedy, extra); 
              }
              }
              setNeed(bi, false, 0);
              }
              return null;
              }
        */
        while (exploring && moveLocation == null) {

            if (currentCount > 15 && energy < my_mass * 0.25) {
                releaseTarget();
                synchronized (resSync) {
                    moveLocation = findNearest(where, resourceLocations);
                }
                break;
            }

            if (targetLocation == null) {
                getTarget(where);
                if (targetLocation == null) {
                    break;
                }
            }
            if (targetLocation.equals(where)) {
                synchronized (resSync) {
                    for (PointInfo a : neighbors) {
                        checkPoint(a.getLocation(), getRate(a));
                    }
                    verifiedLocations++;
                    if (verifiedLocations >= foundLocations) {
                        exploring = false;
                    }
                }
                targetLocation = null;
            } else {
                return moveTo(targetLocation, reachable);
            }
        }	

    	if ((ev  = moveAndAttack(bi)) != null) {
            return ev;
    	}

    	if (moveLocation == null) {
            synchronized (resSync) {
                if ((! resourceLocations.contains(where)) ||
                    (energy > my_mass * 0.5 &&
                     Util.rnd(isFullControl() ? 1000 : 150) == 0)) {
                    moveLocation = getNewTarget(where);
                }
            }
        }

        if (moveLocation != null) {
            if (moveLocation.equals(where)) {
                moveLocation = null;
            } else {
                return moveTo(moveLocation, reachable);
            }
    	}
    	
        return null;
    }

    private void getTarget(Location where) {
    	synchronized (resSync) {
            targetLocation = findNearest(where, unverified);
            unverified.remove(targetLocation);
    	}
    }
    
    private void releaseTarget() {
    	if (targetLocation == null) {
            return;
    	}
    	synchronized (resSync) {
            unverified.add(targetLocation);
            targetLocation = null;
    	}
    }

    private Event moveTo(Location to, List<Location> reachable) {

    	Location l = findNearest(to, reachable);
    	Integer enemy; 
        if ((enemy  = getEnemy(l)) != null) {
            return new Event(EventKind.ACTION_MOVE_ATTACK,
                             enemy);
        }	
        return new Event(EventKind.ACTION_MOVE_TO, l);
    }
    
    private Event attack(Location l) {
    	
    	if (! canAttack) {
            return null;
    	}
    	
    	Integer enemy; 
        if ((enemy = getEnemy(l)) != null) {
            //			   System.out.println("attacking " + bi.getOwner(this, enemy_id) + " " + enemy_id);
            return new Event(EventKind.ACTION_ATTACK, enemy);   
        }
        return null;
    }

    private Event moveAndAttack(BeingInterface bi) {
    	
    	if (! canAttack ||
            currentCount < 100 ||
            bi.getEnergy(this) < my_mass * 0.2) {
            return null;
    	}
    	
        Integer enemy = getEnemy(bi.getPointInfo(this).
                                 getLocation(), my_speed); 
    	if (enemy == null) {
            return null;
    	}
        return new Event(EventKind.ACTION_MOVE_ATTACK, enemy);
    }
    
    private Event born(BeingInterface bi) {

    	float energy = bi.getEnergy(this);

    	if (exploring) {
            if (energy > my_mass * 0.8) {
                BeingParams bp = getParams();
                //                if (currentCount < 5) {
                //                	bp.M = n_mass;
                //                	bp.S = n_speed;
                //                } else {
                bp.M = (float) (my_mass * 0.81);
                if (bp.M < t_mass) {
                    bp.M = t_mass;
                }
                bp.S = (float) (my_speed * 1.19);
                if (bp.S > t_speed) {
                    bp.S = t_speed;
                }
                //                }
                bp.parameter = id;        
                return new Event(EventKind.ACTION_BORN, bp);
            }
            return null;
    	}

        PointInfo pi = bi.getPointInfo(this);

        if ((energy > my_mass * 0.99) &&
            (pi.getCount(this) > my_mass * 1.5)) {
            BeingParams bp = getParams();
            bp.M = (float) (my_mass * 0.81);
            if (bp.M < t_mass) {
                bp.M = t_mass;
            }
            bp.S = (float) (my_speed * 1.19);
            if (bp.S > t_speed) {
                bp.S = t_speed;
            }
            bp.parameter = id;
            return new Event(EventKind.ACTION_BORN, bp);
        }
        return null;
    }

    private float getRate(PointInfo a) {
    	float growth = a.getGrowthRate(this);
    	float max = a.getMaxCount(this);
    	if (growth > max) {
            growth = max;
    	}
    	return growth < t_mass * 0.001 ? 0 : growth; 
    }
    /*
      private void setNeed(BeingInterface bi, boolean isParent, float delta) {
      if (! exploring) {
      return;
      }
      float energy = bi.getEnergy(this);
      if (isParent) {
      energy /= 2;
      }
      energy -= delta;
      float want = (float) (my_mass * 0.705 - energy);
      if (want > 0) {
      need.put(id, new Float(want));
      }
      }
    */
    public void processEvent(BeingInterface bi, Event e) {
        switch (e.kind()) {
        case BEING_BORN:
            synchronized (resSync) {
                init(bi, bi.getId(this), (BeingParams)e.param());
                if (id == null) {
                    System.out.println("zzzz");
                    System.exit(1);
                }
                if (lowest == -1) {
                    lowest = id;
                    System.out.println("!!!!! " + id);
                }
                list.set(id);
                currentCount++;
            }
            //				setNeed(bi, false, 0);
            break;
        case BEING_DEAD:
            synchronized (resSync) {
                list.clear(id);
                if (id == lowest) {
                    lowest = list.nextSetBit(lowest + 1);
                    System.out.println("$$$$$ " + lowest);
                }
                currentCount--;
            }
            if (exploring) {
                releaseTarget();
            }
            changeCount(oldLocation, -1);
            break;
        }
    }
    
    public String getName() {
        return "# " + id;
    }
    
    public BeingParams getParams() {
        return new BeingParams(n_mass, n_speed);
    }
    
    public String getOwnerName() {
        return boss;
    }
    
    private void init(BeingInterface bi, Integer id, BeingParams bp) {
        this.id = id;
        my_speed = bp.S;
        my_mass = bp.M;
        if (bossHandle == null) {
            bossHandle = bi.getOwner(this, id);
        }
    }
}
