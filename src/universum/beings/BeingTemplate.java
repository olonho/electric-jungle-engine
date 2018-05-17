package universum.beings;

import universum.bi.*;
import universum.util.*;


// when developing - write descriptor based on template.properties 
// do java -jar universum.jar -makebeing my.properties 
// add line like
//  universum.beings.BeingTemplate=my.properties:My Name 
// into beings.properties and make clean
public class BeingTemplate implements Being {
    // usually must be empty
    public BeingTemplate() {
    }
    
    // main entry point of the creature - its actions in the world
    public synchronized Event makeTurn(BeingInterface bi) {
        return null;
    }
    
    // notification on external events
    public synchronized void processEvent(BeingInterface bi, Event e) {        
    }
    
    // name of this creature
    public String getName() {
        return "name of the creature";
    }
    
    // return our parameter
    public BeingParams getParams() {
        // parameters of the initial creature, mass and speed, set to 
        // your preferred values
        BeingParams bp = new BeingParams(0f, 0f);
        return bp;
    }
    
    // your name, as shown in winners table
    public String getOwnerName() {
        return "your name";
    }
    
    // this method is invoked once per game, to make sure all static 
    // are properly inited
    public void reinit(UserGameInfo info) {
    }
}
