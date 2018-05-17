package universum.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class ConnectDialog extends JDialog implements ActionListener {
    private JButton cancel;        
    Cancellable what;

    ConnectDialog(String message, Cancellable what) {
        super();
        
        this.what = what;
        setLayout(new BorderLayout());

        final int width = 200, height = 100;
        setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(d.width/2 - width/2, d.height/2 - height/2);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setString(message);
        bar.setStringPainted(true);
        add(bar, BorderLayout.CENTER);
        add(cancel = new MyButton("Cancel", this), BorderLayout.SOUTH);
        validate();
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();        
        if (source == cancel) {
            if (what != null) {
                what.cancel();
            }
        }
    }
}
