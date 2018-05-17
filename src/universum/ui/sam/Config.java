package universum.ui.sam;

import java.awt.Image;
import java.net.URL;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import universum.Main;
import universum.util.Util;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
public final class Config
{
    private final ResourceBundle bundle;

    public Config( String name )
    {
        this.bundle = Util.findBundle( name );
    }

    public void addEntitiesTo( EntityTableModel model )
    {
        Enumeration<String> keys = this.bundle.getKeys();
        while ( keys.hasMoreElements() )
        {
            String name = keys.nextElement();
            String value = getString( name );
            if ( value != null )
            {
                String[] array = value.split( "\\|" );
                if ( ( 2 < array.length ) && !"".equals( array[2] ) )
                {
                    System.out.println( "Making being: " + name );
                    Main.makeBeing( array[2] );
                }
                value = array[0];
            }
            model.addEntity( name, Main.beingPath( value ) );
        }
    }

    public String getFormat( String prefix )
    {
        return getString( prefix + ".format" );
    }

    public Icon getIcon( String prefix )
    {
        return getImageIcon( prefix + ".icon" );
    }

    public Integer getInteger( String key )
    {
        String value = getString( key );
        if ( value == null )
            return null;

        try
        {
            return Integer.valueOf( value );
        }
        catch ( NumberFormatException exception )
        {
            System.err.println( exception.getMessage() );
            return null;
        }
    }

    public Image getImage( String prefix )
    {
        ImageIcon image = getImageIcon( prefix + ".image" );
        return ( image != null )
                ? image.getImage()
                : null;
    }

    public ImageIcon getImageIcon( String key )
    {
        String name = getString( key );
        if ( name == null )
            return null;

        URL url = Util.findResource(name );
        if ( url == null )
            return null;

        return new ImageIcon( url );
    }

    public String getString( String key )
    {
        try
        {
            return this.bundle.getString( key );
        }
        catch ( MissingResourceException exception )
        {
            System.err.println( exception.getMessage() );
            return null;
        }
    }

    public String getText( String prefix )
    {
        return getString( prefix + ".text" );
    }

    public String getTitle( String prefix )
    {
        return getString( prefix + ".title" );
    }

    public String getTooltip( String prefix )
    {
        return getString( prefix + ".tooltip" );
    }
}
