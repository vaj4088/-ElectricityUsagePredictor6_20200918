package eup;

import javax.swing.JComponent ;
import java.awt.Color ;

public interface Feedbacker {

    public static final int TO_GUI = 1; 
    public static final int TO_FILE = 2;
    public static final int TO_OUT = 4;
    public static final int FLUSH = 8;

    
    // Default logs to GUI and to file.
    // Adds line termination.
    public void log(final Object ob) ;

    // Logs to place determined by adding constants.
    // Adds line termination.
    public void log(final Object ob, int where) ;
    
    // Logs to place determined by adding constants.
    // Does NOT add line termination.
    public void logBare(final Object ob, int where) ;

    // Default:
    // If enable is true, set the progress bar as indeterminate.
    // If enable is false, set the progress bar to 0.
    // Use the String info as a message on the progress bar.   
    public void progressAnnounce(final boolean enable,
	    final String info) ;

    // Default:
    // If enable is true, set the progress bar as indeterminate.
    // If enable is false, set the progress bar to 0.
    // No message on the progress bar.   
    public void progressAnnounce(final boolean enable) ;

    // Set the progress bar to the value specified (0 to 1000), and
    // use the String info as a message on the progress bar.
    public void progressAnnounce(final float perThousand, final String info);
    
    // Return the progress bar.
    public JComponent getProgressBar();
    
    // Default sets the foreground color of the progress bar 
    // to the designated color, and
    // returns it with the designated color.
    public JComponent getProgressBar(final Color color) ;

    public JComponent getOperationsLog();
    
    public void activityAnnounce(
                                 final int currentValue, 
                                 final String info, 
                                 final int maxValue
                                ) ;
    
}