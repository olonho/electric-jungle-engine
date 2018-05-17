package universum.engine;

/**
 *
 * @author nike
 */
public class BasicEvent<T> implements Cloneable {
    protected T       param;
    private Integer   sender;
    private Integer   target;
    
    public BasicEvent(T param, Integer target) {
        this.param = param;
        this.target = target;
    }

    public final T param() {
        return param;
    }
    
    void setSender(Integer sender) {
        this.sender = sender;
    }
    
    public Integer sender() {
        return this.sender;
    }

    public Integer target() {
        return this.target;
    }
}
