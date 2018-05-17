package universum.engine;

class DataBuffer {
    private byte[] buf;
    int size, refCount;
       
    DataBuffer(int size) {
        this.size = size;
        this.refCount = -1;
    }

    public synchronized boolean reserveIfAvailable() {
        if (refCount < 0) {
            buf = new byte[size];
            refCount = 0; 
        }
        if (refCount == 0) {
            addRef();
            return true;
        }
        return false;
    }

    public synchronized DataBuffer addRef() {
        if (refCount == 0) {
            //System.out.println("reserved "+this+" s="+size);
        }
        refCount++;
        return this;
    }

    public synchronized void release() {
        if (--refCount == 0) {
            //System.out.println("freed "+this);
        }
    }

    public byte[] data() {
        if (refCount <= 0) {
            throw new RuntimeException("wrong status: "+refCount);
        }
        return buf;
    }
}

class BufferPool {
    private int bufSize;
    private DataBuffer pool[];

    BufferPool(int numBuffers, int bufSize) {
        this.bufSize = bufSize;
        pool = new DataBuffer[numBuffers];
        for (int i=0; i<pool.length; i++) {
            pool[i] = new DataBuffer(bufSize);
        }
    }
    synchronized DataBuffer getFreeBuf() {
        if (pool == null) {
            return null;
        }

        for (DataBuffer db : pool) {
            if (db.reserveIfAvailable()) {
                return db;                    
            }
        }
        System.out.println("buffer pool exhausted: "+pool.length);
        DataBuffer db = new DataBuffer(bufSize);
        db.reserveIfAvailable();
        return db;
    }
     void cleanup() {
        for (int i=0; i<pool.length; i++) {
             pool[i] = null;
        }
        pool = null;
    }
}
