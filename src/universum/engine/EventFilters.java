package universum.engine;

import universum.bi.Event;

/**
 *
 * @author nike
 */
public class EventFilters {
    public static EventFilter Any = new EventFilter() {
        public boolean match(BasicEvent e) {
            return true;
        }
    };
    
    public static EventFilter None = new EventFilter() {
        public boolean match(BasicEvent e) {
            return false;
        }
    };

}
