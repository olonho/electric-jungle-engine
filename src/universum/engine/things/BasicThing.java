package universum.engine.things;

import  universum.engine.Universe;
import universum.bi.*;

abstract public class BasicThing implements Entity {
    protected Universe universe;
    protected Object owner;
    public void setUniverse(Universe u) {
        this.universe = u;
    }
    public void setOwner(Object owner) {
        this.owner = owner;
    }
    public abstract void processEvent(BeingInterface bi, Event e);
    public abstract Event makeTurn(BeingInterface bi);
}
