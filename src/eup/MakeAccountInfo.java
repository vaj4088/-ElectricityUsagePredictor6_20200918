/**
 * 
 */
package eup;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import eup.Info.PreferencesEnum;
/**
 * @author Ian Shef
 *
 */
public class MakeAccountInfo {
    JComponent menuItem = null ;
    final CountDownLatch maiCdl = new CountDownLatch(1) ;
//    private static enum EXECUTE { 
//	CANCEL(0),
//	REPLACE(1), 
//	APPEND(2) ;
//
//	private final int value ;
//	EXECUTE(int value) {this.value = value ; }
//	@SuppressWarnings("unused")
//	int valueOf() { return value ; }
//    }   
    static final String spaces = "          " ;
    static final int horizontalStrutWidth = 10 ;
    private static final int GRAY = 230 ;
    static final Color MAIGRAY = new Color( GRAY, GRAY, GRAY) ;
    MAIJLabel warning = new MAIJLabel() ;
    MAIJButton done = new MAIJButton(spaces) ;

    SpinnerNumberModel greenModelStart = 
	    /* value, minimum, maximum, step size  */
	    new SpinnerNumberModel(
		    ((Integer)Info.PreferencesEnum.
			    keyGreenZoneStart.getStoredValue()).intValue(),
		    0,
		    ((Integer)Info.PreferencesEnum.
			    keyGreenZoneEnd.getStoredValue()).intValue(),
		    1) ;
//	    new SpinnerNumberModel(1000, 0, 9_999, 1) ;
    SpinnerNumberModel greenModelEnd = 
	    new SpinnerNumberModel(
		    ((Integer)Info.PreferencesEnum.
			    keyGreenZoneEnd.getStoredValue()).intValue(),
		    ((Integer)Info.PreferencesEnum.
			    keyGreenZoneStart.getStoredValue()).intValue(),
		    9_999,
		    1) ;
//	    new SpinnerNumberModel(2000, 1000, 9_999, 1) ;

    final MAIValue field[] = {
	    new MAIJTextField((String)
		    Info.PreferencesEnum.keyUserID.getStoredValue()),
	    new MAIJPasswordField((String)
		    Info.PreferencesEnum.keyPassword.getStoredValue()),
	    new MAIJSpinner(greenModelStart),
	    new MAIJSpinner(greenModelEnd),
    } ;

    {
	DocumentListener changes = new DocumentListener() {
	    @Override
	    public void changedUpdate(DocumentEvent arg0) {
		warnAboutMissingInput() ;
	    }
	    @Override
	    public void insertUpdate(DocumentEvent arg0) {
		warnAboutMissingInput() ;
	    }
	    @Override
	    public void removeUpdate(DocumentEvent arg0) {
		warnAboutMissingInput() ;
	    }
	} ;
	((JTextComponent)field[0]).getDocument().addDocumentListener(changes) ;
	((JTextComponent)field[1]).getDocument().addDocumentListener(changes) ;
    }

    int buttonResult ;
    
    MakeAccountInfo() {
	new Info() ;
    }
    
    void getAndStoreAccountInfo(JComponent c) {
	getAndStoreAccountInfo() ;
	menuItem = c ;
    }

    private void getAndStoreAccountInfo() {
	Runnable info = new Runnable() {
	    @Override
	    public void run() {
		JFrame jf = new JFrame("Info") ;
		Box jFrame = Box.createVerticalBox() ;
		jf.add(jFrame) ;
		jf.addWindowListener(new WindowAdapter() 
		{
		    @Override
		    public void windowClosing(WindowEvent e) {
			if (menuItem != null) {
			    menuItem.setEnabled(true) ;
			}
			jf.dispose() ;
		    }
		});
		greenModelStart.addChangeListener(new ChangeListener() {
		    @SuppressWarnings("boxing")
		    @Override
		    public void stateChanged(ChangeEvent e) {
			Integer gmsValue = 
				(Integer)greenModelStart.getValue() ;
			greenModelEnd.setMinimum(gmsValue.intValue()) ;
		    }
		});
		greenModelEnd.addChangeListener(new ChangeListener() {
		    @SuppressWarnings("boxing")
		    @Override
		    public void stateChanged(ChangeEvent e) {
			Integer gmeValue = 
				(Integer)greenModelEnd.getValue() ;
			greenModelStart.setMaximum(gmeValue.intValue()) ;
		    }
		});

		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("Account Info")
			.component(new MAIBoxHorizontal())
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label(" ")
			.component(new MAIBoxHorizontal())
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("")
			.component(new MAIBoxHorizontal())
			.build()
			) ;
		warning.setAlignmentX(Component.CENTER_ALIGNMENT) ;
		warning.setOpaque(true) ;
		warning.setBackground(Color.WHITE) ;
		setWarning("Enter all parameters", Color.RED) ;

		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("")
			.component(warning)
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("User ID       ")
			.component(field[0])
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("Password ")
			.component(field[1])
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("Green Zone Start ")
			.component(field[2])
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label("Green Zone End   ")
			.component(field[3])
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label(" ")
			.component(new MAIBoxHorizontal())
			.build()
			) ;
		jFrame.add(
			(new AccountInfoItem.Builder())
			.label(" ")
			.component(done)
			.build()
			) ;
		done.setOpaque(true) ;
		done.setForeground(Color.GREEN.darker()) ;
		done.setBackground(MAIGRAY) ;
		done.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12) ) ;
		done.setEnabled(false) ;
		done.addActionListener(
			new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
				int pei = 0 ;
				for (PreferencesEnum pe : 
				    PreferencesEnum.values()) {
				    pe.setValue(field[pei++].getMAIInfo()) ;
				    pe.storeValue() ;
				}
				/*
				 * Re-enable the menu item.
				 */
				if ( menuItem != null ) {
				    menuItem.setEnabled(true) ;
				}
				/*
				 * Go away by disposing of the frame.
				 */
				jf.dispose() ;
			    }
			}) ;
		warnAboutMissingInput() ;
		setCaretToEnd() ;
		jf.setLocationRelativeTo(null) ; // Centered.
		jf.setAlwaysOnTop(true) ;
		jf.pack() ;
		jf.setVisible(true);
	    }
	} ;
	
	if (SwingUtilities.isEventDispatchThread()) {
	    info.run() ;
	} else {
	    try {
		SwingUtilities.invokeAndWait(info) ;
	    } catch (InvocationTargetException
		    | InterruptedException e) {
		e.printStackTrace();
	    }
	} 
    }

    void setWarning(String s, Color color) {
	/*
	 * Color.RED
	 * or
	 * Color.GREEN.darker().darker()
	 */
	warning.setText(s) ;
	warning.setForeground(color) ;
    }

    void warnAboutMissingInput() {
	/*
	 * A successful test:
	 * 
	 * 	setWarning(
	 *                  String.valueOf(System.currentTimeMillis()), 
	 *                  Color.BLUE
	 *                 ) ;
	 *                  
	 */
	if ( (field[0]).getMAIInfo().length() == 0) {
	    setWarning("Provide the User ID", Color.RED) ;
	    done.setText(spaces) ;
	    done.setBackground(MAIGRAY) ;
	    done.setEnabled(false) ;
	} else {
	    if ( field[1].getMAIInfo().length() == 0) {
		setWarning("Provide the Password", Color.RED) ;
		done.setText(spaces) ;
		done.setBackground(MAIGRAY) ;
		done.setEnabled(false) ;
	    } else {
		setWarning("Looks good so far", Color.GREEN.darker().darker()) ;
		done.setText("Save and Done") ;
		done.setBackground(new Color(245, 240, 60)) ;
		done.setEnabled(true) ;
	    }
	}
    }
    
    void setCaretToEnd() {
	for (MAIValue v : field) {
	    if (v instanceof JTextField) { // Also returns false for null v.
		JTextField t = (JTextField)v ;
		t.setCaretPosition(t.getText().length()) ;
	    }
//	    if (v instanceof JSpinner) { // Also returns false for null v.
//		JFormattedTextField t = 
//			((JSpinner.DefaultEditor)((JSpinner)v).getEditor()).
//			getTextField() ;
//		String text = t.getText() ;
//		int l = text.length() ;
//		t.setCaretPosition(l) ;
//	    }
	}
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	new MakeAccountInfo().getAndStoreAccountInfo() ;
    }

    interface MAIValue {
	String getMAIInfo() ;
    }

    public static class MAIJTextField
    extends JTextField
    implements MAIValue
    {
	MAIJTextField(String initialText) {
	    super(initialText) ;
	}
		
	private static final long serialVersionUID = 1L;
	@Override
	public String getMAIInfo() {return getText();}
    }
    public static class MAIJLabel
    extends JLabel
    implements MAIValue
    {
	private static final long serialVersionUID = 1L;
	@Override
	public String getMAIInfo() {return getText() ;}
	
	MAIJLabel() {super() ; } 
	
	MAIJLabel(String s) {
	    super(s) ;
	}
    }
    public static class MAIJPasswordField
    extends JPasswordField
    implements MAIValue
    {
	MAIJPasswordField(String initialText) {
	    super(initialText) ;
	}

	private static final long serialVersionUID = 1L;
	@Override
	public String getMAIInfo() {
	    return new String(getPassword());
	}
    }
    public static class MAIJButton
    extends JButton
    implements MAIValue
    {
	private static final long serialVersionUID = 1L;
	MAIJButton(String arg) {
	    super(arg) ;
	}
	@Override
	public String getMAIInfo() {
	    return getText() ;
//	    String result ;
//	    String text = getText() ;
//	    boolean enumValue = false ;
//	    for (EXECUTE e: EXECUTE.values()) {
//		if (text.equals(e.name())) {
//		    enumValue = true ;
//		    break ;
//		}
//	    }
//	    if (enumValue) {
//		result = text ;
//	    } else {
//		result = "Unknown value for MAIJButton." ;
//	    }
//	    return result ;
	}
    }
    public static class MAIBoxHorizontal
    extends Box
    implements MAIValue
    {
	private static final long serialVersionUID = 1L;
	public MAIBoxHorizontal() {
	    super(BoxLayout.X_AXIS) ; }
	@Override
	public String getMAIInfo() {
	    return "A horizontal Box." ;
	}
    }
    public static class MAIJSpinner
    extends JSpinner
    implements MAIValue
    {
	private static final long serialVersionUID = 1L;
	MAIJSpinner(SpinnerNumberModel arg) {
	    super(arg) ;
	}
	@Override
	public String getMAIInfo() {
	    return String.valueOf(((Number)getValue()).intValue()) ;
	}
    }
    public static class AccountInfoItem extends Box {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L ;
	private JLabel jl ;
	private MAIValue jc ;

	private AccountInfoItem() {  
	    // No publicly accessible 
	    // no-arg constructor.
	    /*
	     * Avoids a warning about no implicit super constructor.
	     */
	    super(BoxLayout.X_AXIS) ; 
	}

	private AccountInfoItem(AccountInfoItem.Builder b) {
	    super(BoxLayout.X_AXIS) ;
	    jl = new JLabel(b.label) ;
	    jc = b.jc ;
	    this.add(jl) ;
	    this.add((JComponent)jc) ;
	}

	public void setJLabel(JLabel jl) {
	    this.jl = jl ;
	}

	public void setJComponent(MAIValue jc) {
	    this.jc = jc ;
	}

	public String getValue(MAIValue jc1) {
	    return jc1.getMAIInfo() ;
	}

	public static class Builder {
	    // Required parameters
	    String label ;
	    MAIValue jc ;

	    // Optional parameters initialized to default values - NONE

	    public Builder label(String s) {
		label = s ;  
		return this ;
	    }

	    public Builder component(MAIValue jx) {
		jc = jx ;
		return this ;
	    }

	    public AccountInfoItem build() {
		return new AccountInfoItem(this) ;
	    }
	}
    }
}

class Info {
    
    enum PreferencesEnum{
	/*
	 * Extend this by creating a variable name (e.g. keyXXX) and
	 * a text description of the variable (e.g. YYY) and extend the
	 * list by adding keyXXX("YYY").  
	 * Also provide a definition for the method getStoredValue() . 
	 * Also provide a definition for the method storeValue() .
	 * 
	 */
	
	keyUserID("userID")
	{
	    @Override
	    Object getStoredValue() { 
		value = Preferences.userNodeForPackage(getClass()).
			get(getKey(), "") ; 
		return value ; 
	    }
	    @Override
	    void storeValue() {
		Preferences.userNodeForPackage(getClass()).
		put(key, (String)value) ; 
	    }
	} ,
	keyPassword("password")	
	{
		@Override
		Object getStoredValue() { 
		    return value = Preferences.userNodeForPackage(getClass()).
			    get(getKey(), "") ; 
		}
		@Override
		void storeValue() {
		    Preferences.userNodeForPackage(getClass()).
		    put(key, (String)value) ; 
		}
	} ,
	keyGreenZoneStart("greenZoneStart")	
	{
		@Override
		Object getStoredValue() { 
		    return value = Integer.valueOf(
			    Preferences.userNodeForPackage(getClass()).
			    getInt(getKey(), 1000)) ; 
		}
		@Override
		void storeValue() {
		    Preferences.userNodeForPackage(getClass()).
		    putInt(key, Integer.parseInt((String) value)) ; 
		}
	} ,
	keyGreenZoneEnd("greenZoneEnd")	
	{
		@Override
		Object getStoredValue() { 
		    return value = Integer.valueOf(
			    Preferences.userNodeForPackage(getClass()).
			    getInt(getKey(), 2000)) ; 
		}
		@Override
		void storeValue() {
		    Preferences.userNodeForPackage(getClass()).
		    putInt(key, Integer.parseInt((String) value)) ; 
		}
	} ,
	;
	
	//
	// enum instance data
	//
	String key = null ;
	Object value = null ;
	
	//
	// enum constructor
	//
	private PreferencesEnum(String key) {
	    this.key = key ;
	}
	//
	// enum methods
	//
	void setValue(Object value) {
	    this.value = value ;
	}
	Object getValue() { return value ; }
	String getKey() {
	    return (key == null)?"<null>":key ;
	}
	//
	// enum Strategy Pattern method declarations
	//
	abstract Object getStoredValue() ;
	abstract void   storeValue() ;
    }

    Info() {
	for (PreferencesEnum e : PreferencesEnum.values()) {
	    e.setValue(e.getStoredValue()) ;
//	    StringBuilder temp = 
//		    new StringBuilder(e.getKey()) ;
//	    temp.append(" has value " + e.getValue() + ".") ;
//	    System.out.println(temp) ;
	}
   }
	public static void main(String[] args) {
	    new Info() ;
		System.out.println(
			"key userID         has a value of " + 
				PreferencesEnum.keyUserID.getValue() + ".") ;
		System.out.println(
			"key password       has a value of " + 
				PreferencesEnum.keyPassword.getValue() + ".") ;
		System.out.println(
			"key greenZoneStart has a value of " + 
				PreferencesEnum.keyGreenZoneStart.getValue() + ".") ;
		System.out.println(
			"key greenZoneEnd   has a value of " + 
				PreferencesEnum.keyGreenZoneEnd.getValue() + ".") ;
	}
}
