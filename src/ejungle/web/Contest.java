package ejungle.web;

import java.net.*;
import java.util.*;

/*
 * This stub class is used to allow successful compilation of
 * universum.ContestRunner. ContestRunner should not be visible
 * to the ClassLoader which loads ejungle.web.* and thus
 * should reside in different classpath.
 */
public abstract class Contest implements Runnable {

    public static enum State {
        NEW      ,
        ACTIVE   ,
        FINISHED ;
    }

    protected int owner;
    protected String key;
    protected Date startTime;
    protected Date endTime;
    protected State state;
    protected int port;
    protected Thread job;

    public static synchronized Contest create(int owner, String[] args, String classpath)
        throws Exception
    {
        throw new Exception("Stub class should not be accessed!");
    }

    public void setListenPort(int port) {
        assert false;
    }

    public int getListenPort() {
        return -1;
    }

    protected abstract void stop();

    public void destroy() {
        throw new RuntimeException();
    }
    
    public abstract void parseArgs(String[] args);
    
    public abstract String[][] getBeingInfo();

    public abstract boolean waitCompletion();

    public abstract String[] results();
    
    public int getOwner() {
        return owner;
    }
    
    public String getKey() {
        return key;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int numTurns() {
        return 0;
    }

}
