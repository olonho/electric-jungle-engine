package universum.ui.sam;

import universum.bi.Being;
import universum.engine.ScoreData;
import universum.engine.Universe;

import javax.swing.ImageIcon;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
final class EntityTableRow
{
    private final String name;
    private final String file;

    private String owner;
    private Universe universe;
    private boolean selected;

    EntityTableRow( String name, String file )
    {
        this.name = name;
        this.file = file;

        this.owner = name.substring( 1 + name.lastIndexOf( '.' ) );
    }

    public boolean init( Universe universe )
    {
        this.universe = universe;
        if ( !this.selected )
            return false;

        Being being = universe.addBeing( this.name, this.file, null );
        if ( being == null )
            return false;

        this.owner = being.getOwnerName();
        return true;
    }

    public ImageIcon getImage()
    {
        ScoreData data = getScoreData();
        return ( data != null )
                ? ImageHolder.getEntity( data.id )
                : null;
    }

    public String getOwner()
    {
        return this.owner;
    }

    public Integer getMass()
    {
        ScoreData data = getScoreData();
        return ( data != null )
                ? Integer.valueOf( ( int )data.score )
                : null;
    }

    public Integer getEnergy()
    {
        ScoreData data = getScoreData();
        return ( data != null )
                ? Integer.valueOf( ( int )data.energy )
                : null;
    }

    public boolean isSelected()
    {
        ScoreData data = getScoreData();
        if ( data != null )
            if ( data.energy <= 0.0f )
                this.selected = false;

        return this.selected;
    }

    public void setSelected( boolean selected )
    {
        this.selected = selected;
    }

    private ScoreData getScoreData()
    {
        return ( this.universe != null )
                ? this.universe.scores().get( this.owner )
                : null;
    }
}
