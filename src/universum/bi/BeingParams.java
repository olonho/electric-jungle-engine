package universum.bi;

/**
 * BeingParams define parameters of the being constructed
 * at the start of the game and born during the game
 *
 * @see Being
 * @see EventKind#BEING_BORN
 * @author nike
 */
public final class BeingParams implements Cloneable {
    /**
     * Mass of the being, defines
     * how much enegry being can hold, but bigger capacity means more
     * energy consumption during moving and living.
     * Also your score is defined as total mass of al your beings 
     * at the end of the game.
     */
    public float M;

    /**
     * Speed of the beings, defines
     * how fast being can move, but faster moving means higher energy 
     * spending even during short moves
     */
    public float S;

    /**
     * Parameter, can be used for customization of new beings
     * as newly created being receives its BeingParams (an argument to
     * ACTION_BORN is passed as an argument for BEING_BORN).
     *
     * @see EventKind#ACTION_BORN
     * @see EventKind#BEING_BORN
     */
    public Object parameter;    
    
    /**
     * Simple constructor, without customization parameter
     */
    public BeingParams(float mass, float speed) {
        this(mass, speed, null);
    }
    
    /**
     * Constructor with customization parameter
     */
    public BeingParams(float mass, float speed, Object parameter) {
        this.M = mass; this.S = speed; this.parameter = parameter;
    }
    
    /**
     * Cloning
     */
    public BeingParams clone() {
        BeingParams o = null;
        try {
            o = (BeingParams)super.clone();
        } catch (CloneNotSupportedException cnse) {}
        return o;
    }
    
    public String toString() {
        return "[m="+M+" s="+S+"]";
    }
}
