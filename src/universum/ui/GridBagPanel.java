package universum.ui;

import java.awt.*;
import javax.swing.*;

class GridBagPanel extends JPanel {
    protected GridBagConstraints c;
    protected GridBagLayout gridbag;


    GridBagPanel(GridBagConstraints c) {
        super(new GridBagLayout());
        this.c = c;
        this.gridbag = (GridBagLayout)getLayout();
    }

    public Component add(Component comp) {
        gridbag.setConstraints(comp, c);
        return super.add(comp);
    }

}
