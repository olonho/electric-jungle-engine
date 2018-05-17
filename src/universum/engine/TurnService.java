package universum.engine;

import java.util.concurrent.*;
import java.util.*;

import universum.bi.*;
import universum.util.*;

// it's not an inner class to avoid subtle problem with
// finalization leading effectively to memory leak
// Hint: for inner classes we cannot set "this" to null
class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    TurnService owner;
    CustomThreadPoolExecutor(TurnService owner, int poolSize) {
        super(poolSize, poolSize,
              0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>());
        this.owner = owner;
    }
    
    public void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (owner != null) {
            owner.done();
        }
    }
  
    void stop() {
         shutdownNow();
         owner = null;
    }
}

class TurnService {
    private final Universe u;
    private final CustomThreadPoolExecutor pool;
    int poolSize;

    TurnService(Universe u) {
        this.u = u;
        poolSize = 4; //Runtime.getRuntime().availableProcessors();
        // we create an additional thread to handle events fast
        // in case all other worker threads are busy        
        this.pool = new CustomThreadPoolExecutor(this, poolSize+1);
    }
   
    // maybe should be made atomic, but not that frequently used in fact
    volatile int active;

    // only single executeMulti allowed at the time
    void executeMulti(Runnable action) {
	if (pool.isShutdown()) {
            return;
        }
        synchronized (this) {
            active += poolSize;
        }
        for (int i=0; i<poolSize; i++) {
            pool.execute(action);
        }
    }

    Future<Object> executeSingle(Callable<Object> action) {
        if (pool.isShutdown()) {
            return null;
        }

        synchronized (this) {
            active++;
        }
        return pool.submit(action);
    }
   
    void waitCompletion() {
        if (pool.isShutdown()) {
            return;
        }

        for (int i = 0; i<1000; i++) {
            if (i > 50) {
                // 1 sec
                u.checkHanging();
            }
            try {          
                synchronized (this) {
                    if (active == 0) {
                        return;
                    }
                    wait(20);
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        // 20 seconds is too much
        u.killAllRunning();
        System.out.println("VLT!");
    }

    synchronized void done() {
        active--;
        if (active == 0) {
            notify();
        }
    }

    void stop() {
        pool.stop();
    }
}
