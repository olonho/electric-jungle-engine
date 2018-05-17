package universum.beings;

import universum.bi.*;
import universum.util.*;
import java.util.*;

/**
 * The first runnable and fightable but yet vulnerable creature of ND family.
 *
 * @author pan
 */
public final class ND implements Being {
    static Object bossHandle = null;
    
    private static final Integer[] INT_ARRAY = new Integer[0];

    // Global counters
    private static int uid;
    private static int turns;

    // Field info
    private static int w;
    private static int h;
    private static int radius;
    private static int cellsTotal;
    private static int cellsKnown;
    private static float field_e[][];
    private static float field_de[][];
    private static float field_max[][];
    private static int   field_t[][];

    // Direction map:
    //   UL U UR
    //   L  *  R
    //   DL D DR
    // Location map is the inverse of direction map
    private static int[] DIRECTION_MAP;
    private static int[] LOCATION_MAP;
    private static final int UL = 0;
    private static final int U  = 1;
    private static final int UR = 2;
    private static final int L  = 3;
    private static final int NA = 4;
    private static final int R  = 5;
    private static final int DL = 6;
    private static final int D  = 7;
    private static final int DR = 8;
    
    // Shared knowledge base
    private static ArrayList<ND> creatures;
    private static LinkedList<Task> tasks;

    // Personal data
    private String name;
    private Integer id;
    private float m;
    private float v;
    private float e;
    private int turn;
    private int x;
    private int y;
    private PointInfo cell;
    private List<PointInfo> adj;
    private List<Location> reachable;
    private Integer[] enemies;
    private BeingInterface bi;
    private Task task;

    public void reinit(UserGameInfo info) {
       uid = 0;
       turns = 0;
       w = 0;
       h = 0;
       cellsTotal = 0;
       cellsKnown = 0;
       DIRECTION_MAP = null;
       LOCATION_MAP = null;
       creatures = new ArrayList<ND>();
       tasks = new LinkedList<Task>();
    }
    
    public String getName() {
        return name;
    }

    public String getOwnerName() {
        return "ND";
    }
    
    public String toString() {
        return name;
    }

    public ND() {
        name = "NightDemon-" + (++uid);
        m = 50.0f;
        v = 3.0f;
        turn = turns;
    }

    public synchronized Event makeTurn(BeingInterface bi) {
        this.bi = bi;
        lookAround();
        while (task.isComplete()) {
          assignTask(Task.freeTask());
        }
        return action();
    }

    private Event action() {
        return task.action(this);
    }

    private void assignTask(Task t) {
        task = t;
        t.commit(this);
    }

    private void lookAround() {
        turns = ++turn;
        e = bi.getEnergy(this);
        cell = bi.getPointInfo(this);
        x = cell.getLocation().getX();
        y = cell.getLocation().getY();
        adj = bi.getNeighbourInfo(this);
        reachable = bi.getReachableLocations(this);
        enemies = cell.getEntities(this, INT_ARRAY);
    }

    private Event feed() {
        float avail = cell.getCount(this);
        if ((e < 0.1f * m && avail > 0.0f) ||
            (e <= (1.0f - Constants.K_bite) * m && avail >= Constants.K_bite * m)) {
          return new Event(EventKind.ACTION_EAT, m - e);
        }
        return null;
    }
    
    private boolean isFriendly(Integer creature) {
        return creature.equals(id) || bi.getOwner(this, creature) == bossHandle;
    }
    
    private boolean canAttack(Integer victim) {
        if (isFriendly(victim)) {
          return false;
        }
        // We're coward if there is not much power to endure the retaliation
        float er = e - m * Constants.K_fightcost - bi.getMass(this, victim) * Constants.K_fight;
        return er > Constants.K_emin * m;
    }

    private Event shareEnergy(Integer other) {
        return new Event(EventKind.ACTION_GIVE, other, 0.5f * e);
    }

    private Event move(int direction, boolean attackAllowed) {
        PointInfo dst = adj.get(DIRECTION_MAP[direction]);
        if (attackAllowed) {
          // When possible combine our movement with attack 
          Integer[] enemies = dst.getEntities(this, INT_ARRAY);
          int ecount = enemies == null ? 0 : enemies.length;
          for (int i = 0; i < ecount; i++) {
            if (!isFriendly(enemies[i])) {
              return new Event(EventKind.ACTION_MOVE_ATTACK, enemies[i]);
            }
          }
        }
        return new Event(EventKind.ACTION_MOVE_TO, dst.getLocation());
    }

    private Event move(int dx, int dy) {
        if (dx == 0 && dy == 0) {
          return null;
        }
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
          return move(dy * 3 + dx + 4, true);
        }
        dx = w == 0 ? x + dx : (x + dx + w) % w;
        dy = h == 0 ? y + dy : (y + dy + h) % h;
        // This loop is to prevent creating new locations on our own
        for (Location l : reachable) {
          if (l.getX() == dx && l.getY() == dy) {
            return new Event(EventKind.ACTION_MOVE_TO, l);
          }
        }
        shouldNotReachHere("x = " + x + ", y = " + y + ", dx = " + dx + ", dy = " + dy);
        return null;
    }

    private Event moveTo(int dx, int dy) {
        dx -= x;
        dy -= y;
        int v = (int)this.v;
        if (dx > w / 2) {
          dx = Math.max(dx - w, -v);
        } else if (dx < -w / 2) {
          dx = Math.min(dx + w, v);
        } else if (dx >= 0) {
          dx = Math.min(dx, v);
        } else {
          dx = Math.max(dx, -v);
        }
        if (dy > h / 2) {
          dy = Math.max(dy - h, -v);
        } else if (dy < -h / 2) {
          dy = Math.min(dy + h, v);
        } else if (dy >= 0) {
          dy = Math.min(dy, v);
        } else {
          dy = Math.max(dy, -v);
        }
        return move(dx, dy);
    }

    public synchronized void processEvent(BeingInterface bi, Event ev) {
        e = bi.getEnergy(this);
        switch (ev.kind()) {
          case BEING_BORN:
            // Who am I?
            id = bi.getId(this);
            if (bossHandle == null) {
                bossHandle = bi.getOwner(this, id);
            }
            assignTask((Task)((BeingParams)ev.param()).parameter);
            creatures.add(this);
            break;
          case BEING_DEAD:
            creatures.remove(this); // should be called before task.release()
            task.release(this);
            break;
          case BEING_ATTACKED:
              //log(name + " is under attack");
            break;
          case BEING_ENERGY_GIVEN:
            break;
        }
    }

    public BeingParams getParams() {
        return new BeingParams(m, v, new FieldSizeTask());
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static void shouldNotReachHere(String msg) {
        throw new RuntimeException(msg);
    }

    private static int dist(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        dx = Math.min(dx, w - dx);
        int dy = Math.abs(y1 - y0);
        dy = Math.min(dy, h - dy);
        return Math.max(dx, dy);
    }

    private int dist(int x0, int y0) {
        return dist(x0, y0, x, y);
    }

    private static class Resource implements Comparable<Resource> {
        int x, y;
        float de;
        float max;

        Resource(int x, int y, float de, float max) {
          this.x = x;
          this.y = y;
          this.de = de;
          this.max = max;
        }

        public int compareTo(Resource other) {
          return Float.compare(de, other.de);
        }

        public String toString() {
          return "(" + x + "," + y + " +" + de + " " + max + ")";
        }
    }

    private static class Task {
        static final Task EMPTY_TASK = new Task();

        static Task freeTask() {
          return tasks.isEmpty() ? EMPTY_TASK : tasks.removeFirst();
        }

        boolean findOwner() {
          for (ND nd : creatures) {
            if (nd.task == EMPTY_TASK) {
              nd.assignTask(this);
              return true;
            }
          }
          return false;
        }

        void enqueue() {
          if (!findOwner()) {
            tasks.addLast(this);
          }
        }

        void commit(ND nd) {
          // Sign a creature for doing this job
        }
        
        void release(ND nd) {
          // Do nothing because nobody needs an empty task
        }

        Event action(ND nd) {
          return null;
        }

        boolean isComplete() {
          return false;
        }
    }

    private static class FieldSizeTask extends Task {
        
        private void createDirectionMap(ND nd) {
          // The knowledge of 'left' and 'right' is the basic of our orientation
          DIRECTION_MAP = new int[9];
          LOCATION_MAP = new int[9];
          for (int i = 0; i < 9; i++) {
            DIRECTION_MAP[i] = -1;
            LOCATION_MAP[i] = -1;
          }
          int neighbours = nd.adj.size();
          for (int i = 0; i < neighbours; i++) {
            int dx, dy;
            Location l = nd.adj.get(i).getLocation();
            if (l.getX() == nd.x - 1 || (nd.x == 0 && l.getX() > 1)) {
              dx = -1; // left
            } else if (l.getX() == nd.x + 1 || (nd.x > 1 && l.getX() == 0)) {
              dx = 1;  // right
            } else {
              dx = 0;  // center
            }
            if (l.getY() == nd.y - 1 || (nd.y == 0 && l.getY() > 1)) {
              dy = -1; // up
            } else if (l.getY() == nd.y + 1 || (nd.y > 1 && l.getY() == 0)) {
              dy = 1;  // down
            } else {
              dy = 0;  // middle
            }
            int d = dy * 3 + dx + 4;
            DIRECTION_MAP[d] = i;
            LOCATION_MAP[i] = d;
          }
        }

        Event action(ND nd) {
          Event result = nd.feed();
          if (result != null) {
            return result;
          }
          // The border - is the place to find the field size
          int v = (int)nd.v;
          if (nd.x < v) {
            for (Location l : nd.reachable) {
              if (l.getX() >= w) {
                w = l.getX() + 1;
              }
            }
          }
          if (nd.y < v) {
            for (Location l : nd.reachable) {
              if (l.getY() >= h) {
                h = l.getY() + 1;
              }
            }
          }
          if (DIRECTION_MAP == null) {
            createDirectionMap(nd);
          }
          return nd.move(-v, -v);
        }

        void createDiscoverTasks() {
          // Each discover subtask is a target circle of radius 1
          for (int x = 1; x <= w; x += 3) {
            if (x == w) x--;
            for (int y = 1; y <= h; y += 3) {
              if (y == h) y--;
              new DiscoverTask(x, y).enqueue();
            }
          }
          // Randomization is a good way to achieve uniform distribution
          Collections.shuffle(tasks);
          DiscoverTask.maxExplorers = cellsTotal / 600;
          log("Discover tasks = " + tasks.size());
        }
        
        boolean isComplete() {
          if (w == 0 || h == 0) {
            return false;
          }
          log("Field size is known after " + turns + " turns: width = " + w + ", height = " + h);
          field_e = new float[w][h];
          field_de = new float[w][h];
          field_max = new float[w][h];
          field_t = new int[w][h];
          radius = Math.max(w, h) / 2;
          cellsTotal = w * h;
          createDiscoverTasks();
          return true;
        }
    }

    private static class SurviveTask extends Task {
        static final float E_SPAWN      = Constants.K_toborn;
        static final float E_CAN_RESCUE = Constants.K_toborn;
        static final float E_SOS        = Constants.K_emin + 0.05f;
        static final float E_NEED_FOOD  = Constants.K_emin + 0.10f;

        float recordCellInfo(PointInfo info, ND nd) {
          int x = info.getLocation().getX();
          int y = info.getLocation().getY();
          boolean firstTime = field_t[x][y] == 0;
          field_e[x][y]  = info.getCount(nd);
          field_de[x][y] = info.getGrowthRate(nd);
          field_max[x][y] = info.getMaxCount(nd);
          field_t[x][y]  = turns;
          if (firstTime) {
            cellsKnown++;
            return field_de[x][y];
          }
          return 0.0f;
        }

        private ND findDying(ND nd) {
          for (ND other : creatures) {
            if (nd.x == other.x && nd.y == other.y &&
                other != nd && other.e < E_SOS * nd.m) {
              log("Saved from death " + other.name);
              return other;
            }
          }
          return null;
        }

        void release(ND nd) {
          if (!findOwner()) {
            tasks.addFirst(this);
          }
        }

        Event action(ND nd) {
          // Update information about this fearful piece of world
          PointInfo bestCell = nd.cell;
          float bestGrowth = recordCellInfo(nd.cell, nd);
          for (PointInfo cell : nd.adj) {
            float growth = recordCellInfo(cell, nd);
            if (growth > bestGrowth) {
              bestCell = cell;
              bestGrowth = growth;
            }
          }

          // If we find a megaresource, guard it immediately
          if (bestGrowth > Constants.K_masscost * nd.m) {
            int x0 = bestCell.getLocation().getX();
            int y0 = bestCell.getLocation().getY();
            DiscoverTask.maxExplorers++;
            release(nd);
            nd.assignTask(new GuardTask(x0, y0));
            return nd.action();
          }

          // Check if we can save someone from imminent death
          int ecount = nd.enemies.length;
          if (ecount > 1 && nd.e > E_CAN_RESCUE * nd.m) {
            ND dying = findDying(nd);
            if (dying != null) {
              return nd.shareEnergy(dying.id);
            }
          }

          // If we are too fat - it's probably time to spawn
          if (!tasks.isEmpty() && nd.e > E_SPAWN * nd.m) {
            return new Event(EventKind.ACTION_BORN, new BeingParams(nd.m, nd.v, Task.freeTask()));
          }

          // Is there any victim on our cell? 
          if (ecount > 1) {
            for (int i = 0; i < ecount; i++) {
              if (nd.canAttack(nd.enemies[i])) {
                return new Event(EventKind.ACTION_ATTACK, nd.enemies[i]);
              }
            }
          }

          // No special action needed
          return null;
        }
    }

    private static class DiscoverTask extends SurviveTask {
        static int maxExplorers;
        int x0;
        int y0;

        DiscoverTask(int x0, int y0) {
          this.x0 = x0;
          this.y0 = y0;
        }

        public String toString() {
          return "(to: " + x0 + "," + y0 + ")";
        }

        void fieldDiscovered() {
          float eTotal = 0.0f;
          float deTotal = 0.0f;
          ArrayList<Resource> resources = new ArrayList<Resource>();
          for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
              eTotal += field_e[i][j];
              deTotal += field_de[i][j];
              if (field_de[i][j] != 0.0f) {
                resources.add(new Resource(i, j, field_de[i][j], field_max[i][j]));
              }
            }
          }
          Collections.sort(resources);
          log("Field completely discovered after " + turns + " turns");
          log("E = " + eTotal + ", dE = " + deTotal + ", fertile cells = " + resources.size());
        }

        boolean isComplete() {
          if (cellsKnown < cellsTotal) {
            int xn = x0 > 0 ? x0 - 1 : w - 1;
            int xp = x0 < w - 1 ? x0 + 1 : 0;
            int yn = y0 > 0 ? y0 - 1 : h - 1;
            int yp = y0 < h - 1 ? y0 + 1 : 0;
            return field_t[xn][yn] != 0 && field_t[x0][yn] != 0 && field_t[xp][yn] != 0 &&
                   field_t[xn][y0] != 0 && field_t[x0][y0] != 0 && field_t[xp][y0] != 0 &&
                   field_t[xn][yp] != 0 && field_t[x0][yp] != 0 && field_t[xp][yp] != 0;
          }
          if (cellsKnown == cellsTotal) {
            fieldDiscovered();
            cellsKnown++;
          }
          return true;
        }

        Location findRichestLocation(ND nd) {
          Location richest = null;
          float maxEnergy = 2 * (Constants.K_movecost * nd.v + Constants.K_masscost * nd.m);
          for (Location l : nd.reachable) {
            float energy = field_e[l.getX()][l.getY()];
            if (energy > maxEnergy) {
              richest = l;
              maxEnergy = energy;
            }
          }
          return richest;
        }

        Event action(ND nd) {
          Event result = nd.feed();
          if (result == null) {
            result = super.action(nd);
          }
          if (result != null) {
            return result;
          }
          if (nd.e < E_NEED_FOOD * nd.m || creatures.size() < maxExplorers) {
            Location l = findRichestLocation(nd);
            if (l != null) {
              return new Event(EventKind.ACTION_MOVE_TO, l);
            }
          }
          return nd.moveTo(x0, y0);
        }
    }

    private static class GuardTask extends SurviveTask {
        int x0;
        int y0;
        float de;

        GuardTask(int x0, int y0) {
          this.x0 = x0;
          this.y0 = y0;
          this.de = field_de[x0][y0];
        }

        Event action(ND nd) {
          Event result = nd.feed();
          if (result != null) {
            return result;
          }
          if (nd.x != x0 || nd.y != y0) {
            return nd.moveTo(x0, y0);
          }
          result = super.action(nd);
          if (result != null) {
            return result;
          }
          return null;
        }

    }

    private static class AttackTask {
        int x0;
        int y0;
        Integer target;

        AttackTask(int x0, int y0, Integer target) {
          this.x0 = x0;
          this.y0 = y0;
          this.target = target;
        }

        boolean isComplete() {
          // TODO: return true if the target is not alive anymore
          return false;
        }

        Event action(ND nd) {
          // Here is a dedicated war machine
          Event result = nd.feed();
          if (result != null) {
            return result;
          }
          if (nd.x == x0 && nd.y == y0) {
            return new Event(EventKind.ACTION_ATTACK, target);
          } else if (nd.dist(x0, y0) <= (int)nd.v) {
            return new Event(EventKind.ACTION_MOVE_ATTACK, target);
          } else {
            return nd.moveTo(x0, y0);
          }
        }
    }

}
