package universum.ui;

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import javax.swing.JOptionPane;
import static java.util.concurrent.TimeUnit.*;

import universum.engine.*;
import universum.bi.*;
import static universum.engine.RemoteProto.*;

class NetInfo extends RemoteInfo {
    SocketChannel sc;    
    String host; 
    int port;

    NetInfo(String host, int port) throws IOException {
        this.host = host; 
        this.port = port;
        init();
    }

    void reconnect() throws IOException {
        sc = SocketChannel.open();
        sc.configureBlocking(true);       
        sc.connect(new InetSocketAddress(host, port));
        sc.finishConnect();
        refresh();
    }

    boolean refresh() throws IOException {  
        if (sc == null) {
            throw new IOException("socket closed");
        }

        try {
            buf.clear();
            buf.putInt(MAGIC);
            buf.putInt(12);
            buf.putInt(CMD_SHOW);
            buf.flip();
            sc.write(buf);
            
            doChannelRead(sc);
            return parseBuf();
        } catch (IOException ioe) {
            if (sc != null) {
                try {
                    sc.close();
                } catch (IOException ioe1) {}
                sc = null;
            }
            throw ioe;
        }
    }    
}

class ProxiedNetInfo extends NetInfo {
    URL url;
    byte[] dataBuf = new byte[1024];

    ProxiedNetInfo(String host, int port) throws IOException {
        super(host, port);
        init();
        url = new URL("http", host, port, "/");
    }


    void reconnect() {
        // does nothing
    }

    boolean refresh() throws IOException {
        buf.clear();
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();
        uc.setUseCaches(false); uc.setDefaultUseCaches(false);
        uc.setDoOutput(false); 
        InputStream is = uc.getInputStream();
        // fill in buffer with content of stream
        int r;
        try {
            while ((r = is.read(dataBuf)) > -1) {
                buf.put(dataBuf, 0, r);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            is.close();
            //uc.disconnect();
        }        
        buf.flip();

        if (buf.remaining() < 8) {
            throw new IOException("short frame: "+buf.remaining());
        }
        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("inconsistent magic: "+magic);
        }
        int len = buf.getInt();
        if (len < 8 || len > BUF_SIZE) {
            throw new IOException("bad frame: "+len);
        }
        if (len != buf.remaining()+8) {
            throw new IOException("wrong len: "+len+" != "+(buf.remaining()+8));
        }

        synchronized (this) {
            return parseBuf();
        }
    }
}

public class RemoteViewer extends GameUI implements Cancellable {    
    private NetInfo ri;
    private final ScheduledExecutorService scheduler;
    private boolean paused, needed;
    private int delay;
    private ConnectDialog cDialog;
    private Thread me;

    public void cancel() {
        me.interrupt();
    }

    public RemoteViewer(String host, int port) throws IOException  {
        super(MODE_OBSERVER);
        needed = true;
        me = Thread.currentThread();
        cDialog = new ConnectDialog("Connecting to "+host+":"+port, this);
        cDialog.setVisible(true);

        try {
            delay = 100;
            ri = new NetInfo(host, port);
            ri.reconnect();
        } catch (Exception ioe) {
            delay = 500; // if we're behind firewall - put less load on proxy server
            ri = new ProxiedNetInfo(host, port);
	    try {
		ri.refresh();
	    } catch (IOException ioe1) {
                cDialog.setVisible(false);
                JOptionPane.showMessageDialog(this,
                                              "Couldn't connect to server: " + 
                                              host+":"+port+" ("+ioe+")",
                                              "Something wrong",
                                              JOptionPane.WARNING_MESSAGE);
	    }
        } finally {
            cDialog.setVisible(false);
        }

        renderer = new Renderer(ri);
        scheduler = Executors.newScheduledThreadPool(1);
        setPaused(false);

        if (false) {
            scheduler.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        if (!paused) {
                            redraw();
                        }
                    }
                }, 0, delay, MILLISECONDS);
        } else {
            new Thread(new Runnable() {
                    public void run() {
                        while (needed) {
                            redraw();
                            try {
                                Thread.sleep(delay);
                            } catch (Exception e) {}
                        }
                    }
            }).start();
        }
    }

    boolean printed = false;    
        
    public synchronized void redraw() {
        try {            
            needed = ri.refresh();
            showResults(ri.getStatus(), new GameResult(ri));
        } catch (IOException ioe) {
            if (!printed) {
                ioe.printStackTrace();
                //printed = true;
            }
            // maybe need reconnect?
            try {
                Thread.sleep(1000);
                ri.reconnect();
            } catch (Exception e) {}
            
        }
        redraw(true);
    }

    private List<ScoreData> scores;
    public int getPlayerCount() {
        if (ri == null || ri.scores() == null) {
            return 0;
        }
        // make a cache
        scores = new ArrayList<ScoreData>(ri.scores().values());
        Collections.sort(scores);

        return ri.players;
    }

    public ScoreData getPlayerScore(int playerId) {
        return scores.get(playerId);
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        //super.setPaused(paused);
    }
}
