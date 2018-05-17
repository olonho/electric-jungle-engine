package universum.ui.sam;

import universum.bi.Constants;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
class ImageHolder
{
    private static final List<ImageIcon> entity = new ArrayList<ImageIcon>();
    private static final List<ImageIcon> resource = new ArrayList<ImageIcon>();

    private ImageHolder()
    {
    }

    static void init( Config config )
    {
        init( config, "entity", entity );
        init( config, "resource", resource );
    }

    private static void init( Config config, String prefix, List<ImageIcon> list )
    {
        for ( int i = 0; ; i++ )
        {
            ImageIcon icon = config.getImageIcon( prefix + i + ".image" );
            if ( icon == null )
                break;

            list.add( icon );
        }
    }

    static ImageIcon getEntity( int id )
    {
        if ( entity.isEmpty() || ( id < 0 ) )
            return null;

        return entity.get( id % entity.size() );
    }

    static ImageIcon getResource( float energy )
    {
        int max = resource.size() - 1;
        if ( ( max < 0 ) || ( energy <= 0.0F ) )
            return null;

        if ( energy >= Constants.MAX_REGULAR )
            return resource.get( 0 );

        if ( max <= 0 )
            return null;

        energy *= ( float )max / Constants.MAX_REGULAR;
        return resource.get( max - ( int )energy );
    }
}