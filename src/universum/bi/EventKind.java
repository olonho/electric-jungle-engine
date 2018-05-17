package universum.bi;

/**
 * Kind of an event. Actually encodes both events and actions,
 * actions can be taken by being, and returned by makeTurn(),
 * and events come to being's processEvent().
 *
 * @see BeingInterface
 * @see Being
 * @author nike
 */
public enum EventKind 
{
        /**
         * Unknown event
         */
        UNKNOWN,
        // following are being events, which come to being from the world
        /** 
         * Very first event being receives, as parameters contains BeingParams
         * of this being passed by parent (can include arbirary parameter).
         *
         * @see #ACTION_BORN
         */
        BEING_BORN,

        /**
         * Notification on entity production. Parameter is id of 
         * the produced entity.
         * For contest there's no avaliable entitites, so it's only 
         * for future engine development.
         *
         * @see #ACTION_PRODUCE
         */
        BEING_PRODUCED,

        /** 
         * Very last event being receives, notifies about its death, 
         * has no paramters.
         */
        BEING_DEAD,

        /**
         * Event received when other being attacks us, has amount of damage 
         * inflicted as parameter, and sender() is id of being who did this damage 
         * to us.
         *
         * @see #ACTION_ATTACK
         */
        BEING_ATTACKED,

        /**
         * Event received when got energy from other beings,
         * parameter is amount of energy given (only your creatures can heal 
         * each another).
         *
         * @see #ACTION_GIVE
         */
        BEING_ENERGY_GIVEN,

        
        // following are actions being can take
        /**
         * Move to particular location, parameter is location where 
         * being wishes to move. Note that speed limits reachable locations.
         *
         * @see BeingInterface#getReachableLocations(Being)
         * @see Constants#K_movecost
         */
        ACTION_MOVE_TO,

        /**
         * Give birth to another creature, parameter is BeingParams
         * of creature it wishes to give birth to, and it must 
         * not vary to much from being's own parameters.
         * Energy is split in half between parent and baby.
         *
         * @see Constants#K_borncost
         * @see Constants#K_minbornvariation
         * @see Constants#K_maxbornvariation
         * @see #BEING_BORN
         */
        ACTION_BORN,

        /**
         * Produce a non-living entity. Parameter is string ID 
         * of entity to be produced.
         * For contest there's no avaliable entitites, so it's only 
         * for future engine development.
         *
         * @see #BEING_PRODUCED
         */
        ACTION_PRODUCE,
        

        /**
         * Attack another creature, parameter is id of creature to be attacked.
         * Attacked creature must be at the same location as attacker.
         * 
         * @see Constants#K_fight
         * @see Constants#K_retaliate
         * @see #BEING_ATTACKED
         */
        ACTION_ATTACK,

        /**
         * Attack another creature and move to the place where it is.
         * Paramter is id of attacked creature.
         * Attacked creature must be reachable for attacker (distance less
         * than speed of attacker), and it does less damage than ACTION_ATTACK
         * 
         * @see Constants#K_fight
         * @see Constants#K_retaliate
         * @see Constants#K_fightmovepenalty
         * @see #BEING_ATTACKED
         */    
        ACTION_MOVE_ATTACK,

        /**
         * Consume energy from the outside world. Amount of energy 
         * consumed per single turn is limited by creature parameters
         * and available energy. Parameter is amount of energy to be consumed.
         *
         * @see Constants#K_bite
         * @see BeingInterface
         */   
        ACTION_EAT,
        
        /**
         * Give energy to another creature, it must also belong to you and be
         * on the same cell.
         * Paramter is id of the target being.
         *
         * @see #BEING_ENERGY_GIVEN
         */
        ACTION_GIVE
};
