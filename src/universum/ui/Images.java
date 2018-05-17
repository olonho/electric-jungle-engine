package universum.ui;

import java.awt.*;
import javax.swing.*;
import java.util.Hashtable;
import java.net.URL;

class Images {
    private static Hashtable<String, Image> cache =  new Hashtable<String, Image>();

    public static ImageIcon getImageIcon(String name) {
        URL url = universum.util.Util.findResource("img/"+name);       
        if (url == null) {
            throw new RuntimeException("no image: "+name);
        }
        return new ImageIcon(url);        
    }

    public static synchronized Image getImage(String name) {
        Image img = cache.get(name);
        if (img != null) {
            return img;
        }
        img = getImageIcon(name).getImage();
        // force loading
        img.getWidth(null);
        cache.put(name, img);
        return img;
    }
    
    public static synchronized Image fromData(String name, byte[] data) {
        Image img = cache.get(name);
        if (img != null) {
            return img;
        }
        img = new ImageIcon(data).getImage();
        // force loading
        img.getWidth(null);
        cache.put(name, img);
        return img;
    }
}
