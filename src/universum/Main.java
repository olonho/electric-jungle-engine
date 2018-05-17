package universum;

import universum.engine.*;
import universum.ui.*;
import universum.bi.Constants;
import universum.bi.GameKind;
import universum.util.ConfigFile;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author nike
 */
public class Main {
    public  JFrame   frame;
    private String   viewFile;
    private boolean  headless = false;
    private String   headlessArgs;
    private boolean  altui = false;
    private boolean  secure = false;
    private GameInfo gameInfo = new GameInfo();
    private String[][] beingList;
    
    public Main() {
    }
    
    private JComponent createCtr() {
        if (viewFile != null) {
            try {
                return new LocalViewer(viewFile, 8889);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return null;
            }
        } else {
            return new Controller(frame, gameInfo, beingList);
        }
    }
    
    private void createMainFrame() {
        if (altui) {
             universum.ui.sam.Application app = 
                 new universum.ui.sam.Application();
             app.run();
             this.frame = app.getFrame();
             return;
        }

        frame = new JFrame("Universum");
        final int WIDTH = 900, HEIGHT = 600;
        frame.setSize(WIDTH, HEIGHT);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(d.width/2 - WIDTH/2, d.height/2 - HEIGHT/2);

        final JComponent ctr = createCtr();

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
           
    /* 
     * as an example see game.properties
     */
    private void fromConfig(String cfg) {
        try { 
            ConfigFile f = new ConfigFile(new File(cfg));
            StringTokenizer st = 
                new StringTokenizer(f.getProperty("beings"), ", ");
            java.util.List<String[]> hargs = new ArrayList<String[]>();
            while (st.hasMoreTokens()) {                
                hargs.add(parseBeing(st.nextToken()));
            }
            beingList = hargs.toArray(new String[hargs.size()][]);
            gameInfo.update(f);                   
        } catch (Exception e) {
            e.printStackTrace();            
            System.exit(1);
        }
    }

    private String[] parseBeing(String n) {
        String[] rv = new String[2];
        int at = n.indexOf('@');
        if (at == -1) {
            throw new IllegalArgumentException("must be class@jar");
        }
        rv[0] = n.substring(0, at);
        rv[1] = n.substring(at+1, n.length());
        
        return rv;
    }
   
    void proceed(String[] args) {
        int i = 0;
        
        boolean gotcha = true;
        while  (i < args.length) {
            if ("-record".equals(args[i]) && args.length > i + 1) {
                i++;
                gameInfo.recordGameFilePath = args[i++];
                continue;
            }

            if ("-seed".equals(args[i]) && args.length > i + 1) {
                i++;
                gameInfo.randomSeed = Integer.parseInt(args[i++]);
                continue;
            }


            if ("-view".equals(args[i]) && args.length > i + 1) {
                i++;
                viewFile = args[i++];
                continue;
            }
            if ("-headless".equals(args[i]) && args.length > i + 1) {
                i++;
                headless = true;
                headlessArgs =  args[i++];
                continue;
            }

            if ("-batch".equals(args[i]) && args.length > i + 1) {
                i++;
                headless = true;
                fromConfig(args[i++]);
                continue;
            }

            if ("-kind".equals(args[i])) {
                i++;
                try {
                    gameInfo.setKind(args[i++]);
                } catch (IllegalArgumentException iae) {
                    System.exit(1);
                }
                continue;
            }

            if ("-altui".equals(args[i])) {
                i++;
                altui = true;
                continue;
            }

            if ("-debug".equals(args[i])) {
                i++;
                Constants.setDebug(true);
                continue;
            }

            if ("-secure".equals(args[i])) {
                i++;
                JungleSecurity.setCheckSecurity(true);
                continue;
            }
            
            if ("-makebeing".equals(args[i])) {
                i++;
                makeBeing(args[i++]);
                System.exit(0);
                continue;
            }
            
            if ("-config".equals(args[i])) {
                i++;
                fromConfig(args[i++]);
                continue;
            }
                        
            gotcha = false;
            i++;
        }

        if (!gotcha) {
            System.out.print("Known command line flags are:\n"+
                             " -headless class1@file1.jar,class2@file2.jar - runs contest in batch mode with no visualization\n"+
                             " -seed - set random seed\n"+
                             " -record file - records game to file\n"+
                             " -view file - plays back recorded game\n"+
                             " -altui - use alternative user interface\n"+
                             " -debug - debugging info\n"+
                             " -kind - set game kind\n"+
                             " -makebeing my.properties - make being by description\n"+
                             " -secure - run with security manager\n"+
                             " -batch file - run from config file in headless mode\n"+
                             " -config file - run from config file in UI mode\n"
                             );            
            return;                               
        }
        
        if (headless) {
            HeadlessController ctr = new HeadlessController(gameInfo, -1);
            java.util.List<String[]> hargs = new ArrayList<String[]>();
            if (headlessArgs != null) {
               StringTokenizer st = new StringTokenizer(headlessArgs, ", ");
               while (st.hasMoreTokens()) {                
                   hargs.add(parseBeing(st.nextToken()));
               }
               beingList = hargs.toArray(new String[hargs.size()][]);
            } 
            ctr.startGame(beingList);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        createMainFrame();
                    }
                });
        }
    }
    
    public static void main(final String[] args) {
        new Main().proceed(args);
    }
    
    private static boolean needRebuild(String jar, String bhome, String files) {
        File fjar = new File(jar);
        if (!fjar.exists()) {
            return true;
        }
        long jmfile = fjar.lastModified();
        StringTokenizer st = new StringTokenizer(files, ",");        
        while (st.hasMoreTokens()) {
            File f = new File(bhome+"/"+st.nextToken());
            if (f.lastModified() > jmfile) {
                return true;
            }
        }
        return false;
    }
   
    private static void copyFile(File from, File to) throws IOException {
        FileInputStream fis  = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int i = 0;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    public static boolean makeBeing(String file) {
        boolean done = false;
        try {
            Properties props = new Properties();
            File ffile = new File(file).getAbsoluteFile();
            if (!ffile.exists()) {
                ffile = new File("./src/universum/beings", ffile.getName());
            }
            if (!ffile.exists()) {
                ffile = new File("../src/universum/beings", ffile.getName());
            }
            if (!ffile.exists()) {
                System.out.println("Cannot find being descriptor: "+file);
                return false;
            }
            InputStream is = new FileInputStream(ffile);
            props.load(is);
            String bhome = ffile.getParentFile().getAbsolutePath();
            String jar = beingPath(props.getProperty("jar"));

            if (!needRebuild(jar, bhome, props.getProperty("files"))) {
                System.out.println(jar+" already up to date");
                return true;
            }

            Tool t = Tool.getTool();
            File being = new File(new File(jar).getParentFile(), "being");
            t.rmdir(being);
            being.mkdir();
            
            java.util.List<String> args = new java.util.ArrayList<String>();
            args.add("-target");  args.add("1.5"); 
            args.add("-classpath");
            String cp1 = 
                new File(being, "../dist").getAbsolutePath();
            String cp2 = 
                new File(".", "universum.jar").getAbsolutePath();            
            // very special case, to handle building of SimpleBeing in Netbeans
            String cp3 = 
                new File(new File(file).getParentFile(), "../../../build/classes").getAbsolutePath();
            String cp4 = System.getProperty("java.class.path");
                
            args.add(cp1+File.pathSeparator+cp2+File.pathSeparator+cp3+
                     File.pathSeparator+cp4);
            args.add("-d"); args.add(being.getAbsolutePath());            
            
            StringTokenizer st = new StringTokenizer(props.getProperty("files"), ",");
            while (st.hasMoreTokens()) {
                args.add(bhome+"/"+st.nextToken());                
            }
            
            int rv = t.compile(args.toArray(new String[args.size()]));            
            if (rv != 0) {
                System.out.println("Compilation error");
                return false;
            }            

            // now make the jar
            File man = new File(being, "BEING.MF"); 
            FileWriter manw = new FileWriter(man);
            String main = "Main-Class: "+props.getProperty("main-class") + "\r\n"; 
            manw.write(main);
            String icon = props.getProperty("icon");
            if (icon != null) {
                File ico = new File(bhome+"/"+icon);
                if (ico.exists()) {
                    copyFile(ico, new File(being.getAbsolutePath()+"/icon.png"));
                    manw.write("Being-Icon: icon.png\r\n" );
                }
            }
            manw.close();

            args.clear();
            args.add("cmf"); args.add(man.getAbsolutePath());
            args.add(jar);
            args.add("-C");  args.add(being.getAbsolutePath());
            args.add(".");
            rv = t.jar(args.toArray(new String[args.size()]));
            man.delete();           

            if (rv == 0) {
                System.out.println("Sucessfully made: "+jar);
            } else {
                System.out.println("Packing error");
            }
            t.rmdir(being);
            done = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return done;
    }

    private static String beings;
    public static String beingPath(String name) {
        if (beings == null) {
            File b = new File("../beings");
            b.mkdir();
            try {
                beings = b.getCanonicalPath() + File.separator;
            } catch (IOException ioe) {}
        }

	if (new File(name).exists()) {
            return name;
        }

        return beings+name;
    }
}
