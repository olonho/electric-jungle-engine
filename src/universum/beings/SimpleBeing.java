package universum.beings;

import universum.bi.*;
import universum.util.*;
import java.util.List;
import static universum.bi.Constants.*;

/**
 * This is simple and intentionally dumb being, just to demonstrate some basics
 * of Electric Jungle.
 *
 * @author nike
 */
public class SimpleBeing implements Being {
    static final String boss = "nike";
    static Object bossHandle = null;

    // initial parameters
    // mass
    static final float M0 = 10f;
    // and speed
    static final float S0 = 1f;
    // when energy level here is less than this - should start looking for better place 
    static final float E_min = 4f;
    // which energy level is acceptable to select this point as new location
    static final float E_min2 = 6f;
    // minimum of energy one should have before bearing baby
    static final float E_born = 0.85f;
    // only when that strong should consider attacking
    static final float E_attack = 0.8f;    
       
    // local copy of params
    private float M;
    private float S;
    
    // threshold to eat
    private float E_eat;
    
    // array to store info about other beings
    Integer[] others;
    // our own id
    Integer id;
    
    // population control
    static int maxCount = 200;
    static int currentCount = 0;
    
    public SimpleBeing() {        
    }

    // this one is the essence of your being - it's invoked once a turn
    // and lets your being do something
    public synchronized Event makeTurn(BeingInterface bi) {      
        Event ev = null;
        // information about point we're in
        PointInfo pi = bi.getPointInfo(this);
        // how much energy do we have now
        float e = bi.getEnergy(this);
        // how much energy available at our current location
        float avail = pi.getCount(this);
        // how much can we eat
        float canEat = M - e;                

        // self-saving comes first
        // food is always important        
        if (e < 0.9 * M && avail > 0.1f && Util.frnd() < 0.8f) {            
            return new Event(EventKind.ACTION_EAT, M);
        }
        
        if (avail < E_min && e < E_eat) {
            // start looking for the better place
            List<PointInfo> ns = bi.getNeighbourInfo(this);
            for (PointInfo n : ns) {
                // if one of our neighbour locations have enough evergy - 
                // move there
                if (n.getCount(this) > E_min2) {
                    // OK, go this way and hope for the best
                    ev = new Event(EventKind.ACTION_MOVE_TO, n.getLocation());
                    break;
                }
            }
            
            if (ev == null) {
                // nothing good around, go to random place
                int idx = Util.rnd(ns.size());
                ev = new Event(EventKind.ACTION_MOVE_TO, ns.get(idx).getLocation());
            }
        }
                
                
        // then children
        if (ev == null) {
            if ((ev = maybeBorn(e)) != null) {
                return ev;
            }
        }
        
        // we use others array to request list of ids presented in this point,
        // and to avoid excessive allocations we just clear our own copy,
        // not request new one
        others = pi.getEntities(this, others);
        for (int i=0; i<others.length; i++) {
            // no more elements
            if (others[i] == null) {
                break;
            }
            // is it me?
            // use fastest possible comparision of ints
            if (id != null && others[i].intValue() != id.intValue()) {
                // if no other actions - try to attack, in general you should
                // only attack other players beings, to transfer energy voluntary -
                // use ACTION_GIVE
                if (ev == null) {
                    ev = maybeAttack(bi, e, others[i]);
                }                
            }
            // clean others array for next use
            others[i] = null;
        }                        
        
        return ev;
    }
    
    private Event maybeAttack(BeingInterface bi, float e, Integer other) {
        if (bossHandle != bi.getOwner(this, other) && 
            e > M*E_attack && Util.frnd() < 0.2f) {
           return new Event(EventKind.ACTION_ATTACK, other);   
        }
        return null;
    }
    
    private Event maybeBorn(float e) {        
        if (currentCount < maxCount && e > E_born*M && Util.frnd()<0.4f) {
            BeingParams bp = getParams();
            // we mark that we're parent of this one, just to demonstrate
            // use of parameter
            bp.parameter = id;        
            return new Event(EventKind.ACTION_BORN, bp);
        }
        return null;
    }
    
    public synchronized void processEvent(BeingInterface bi, Event e) {
        switch (e.kind()) {
            case BEING_BORN:
                // increase population counter
                currentCount++;                
                init(bi, bi.getId(this), (BeingParams)e.param());
                break;
            case BEING_DEAD:
                // gonna get dead, decrease population counter
                currentCount--;
                break;
            case BEING_ATTACKED:
                // now just print if one attacks us
                bi.log(this, "attacked by "+e.sender()+" of "+
                       bi.getOwner(this, e.sender())+" damage: "+e.param());
                break;
            case BEING_ENERGY_GIVEN:
                // ... or gives a gift
                bi.log(this, "got "+e.param()+" energy from "+e.sender());
                break;
        }        
    }
    
    public String getName() {
        return "Simple";
    }
    
    // this method is invoked once per game, to make sure all static are properly inited
    public void reinit(UserGameInfo info) {	
        currentCount = 0;
        bossHandle = null;
	Util.log("game "+info.kind+ " maxTurns="+info.maxTurns);
    }
    
    // return our parameters
    public BeingParams getParams() {
        BeingParams bp = new BeingParams(M0, S0);
        return bp;
    }
    
    // who this entity belongs to (and score accounted for)
    public String getOwnerName() {
        return boss;
    }
    
    private void init(BeingInterface bi, Integer id, BeingParams bp) {
         this.id = id;
         this.M  = bp.M;
         this.S  = bp.S;
         this.E_eat = 0.6f * M;
         this.others = new Integer[1];
         if (bossHandle == null) {
             bossHandle = bi.getOwner(this, id);
         }
         
         // this one just shows possible uses of the parameter,
         // being should feel free to use parameter for more sophisticated
         // things, such as 'gene' of the being, or some other program
         // for evolution
         // bi.log(this, "parent is "+bp.parameter);
    }
}
