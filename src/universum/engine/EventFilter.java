package universum.engine;

import universum.bi.Event;

/**
 *
 * @author nike
 */
public interface EventFilter {
    boolean match(BasicEvent e);
}
