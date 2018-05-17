package universum.ui;

public interface GameOwner {    
    // port remote broadcaster should listen upon,
    // if 0 it autoselects and notfies owner
    // if -1 - doesn't listen at all
    public int  getListenPort();
    public void setListenPort(int port);   
    public void redraw();
    public void notifyAboutCompletion(GameResult gr);
    public void notifyOnTurnEnd();
}
