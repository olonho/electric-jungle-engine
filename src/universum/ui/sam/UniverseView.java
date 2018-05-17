package universum.ui.sam;

import universum.bi.Location;
import universum.engine.Universe;
import universum.engine.LocationUtil;
import universum.util.Walker;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
final class UniverseView
        extends JComponent
        implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    private static final int MAX_ZOOM = 32;
    private static final int MIN_ZOOM = 4;

    private Universe universe;

    private int width;
    private int height;
    private int zoom = MIN_ZOOM;

    private boolean showGrid;
    private boolean showId;

    private final Point point = new Point();
    private final Point mouse = new Point();

    private final String entity;
    private final String resource;

    UniverseView( String entity, String resource )
    {
        this.entity = entity;
        this.resource = resource;
        setDoubleBuffered( true );
        addComponentListener( this );
        addMouseListener( this );
        addMouseMotionListener( this );
        addMouseWheelListener( this );
    }

    public void init( Universe universe )
    {
        List<Integer> list = universe.diameters();
        if ( 2 == list.size() )
        {
            if ( this.universe != null )
                this.universe.apocalypse();

            this.universe = universe;
            this.width = list.get( 0 );
            this.height = list.get( 1 );
            updateZoom( 0 );
            repaint();
        }
    }

    public void showGrid( boolean showGrid )
    {
        if ( this.showGrid != showGrid )
        {
            this.showGrid = showGrid;
            repaint();
        }
    }

    public void showId( boolean showId )
    {
        if ( this.showId != showId )
        {
            this.showId = showId;
            repaint();
        }
    }

    public void updateZoom( int zoom )
    {
        int width = getWidth();
        int height = getHeight();
        updateZoom( zoom, width >> 1, height >> 1, width, height );
    }

    private void updateZoom( int zoom, int oldX, int oldY, int width, int height )
    {
        float x = ( float )( oldX - this.point.x ) / ( float )this.zoom;
        float y = ( float )( oldY - this.point.y ) / ( float )this.zoom;

        zoom += this.zoom;
        if ( zoom > MAX_ZOOM )
            zoom = MAX_ZOOM;

        zoom = calcZoom( zoom, this.width, width );
        zoom = calcZoom( zoom, this.height, height );
        if ( zoom < MIN_ZOOM )
            zoom = MIN_ZOOM;

        if ( this.zoom != zoom )
        {
            this.zoom = zoom;

            int newX = ( int )( ( float )this.point.x + ( float )this.zoom * x );
            int newY = ( int )( ( float )this.point.y + ( float )this.zoom * y );

            updatePoint( oldX - newX, oldY - newY );
            repaint();
        }
    }

    private static int calcZoom( int value, int size, int max )
    {
        if ( size <= 0 )
            return value;

        size = 1 + max / size;
        return ( size <= value )
                ? value
                : size;
    }

    private void updatePoint( int x, int y )
    {
        if ( ( this.zoom > 0 ) && ( this.width > 0 ) && ( this.height > 0 ) )
        {
            x = calcPoint( this.point.x + x, this.zoom * this.width );
            y = calcPoint( this.point.y + y, this.zoom * this.height );
            if ( ( this.point.x != x ) || ( this.point.y != y ) )
            {
                this.point.x = x;
                this.point.y = y;
                repaint();
            }
        }
    }

    private static int calcPoint( int value, int size )
    {
        while ( value <= -size )
            value += size;

        while ( value > 0 )
            value -= size;

        return value;
    }

    private void paint( Graphics g, ImageIcon icon, int x, int y, int w, int h )
    {
        Image image = icon.getImage();
        g.drawImage( image, x, y, this.zoom, this.zoom, this );
        g.drawImage( image, x + w, y, this.zoom, this.zoom, this );
        g.drawImage( image, x, y + h, this.zoom, this.zoom, this );
        g.drawImage( image, x + w, y + h, this.zoom, this.zoom, this );
    }

    @Override @SuppressWarnings("deprecation") 
    protected void paintComponent( final Graphics g )
    {
        int width = getWidth();
        int height = getHeight();

        g.setColor( Color.WHITE );
        g.fillRect( 0, 0, width, height );

        if ( 0 < this.zoom )
        {
            if ( this.universe != null )
            {
                this.universe.forAllR( new Walker<Location>()
                {
                    public void walk( Location location )
                    {
                        int x = point.x + zoom * location.getX();
                        int y = point.y + zoom * location.getY();
                        int w = zoom * UniverseView.this.width;
                        int h = zoom * UniverseView.this.height;

                        float energy = universe.getResourceCount( location );
                        ImageIcon icon = ImageHolder.getResource( energy );
                        if ( icon != null )
                        {
                            paint( g, icon, x, y, w, h );
                        }
                        else
                        {
                            g.setColor( ResourceColor.getColor( energy ) );
                            g.fillRect( x, y, zoom, zoom );
                            g.fillRect( x + w, y, zoom, zoom );
                            g.fillRect( x, y + h, zoom, zoom );
                            g.fillRect( x + w, y + h, zoom, zoom );
                        }
                    }
                } );
                this.universe.forAllE( new Walker<Integer>()
                {
                    public void walk( Integer id )
                    {
                        Location location = universe.getLocation( id );

                        int x = point.x + zoom * location.getX();
                        int y = point.y + zoom * location.getY();
                        int w = zoom * UniverseView.this.width;
                        int h = zoom * UniverseView.this.height;

                        ImageIcon icon = !showId
                                ? ImageHolder.getEntity( universe.getType( id ) )
                                : null;

                        if ( icon != null )
                        {
                            paint( g, icon, x, y, w, h );
                        }
                        else
                        {
                            String name = showId
                                    ? id.toString()
                                    : universe.getOwner( id );

                            Rectangle2D rectangle = g.getFontMetrics().getStringBounds( name, g );

                            int offset = zoom >> 1;
                            x += offset - ( int )rectangle.getCenterX();
                            y += offset - ( int )rectangle.getCenterY();

                            g.setColor( Color.BLACK );
                            g.drawString( name, x, y );
                            g.drawString( name, x + w, y );
                            g.drawString( name, x, y + h );
                            g.drawString( name, x + w, y + h );
                        }
                    }
                } );
            }
            if ( this.showGrid )
            {
                g.setColor( Color.BLACK );

                for ( int x = this.point.x; x < width; x += this.zoom )
                    g.drawLine( x, 0, x, height );

                for ( int y = this.point.y; y < height; y += this.zoom )
                    g.drawLine( 0, y, width, y );
            }
        }
    }

    public void componentResized( ComponentEvent event )
    {
        updateZoom( 0 );
        repaint();
    }

    public void componentMoved( ComponentEvent event )
    {
    }

    public void componentShown( ComponentEvent event )
    {
    }

    public void componentHidden( ComponentEvent event )
    {
    }

    public void mouseClicked( MouseEvent event )
    {
    }

    public void mousePressed( MouseEvent event )
    {
        this.mouse.x = event.getX();
        this.mouse.y = event.getY();
    }

    public void mouseReleased( MouseEvent event )
    {
    }

    public void mouseEntered( MouseEvent event )
    {
    }

    public void mouseExited( MouseEvent event )
    {
    }

    public void mouseDragged( MouseEvent event )
    {
        int x = event.getX();
        int y = event.getY();
        updatePoint( x - this.mouse.x, y - this.mouse.y );
        this.mouse.x = x;
        this.mouse.y = y;
    }

    public void mouseMoved( MouseEvent event )
    {
        final StringBuilder sb = new StringBuilder();
        if ( ( this.universe != null ) && ( this.zoom > 0 ) )
        {
            final Location location = LocationUtil.createLocation(
                    calcLocation( this.point.x, this.width, event.getX() ),
                    calcLocation( this.point.y, this.height, event.getY() ) );

            if ( this.resource != null )
            {
                float energy = this.universe.getResourceCount( location );
                if ( energy > 0.0f )
                    updateTooltip( sb, String.format(
                            this.resource,
                            Float.valueOf( energy ) ) );
            }
            if ( this.entity != null )
                this.universe.forAllE( new Walker<Integer>()
                {
                    public void walk( Integer id )
                    {
                        if ( location.equals( universe.getLocation( id ) ) )
                            updateTooltip( sb, String.format(
                                    entity,
                                    universe.getOwner( id ),
                                    id,
                                    Float.valueOf( universe.getEnergy( id ) ) ) );
                    }
                } );
        }
        setToolTipText( ( 0 < sb.length() )
                ? sb.toString()
                : null );
    }

    public void mouseWheelMoved( MouseWheelEvent event )
    {
        updateZoom( event.getWheelRotation(), event.getX(), event.getY(), getWidth(), getHeight() );
    }

    private int calcLocation( int value, int size, int pos )
    {
        pos -= value;
        pos /= this.zoom;
        return ( pos > size )
                ? pos - size
                : pos;
    }

    private static void updateTooltip( StringBuilder sb, String line )
    {
        sb.append( ( 0 < sb.length() )
                ? "<br>"
                : "<html>" );

        sb.append( line );
    }
}
