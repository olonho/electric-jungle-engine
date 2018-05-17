package universum.engine.things;

import universum.engine.Universe;
import universum.util.Util;
import universum.bi.*;

public class Trap extends BasicThing {
    public Trap() {
    }
    public void processEvent(BeingInterface bi, Event e) {
        switch (e.kind()) {
        case BEING_BORN:
            initTrap(bi.getLocation(this));
            break;
        default:
        }       
    }
    public Event makeTurn(BeingInterface bi) {        
        return null;
    }

    private void initTrap(Location loc) {
        Util.log("setting trap at: "+loc);    
    }
}
