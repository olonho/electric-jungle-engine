package universum.engine;

import universum.bi.Entity;
import universum.engine.things.BasicThing;

final class EntityData {
    private Universe universe;
    private String id;
    private EntityFactory ef;
    private float cost;

    EntityData(Universe u, String id, EntityFactory ef, float cost) {
        this.id = id;
        this.ef = ef;
        this.cost = cost;
        this.universe = u;
    }

    public Entity produce(Object owner) {
        Entity e =  ef.produce(id);
        if (e == null || !(e instanceof BasicThing)) {
            return null;
        }
        BasicThing bt = (BasicThing)e;
        bt.setUniverse(universe);
        bt.setOwner(owner);
        return bt;
    }

    float cost() {
        return this.cost;
    }
}
