package eup;
/**
 * 
 */


import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.github.lgooddatepicker.components.DatePicker;
/**
 * The visual environment for the Electricity Usage Predictor. 
 * Construction (instantiation)
 * MUST take place on the EDT (Event Dispatch Thread) because Swing objects are
 * created during construction.
 * 
 * @author Ian Shef
 * 
 */
public class ElectricityUsagePredictor
extends JFrame
implements ActionListener {

    /**
     * 
     */
    private static final boolean DEBUG_SHOW_MESSAGES = false ;
    private static final boolean disableOutRedirection = false;
    private static final boolean disableErrRedirection = false;
    private static final boolean USE_PERSISTENT_STORAGE = true ;
    
    private static final int startProgress1 = 100 ;
    private static final int changeProgress = 100 ;
    private static final int startProgress2 = 
	    startProgress1 + 3 * changeProgress ;
    private static final int startProgress3 = 
	    startProgress1 + 7 * changeProgress ;
    private static final int EXTRA_BUTTON_SPACE = 10 ;
    
    private static final long serialVersionUID = 1L;

    DatePicker datePickerCurrentBillDate ;
    DatePicker datePickerCurrentDate     ;
    DatePicker datePickerNextBillDate    ;

    Feedbacker fb;

    static
    	java.util.concurrent.atomic.AtomicReference<ElectricityUsagePredictor>
    		guiAtomicReference = new 
    		java.util.concurrent.atomic.AtomicReference
    		<ElectricityUsagePredictor>() ;
    
    private CountDownLatch cdl ;
    private JButton jb ;
//    private volatile Date cBD ;
//    private volatile Date cD ;
//    private volatile Date nBD ;
    private volatile LocalDate cBD ;
    private volatile LocalDate cD ;
    private volatile LocalDate nBD ;
    
	/*
	 * Used for getting stored billing date information.
	 */
	
    Map<?extends String, ? extends String> store = 
	    CommonPreferences.getPreferences() ;
    Map<? extends String, ? extends Setting> settingsMap = 
	    Setting.getSettingsMap() ;

	/*
	 * End of used for getting stored billing date information.
	 */
    
//    public static final String MOST_RECENT_BILL_DATE_YEAR  = 
//	    "mostRecentBillDateYear" ;
//    public static final String MOST_RECENT_BILL_DATE_MONTH = 
//	    "mostRecentBillDateMonth" ;
//    public static final String MOST_RECENT_BILL_DATE_DAY   = 
//	    "mostRecentBillDateDay" ;
//    public static final String NEXT_BILL_DATE_YEAR  = "nextBillDateYear" ;
//    public static final String NEXT_BILL_DATE_MONTH  = "nextBillDateMonth" ;
//    public static final String NEXT_BILL_DATE_DAY  = "nextBillDateDay" ;

    /**
     * 
     */
    public ElectricityUsagePredictor() {
	super();
	setupGUI();
    }

    /**
     * @param title
     */
    public ElectricityUsagePredictor(String title) {
	super(title);
	setupGUI();
    }

    /**
     * @param gc
     */
    public ElectricityUsagePredictor(GraphicsConfiguration gc) {
	super(gc);
	setupGUI();
    }

    /**
     * @param title
     * @param gc
     */
    public ElectricityUsagePredictor(String title, GraphicsConfiguration gc) {
	super(title, gc);
	setupGUI();
    }

    private final void setupGUI() {
	setTaskbarIcon("resources/Attempt3.gif");
	addWidgets();
	connectInternalListeners();
	logJavaConfiguration();
    }

    /**
     * @param iconFilepath Path and filename of icon, 
     *                     such as "resources/icon.gif"
     * 
     */
    private void setTaskbarIcon(String iconFilepath) {
	//
	// To set the icon displayed for the system when
	// this program is running.
	//
	try{    
	    setIconImage(ImageIO.read(new File(iconFilepath))) ;   
	}
	catch (Exception ex){
	    ex.printStackTrace() ;
	    System.exit(-26) ;
	}
	//
	//
	//
    }

    private void logJavaConfiguration() {
	for (String key : System.getProperties().stringPropertyNames()) {
	    StringBuilder sb = new StringBuilder("Property: ");
	    sb.append(key);
	    sb.append("=");
	    sb.append(System.getProperty(key));
	    fb.log(sb, Feedbacker.TO_FILE);
	}
    }

    /**
     * Must be called while on Event Dispatch Thread (EDT).
     */
    private final void addWidgets() {
	/*
	 * 
	 */
	JMenuItem menuitem = new JMenuItem("Account Information") ;
	JMenu menu = new JMenu("Preferences") ;
	JMenuBar menubar  = new JMenuBar() ;
	menu.add(menuitem) ;
	menubar.add(menu) ;
	setJMenuBar(menubar) ;
	menuitem.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JComponent item = (JComponent)e.getSource() ;
		item.setEnabled(false) ;
		new MakeAccountInfo().getAndStoreAccountInfo(item) ;
	    }
	});
	/*
	 * 
	 */
	fb = new FeedbackerImplementation();
	@SuppressWarnings("unused")
	PrintStream out0 = System.out; // Saving original System.out ;
	@SuppressWarnings("unused")
	PrintStream err0 = System.err; // Saving original System.err ;
	if (!disableOutRedirection) {
	    System.setOut(new PrintStream(new FeedbackerOutputStream(fb,
		    "<font color=\"green\">")));
	}
	if (!disableErrRedirection) {
	    System.setErr(new PrintStream(new FeedbackerOutputStream(fb,
		    "<font color=\"red\">")));
	}

	setDefaultCloseOperation(EXIT_ON_CLOSE);

	try {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	} catch (ClassNotFoundException e) {
	    // Live with existing Look and Feel.
	    fb.log("ClassNotFoundException while setting Look and Feel.");
	} catch (InstantiationException e) {
	    // Live with existing Look and Feel.
	    fb.log("InstantiationException while setting Look and Feel.");
	} catch (IllegalAccessException e) {
	    // Live with existing Look and Feel.
	    fb.log("IllegalAccessException while setting Look and Feel.");
	} catch (UnsupportedLookAndFeelException e) {
	    // Live with existing Look and Feel.
	    fb.log("UnsupportedLookAndFeelException"
		    + " while setting Look and Feel.");
	}
	
	//  Create three horizontal boxes, one above the other above the other.
	//  The upper box will be used for date pickers and a button.
	//  The middle box will have a progress indication.
	//  The bottom box will be used for the operations log.
	//
	//
	//  Pennywise Power used the "Start Read" reading on a "Date",
	//  (from smartmetertexas.com)
	//  truncated to an integer.
	//  So does Energy Express.
	//
	Box vbox  = Box.createVerticalBox() ;
	Box hbox1 = Box.createHorizontalBox() ;
	vbox.add(hbox1) ;
	
	Box hbox2 = Box.createHorizontalBox() ;
	JComponent pb = fb.getProgressBar(Color.GREEN) ;
	pb.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder("Progress"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				pb.getBorder()));
	Box hbox3 = Box.createHorizontalBox() ;
	vbox.add(hbox2) ;
	vbox.add(hbox3) ;

	hbox2.add(pb) ;
	hbox3.add(fb.getOperationsLog()) ;
//
//	Properties p = new Properties();
//	p.put("text.today","Today") ;
//	p.put("text.month","Month") ;
//	p.put("text.year","Year") ;

	/*
	 * Did not work for me:
	 * p.put("text.today","Today") ;
	 * p.put("text.month","Month") ;
	 * p.put("text.year","Year") ;
	 * 
	 * or use a JDateComponentFactory
	 * 
	 * 
	 * or use jxdatepicker (capitalization?).
	 */
	
	/*
	 * Getting stored billing date information.
	 */
	
//	    DatePicker datePickerCurrentDate     ;
//	    DatePicker datePickerNextBillDate    ;
	
	datePickerCurrentBillDate = new DatePicker() ;
	datePickerCurrentBillDate.setDate(
		LocalDate.of(
			Integer.parseInt(
				store.get(Setting.MOST_RECENT_BILL_DATE_YEAR)), 
			Integer.parseInt(
				store.get(Setting.MOST_RECENT_BILL_DATE_MONTH)), 
			Integer.parseInt(
				store.get(Setting.MOST_RECENT_BILL_DATE_DAY))
			)) ;
	
//	UtilDateModel modelCurrentBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(
//	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_YEAR)), 
//	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_MONTH)), 
//	       Integer.parseInt(store.get(Setting.MOST_RECENT_BILL_DATE_DAY))).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;

	/*
	 * Got the stored billing date information.
	 */
	
//	UtilDateModel modelCurrentBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(2019, Month.JANUARY, 9).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
	/*
	 * 
	 
	JDatePanelImpl datePanelCurrentBillDate = 
		new JDatePanelImpl(modelCurrentBillDate, p);
	datePickerCurrentBillDate = 
		new JDatePickerImpl(datePanelCurrentBillDate,
		new DateLabelFormatter());
	 * 
	 */
	/*
	 * 
	 */

	datePickerCurrentBillDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder("Date of Most Recent Bill"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerCurrentBillDate.getBorder()));
	hbox1.add(datePickerCurrentBillDate) ;
	
	
	datePickerCurrentDate = new DatePicker() ;
	datePickerCurrentDate.setDateToToday() ;

//	UtilDateModel modelCurrentDate = new UtilDateModel(
//		Date.from(LocalDate.now().
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;
//	JDatePanelImpl datePanelCurrentDate = 
//		new JDatePanelImpl(modelCurrentDate, p);
//	datePickerCurrentDate = 
//		new JDatePickerImpl(datePanelCurrentDate,
//		new DateLabelFormatter());

	datePickerCurrentDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder(
					"Current Date"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerCurrentDate.getBorder()));
//	Box vboxCurrent = Box.createVerticalBox() ;
//	hbox1.add(vboxCurrent) ;
//	vboxCurrent.add(new )
	hbox1.add(datePickerCurrentDate) ;
	
	/*
	 * Getting stored billing date information.
	 */
	
	datePickerNextBillDate = new DatePicker() ;
	datePickerNextBillDate.setDate(
		LocalDate.of(
			    Integer.parseInt(store.get(
				    Setting.NEXT_BILL_DATE_YEAR)), 
			    Integer.parseInt(store.get(
				    Setting.NEXT_BILL_DATE_MONTH)), 
			    Integer.parseInt(store.get(
				    Setting.NEXT_BILL_DATE_DAY))
			)) ;

//	UtilDateModel modelNextBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(
//		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_YEAR)), 
//		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_MONTH)), 
//		    Integer.parseInt(store.get(Setting.NEXT_BILL_DATE_DAY))).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;

	/*
	 * Got the stored billing date information.
	 */
	
//	UtilDateModel modelNextBillDate = new UtilDateModel(
//		Date.from(LocalDate.of(2019, Month.FEBRUARY, 8).
//			atStartOfDay(ZoneId.systemDefault()).toInstant())) ;

	//	JDatePanelImpl datePanelNextBillDate = 
	//	new JDatePanelImpl(modelNextBillDate, p);
//	datePickerNextBillDate = 
//		new JDatePickerImpl(datePanelNextBillDate,
//		new DateLabelFormatter());
	datePickerNextBillDate.setBorder(BorderFactory.
		createCompoundBorder(BorderFactory.
			createCompoundBorder(BorderFactory.
				createTitledBorder(
					"Expected Date of Next Bill"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)),
				datePickerNextBillDate.getBorder()));
	hbox1.add(datePickerNextBillDate) ;
	
	jb = new JButton("GO (predict)") ;
        jb.setContentAreaFilled(false) ;
	jb.setOpaque(true) ;
	jb.setBackground(Color.GREEN) ;
	jb.setFont(jb.getFont().deriveFont(Font.BOLD)) ;
	jb.setBorder(BorderFactory.createRaisedSoftBevelBorder())  ;
	hbox1.add(Box.createHorizontalStrut(EXTRA_BUTTON_SPACE)) ;
	hbox1.add(jb) ;
	hbox1.add(Box.createHorizontalStrut(EXTRA_BUTTON_SPACE)) ;

	add(vbox) ;
	pack();
	if (DEBUG_SHOW_MESSAGES) fb.log("Making visible.", Feedbacker.TO_FILE + 
		Feedbacker.TO_GUI);
	setVisible(true);
	jb.addActionListener(this) ;
    } 

    /**
     * Must be called while executing on Event Dispatch Thread (EDT).
     */
    private void connectInternalListeners() {
	fb.log("GUI controller setup commencing.", Feedbacker.TO_FILE) ;
	if (DEBUG_SHOW_MESSAGES) {
	    fb.log("Attempting to show debug messages.", Feedbacker.TO_FILE) ;
	} else {
	    fb.log("Not showing dbug messages.", Feedbacker.TO_FILE) ;
	}
	if (USE_PERSISTENT_STORAGE) {
	    fb.log("Attempting to use persistent storage.", 
		    Feedbacker.TO_FILE) ;
	} else {
	    fb.log("Not using persistent storage.", 
		    Feedbacker.TO_FILE + Feedbacker.TO_GUI) ;
	}
	fb.log("GUI controller setup completed.", Feedbacker.TO_FILE) ;
    }

    @Override
    public String toString() {
	return this.getClass().getName();
    }

    /**
     * To be called from the EDT.
     */
    static Feedbacker getFeedbacker() {
	return ((ElectricityUsagePredictor)(Frame.getFrames()[0])).fb ; 
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	Integer h ;
	boolean changedTheDate ;
	//
	// ElectricityUsagePredictor extends JFrame and
	// thus must be set up via the 
	// EDT (Event Dispatch Thread) !
	//
	// See
	// http://weblogs.java.net/blog/alexfromsun/archive/2006/01/
	// debugging_swing_2.html
	// for how to do this with a RunnableFuture that can provide a
	// result back to the caller.
	//
	try {
	    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
		@Override
		public void run() {
		    ElectricityUsagePredictor gui = 
			    new ElectricityUsagePredictor(
			    "Electricity Usage Predictor");
		    if (DEBUG_SHOW_MESSAGES) {
			gui.fb.log("Finished setting up GUI.");
		    }
		    guiAtomicReference.set(gui);
		}
	    });
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    // Restore the interrupted status
	    Thread.currentThread().interrupt();
	    // Now allow the main thread to exit.
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	    // Now cause the main thread to exit.
	    System.exit(-27) ;
	}
	while (true) {
	    ElectricityUsagePredictor gui = guiAtomicReference.get() ;
	    gui.datePickerCurrentBillDate.setEnabled(true) ;
	    gui.datePickerCurrentDate.setEnabled(true) ;
	    gui.datePickerNextBillDate.setEnabled(true) ;
	    /*
	     * Workaround for enabling the date pickers.
	     */
//	    gui.datePickerCurrentBillDate.getComponent(1).setEnabled(true) ;
//	    gui.datePickerCurrentDate.getComponent(1).setEnabled(true) ;
//	    gui.datePickerNextBillDate.getComponent(1).setEnabled(true) ;
	    /*
	     * End workaround for enabling the date pickers.
	     */
	    gui.jb.setEnabled(true);
	    gui.fb.progressAnnounce(false, "Waiting for GO (predict)") ;
	    gui.cdl = new CountDownLatch(1) ;
	    try {
		//
		//  WAIT HERE until the GO button is pushed.
		//
		gui.cdl.await() ;
		//
		//  CONTINUE because the GO button was pushed.
		//
	    } catch (InterruptedException e) {
		e.printStackTrace();
		// Restore the interrupted status
		Thread.currentThread().interrupt();
	    }
	    gui.datePickerCurrentBillDate.setEnabled(false) ;
	    gui.datePickerCurrentDate.setEnabled(false) ;
	    gui.datePickerNextBillDate.setEnabled(false) ;
	    /*
	     * Workaround for disabling the date pickers.
//	     */
//	    gui.datePickerCurrentBillDate.getComponent(1).setEnabled(false) ;
//	    gui.datePickerCurrentDate.getComponent(1).setEnabled(false) ;
//	    gui.datePickerNextBillDate.getComponent(1).setEnabled(false) ;
	    /*
	     * End workaround for disabling the date pickers.
	     */
	    gui.jb.setEnabled(false) ;
	    gui.fb.activityAnnounce(
		    startProgress1, 
		    "Starting...", 
		    startProgress1+changeProgress
		    ) ;
	    //
	    // cBDLD is current Bill Date as a Local Date
	    //
//	    LocalDate cBDLD = gui.cBD.toInstant().
//		    atZone(ZoneId.systemDefault()).
//		    toLocalDate() ;
	    LocalDate cBDLD = gui.cBD ;
	    //
	    // cDLD is current date as a Local Date.
	    //
//	    LocalDate cDLD = gui.cD.toInstant().
//		    atZone(ZoneId.systemDefault()).
//		    toLocalDate() ;
	    LocalDate cDLD = gui.cD ;
	    
	    /*
	     * nBDLD is next Bill Date as a LocalDate.
	     */
//	    LocalDate nBDLD = gui.nBD.toInstant().
//		    atZone(ZoneId.systemDefault()).toLocalDate() ;
	    LocalDate nBDLD = gui.nBD ;
	    
	    /*
	     *  Store current billing date for future use.
	     */
	    writeToPersistentStorage(
		    gui.settingsMap, 
		    Setting.MOST_RECENT_BILL_DATE_YEAR, 
		    Setting.MOST_RECENT_BILL_DATE_MONTH, 
		    Setting.MOST_RECENT_BILL_DATE_DAY, 
		    Setting.MOST_RECENT_BILL_DATE_READING,
		    Setting.MOST_RECENT_BILL_READING_VALIDITY,
		    cBDLD
		    ) ;
	    /*
	     * End of storing year, month and day of the current billing date.
	     */
	    
	    /*
	     *  Store next billing date for future use.
	     */
	    writeToPersistentStorage(
		    gui.settingsMap, 
		    Setting.NEXT_BILL_DATE_YEAR, 
		    Setting.NEXT_BILL_DATE_MONTH, 
		    Setting.NEXT_BILL_DATE_DAY, 
		    null,
		    null,
		    nBDLD
		    ) ;
	    /*
	     * End of storing year, month and day of the next billing date.
	     */

	    
	    /*
	     * Get data for the current bill date meter reading.
	     */
	    int currentBillMeterReading ;
	    LocalDate currentBillDateUsed ;
	    SmartMeterTexasDataInterface gdcBDLD = null ;
	    boolean usedMostRecentBillReadingCache = false ;
	    gui.fb.activityAnnounce(
		    startProgress2, 
		    "Getting data for most recent billing date.", 
		    startProgress2+changeProgress
		    ) ;
	    if (canUsePersistentStorageValue(
		    gui.settingsMap,
		    Setting.MOST_RECENT_BILL_DATE_YEAR,
		    Setting.MOST_RECENT_BILL_DATE_MONTH,
		    Setting.MOST_RECENT_BILL_DATE_DAY,
		    Setting.MOST_RECENT_BILL_READING_VALIDITY,
		    cBDLD
		    )) {
		currentBillMeterReading = 
			Integer.parseInt(
				CommonPreferences.get(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_DATE_READING)
				)) ;
		currentBillDateUsed = cBDLD ;
		usedMostRecentBillReadingCache = true ;
	    } else {
		    gdcBDLD = new SmartMeterTexasDataCollector.Builder()
			    .date(cBDLD)
			    .startProgressAt(startProgress2)
			    .changeProgressBy(changeProgress)
			    .labelTheProgress(
				    "Getting data for most " +
				    "recent billing date.")
			    .build();
		    gdcBDLD.setFeedbacker(gui.fb) ;
		    currentBillMeterReading = gdcBDLD.getStartRead();
		    //
		    // Write data for current billing date to
		    // persistent storage.
		    //
			writeToPersistentStorage(
				gui.settingsMap, 
				Setting.MOST_RECENT_BILL_DATE_YEAR,
				Setting.MOST_RECENT_BILL_DATE_MONTH,
				Setting.MOST_RECENT_BILL_DATE_DAY,
				Setting.MOST_RECENT_BILL_DATE_READING,
				Setting.MOST_RECENT_BILL_READING_VALIDITY,
				gdcBDLD.getDate()
				) ;
			CommonPreferences.set(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_DATE_READING),
				Integer.toString(currentBillMeterReading)
				) ;
			CommonPreferences.set(
				gui.settingsMap.
				get(Setting.MOST_RECENT_BILL_READING_VALIDITY),
				Setting.VALID
				) ;
		    currentBillDateUsed = gdcBDLD.getDate();
		    usedMostRecentBillReadingCache = false ;
	    }
	    /*
	     * End of
	     * get data for the current bill date meter reading.
	     */

	    /*
	     * Get data for the current date meter reading.
	     */
	    int currentMeterReading ;
	    LocalDate currentDateUsed ;
	    SmartMeterTexasDataInterface gdcDLD = null;
	    boolean usedCurrentDateReadingCache = false ;
	    gui.fb.activityAnnounce(
		    startProgress3, 
		    "Getting data for current date.", 
		    startProgress3+changeProgress
		    ) ;
	    if (canUsePersistentStorageValue(
		    gui.settingsMap,
		    Setting.CURRENT_DATE_YEAR,
		    Setting.CURRENT_DATE_MONTH,
		    Setting.CURRENT_DATE_DAY,
		    Setting.CURRENT_READING_VALIDITY,
		    cDLD
		    )) {
		currentMeterReading = 		
			Integer.parseInt(
				CommonPreferences.get(
				gui.settingsMap.
				get(Setting.CURRENT_DATE_READING)
				)) ;
		currentDateUsed = cDLD ;
		changedTheDate = false ;
		usedCurrentDateReadingCache = true ;
	    } else {
		gdcDLD = 
			new SmartMeterTexasDataCollector.Builder().
			date(cDLD).
			startProgressAt(startProgress1).
			changeProgressBy(changeProgress).
			labelTheProgress("Getting data for current date.").
			build() ;
		gdcDLD.setFeedbacker(gui.fb) ;
		currentMeterReading     = 
			gdcDLD.getStartRead() ;
		    //
		    // Write data for current date to
		    // persistent storage.
		    //
		writeToPersistentStorage(
			gui.settingsMap, 
			Setting.CURRENT_DATE_YEAR,
			Setting.CURRENT_DATE_MONTH,
			Setting.CURRENT_DATE_DAY, 
			Setting.CURRENT_DATE_READING,
			Setting.CURRENT_READING_VALIDITY,
			gdcDLD.getDate()
			) ;
		CommonPreferences.set(
			gui.settingsMap.
			get(Setting.CURRENT_DATE_READING),
			Integer.toString(currentMeterReading)
			) ;
		CommonPreferences.set(
			gui.settingsMap.
			get(Setting.CURRENT_READING_VALIDITY),
			Setting.VALID
			) ;
		currentDateUsed = gdcDLD.getDate() ;
		changedTheDate = gdcDLD.isDateChanged() ;
		usedCurrentDateReadingCache = false ;
	    }
	    /*
	     * End of
	     * get data for the current date meter reading.
	     */
	    
	    Predictor predictor = new Predictor.Builder().
		    currentBillDate(currentBillDateUsed).
		    currentBillMeterReading(currentBillMeterReading).
		    currentDate(currentDateUsed).
		    currentMeterReading(currentMeterReading).
		    nextBillDate(nBDLD).
		    build();
	    String nonbreakingSpace = "\u00A0" ;
	    String fiveNonBreakingSpaces = nonbreakingSpace + 
		    nonbreakingSpace + nonbreakingSpace + 
		    nonbreakingSpace + nonbreakingSpace ;
	    StringBuilder sb = new StringBuilder("\r\n") ;
	    sb.append("Current Bill Date: ") ;
	    sb.append(predictor.getDateBillCurrent()) ;
	    sb.append("\r\n") ; 
	    sb.append("Current Bill Meter Reading: ") ;
	    h = Integer.valueOf(predictor.getMeterReadingBillCurrent()) ;
	    sb.append(h.intValue()) ;
	    if (usedMostRecentBillReadingCache) {
		sb.append(
			fiveNonBreakingSpaces +
			"<<<<<<<<<<<<  Cached Value Used  >>>>>>>>>>>>"
			) ;
	    }
	    sb.append("\r\n\r\n") ;
	    sb.append("Current ");
	    sb.append(fiveNonBreakingSpaces) ;
	    sb.append(nonbreakingSpace) ;
	    sb.append("Date: ");
	    sb.append(predictor.getDateCurrent().toString()) ;
	    if (changedTheDate) {
		sb.append(
			fiveNonBreakingSpaces +
				"<<<<<<<<<<<< "+
				"LATEST DATA AVAILABLE USED"+
				" >>>>>>>>>>>>"
				) ;
	    }
	    sb.append("\r\n") ;
	    sb.append(
		    "Current" +
			    fiveNonBreakingSpaces + 
			    nonbreakingSpace + 
			    nonbreakingSpace + 
			    "Meter Reading: "
		    ) ;
	    h = Integer.valueOf(predictor.getMeterReadingCurrent()) ;
	    sb.append(h.intValue()) ;
	    if (usedCurrentDateReadingCache) {
		sb.append(
			fiveNonBreakingSpaces +
			"<<<<<<<<<<<<  Cached Value Used  >>>>>>>>>>>>"
			) ;
	    }
	    sb.append("\r\n\r\n") ;
	    sb.append("Next ") ;
	    sb.append(fiveNonBreakingSpaces) ;
	    sb.append("Bill Date   : ") ;
	    sb.append(predictor.getDateBillNext().toString()) ;
	    sb.append(" (") ;
	    sb.append(Long.valueOf(predictor.
		    billingCycleDurationDays()).toString()) ;
	    sb.append(" day billing cycle).") ;
	    sb.append("\r\n") ;
	    sb.append("Days Remaining : ") ;
	    sb.append(Long.valueOf(predictor.daysRemaining()).toString()) ;
	    sb.append("\r\n") ;
	    gui.fb.progressAnnounce(false) ;
	    int predictedUsage = predictor.predictUsage() ;
	    String predictedUsageString ;
	    if (predictor.getDateBillCurrent().
		    equals(predictor.getDateCurrent())) 
	    {
		predictedUsageString = 
			"No prediction is possible " +
				"because Date of Most Recent Bill " + 
				"and date of latest data are the same." ;
	    } else {
		predictedUsageString = String.valueOf(predictedUsage);
	    }
	    SmartMeterTexasDataCollector.AccountInfo accountInfo = null ;
	    int greenStart ;
	    int greenEnd ;
	    if (gdcDLD != null) {
		greenStart = gdcDLD.getGreenStart() ;
		greenEnd   = gdcDLD.getGreenEnd() ;
	    } else {
		if (gdcBDLD != null) {
		    greenStart = gdcBDLD.getGreenStart() ;
		    greenEnd   = gdcBDLD.getGreenEnd() ;
		} else {
		    accountInfo = 
			    new SmartMeterTexasDataCollector.AccountInfo() ;
		    greenStart = accountInfo.getGreenStart() ;
		    greenEnd   = accountInfo.getGreenEnd() ;
		}
	    }
	    @SuppressWarnings("resource")
	    PrintStream where = 
	        ((predictedUsage>=greenStart) && (predictedUsage<=greenEnd))?
	        	System.out:System.err ;
	    //
	    //  Above does not require EDT.
	    //
	    //
	    //  Below outputs to gui so should be on EDT.
	    //
	    SwingUtilities.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	            System.out.println(sb) ;
		    where.print("Predicted Usage : ") ;
		    where.println(predictedUsageString) ;
	        }
	    });
	}
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
 	ElectricityUsagePredictor gui = guiAtomicReference.get() ;

// 	JDatePickerImpl dPCBD = gui.datePickerCurrentBillDate ;
// 	JDatePickerImpl dPCD  = gui.datePickerCurrentDate ;
// 	JDatePickerImpl dPNBD = gui.datePickerNextBillDate ;
// 	cBD = (Date) dPCBD.getModel().getValue() ;
// 	cD  = (Date)  dPCD.getModel().getValue() ;
// 	nBD = (Date) dPNBD.getModel().getValue() ;
 	
 	DatePicker dPCBD = gui.datePickerCurrentBillDate ;
 	DatePicker dPCD  = gui.datePickerCurrentDate ;
 	DatePicker dPNBD = gui.datePickerNextBillDate ;
 	
 	cBD = dPCBD.getDate() ;
 	cD  = dPCD .getDate() ;
 	nBD = dPNBD.getDate() ;
 	
 	//
 	//  Above should be on EDT.
 	//
 	//
 	//  Below does not require EDT and
 	//  gets us off of the EDT.
 	//
 	cdl.countDown() ;
     }

    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msg(Object ob) {
	if (null == fb) {
	    System.out.println(ob);
	} else {
	    fb.log(ob, Feedbacker.TO_OUT + Feedbacker.TO_FILE);
	}
    }
    
    /**
     * A convenience method for displaying a line of text on System.out.
     * 
     * @param ob
     *            An <tt>int</tt> to be displayed on
     *            System.out.
     */
    void msg(int ob) {
	msg(Integer.toString(ob)) ;
    }
    
    /**
     * A convenience method for displaying a line of text on System.out but
     * without a newline.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgNoNewline(Object ob) {
	if (null == fb) {
	    System.out.print(ob);
	} else {
	    fb.logBare(ob, Feedbacker.TO_OUT + Feedbacker.TO_FILE);
	}
    }
    
    /**
     * A convenience method for displaying a line of text on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgEDT(Object ob) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msg(ob) ;
	    }
	    
	});
    }
    
    /**
     * A convenience method for displaying a line of text 
     * without appending a newline character
     * on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgNoNewlineEDT(Object ob) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msgNoNewline(ob) ;
	    }
	    
	});
    }
    
    /**
     * @param settingsMap2
     * @param storedNameYear
     * @param storedNameMonth
     * @param storedNameDay
     * @param storedNameValidity
     * @param lD
     * @return boolean true if the Map given by parameter settingsMap2 has a
     *                      Setting whose name is given by the parameter
     *                      storedNameValidity and whose value is given by
     *                      Setting.VALID (currently "Valid") AND
     *                      whose year is in a setting named by the
     *                      parameter storedNameYear and whose value is the
     *                      same as the year of parameter lD, AND
     *                      whose month is in a setting named by the
     *                      parameter storedNameMonth and whose value is the
     *                      same as the month of parameter lD, AND
     *                      whose day is in a setting named by the
     *                      parameter storedNameDay and whose value is the
     *                      same as the day of parameter lD .
     *                      Basically, return true if the stored setting is
     *                      valid and there is a match to the date...
     *                      otherwise return false.
     */
    private static boolean canUsePersistentStorageValue(
	    final Map<? extends String, ? extends Setting> settingsMap2,
	    final String storedNameYear,
	    final String storedNameMonth,
	    final String storedNameDay,
	    final String storedNameValidity,
	    final LocalDate lD
	    ) {

	boolean valid ;
	String lDYear = String.valueOf(lD.getYear()) ;
	String lDMonth = String.valueOf(lD.getMonthValue()) ;
	String lDDay = String.valueOf(lD.getDayOfMonth()) ;
	
	if (!USE_PERSISTENT_STORAGE) return false ;
	if (storedNameValidity == null) {
	    valid = false ;
	} else {
	    valid = (
		    CommonPreferences.get(
			    settingsMap2.
			    get(storedNameValidity)
			    )
		    ).equals(Setting.VALID) ;
	}
	String storedYear =
		CommonPreferences.get(
			settingsMap2.
			get(storedNameYear)
			) ;
	String storedMonth = 
		CommonPreferences.get(
			settingsMap2.
			get(storedNameMonth)
			) ;
	String storedDay = 
		CommonPreferences.get(
			settingsMap2.
			get(storedNameDay)
			) ;

	return (
		valid &&
		storedYear.equals(lDYear) &&
		storedMonth.equals(lDMonth) &&
		storedDay.equals(lDDay)
		) ;
    }

    /**
     * @param settingsMap2
     * @param storedNameYear
     * @param storedNameMonth
     * @param storedNameDay
     * @param storedNameReading
     * @param storedNameValidity
     * @param lD
     * 
     * If this item 
     * (date given by parameter lD,
     *  names given by parameters
     *  storedNameYear, storedNameMonth, storedNameDay, and storedNameValidity)
     * is not already in the Map given by the parameter
     * settingsMap2, then store it, storing the parts of lD using the
     * names given by parameters
     * storedNameYear, storedNameMonth, and storedNameDay,
     * and store the reading as being invalid
     * (using storedNameValidity and a reading of -1).
     */
    private static void writeToPersistentStorage(
	    final Map<? extends String, ? extends Setting> settingsMap2,
	    final String storedNameYear, 
	    final String storedNameMonth,
	    final String storedNameDay, 
	    final String storedNameReading,
	    final String storedNameValidity,
	    final LocalDate lD
	    ) {
	String lDYear = String.valueOf(lD.getYear()) ;
	String lDMonth = String.valueOf(lD.getMonthValue()) ;
	String lDDay = String.valueOf(lD.getDayOfMonth()) ;
	
	if (canUsePersistentStorageValue(
		settingsMap2, 
		storedNameYear, 
		storedNameMonth, 
		storedNameDay,
		storedNameValidity,
		lD
		)) 
	{
	    // Nothing to do when date matches
	} else {
	    // 
		CommonPreferences.set(
			settingsMap2.
			get(storedNameYear), 
			lDYear) ;
		CommonPreferences.set(
			settingsMap2.
			get(storedNameMonth), 
			lDMonth) ;
		CommonPreferences.set(
			settingsMap2.
			get(storedNameDay), 
			lDDay) ;
		if (storedNameReading != null) {
		    CommonPreferences.set(
			    settingsMap2.
			    get(storedNameReading),
			    "-1");
		}
		if (storedNameValidity != null) {
		    CommonPreferences.set(settingsMap2.get(storedNameValidity),
			    Setting.INVALID);
		}
	}
    }
    
    class DateLabelFormatter extends AbstractFormatter {
	private static final long serialVersionUID = 1L;
	private String datePattern = "EEEEEEEEE LLLLLLLLL dd, yyyy";
	private SimpleDateFormat dateFormatter = 
		new SimpleDateFormat(datePattern);

	@Override
	public Object stringToValue(String text) throws ParseException {
	    return dateFormatter.parseObject(text);
	}

	@Override
	public String valueToString(Object value) {
	    if (value != null) {
		Calendar cal = (Calendar) value;
		return dateFormatter.format(cal.getTime());
	    }
	    return "";
	}
    }
}
