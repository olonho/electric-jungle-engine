package universum.ui;

import java.util.*;
import java.util.zip.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import universum.engine.*;
import universum.util.Util;

class FileInfo extends RemoteInfo {
    ZipInputStream zis;
    ReadableByteChannel rbc;

    FileInfo(String url) throws IOException {
        super.init();
        InputStream is;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            is = new URL(url).openConnection().getInputStream();
        } else {
            is = new FileInputStream(url);
        }
        zis = new ZipInputStream(is);
        zis.getNextEntry();
        rbc = Channels.newChannel(zis);
    }

   boolean refresh() throws IOException {       
        doChannelRead(rbc);
        return parseBuf();
    }   
}

public class LocalViewer extends GameUI implements GameOwner {    
    private FileInfo fi;
    private boolean paused, needed;
    private int delay = 200;
    private int port;
    private RemoteRenderer broadcaster;
    
    public LocalViewer(String file, int port) throws IOException  {
        super(MODE_PLAYER);

        this.port = port;
        this.needed = true;
        this.fi = new FileInfo(file);
        this.renderer = new Renderer(fi);
        setPaused(false);

        new Thread(new Runnable() {
                public void run() {
                    while (needed) {
                        if (!paused) {
                            redraw();
                        }
                        try {
                            if (delay > 0) {
                                Thread.sleep(delay);
                            }
                        } catch (Exception e) {}
                    }
                    try {
                        fi.zis.close();
                    } catch (IOException e) {}
                }
            }).start();
    }
    
    
    private void updateBroadcaster() {
        if (broadcaster == null && port >= 0) {
            broadcaster = new RemoteRenderer(fi, this); 
        }
        if (broadcaster != null) {
            broadcaster.makeSnapshot(true);
        }
    }

    public synchronized void redraw() {
        try {
            needed = fi.refresh();
            updateBroadcaster();
            showResults(fi.getStatus(), new GameResult(fi));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        redraw(true);
    }

    private List<ScoreData> scores;
    public int getPlayerCount() {
        if (fi == null || fi.scores() == null) {
            return 0;
        }
        
        // make a cache
        scores = new ArrayList<ScoreData>(fi.scores().values());
        Collections.sort(scores);

        return fi.players;
    }

    public ScoreData getPlayerScore(int playerId) {       
        return scores.get(playerId);
    }


    public void setPaused(boolean paused) {
        this.paused = paused;
        super.setPaused(paused);
    }

    protected void setStopped() {
        System.exit(0);
    }
    
    protected void speedChanged(int v) {
        // XXX: rewrite me
        delay = 200 - v; 
        assert delay >= 0;
        redraw(true);
    }
    
    public byte[] getIconData(Integer id) {
        return null;
    }

    // of GameOwner
    public int getListenPort() {
        return port;
    }
    public void setListenPort(int port) {
        this.port = port;
    }
    
    public void notifyAboutCompletion(GameResult gr) {
    }

    public void notifyOnTurnEnd() {
    }
}
