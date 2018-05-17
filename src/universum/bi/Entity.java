package universum.bi;

import universum.engine.*;

/**
 * Everything in the world should implement this interface.
 *
 * @author nike
 */
public interface Entity {
    /**
     * Handle an event, we pass BeingInterface, as it supersedes 
     * EntityInterface
     **/
    public void processEvent(BeingInterface bi, Event e);

    /**
     * Make this entity's turn, returned value is an event describing
     * action this being is planning to perform, see EventKind for 
     * possible event types. Note that only after all entities completed
     * turn (or killed if timed out), planned action is actually commited.
     * Also not that this function shouldn't be too heavyweight, otherwise
     * being can be killed by watchdog in engine.
     * 
     * @return event to perform
     * @see EventKind
     */
    public Event makeTurn(BeingInterface bi);
}
