package universum.engine;

import universum.bi.Entity;

public interface EntityFactory {
    Entity produce(String id);
}
