/**
 * 
 */
package eup;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * @author Ian Shef
 *
 */
public class ProgressBarLimited 
extends JComponent 
{
    
    private static float STROKEWIDTHFLOAT = 2.0F ;
    private static int   STROKEWIDTHINT   = Math.round(STROKEWIDTHFLOAT) ;
    private static Stroke STROKE = new BasicStroke(STROKEWIDTHFLOAT) ;
    private static int BRIGHTNESS = 160 ;
    private static Paint GRAYLINE = 
	    new Color(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS) ;
 
    private static int OUTERX =  2 ;
    private static int OUTERY = 10 ;
    private static int OUTERHEIGHT = 10;
    
    private static int INNERX = OUTERX + STROKEWIDTHINT - 1 ;
    private static int INNERY = OUTERY + STROKEWIDTHINT - 1 ;
    private static int INNERHEIGHT = OUTERHEIGHT - STROKEWIDTHINT ;
    
    private static final float POSITIONS[] = {
	    0.0F, 
	    0.5F, 
	    1.0F
	    } ;
    private static final Color COLORS[]    = {
//	    Color.ORANGE, 
	    Color.MAGENTA,
	    Color.YELLOW,
	    Color.GREEN
    } ;
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L ;
    
    private int minimum ;
    private int value ;
    private int maximum ;
    
    private int width  = 0 ;
    
    private Toolkit tk = Toolkit.getDefaultToolkit() ;
 
    public ProgressBarLimited() {
	initProgressBarLimited(  0, 100) ;
    }
    
    public ProgressBarLimited(int min, int max) {
	initProgressBarLimited(min, max) ;
    }
    
    private void initProgressBarLimited(int min, int max) {
	minimum = min ;
	maximum = max ;
	setMinimumSize(  new Dimension(              100,  22)) ;
	setPreferredSize(new Dimension(              500,  22)) ;
	setMaximumSize(  new Dimension(Integer.MAX_VALUE, 100)) ;
    }
    
    /**
     * @param b unused 
     */
    public void setIndeterminate(boolean b) {
	// Do nothing.
    }
    /**
     * @param b unused 
     */
    public void setStringPainted(boolean b) {
	// Do nothing.
    }
    
    private void setValueHelper(int v) {
	value = constrain(minimum, v, maximum) ;
	repaint() ;  // We changed the painting of the bar...  
		     // Request to paint the bar.
	tk.sync() ;  // Get the bar painted right away so it is not jumpy.
    }
    
    public void setValue(int v) {
	if (SwingUtilities.isEventDispatchThread()) {
	    setValueHelper(v) ;
	} else {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    setValueHelper(v) ;
		}
	    }) ;
	}
    }
    
    private int constrain(int lowerLimit, int x, int upperLimit) {
	return Math.min(Math.max(x,  lowerLimit), upperLimit) ;
    }

    @Override
    protected void paintComponent(final Graphics g) {
	super.paintComponent(g) ;
	Insets in = getInsets() ;
	width  = getWidth()  ;
	Rectangle2D.Float r2DFOuter = new Rectangle2D.Float( 
		OUTERX + in.left,
		OUTERY + in.top,
		width  - 4 - in.right - in.left,
		OUTERHEIGHT -in.bottom
		) ;
//	Rectangle2D.Float r2DFInner = new Rectangle2D.Float(
//		INNERX + in.left,
//		INNERY + in.top,
//		width - 4 - in.right - in.left,
//		INNERHEIGHT -in.bottom
//		 ) ;
//	//
	// x11, y1, x2, y2, fractions, colors
	//
	Paint AREAFILL = new LinearGradientPaint(
		INNERX + in.left,
		INNERY + in.top,
		width  - 4 - in.right - in.left,
		INNERY - in.bottom,
		POSITIONS,
		COLORS
		) ;
	//
	// Get a temporary Graphics2D.
	//
	Graphics2D gr = (Graphics2D)g.create() ;
	//
	// Paint the background of the bar.
	//
	gr.setColor(Color.LIGHT_GRAY) ;
//	gr.fill(r2DFInner) ;
	gr.fill(r2DFOuter) ;
	//
	// Paint the border of the bar.
	//
	gr.setStroke(STROKE) ;
	gr.setPaint(GRAYLINE) ;
	gr.draw(r2DFOuter) ;
	//
	// Paint the interior of the bar.
	//
	gr.setPaint(AREAFILL) ;
	int tmpWidth = width - in.right - in.left - INNERX - 3 ;
	tmpWidth *= (float)value / (maximum - minimum) ;
	gr.setClip(INNERX + in.left,
		INNERY + in.top,
		tmpWidth,
		INNERHEIGHT);
//	gr.fill(r2DFInner) ;
	gr.fill(r2DFOuter) ;
	//
	// Done drawing, dispose of the created Graphics2D.
	//
	gr.dispose() ;
    } 

    /**
     * @param args
     */
    public static void main(String[] args) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
	        JFrame f = new JFrame("TEST") ;
	        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE) ;
	        Box b = Box.createVerticalBox() ;
	        f.add(b) ;
	        Feedbacker fb = new FeedbackerImplementation() ;
	        fb.activityAnnounce(500, "This also is a test.", 600) ;
	        ProgressBarLimited pbl1 = new ProgressBarLimited( 0, 1000) ;
	        ProgressBarLimited pbl2 = new ProgressBarLimited( 0, 1000) ;
	        b.add(pbl1) ;
	        b.add(fb.getProgressBar()) ;
	        b.add(pbl2) ;
	        pbl1.setValue(500) ;
	        pbl2.setValue(600) ;
	        f.setSize(750,700) ;  // width, height including bar at top.
	        f.setVisible(true) ;
	    }
	});
    }
}
