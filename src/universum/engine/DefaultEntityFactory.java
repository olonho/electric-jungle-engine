package universum.engine;

import universum.bi.Entity;
import java.util.*;

public class DefaultEntityFactory implements EntityFactory {
    Map<String, Class> things;
    DefaultEntityFactory(String ... things) {
        this.things = new HashMap<String, Class>();
        for (String t : things) {
            registerThing(t);
        }
    }
   
    public boolean registerThing(String t) {
        Class c = null;
        try {
            c = Class.forName(t);
        } catch (Exception e) {}
        if (c == null) {
             try {
                 c = Class.forName("universum.engine.things."+t);
             } catch (Exception e) {}
        }
        if (c != null) {
            things.put(t, c);
        }        
        return c != null;
    }

    public Entity produce(String id) {
        Class c = things.get(id);
        try {
            if (c == null) {
                return null;
            }
            return (Entity)c.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
