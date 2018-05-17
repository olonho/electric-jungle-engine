package universum.ui.sam;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * TODO:description
 *
 * @author Sergey A. Malenkov
 */
abstract class ConfiguredAction
        extends AbstractAction
{
    protected ConfiguredAction( Config config, String name )
    {
        String prefix = "action." + name;
        putValue( Action.NAME, config.getText( prefix ) );
        putValue( Action.SHORT_DESCRIPTION, config.getTooltip( prefix ) );
        putValue( Action.SMALL_ICON, config.getIcon( prefix ) );
    }
}