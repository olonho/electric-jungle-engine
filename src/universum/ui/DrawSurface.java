package universum.ui;

/**
 *
 * @author nike
 */

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class DrawSurface extends JPanel {
    // back buffer
    BufferedImage bimg;
    int biw, bih;
    RendererHolder owner;
    boolean repaint;
    
    public DrawSurface(RendererHolder owner) {
        this.owner = owner;
        this.repaint = false;
        setDoubleBuffered(true);
        //setBackground(Color.WHITE);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(700, 500);
    }
    
    // only package wide, GameOwner's redraw() is the public API
    void redraw() {
        repaint = true;
        repaint();
    }

    public void paint(Graphics g) {
        boolean needRepaint = repaint;
        Dimension d = getSize();
        if (bimg == null || biw != d.width || bih != d.height) {
            repaint = false;
            Graphics2D g2 = (Graphics2D)g;
            bimg = (BufferedImage) g2.getDeviceConfiguration().createCompatibleImage(d.width, d.height);
            biw = d.width; bih = d.height;
            needRepaint = true;
        }
        
        if (needRepaint) {
            repaint = false;
            clear(g);
            Renderer ren = owner.getRenderer();
            if (ren != null) {
                draw(ren);
            }            
        }
        
        if (bimg != null)  {
            g.drawImage(bimg, 0, 0, null);
            getToolkit().sync();
        } else {
            clear(g);
        }
    }
    
    public synchronized void render(Renderer ren, int w, int h, Graphics2D g2) {
        //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ren.render(w, h, g2);
        //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    public void clear() {
        if (bimg != null) {
            Graphics2D g2 = bimg.createGraphics();
            clear(g2);
            g2.dispose();
        }
        repaint();
    }
    
    protected void clear(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, biw, bih);
    }
    
    public synchronized void draw(Renderer ren) {
        if (ren == null) {
            return;
        }
        
        if (bimg != null) {
            Graphics2D g2 = bimg.createGraphics();
            clear(g2);
            render(ren, biw, bih, g2);
            g2.dispose();
        } else {
            System.out.println("No backbuffer yet");
        }
        repaint();
    }
    
    BufferedImage getImage() {
        return bimg;
    }
}
