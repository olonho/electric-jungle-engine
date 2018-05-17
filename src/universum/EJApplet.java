package universum;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import universum.bi.*; 
import universum.ui.*;

public class EJApplet extends JApplet {
    GameUI ctr;
    String errorMsg = null;

    public void init() {
        ctr = makeViewer();
        
	if (ctr == null) {
	    showStatus(errorMsg != null ? errorMsg : "ERROR");
	    System.err.println(errorMsg);
	    repaint();
	    return;
	}

        Container c = getContentPane(); 
        c.setLayout(new BorderLayout());
        c.add(ctr, BorderLayout.CENTER);
    }
    
    public void start() {
    }
    
    public void stop() {
        if (ctr != null) {
            getContentPane().remove(ctr);
            ctr = null;
        }
    }

    public String getAppletInfo() {
        return "Electric Jungle viewer: "+Constants.VERSION;
    }

    public String[][] getParameterInfo() {
        String[][] info = {
            {"host",            "string",         "remote server"},
            {"port",            "int",            "remote port"}     
        };
        return info;
    }

    private GameUI makeViewer() {
	try {
	    String save = getParameter("save");
	    if (save != null) {
	        return new LocalViewer(save, -1);
	    }
	    String host = getParameter("host");
	    int port = Integer.parseInt(getParameter("port"));
	    return new RemoteViewer(host, port);
	} catch (Throwable t) {
	    t.printStackTrace();
	    errorMsg = t.toString();
	    return null;
	}
    }
    
    static void createAndShowGUI(String save, String host, String portStr) {
        final JFrame frame = new JFrame("Universum");
        final int width = 900, height = 600;
        frame.setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(d.width/2 - width/2, d.height/2 - height/2);

        final JComponent ctr;

        try {
            ctr = save != null ? new LocalViewer(save, -1)
                               : new RemoteViewer(host, Integer.parseInt(portStr));
        } catch (Exception ioe) {
            ioe.printStackTrace();
            return;
        }

        frame.addWindowListener( new WindowAdapter() {
                public void windowOpening( WindowEvent e ) {
                    ctr.repaint();
                }
                    
                public void windowClosing( WindowEvent e ) {
                    frame.dispose();
                    System.exit(0);
                }
            });
            
        Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());
        c.add(ctr, BorderLayout.CENTER);
        frame.validate();
        frame.setVisible(true);
        frame.repaint();
    }

    // to play as JNLP
    public static void main(String[] args) throws Exception {        
        final String host = System.getProperty("javaws.ejungle.host");
        final String portStr = System.getProperty("javaws.ejungle.port");
        final String save = System.getProperty("javaws.ejungle.save");

        if (save == null && (host == null || portStr == null)) {
            JOptionPane.showMessageDialog(null,
                                          "Invalid session?",
                                          "Something wrong",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(save, host, portStr);
            }
        });
    }
}
