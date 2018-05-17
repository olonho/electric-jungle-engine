package universum.ui.sam;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import static javax.swing.JFileChooser.APPROVE_OPTION;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
final class AddEntityDialog
        extends FileFilter
{
    private static final String PREFIX = "dialog.add.entity";

    private final EntityTableModel model;
    private final String title;
    private final String text;
    private final String ext;

    private JFileChooser chooser;

    AddEntityDialog( Config config, EntityTableModel model )
    {
        this.model = model;
        this.title = config.getTitle( PREFIX );
        this.text = config.getText( PREFIX );
        this.ext = config.getString( PREFIX + ".ext" );
    }

    public void show( JFrame parent )
    {
        if ( this.chooser == null )
        {
            this.chooser = new JFileChooser( "." );
            this.chooser.addChoosableFileFilter( this );
            this.chooser.setAcceptAllFileFilterUsed( false );
            this.chooser.setDialogTitle( this.title );
            this.chooser.setFileHidingEnabled( true );
            this.chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
            this.chooser.setMultiSelectionEnabled( true );
        }
        if ( APPROVE_OPTION == this.chooser.showOpenDialog( parent ) )
            for ( File file : this.chooser.getSelectedFiles() )
                parse( file );
    }

    @Override
    public boolean accept( File file )
    {
        if ( file.isDirectory() )
            return true;

        String name = file.getName();
        int index = name.lastIndexOf( '.' );
        return ( 0 <= index ) && name.substring( index ).equalsIgnoreCase( this.ext );
    }

    @Override
    public String getDescription()
    {
        return this.text;
    }

    private void parse( File file )
    {
        JarFile jar=null;
        try
        {
            jar = new JarFile( file );

            Manifest manifest = jar.getManifest();
            if ( manifest == null )
                throw new IOException( "Java Archive does not contain Manifest file" );

            String name = manifest.getMainAttributes().getValue( Attributes.Name.MAIN_CLASS );
            if ( ( name == null ) || ( 0 == name.length() ) )
                throw new IOException( "Manifest file does not contain Main-Class attribute" );

            this.model.addEntity( name, file.toString() );
        }
        catch ( IOException exception )
        {
            log( file, exception.getMessage() );
        }
        finally
        {
            if ( jar != null )
                try
                {
                    jar.close();
                }
                catch ( IOException exception )
                {
                    log( file, exception.getMessage() );
                }
        }
    }

    private static void log( File file, String message )
    {
        System.out.println( file + ": " + message );
    }
}