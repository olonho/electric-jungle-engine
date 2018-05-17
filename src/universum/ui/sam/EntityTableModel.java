package universum.ui.sam;

import universum.engine.Universe;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
final class EntityTableModel
        extends AbstractTableModel
{
    private final List<String> columns = new ArrayList<String>();
    private final List<EntityTableRow> rows = new ArrayList<EntityTableRow>();

    private Universe universe;

    EntityTableModel( Config config )
    {
        Integer count = config.getInteger( "table.columns.count" );
        if ( count != null )
            for ( int i = 0; i < count; i++ )
                this.columns.add( config.getText( "table.column" + i ) );
    }

    public boolean init( Universe universe )
    {
        this.universe = universe;
        if ( universe == null )
            return false;

        boolean initialized = false;
        for ( EntityTableRow entity : this.rows )
            initialized |= entity.init( universe );

        fireTableDataChanged();
        return initialized;
    }

    public void addEntity( String name, String file )
    {
        if ( name != null )
        {
            int row = this.rows.size();
            this.rows.add( new EntityTableRow( name, file ) );
            fireTableRowsInserted( row, row );
        }
    }

    public int getRowCount()
    {
        return this.rows.size();
    }

    public int getColumnCount()
    {
        return this.columns.size();
    }

    @Override
    public String getColumnName( int column )
    {
        return this.columns.get( column );
    }

    @Override
    public Class<?> getColumnClass( int column )
    {
        if ( column == 0 )
            return Boolean.class;

        if ( column == 1 )
            return ImageIcon.class;

        if ( column == 2 )
            return String.class;

        return Integer.class;
    }

    @Override
    public boolean isCellEditable( int row, int column )
    {
        if ( column != 0 )
            return false;

        if ( this.universe == null )
            return true;

        return !this.rows.get( row ).isSelected();
    }

    public Object getValueAt( int row, int column )
    {
        EntityTableRow entity = this.rows.get( row );

        if ( column == 0 )
            return Boolean.valueOf( entity.isSelected() );

        if ( column == 1 )
            return entity.getImage();

        if ( column == 2 )
            return entity.getOwner();

        return ( column == 3 )
                ? entity.getMass()
                : entity.getEnergy();
    }

    @Override
    public void setValueAt( Object value, int row, int column )
    {
        if ( column == 0 )
        {
            EntityTableRow entity = this.rows.get( row );
            boolean selected = Boolean.TRUE.equals( value );
            if ( this.universe == null )
            {
                entity.setSelected( selected );
            }
            else if ( selected )
            {
                entity.setSelected( true );
                entity.init( this.universe );
                fireTableRowsUpdated( row, row );
            }
        }
    }
}