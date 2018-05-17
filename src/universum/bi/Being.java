package universum.bi;

/**
 * Being is interface for living creatures.
 *
 * @author nike
 */
public interface Being extends Entity {
    /**
     * Invoked once per externally added being, before game start, 
     * provides basic information about game it will participate in
     */
    void reinit(UserGameInfo info);

    
    /**
     * Being's name, could be anything, for unique id 
     * use id returned by BeingInterface.getId(Entity e)
     *
     * @return being name
     * @see BeingInterface
     */
    String       getName();

    /**
     * Name of the player who owns this being,
     * used only once to create first being, all others inherit the same owner
     *
     * @return owner name
     */
    String       getOwnerName();

    /**
     * Parameters of being,
     * used only once to create first being, for others BeingParams defined
     * by parent (argument to BEING_BORN event)
     *
     * @return being parameters
     * @see Event
     * @see EventKind
     */     
    BeingParams  getParams();
}
