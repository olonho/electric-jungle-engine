package universum.ui.sam;

import universum.bi.Constants;

import java.awt.Color;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
class ResourceColor
{
    private static final int MAX_COLOR = 255;
    private static final Color[] COLOR = new Color[MAX_COLOR + 1];

    private ResourceColor()
    {
    }

    static Color getColor( float energy )
    {
        if ( energy > Constants.MAX_REGULAR )
            return Color.RED;

        energy *= ( float )MAX_COLOR;
        energy /= Constants.MAX_REGULAR;

        int index = ( int )energy;
        if ( COLOR[index] == null )
            COLOR[index] = new Color( MAX_COLOR, MAX_COLOR, MAX_COLOR - index );

        return COLOR[index];
    }
}