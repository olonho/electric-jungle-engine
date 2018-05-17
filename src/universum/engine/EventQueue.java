package universum.engine;

import java.util.LinkedList;
import universum.bi.Event;

/**
 *
 * @author nike
 */
public class EventQueue {
    private LinkedList<Event> list;
    
    /** Creates a new instance of EventQueue */
    public EventQueue() {
        list = new LinkedList<Event>();
    }
    
    public synchronized Event getEvent() {
        if (list.size() > 0) {
            return list.removeFirst();
        }
        return null;
    }
    
    public synchronized void putEvent(Event e) {
        list.addLast(e);
    }
}
