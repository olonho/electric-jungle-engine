package universum.ui;

import javax.swing.JButton;
import java.awt.Insets;
import java.awt.event.ActionListener;

class MyButton extends JButton {
    private static Insets smallMargin = new Insets(1, 1, 1, 1);

    MyButton(String name, ActionListener al) {
        super(name);
        this.addActionListener(al);
    }

    MyButton(String image, String hint, ActionListener al) {
        super(Images.getImageIcon(image));
        setMargin(smallMargin);
        setToolTipText(hint);
        this.addActionListener(al);
    }
}
