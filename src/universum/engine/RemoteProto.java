package universum.engine;

public class RemoteProto {
    // magic
    public static final int MAGIC = 0x11223344;

    // buffer size 
    public static final int BUF_SIZE = 256 * 1024;

    // game status
    public static final int STATUS_UNKNOWN     = 0;
    public static final int STATUS_NOT_STARTED = 1;
    public static final int STATUS_RUNNING     = 2;
    public static final int STATUS_PAUSED      = 3;
    public static final int STATUS_COMPLETED   = 4;
    public static final int STATUS_FINISHED    = 5;
    

    //// Commands
    // show
    public static final int CMD_SHOW = 0x1;
}
