package universum.engine;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.lang.ref.*;

import universum.util.*;
import universum.ui.GameOwner;
import universum.ui.GameResult;
import universum.bi.*;
import static universum.engine.RemoteProto.*;

public class RemoteRenderer implements Runnable {
    private RenderingInfo ri;
    private volatile int port;
    private Thread serverThread; 
    private Snapshot snap;
    private Selector selector;
    private ByteBuffer cmdBuf;
    private GameOwner owner;
    private BufferPool pool;
   
    class Snapshot {
	private DataBuffer data;
	private int len; 
        private ByteBuffer buf;
        private Walker<Integer>  eSnap = new Walker<Integer>() {
	    public void walk(Integer id) {                
                float energy = ri.getEnergy(id);                
                Location loc = ri.getLocation(id);
                int owner = ri.getType(id);
                buf.putInt(id);
                buf.putInt(owner);
                LocationUtil.toBuffer(buf, loc);
                buf.putFloat(energy);
            }
	};
        private Walker<Location> rSnap = new Walker<Location>() {
	    public void walk(Location l) {
                float e = ri.getResourceCount(l);
                buf.putFloat(e);
                LocationUtil.toBuffer(buf, l);
            }
	};     

        Snapshot() {	    
	    snap();
	}

	synchronized void snap() {
            if (data != null) {
                // we don't want our older data
                data.release();
                data = null;
                buf = null;
            }
            if (pool == null) {
                return;
            }
            data = pool.getFreeBuf();
            if (data == null) {
                return;
            }
            buf = ByteBuffer.wrap(data.data());
            
            makeHeader();            
            ri.forAllE(eSnap);
            buf.putInt(-1); // terminator
            ri.forAllR(rSnap);
            buf.putFloat(-1f); // terminator
	    
	    // write length
	    int pos = buf.position();
	    buf.position(4);
	    buf.putInt(pos);
	    buf.position(pos);
	    len = pos;
	}

	synchronized DataBuffer bytes() {
	    return data.addRef();
	}

	synchronized int length() {
	    return len;
	}

        private void putString(ByteBuffer bb, String s) {
            int len = s.length();
            bb.putInt(len);
            for (int i=0; i<len; i++) {
                bb.putChar(s.charAt(i));
            }
        }
        
        private void makeHeader() {
            buf.putInt(MAGIC);
            buf.putInt(0); // leave space for the size

            // record status
            int status = ri.getStatus();
            buf.putInt(status);

            // record kind
            byte kind = (byte)Util.kind2Int(ri.getGameKind());
            buf.put(kind);

	    // record diameters
	    List<Integer> d = ri.diameters();
	    buf.putInt(d.get(0)); buf.putInt(d.get(1));	    
	    
	    // record number of turns
	    buf.putInt(ri.maxTurns());
	    buf.putInt(ri.numTurns());
	    
	    // now score table
            Map<String, ScoreData> scores = ri.scores();
            for (String p : scores.keySet()) {
                ScoreData sd = scores.get(p);
                buf.putInt(sd.id);
                buf.putFloat(sd.score);
                buf.putFloat(sd.energy);                
                putString(buf, p);
                if (status >= STATUS_COMPLETED) {
                    putString(buf, sd.getErrors());
                }
            }
            buf.putInt(-1); // terminator
        }
    }

    public RemoteRenderer(RenderingInfo ri, GameOwner owner) {
	this.ri = ri;
        this.owner = owner;
	this.port = owner.getListenPort();
	this.cmdBuf = ByteBuffer.allocate(1024);
        this.pool = new BufferPool(10, BUF_SIZE);        
	this.snap = new Snapshot();
        // we start network socket only if requested so
        if (port >= 0) {
            serverThread = new Thread(this);
            serverThread.start();
        }
    }

    public void makeSnapshot(boolean force) {
        if (stopped) {
            return;
        }
        // make snapshot if have active connections
        if (force || conn.keySet().size() > 0 || Constants.ALLOW_HTTP_WATCHING){
            snap.snap();
        }
    }

    DataBuffer getSnappedData() {
        return snap.bytes();
    }

    int getSnappedDataLen() {
        return snap.length();
    }
    
    public void run() {
	ServerSocketChannel ssChannel = null;
	try {
	    this.selector = Selector.open();
	    
	    // Create  non-blocking server socket
	    ssChannel = ServerSocketChannel.open();
	    ssChannel.socket().setReuseAddress(true);
	    ssChannel.configureBlocking(false);	    	    
	    // note that JDK1.5 has bug in socket implementation
	    // on Solaris preventing us from second bind(), thus
	    // use 1.6
	    ssChannel.socket().bind(port > 0 ? 
                                    new InetSocketAddress(port) : null);
            port = ssChannel.socket().getLocalPort();
            owner.setListenPort(port);
	    ssChannel.register(selector, ssChannel.validOps());
	    
	    while (!stopper) {
		// Wait for an event
		selector.select();
		
		// Get list of selection keys with pending events
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();
		
		// Process each key
		while (it.hasNext()) {
		    SelectionKey key = it.next();
		    it.remove();

		    try {
			processSelectionKey(key);			
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }

	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {		
		if (ssChannel != null) {
		    System.out.println("closing server: "+ssChannel.socket());
		    ssChannel.close();		    
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }

            // kill all pending connections
            Iterator<SocketChannel> i  = conn.keySet().iterator();
            while (i.hasNext()) {
                SocketChannel sc = i.next();
                try {
                    killConnection(sc, false);
                } catch (IOException ioe) {}
                i.remove();
            }
	}
        stopped = true;
        pool.cleanup();
    }
    
    private void processSelectionKey(SelectionKey selKey) throws IOException {
        // Since the ready operations are cumulative,
        // need to check readiness for each operation        
	if (selKey.isValid() && selKey.isReadable()) {
	    SocketChannel sc = (SocketChannel)selKey.channel();
            boolean kill;
            try {
                kill = read(sc);
            } catch (Exception e) {
                e.printStackTrace();
                kill = true;
            }
            
            if (kill) {
		killConnection(sc);
                return;
	    }
	}

	if (selKey.isValid() && selKey.isWritable()) {
	    SocketChannel sc = (SocketChannel)selKey.channel();
            boolean kill;            
	    try {
                kill = write(sc);
            } catch (Exception e) {
                kill = true;
            }
            
            if (kill) {
		killConnection(sc);
                return;	    
            }
	}
	
	if (selKey.isValid() && selKey.isAcceptable()) {
	    accept((ServerSocketChannel)selKey.channel());
	} 
	
	if (selKey.isValid() && selKey.isConnectable()) {
            // Get channel with connection request
            connect(selKey, (SocketChannel)selKey.channel());
	}	
    }

	    
    private void connect(SelectionKey selKey, SocketChannel sc) 
        throws IOException {
	boolean success = sc.finishConnect();
	if (!sc.finishConnect()) {
	    // An error occurred; handle it	    
	    // Unregister the channel with this selector
	    selKey.cancel();
	}
    }
	    
    private void accept(ServerSocketChannel ssc) throws IOException {
	SocketChannel sChannel = ssc.accept();
	
        // If serverSocketChannel is non-blocking, sChannel may be null
        if (sChannel == null) {
            // There were no pending connection requests; try again later.
            // To be notified of connection requests,
	    return;
        } else {
            // Use the socket channel to communicate with the client
	    addConnection(sChannel);	   
        }
    }

    private Hashtable<SocketChannel, UserConnection> 
	conn = new Hashtable<SocketChannel, UserConnection>();

    class UserConnection {
	SocketChannel sc;
	DataBuffer data;
        boolean isHttp;
        private ByteBuffer bbuf;

	UserConnection(SocketChannel sc) {
	    this.sc = sc;
            this.isHttp = false;
        }

        void setDataBuffer(DataBuffer data) {
            if (this.data != null) {
                this.data.release();
                this.data = null;
                this.bbuf = null;
            }
            // we already addRef()
            this.data = data;
        }

        ByteBuffer buf() {
            assert data != null;
            if (bbuf == null) {
                bbuf = ByteBuffer.wrap(data.data());
            }
            return bbuf;
        }

        void close() throws IOException {
            if (data != null) {
                data.release();                
                data = null;
            }
            sc.close();
        }
    }

    private void addConnection(SocketChannel sc) throws IOException {	
	System.out.println("Connection from: "+
                           sc.socket().getRemoteSocketAddress());

	conn.put(sc, new UserConnection(sc));
	sc.configureBlocking(false);

	// we care only about incoming requests initially
	sc.register(selector, SelectionKey.OP_READ);
    }

    private void killConnection(SocketChannel sc, boolean remove) 
        throws IOException {
        System.out.println("closed: "+sc);
        UserConnection uc = conn.get(sc);
        uc.close();
	if (remove) {
            conn.remove(sc);
        }
    }

    private void killConnection(SocketChannel sc) throws IOException {
        killConnection(sc, true);
    }

    private boolean readWhole(SocketChannel sc, ByteBuffer buf, int len) 
        throws IOException {
	int read = buf.position(), r, iter = 0;
	
	buf.limit(len);
	do {
	    r = sc.read(buf);	   
	    if (r == -1) {
		// No more bytes can be read from the channel
                System.out.println("will of -1");
		killConnection(sc);
		return buf.position() >= len;
	    }
	    
	    // we not gonna work on SO slow connections
	    if (iter++ > 20) {
		System.out.println("too many iters: l="+len+" r="+read);
		return false;
	    }

	    read += r;
	    
	} while (read < len);	

	return true;
    }

    private boolean write(SocketChannel sc) throws IOException {
        UserConnection uc = conn.get(sc);
        ByteBuffer buf = uc.buf();

	if (buf != null) {
	    if (buf.hasRemaining()) {
		sc.write(buf);
	    }
	    
	    if (!buf.hasRemaining()) {
		// if nothing else to write - we don't need further updates
		// on writeability of the channel                
                sc.register(selector, SelectionKey.OP_READ);
		//uc.buf = null;
	    }
	}

	return uc.isHttp;
    }


    private void readHttpReq(SocketChannel sc, ByteBuffer buf) throws IOException {
        buf.limit(1024);
        int iters = 0;
        do {
            int r = sc.read(buf);
            if (r < 0) {
                break;
            }            
            if (r == 0) {
                // try to find CR/LF
                byte[] b = buf.array();
                int len = buf.position();
                for (int i=0; i<len-3; i++) {
                    if (b[i] == 0xd && b[i+1] == 0xa &&
                        b[i+2] == 0xd && b[i+3] == 0xa) {
                        return;
                    }
                }
                if (iters++ > 100) {
                    return;
                }
            }
        } while (true);
    }

    private boolean read(SocketChannel sc) throws IOException {
	UserConnection uc = conn.get(sc);
	ByteBuffer buf = cmdBuf;
	buf.clear();
      
	if (!readWhole(sc, buf, 8)) {
            System.out.println("cannot read start!");
	    return false;
	}

	// To read the bytes, flip the buffer
	buf.flip();
	
	assert buf.remaining() >= 8;
	
	int magic = buf.getInt();

        if (magic == 0x47455420) { // "GET "
            // we hack here
            // read remaining bytes of HTTP request
            buf.limit(1024);
            sc.read(buf);
            //readHttpReq(sc, buf);
            // we only support CMD_SHOW for now
            sendFrame(uc, true);
            return true;
        }

	if (magic != MAGIC) {
            buf.flip();
            readWhole(sc, buf, 128);
	    System.out.println("Bad magic: "+magic);
	    return false;
	}
	
	int len = buf.getInt();
	
	buf.clear();
	if (!readWhole(sc, buf, len-8)) {
	    return false;
	}	
	buf.flip();	
	
	int cmd = buf.getInt();
	//System.out.println("cmd="+cmd);

	// execute command
	switch (cmd) {
	case CMD_SHOW:
	    sendFrame(uc, false);
	    break;
	default:
	}
	
	return uc.isHttp;
    }

    private void sendFrame(UserConnection uc, boolean fakeHttp) 
        throws IOException  {
	uc.setDataBuffer(snap.bytes());
        int len = snap.length();
        //System.out.println("sendFrame: "+len+" h="+fakeHttp);
        if (fakeHttp) {
            String req = "HTTP/1.1 200 OK\r\n"+
                "Content-Length: "+len+"\r\n"+
                "Accept-Ranges: bytes\r\n"+
                "Connection: close\r\n"+
                "Content-Type: application/octet-stream\r\n\r\n";
            cmdBuf.clear();
            cmdBuf.put(req.getBytes("US-ASCII"));
            cmdBuf.flip();
            uc.sc.write(cmdBuf);
            //System.out.println("sf: "+uc.sc);
            uc.isHttp = true;
        }

	ByteBuffer buf = uc.buf();
	buf.rewind();
	buf.limit(len);

	int w = uc.sc.write(buf);
        //System.out.println("wrote: "+w+" to sc="+uc.sc);        

	if (buf.hasRemaining()) {
	    // to let us proceed with the buffer when this write
	    // request completes
	    uc.sc.register(selector, 
                           SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
	
    }
     
    volatile boolean stopper = false, stopped = false;
    public void stop() {
        if (stopped) {
           return;
        }
	stopper = true;
	selector.wakeup();
    }
    
    GameResult result;    
    void setResult(GameResult result) {
        this.result = result;
    }
}
