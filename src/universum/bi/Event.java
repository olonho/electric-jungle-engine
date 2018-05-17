package universum.bi;

import universum.engine.BasicEvent;

/**
 * Event is an abstraction describing both actions and events
 * in Electric Jungle. EventKind describe actual meaning of
 * this particular event.
 *
 * @see EventKind
 * @see BeingInterface
 * @author nike
 */
public final class Event extends BasicEvent<Object>
 {
     private final EventKind kind;
     
     /**
      * Create new Event of a kind 
      */
     public Event(EventKind kind) {
         this(kind, null);
     }
     
     /**
      * Create new Event of a kind with kind-specific parameter
      */
     public Event(EventKind kind, Object param) {
         this(kind, null, param);          
     }
     
     /**
      * Create new Event of a kind with kind-specific parameter
      * for particular target (engine-only)
      */
     public Event(EventKind kind, Integer target, Object param) {
         super(param, target);
         this.kind = kind;
     }
     
     /**
      * Return kind of this Event
      */
     public EventKind kind() {
         return this.kind;
     }
     
     public String toString() {         
         StringBuffer sb = new StringBuffer();
         sb.append("EVENT: kind=");
         sb.append(kind.toString());
         sb.append(" param=");       
         sb.append(param() == null ? "<null>" : param().toString());
         return sb.toString();
    }
    
     public Event clone() {
         Constants.checkPerms();
         Event o = null;
         try {
             o = (Event)super.clone();
         } catch (CloneNotSupportedException cnse) {}
         return o;
     }
}
