package universum.ui;

import java.awt.*;
import java.awt.image.*;
import javax.swing.ImageIcon;
import java.util.*;

import universum.bi.Being;
import universum.bi.Entity;
import universum.bi.Location;
import universum.bi.Constants;
import universum.engine.*;
import universum.util.*;

/**
 *
 * @author nike
 */
public class Renderer {
    RenderingInfo ri;   
    
    // those are transient fields, valid only during rendering operation
    Graphics2D g;
    // those are cache
    int w, h, x0, y0, d1, d2;
    float sx, sy;
    EntitiesRenderer entitiesRend;
    EnergyRenderer   energyRend;    
    Image beings[][], resources[];
    ImageIcon beingIcon[];
    Color playfieldColor =  new Color(0x9f, 0xb9, 0xff);
        // new Color(0xa1, 0xa1, 0xf3);
        //new Color(0x16, 0xaa, 0xff);

    static String[] playerIcons = {
        "smile25.png",
        "angry25.png",
        "actor25.png",
        "babelfish25.png",
        "bug25.png",
        "cow25.png",               
        "butterfly25.png",
        "kitten25.png"
    };
    static String[] appleIcons = {
        "apple-red15.png",
        "apple1.png",
        "apple2.png",
        "apple3.png",
        "apple4.png",
        "apple5.png",
        "apple6.png",
    };

    int MAX_PLAYERS = playerIcons.length; 
    int MAX_RES = appleIcons.length;

    public Renderer(RenderingInfo ri) {
        this.ri = ri;
        entitiesRend = new EntitiesRenderer();
        energyRend = new EnergyRenderer();
        
        beings = new Image[MAX_PLAYERS][11];
        beingIcon = new ImageIcon[MAX_PLAYERS];
        
        for (int i=0; i<MAX_PLAYERS; i++) {
            initPlayer(playerIcons[i], beings, i);
            beingIcon[i] = new ImageIcon(beings[i][2]);
        }
        
        resources = new Image[MAX_RES];
        
        for (int i=0; i<MAX_RES; i++) {
            resources[i] = Images.getImage(appleIcons[i]);
        }
    }
    
    private void initPlayer(String name, Image array[][], int idx) {
        initImages(Images.getImage(name), array[idx]);       
    }

    private void initImages(Image base, Image array[]) {
        for (int i=0; i<11; i++) {
            array[i] = base.getScaledInstance(i*2+10, i*2+10, 
                                              Image.SCALE_SMOOTH);
            // It looks absurd but the scaled images are loaded lazily,
            // and here's a way to force image preloading
            array[i].getWidth(null);
        }
    }

    class EntitiesRenderer implements Walker<Integer> {               
        private Map<Integer, Image[]> 
            type2Image = new HashMap<Integer, Image[]>();

        private int energyToSize(float e) {
            if (e < 8f) {
                return 5;
            }
            if (e >= 208f) {
                return 25;
            }
            return (int)((e - 8) / 10f) + 5;
        }
        
        private int sizeToIdx(int size) {
            return (size - 5) / 2;
        }

        Image imageForType(int type, int size) {
            Image[] imgs = type2Image.get(type);
            if (imgs == null) {
                byte[] data = ri.getIconData(type); 
                if (data == null) {
                    imgs = beings[type % MAX_PLAYERS];
                } else {
                    imgs = new Image[11];
                    initImages(Images.fromData("_image_"+type, data), imgs);
                }
                type2Image.put(type, imgs);
            }           
            return imgs[sizeToIdx(size)];
        }

        private Image imageFor(Integer id, int size) {
            return imageForType(ri.getType(id), size);           
        }

        public void walk(Integer id) {
	    float energy = ri.getEnergy(id);                
	    Location loc = ri.getLocation(id);
	    int size = energyToSize(energy);
	    int x = getX(loc) + (int)(sx*0.5f) - size / 2; 
	    int y = getY(loc) + (int)(sy*0.5f) - size / 2;           
            Image img = imageFor(id, size);
            g.drawImage(img, x, y, null);

	    if (Constants.getDrawNumber()) {
		g.setColor(Color.BLACK);
		String ids = id.toString();
		FontMetrics fm = g.getFontMetrics();
		g.drawString(ids, x + size/2-fm.stringWidth(ids)/2, 
			     y + size/2+4);
            }
            foundOne(loc);
        }

        private void foundOne(Location l) {
            Integer i = multi.get(l);
            if (i == null) {
                i = 0;
            }
            multi.put(l, i+1);
        }
    }
    
    class EnergyRenderer implements Walker<Location> {
        float minEnergy = 0f, maxEnergy = Constants.MAX_REGULAR;

        private Image resourceFor(float e) {
            if (e < Constants.MAX_REGULAR / 10f) return resources[MAX_RES-1];
            if (e > maxEnergy) return resources[0];

            int i = (int) ((maxEnergy - e) / maxEnergy  * MAX_RES)+1;
            if (i >= MAX_RES) i = MAX_RES-1;
            return resources[i];
        }

        public void walk(Location l) {
            float e = ri.getResourceCount(l);
            int x = getX(l); int y = getY(l);
            g.drawImage(resourceFor(e), x-7, y-5, null);
        }        
    }
    

    Map<Location, Integer> multi = new HashMap<Location, Integer>();    

    private void renderPlayfield() {
        final int BORDER = 8;
        
        x0 = BORDER; y0 = BORDER;
        
        int x = w / 2;
        int y = h / 2;
        
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, w, h);
        
        w -= 2 * BORDER;
        h -= 2 * BORDER;
        // compute scaling coeeficients
        java.util.List<Integer> diameters = ri.diameters();        
        if (diameters == null) {
            return;
        }

        d1 = diameters.get(0); d2 = diameters.get(1);
        sx = (float)w / (float)d1;
        sy = (float)h / (float)d2;
        
        g.setColor(playfieldColor);
        g.fillRect(x0, y0, w, h);
                
        g.setColor(Color.LIGHT_GRAY);
        if (Constants.getDrawGrid()) {
            for (int i=0; i<d1; i++) {
                g.drawLine(getX(i), getY(0), getX(i), getY(d2));
            }
            for (int i=0; i<d2; i++) {
                g.drawLine(getX(0), getY(i), getX(d1), getY(i));
            }
        }
        
    }

    protected void renderResources() {
	ri.forAllR(energyRend);        
    }
    
    protected void renderEntities() {
        multi.clear();

        ri.forAllE(entitiesRend);
        
        g.setFont(new Font("Arial Bold", Font.BOLD, 10));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();

        for (Location l : multi.keySet()) {
            Integer count = multi.get(l);
            if (count > 1) {
                String c = count.toString();
                int x = getX(l);
                int y = getY(l);
                int w =  fm.stringWidth(c);
		g.drawString(Integer.toString(count), 
                             x - w/2, y);
            }
        }
    }

    public void render(int w, int h, Graphics2D g2) {
        // cache in fields
        this.g = g2; this.w = w; this.h = h;
        
        if (ri.getStatus() == RemoteProto.STATUS_UNKNOWN) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.RED); 
            g.setFont(new Font("Arial Bold", Font.BOLD, 24));
            g.drawString("Not ready", w/2 - 40, h/2 - 20);
            return;
        }
        
        // in theory renderer should be able to handle various topologies
        // be stupid for now
        renderPlayfield();
        
        // render resources
        renderResources();
        

        // now render every entity in the universe
        renderEntities();

        

        // cleanup cached values
        this.g = null; this.w = 0; this.h = 0;
        
    }

    public ImageIcon getPlayerIcon(int playerId) {
        return new ImageIcon(entitiesRend.imageForType(playerId, 12));
    }
    
    private int getX(int x) {
        return (int)(sx * x +0.5f) + x0;
    }
    
    private int  getY(int y) {
        return (int)(sy * y +0.5f) + y0;
    }
    
    private int getX(Location loc) {
        return (int)(w * LocationUtil.projectionX(loc, d1)+0.5f) + x0;
    }
    
    private int  getY(Location loc) {
        return (int)(h * LocationUtil.projectionY(loc, d2)+0.5f) + y0;
    }
}
