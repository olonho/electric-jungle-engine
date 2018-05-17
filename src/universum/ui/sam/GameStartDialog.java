package universum.ui.sam;

import universum.bi.Constants;
import universum.bi.GameKind;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
final class GameStartDialog
{
    private static final int TURNS = 0;
    private static final int WIDTH = 1;
    private static final int HEIGHT = 2;
    private static final int NORMAL = 3;
    private static final int GOLDEN = 4;

    private static final String TITLE = "dialog.game.start";
    private static final Comparable[][] SPINNER = {
            {".turns", Constants.getMaxTurns(), 0, 10000, 100},
            {".width", Constants.getWidth(), 10, 1000, 10},
            {".height", Constants.getHeight(), 10, 1000, 10},
            {".normal", Constants.NUM_REGULAR, 10, 1000, 10},
            {".golden", Constants.NUM_GOLDEN, 0, 10, 1}};

    private final JSpinner[] spinner = new JSpinner[SPINNER.length];
    private final Config config;

    private JComboBox combo;
    private JPanel panel;
    private String title;

    GameStartDialog( Config config )
    {
        this.config = config;
    }

    public boolean show( JFrame parent )
    {
        return OK_OPTION == showConfirmDialog( parent, getPanel(), getTitle(), OK_CANCEL_OPTION );
    }

    public GameKind getGameKind()
    {
        return ( GameKind )getComboBox().getSelectedItem();
    }

    public int getTurns()
    {
        return getValue( TURNS );
    }

    public int getWidth()
    {
        return getValue( WIDTH );
    }

    public int getHeight()
    {
        return getValue( HEIGHT );
    }

    public int getNormal()
    {
        return getValue( NORMAL );
    }

    public int getGolden()
    {
        return getValue( GOLDEN );
    }

    private int getValue( int index )
    {
        return ( Integer )getSpinner( index ).getValue();
    }

    private JComboBox getComboBox()
    {
        if ( this.combo == null )
        {
            this.combo = new JComboBox( GameKind.values() );
            this.combo.setSelectedItem( GameKind.JUNGLE );
        }
        return this.combo;
    }

    private JPanel getPanel()
    {
        if ( this.panel == null )
        {
            GridBagConstraints gbc = new GridBagConstraints();
            this.panel = new JPanel( new GridBagLayout() );

            gbc.gridy = 1;
            gbc.insets.top = 2;
            gbc.insets.left = 2;
            gbc.insets.right = 2;
            gbc.insets.bottom = 2;
            gbc.anchor = GridBagConstraints.EAST;
            this.panel.add( createLabel( ".kind" ), gbc );

            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            this.panel.add( getComboBox(), gbc );

            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            this.panel.add( createLabel( WIDTH ), gbc );
            this.panel.add( getSpinner( WIDTH ), gbc );
            this.panel.add( createLabel( HEIGHT ), gbc );
            this.panel.add( getSpinner( HEIGHT ), gbc );

            gbc.gridy = 3;
            this.panel.add( createLabel( TURNS ), gbc );

            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            this.panel.add( getSpinner( TURNS ), gbc );

            gbc.gridy = 4;
            gbc.gridwidth = 4;
            this.panel.add( createLabel( ".resource" ), gbc );

            gbc.gridy = 5;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            this.panel.add( createLabel( NORMAL ), gbc );

            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            this.panel.add( getSpinner( NORMAL ), gbc );

            gbc.gridy = 6;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            this.panel.add( createLabel( GOLDEN ), gbc );

            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            this.panel.add( getSpinner( GOLDEN ), gbc );
        }
        return this.panel;
    }

    private String getTitle()
    {
        if ( this.title == null )
            this.title = this.config.getTitle( TITLE );

        return this.title;
    }

    private JSpinner getSpinner( int index )
    {
        if ( this.spinner[index] == null )
            this.spinner[index] = new JSpinner(
                    new SpinnerNumberModel(
                            ( Number )SPINNER[index][1],
                            SPINNER[index][2],
                            SPINNER[index][3],
                            ( Number )SPINNER[index][4] ) );

        return this.spinner[index];
    }

    private JLabel createLabel( int index )
    {
        JLabel label = createLabel( SPINNER[index][0] );
        label.setLabelFor( getSpinner( index ) );
        return label;
    }

    private JLabel createLabel( Object name )
    {
        return new JLabel( this.config.getText( TITLE + name ) );
    }
}