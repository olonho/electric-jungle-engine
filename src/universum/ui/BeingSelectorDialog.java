package universum.ui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.util.jar.*;
import java.io.File;
import javax.swing.filechooser.FileFilter;

import universum.bi.Constants;

class BeingSelectorDialog extends JDialog implements ActionListener {
    private JButton        addjar, ok, cancel;
    private JList          selector;
    private JFileChooser   filechooser;
    private Controller     ctr;
    private Vector<String> beingClasses;

    void loadBeingList(Vector<String> list) {
        Set<Object> keys = ctr.cfgBeings.keySet();
        for (Object beingClass : keys) {          
            String jar = ctr.getClassJar((String)beingClass, true);
            // put in selector only available jars
            if (new File(jar).exists()) {
                list.add((String)beingClass);
            }
        }
    }

    java.util.List<String> parseFiles(File[] files) {
        int len = files.length;
        java.util.List<String> rv = new ArrayList<String>();
        for (int i = 0; i<len; i++) {
            try {
                JarFile f = new JarFile(files[i]);
                Manifest mf = f.getManifest();
                if (mf != null) {
                    String mainClass = mf.getMainAttributes().
                        getValue(Attributes.Name.MAIN_CLASS);
                    if (mainClass != null && !mainClass.equals("")) {
                        beingClasses.add(mainClass);
                        rv.add(mainClass);
                        ctr.cfgBeings.setProperty(mainClass, files[i] + "|Unknown");
                    } else {
                        System.out.println(files[i] + ": no Main-Class attribute in Manifest");
                    }
                } else {
                    System.out.println(files[i] + ": no Manifest file");
                }
                f.close();
            } catch (Exception e) {
                System.out.println(files[i] + ": " + e);
            }
        }
        selector.setListData(beingClasses);
        
        return rv;
    }

    BeingSelectorDialog(Controller ctr) {
        super(ctr.frame, "Add beings", false);
        this.ctr = ctr;
        
        final int width = 300, height = 300;
        setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(d.width/2 - width/2, d.height/2 - height/2);
        
        setLayout(new BorderLayout(0, 10));
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                setVisible(false);
            }
        });

        filechooser = new JFileChooser(".");
        filechooser.setDialogTitle("Add beings from JAR");
        filechooser.setMultiSelectionEnabled(true);
        filechooser.setAcceptAllFileFilterUsed(false);
        filechooser.addChoosableFileFilter(new FileFilter() {
            public String getDescription() {
                return "JAR files";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String name = f.getName();
                return name.endsWith(".jar") || name.endsWith(".JAR");
            }
        });
        
        loadBeingList(beingClasses = new Vector<String>());
        selector = new JList(beingClasses);
        selector.setBackground(SystemColor.menu);
        selector.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    actionPerformed(new ActionEvent(ok, 0, null));
                }
            }
        });
        JScrollPane listbox = new JScrollPane(selector);
        listbox.setBackground(selector.getBackground());
        listbox.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Available creatures:"));

        addjar = new MyButton("Add from JAR", this);
        addjar.setMargin(new Insets(0, 0, 0, 0));
        addjar.setBackground(selector.getBackground());

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(listbox, BorderLayout.CENTER);
        controls.add(addjar, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(1, 2));
        buttons.add(ok = new MyButton("OK", this));
        buttons.add(cancel = new MyButton("Cancel", this));
        
        add(controls, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        
        validate();
    }
    
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        
        if (source == ok) {
            setVisible(false);
            Object[] selected = selector.getSelectedValues();
            for (Object klazz : selected) {
                ctr.addBeing((String)klazz);
            }
            ctr.redraw();
        } else if (source == cancel) {
            setVisible(false);
        } else if (source == addjar) {
            if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.util.List<String> chosen = 
                    parseFiles(filechooser.getSelectedFiles());
                for (String c : chosen) {
                    selector.setSelectedValue(c, false);
                }
            }
        }
    }  
}
