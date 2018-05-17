package universum.ui.sam;

import universum.bi.Constants;
import universum.engine.GameInfo;
import universum.engine.Universe;
import universum.ui.GameOwner;
import universum.ui.GameResult;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumnModel;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

/**
 * TODO:description.
 *
 * @author Sergey A. Malenkov
 */
public final class Application
        implements Runnable, GameOwner, ChangeListener
{
    /**
     * The maint entry to the application.
     *
     * @param args  command line arguments
     */
    public static void main( String[] args )
    {
        SwingUtilities.invokeLater( new Application() );
    }

    private final GameInfo gi = new GameInfo();
    private final Config config = new Config( "main" );
    private final String formatTurn = this.config.getFormat( "view.turn" );
    private final EntityTableModel model = new EntityTableModel( this.config );

    private final Action actionAddEntity = new ConfiguredAction( this.config, "add.entity" )
    {
        private final AddEntityDialog dialog = new AddEntityDialog( config, model );

        public void actionPerformed( ActionEvent event )
        {
            this.dialog.show( getFrame() );
        }
    };
    private final Action actionExit = new ConfiguredAction( this.config, "exit" )
    {
        public void actionPerformed( ActionEvent event )
        {
            System.exit( 0 );
        }
    };
    private final Action actionShowToolBar = new ConfiguredAction( this.config, "show.toolbar" )
    {
        public void actionPerformed( ActionEvent event )
        {
            Object source = event.getSource();
            if ( source instanceof AbstractButton )
            {
                AbstractButton button = ( AbstractButton )source;
                boolean selected = button.isSelected();

                JToolBar toolbar = getToolBar();
                toolbar.setVisible( selected );
                if ( toolbar.isFloatable() )
                    toolbar.setFloatable( false );
            }
        }
    };
    private final Action actionShowGrid = new ConfiguredAction( this.config, "show.grid" )
    {
        public void actionPerformed( ActionEvent event )
        {
            Object source = event.getSource();
            if ( source instanceof AbstractButton )
            {
                AbstractButton button = ( AbstractButton )source;
                getUniverseView().showGrid( button.isSelected() );
            }
        }
    };
    private final Action actionShowId = new ConfiguredAction( this.config, "show.id" )
    {
        public void actionPerformed( ActionEvent event )
        {
            Object source = event.getSource();
            if ( source instanceof AbstractButton )
            {
                AbstractButton button = ( AbstractButton )source;
                getUniverseView().showId( button.isSelected() );
            }
        }
    };
    private final Action actionZoomIn = new ConfiguredAction( this.config, "zoom.in" )
    {
        public void actionPerformed( ActionEvent event )
        {
            getUniverseView().updateZoom( 1 );
        }
    };
    private final Action actionZoomOut = new ConfiguredAction( this.config, "zoom.out" )
    {
        public void actionPerformed( ActionEvent event )
        {
            getUniverseView().updateZoom( -1 );
        }
    };
    private final Action actionGameStart = new ConfiguredAction( this.config, "game.start" )
    {
        private final GameStartDialog dialog = new GameStartDialog( config );

        public void actionPerformed( ActionEvent event )
        {
            boolean paused = false;
            if ( universe == null )
            {
                if ( this.dialog.show( getFrame() ) )
                {
                    gi.maxTurns = this.dialog.getTurns();
                    gi.fieldWidth = this.dialog.getWidth();
                    gi.fieldHeight = this.dialog.getHeight();
                    gi.numRegular = this.dialog.getNormal();
                    gi.numGolden = this.dialog.getGolden();

                    universe = new Universe( Application.this, gi );

                    paused = !model.init( universe );
                    getUniverseView().init( universe );
                    universe.bigbang();
                }
            }
            if ( universe != null )
            {
                universe.setPaused( paused );

                actionGameStart.setEnabled( paused );
                actionGamePause.setEnabled( !paused );
                actionGameStop.setEnabled( true );

                if ( paused )
                    showMessageDialog( "dialog.reminder", null );
            }
        }
    };
    private final Action actionGamePause = new ConfiguredAction( this.config, "game.pause" )
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( universe != null )
                universe.setPaused( true );

            actionGameStart.setEnabled( true );
            actionGamePause.setEnabled( false );
        }
    };
    private final Action actionGameStop = new ConfiguredAction( this.config, "game.stop" )
    {
        public void actionPerformed( ActionEvent event )
        {
            actionGamePause.actionPerformed( null );
            actionGameStop.setEnabled( false );
            universe = null;
            model.init( universe );
        }
    };
    private final Action actionAbout = new ConfiguredAction( this.config, "about" )
    {
        public void actionPerformed( ActionEvent event )
        {
            showMessageDialog( "dialog.about", null );
        }
    };

    private JFrame frame;
    private JToolBar toolbar;
    private Universe universe;
    private UniverseView view;
    private JLabel turns;

    public void run()
    {
        ImageHolder.init( this.config );
        String laf = this.config.getString( "look.and.feel" );
        if ( laf != null )
        {
            if ( laf.equals( "system" ) )
                laf = UIManager.getSystemLookAndFeelClassName();

            try
            {
                UIManager.setLookAndFeel( laf );
            }
            catch ( Exception exception )
            {
            }
        }
        this.actionGamePause.setEnabled( false );
        this.actionGameStop.setEnabled( false );
        getFrame().setVisible( true );
    }

    public int getListenPort()
    {
        return 8889;
    }

    public void setListenPort(int port)
    {
        assert port == 8889;
    }


    public void redraw()
    {
        this.model.fireTableDataChanged();
        getUniverseView().repaint();
        getTurnsLabel().setText( getTurnsText() );
    }

    public void notifyAboutCompletion( GameResult gr )
    {
        this.actionGameStop.actionPerformed( null );
        showMessageDialog( "dialog.game.result",
                           gr.toString().replace( "\t", "   " ) );
    }

    public void notifyOnTurnEnd() {}
    
    public void stateChanged( ChangeEvent event )
    {
        Object source = event.getSource();
        if ( source instanceof JSlider )
        {
            JSlider slider = ( JSlider )source;
            int value = slider.getValue();
            if ( value > 0 )
            {
                gi.turnDelay = 0;
                Constants.setRefreshTurns( value / 10 + 1 );
            }
            else
            {
                gi.turnDelay = -value;
                Constants.setRefreshTurns( 1 );
            }
        }
    }

    private JMenuBar createMenuBar()
    {
        JMenuBar menu = new JMenuBar();
        menu.add( createMenuFile() );
        menu.add( createMenuView() );
        menu.add( createMenuGame() );
        menu.add( createMenuHelp() );
        return menu;
    }

    private JMenu createMenuFile()
    {
        JMenu menu = new JMenu( this.config.getText( "menu.file" ) );
        menu.add( new JMenuItem( this.actionAddEntity ) );
        menu.addSeparator();
        menu.add( new JMenuItem( this.actionExit ) );
        return menu;
    }

    private JMenu createMenuView()
    {
        JMenuItem item = new JCheckBoxMenuItem( this.actionShowToolBar );
        item.setSelected( true );

        JMenu menu = new JMenu( this.config.getText( "menu.view" ) );
        menu.add( item );
        menu.addSeparator();
        menu.add( new JCheckBoxMenuItem( this.actionShowGrid ) );
        menu.add( new JCheckBoxMenuItem( this.actionShowId ) );
        menu.addSeparator();
        menu.add( new JMenuItem( this.actionZoomIn ) );
        menu.add( new JMenuItem( this.actionZoomOut ) );
        return menu;
    }

    private JMenu createMenuGame()
    {
        JMenu menu = new JMenu( this.config.getText( "menu.game" ) );
        menu.add( new JMenuItem( this.actionGameStart ) );
        menu.add( new JMenuItem( this.actionGamePause ) );
        menu.add( new JMenuItem( this.actionGameStop ) );
        return menu;
    }

    private JMenu createMenuHelp()
    {
        JMenu menu = new JMenu( this.config.getText( "menu.help" ) );
        menu.add( new JMenuItem( this.actionAbout ) );
        return menu;
    }

    private static JPanel createInvisiblePanel()
    {
        // disable horizontal toolbar
        JPanel panel = new JPanel();
        panel.setVisible( false );
        return panel;
    }

    private JSlider createSlider()
    {
        int min = this.config.getInteger( "slider.speed.min" );
        int max = this.config.getInteger( "slider.speed.max" );

        JSlider slider = new JSlider( min, max, -Constants.getTurnDelay() );
        slider.addChangeListener( this );
        return slider;
    }

    private JScrollPane createTable()
    {
        new Config( "beings" ).addEntitiesTo( this.model );

        JTable table = new JTable( this.model );
        table.setRowSelectionAllowed( false );
        table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        table.setShowVerticalLines( false );

        Integer height = this.config.getInteger( "table.rows.height" );
        if ( height != null )
            table.setRowHeight( height );

        TableColumnModel model = table.getColumnModel();
        int count = model.getColumnCount();
        for ( int i = 0; i < count; i++ )
        {
            Integer width = this.config.getInteger( "table.column" + i + ".width" );
            if ( width != null )
                model.getColumn( i ).setPreferredWidth( width );
        }

        // set preferred height to preferred width
        Dimension size = table.getPreferredSize();
        size.height = size.width;

        // set preferred size for scroll table
        JScrollPane panel = new JScrollPane( table );
        panel.setPreferredSize( size );
        return panel;
    }

    public JFrame getFrame()
    {
        if ( this.frame == null )
        {
            String prefix = "frame.main";
            String title = this.config.getTitle( prefix );
            Image image = this.config.getImage( prefix );
            this.frame = new JFrame( title );
            this.frame.setIconImage( image );
            this.frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
            this.frame.setJMenuBar( createMenuBar() );
            this.frame.add( BorderLayout.EAST, getToolBar() );
            this.frame.add( BorderLayout.CENTER, getUniverseView() );
            this.frame.add( BorderLayout.NORTH, createInvisiblePanel() );
            this.frame.add( BorderLayout.SOUTH, createInvisiblePanel() );
            this.frame.pack();
            this.frame.setLocationRelativeTo( null );
        }
        return this.frame;
    }

    private JToolBar getToolBar()
    {
        if ( this.toolbar == null )
        {
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints gbc = new GridBagConstraints();

            this.toolbar = new JToolBar( SwingConstants.VERTICAL );
            this.toolbar.setName( this.config.getTitle( "toolbar.main" ) );
            this.toolbar.setLayout( layout );

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 4;
            gbc.insets.top = 2;
            gbc.insets.left = 2;
            gbc.insets.right = 2;
            gbc.insets.bottom = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            this.toolbar.add( getTurnsLabel(), gbc );

            gbc.gridy = 2;
            this.toolbar.add( createSlider(), gbc );

            gbc.gridy = 3;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            layout.addLayoutComponent(
                    this.toolbar.add( this.actionGameStart ), gbc );

            gbc.gridx = 2;
            layout.addLayoutComponent(
                    this.toolbar.add( this.actionGamePause ), gbc );

            gbc.gridx = 3;
            layout.addLayoutComponent(
                    this.toolbar.add( this.actionGameStop ), gbc );

            gbc.gridx = 4;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.EAST;
            layout.addLayoutComponent(
                    this.toolbar.add( this.actionAddEntity ), gbc );

            gbc.gridx = 1;
            gbc.gridy = 4;
            gbc.gridwidth = 4;
            gbc.weightx = 0.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            this.toolbar.add( createTable(), gbc );
        }
        return this.toolbar;
    }

    private UniverseView getUniverseView()
    {
        if ( this.view == null )
        {
            String entity = this.config.getFormat( "view.entity" );
            String resource = this.config.getFormat( "view.resource" );

            this.view = new UniverseView( entity, resource );
            this.view.setBorder( BorderFactory.createEtchedBorder() );

            Integer width = this.config.getInteger( "panel.view.width" );
            Integer height = this.config.getInteger( "panel.view.height" );
            if ( ( width != null ) && ( height != null ) )
                this.view.setPreferredSize( new Dimension( width, height ) );
        }
        return this.view;
    }

    private JLabel getTurnsLabel()
    {
        if ( this.turns == null )
            this.turns = new JLabel( getTurnsText() );

        return this.turns;
    }

    private String getTurnsText()
    {
        int num = ( this.universe != null )
                ? this.universe.numTurns()
                : 0;

        return ( this.formatTurn == null )
                ? Integer.toString( num )
                : String.format(
                        this.formatTurn,
                        Integer.valueOf( num ),
                        Integer.valueOf( this.gi.maxTurns ) );
    }

    private void showMessageDialog( String prefix, String message )
    {
        String title = this.config.getTitle( prefix );
        String text = this.config.getText( prefix );
        if ( message != null )
            text = text + '\n' + message;

        JOptionPane.showMessageDialog( getFrame(), text, title, INFORMATION_MESSAGE );
    }
}
