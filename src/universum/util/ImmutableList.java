package universum.util;

import java.util.*;

/**
 * This is a List that doesn't permit add, set and remove operations.
 *
 * @author pan
 */
public class ImmutableList<E> extends AbstractList<E> implements List<E> {
    private E[] data;
        
    public ImmutableList(E[] array) {     
        this.data = array;
    }

    public int size() {
        return data == null ? 0 : data.length;
    }
    
    public E get(int index) {
        return data[index];
    }

    public Object[] toArray() {
        int size = size();
        if (size == 0) {
            return new Object[0];
        }
        Object[] rv = new Object[size];
        System.arraycopy(data, 0, rv, 0, size);
        return rv;
    }
    
}
