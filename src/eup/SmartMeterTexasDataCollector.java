package eup;

import java.awt.Container;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * A <tt>SmartMeterTexasDataCollector</tt> represents the actions needed to
 * access the
 * web pages containing the electrical meter data at smartmetertexas.com.
 * Access is via <tt>GET</tt> and <tt>POST</tt> methods accessed by using the
 * HTTP protocol. Most cookies are automatically handled by the
 * underlying Apache Commons HttpClient version 3.1 code.
 * <p>
 * 
 * @author Ian Shef
 * @version 1.0 25 Aug 2018
 * 
 * @since 1.0
 * 
 */

/*
 * Watch out for <span> containing <div> with
 * 
 * Your request could not be processed at this time. Please try again later.
 * 
 */

public class SmartMeterTexasDataCollector implements 
SmartMeterTexasDataInterface
{
    /*
     * Some fields are volatile due to access from multiple threads.
     */
    private volatile LocalDate date ; // The date of this object.
    private volatile int startRead ;
//    private volatile int endRead ;
    private volatile boolean dateChanged = false ;
    private volatile boolean dataValid = false ;

    private final Object lock = new Object() ;
    
//    private final AccountInfo accountInfo = new AccountInfo() ;

    private static final boolean DEBUG_SHOW_MESSAGES = false ;
    private static final boolean DEBUG_SHOW_BROWSER = false ;
    private static final int RETRY_LIMIT = 5 ;
    private static final int DATA_RETRY_LIMIT = 5 ;
    private static final int DATA_RETRY_MILLIS = 1000 ;
    private static final String SEARCH_FOR = "(Kwh)" ;
    private static final String TITLE = "Dashboard" ;
    private static final String NO_DATA = "No data available " +
	    "for the date range you requested." ;
    private final static String EMPTY = "" ;
    private static final DateTimeFormatter DATE_PATTERN = 
	    DateTimeFormatter.ofPattern("MM'/'dd'/'yyyy") ;
    private static final String MAINTENANCE = 
	    "Smart Meter Texas is currently undergoing maintenance." ;
    private static final String WEBDRIVER = "eup/resources/geckodriver" ;
  
    WebDriver browser ;
    FirefoxOptions firefoxOptions = new FirefoxOptions() ;
//    int tries = 1 ;

	//
	//
	//  Selenium javadoc documentation available through
	//
	//  https://www.selenium.dev/selenium/docs/api/java/
        //
        // and
        //
        //  https://www.javadoc.io/static/org.seleniumhq.selenium/selenium-api/2.50.1/index.html?org/openqa/selenium/WebDriver.html
	//
	//  Web Scraping Documentation
	//
	//  https://towardsdatascience.com/top-25-selenium-functions-that-will-make-you-pro-in-web-scraping-5c937e027244
	//
	//


//    private static final String msgDown = "No results found.";
//    private static final String notResponding = 
//	    "Third-party server not responding." ;
//    private static final String gatewayTimeout =
//	    "Gateway Timeout" ;
//    private static final String msgNoResource = 
//	    "The Access Manager WebSEAL server cannot find the resource " +
//		    "you have requested." ;

    //
    // The following are:
    // volatile due to potential access from multiple threads, and
    // static so that values are maintained throughout the
    //        lifetime of this class.
    //
    static volatile LocalDate cachedDate ;
    static volatile long cachedMeterReading ;
    static volatile boolean cachedValuesValid = false ;
    static volatile boolean cachedValuesUsed  = false ;
    final Object cacheLock                    = new Object() ;
    //
    //
    //	    


    Feedbacker fb;

    String addressSuffix = "" ;

    // The following can be enabled to use a proxy.
    //
    private boolean displayUseProxy = false ;
    
    //    private int accessCount = 1 ;
    private int progressStart ;
    private int progressDelta ;
    private String progressLabel ;
    
    private int progress ;

    static final AtomicInteger ai = new AtomicInteger() ;

    /**
     * No publicly-available no-argument constructor.
     */
    private SmartMeterTexasDataCollector() {
	    /*
	     * vvvv    To get rid of warnings. vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	     */
	msg(Integer.valueOf(progressDelta)) ;
	msg(Integer.valueOf(progressLabel)) ;
	msg(Integer.valueOf(progress)) ;
	    /*
	     * ^^^^    To get rid of warnings. ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	     */
    }

    /**
     * A private constructor for getting
     * Smart Meter of Texas information
     * from my electrical meter.  Use
     * the builder SmartMeterTexasDataCollector.Builder
     * to instantiate a SmartMeterTexasDataCollector.
     * 
     */
    private SmartMeterTexasDataCollector(Builder builder) {
	this.date = builder.date ;
	this.progressStart = builder.progressStart ;
	this.progressDelta = builder.progressDelta ;
	this.progressLabel = builder.progressLabel ;
	progress = progressStart ;
//	System.setProperty(
//		"webdriver.gecko.driver", 
//		"/home/vaj4088/git/-ElectricityUsagePredictor6_20200918/" +
//		"drivers/geckodriver"
//		) ;
	System.setProperty(
		"webdriver.gecko.driver", WEBDRIVER
		) ;
	if (displayUseProxy)     useProxy(firefoxOptions) ;
	if (!DEBUG_SHOW_BROWSER) firefoxOptions.setHeadless(true);
	if (DEBUG_SHOW_MESSAGES) msg("Built " + this) ;
	if ( !findGeckoDriver() ) {
	    sleepMillis(10000) ;
	    System.exit(-30) ;
	}
    }

    private boolean findGeckoDriver() {
	boolean result = true ;
	File gecko = new File(WEBDRIVER) ;
	
	if (!gecko.exists() || !gecko.isFile()) {
	    System.err.println("COULD NOT FIND EXPECTED FILE: " + WEBDRIVER) ;
	    result = false ;
	    System.out.println("in directory " +
	    (new File("~")).getAbsolutePath() ) ;
	}
	return result ;
    }
    
    @Override
    public void setFeedbacker(Feedbacker fb) {
	this.fb = fb;
    }

    static Feedbacker setupFeedbacker() {
	final ArrayList<Feedbacker> holder = Util.makeArrayList(1);
	try {
	    javax.swing.SwingUtilities.invokeAndWait((new Runnable() {
		@Override
		public void run() {
		    final FeedbackerImplementation fb1 = 
			    new FeedbackerImplementation();
		    JFrame frame = new JFrame(fb1.toString());
		    Container cp = frame.getContentPane();
		    cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		    cp.add(fb1.getProgressBar());
		    cp.add(fb1.getOperationsLog());
		    frame.setDefaultCloseOperation(
			    WindowConstants.EXIT_ON_CLOSE);
		    frame.pack();
		    frame.setVisible(true);
		    System.setOut(new PrintStream(new FeedbackerOutputStream(
			    fb1, "<font color=\"green\">")));
		    System.setErr(new PrintStream(new FeedbackerOutputStream(
			    fb1, "<font color=\"red\">")));
		    holder.add(fb1);
		} // end of run()
	    }));
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	}
	return holder.get(0);
    }

    public static class Builder {
	// Required parameters
	private LocalDate date ;
	private int progressStart = 20 ;
	private int progressDelta = 20 ;
	private String progressLabel = "Get Data" ;
	
	// Optional parameters initialized to default values - NONE
	
	public Builder date(LocalDate dateOfSMT) {
	    date = dateOfSMT ;  
	    return this ;
	}
	
	public Builder startProgressAt(int startProgressAt) {
	    progressStart = startProgressAt ;
	    return this ;
	}
	
	public Builder changeProgressBy(int changeProgressBy) {
	    progressDelta = changeProgressBy ;
	    return this ;
	}
	
	public Builder labelTheProgress(String labelForTheProgress) {
	    progressLabel = labelForTheProgress ;
	    return this ;
	}
	
	public SmartMeterTexasDataCollector build() {
	    return new SmartMeterTexasDataCollector(this) ;
	}
    }
    
    /**
     * A main program for testing purposes to develop and test web access.
     * 
     * @param args
     *            Required but currently unused.
     */
    public static void main(String[] args) {
	ElectricityUsagePredictor.main(null) ;
    }

    @Override
    public String toString() {
	return new String(getClass().getName() + " for " + date + ".");
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
     * A convenience method for displaying a line of text on System.out
     * using the Event Dispatch Thread.
     * 
     * @param ob
     *            An <tt>Object</tt> or a <tt>String</tt> to be displayed on
     *            System.out. If an <tt>Object</tt>, its toString() method will
     *            be called.
     */
    void msgEDT(Object ob) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		msg(ob) ;
	    }
	}) ;
    }

    @SuppressWarnings("boxing")
    private final void useProxy(FirefoxOptions f) {
	int result = -1 ;
	/*
	 * 
NOTE: you must make sure you are NOT on the EDT when you call this code, 
as the get() will never return and the EDT will never be released to go 
execute the FutureTask...  Eric Lindauer Nov 20 '12 at 6:08
	 *
	 */
	Callable<Integer> c = new Callable<Integer>() {
	    @Override public Integer call() {
		return JOptionPane.showConfirmDialog(null,
			"Do you want to use the proxy?") ;
	    }
	} ;
	FutureTask<Integer> dialogTask = 
		new FutureTask<Integer>(c);
	if (SwingUtilities.isEventDispatchThread()) {
	    try {
		result = c.call() ;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(dialogTask);
	    } catch (InvocationTargetException e1) {
		e1.printStackTrace();
	    } catch (InterruptedException e1) {
		e1.printStackTrace();
	    }
	    try {
		result = dialogTask.get().intValue() ;
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    } catch (ExecutionException e) {
		e.printStackTrace();
	    }
	}
	if (result == JOptionPane.YES_OPTION) {
	    f.setProxy((new Proxy()).setHttpProxy("localhost:8080")) ;
	}
    }


    private WebDriver login() {
	Context context ; 
	//
	// There are lots of warnings and INFO messages... ignore them.
	//
	if (DEBUG_SHOW_MESSAGES) System.out.println(
		"Ignoring messages and going to the login page.") ;
	context = saveContextAndHideMessages() ;
	try {
	    browser = new FirefoxDriver(firefoxOptions) ;
	    browser.get("https://smartmetertexas.com");
	    if (DEBUG_SHOW_MESSAGES) {
		System.out.println("Went to the login page.") ;
	    }
	} finally {
	    //
	    // Done with ignoring warnings and INFO messages.  Restore...
	    //
	    if (DEBUG_SHOW_MESSAGES) {
		System.out.println("Done ignoring messages.") ;
	    }
	    restoreContextAndUnhideMessages(context) ;
	}
	//
	// Initializing to null avoids an error message.
	//
	WebElement userid = null ;
	
	try {
	    userid = browser.findElement(By.name("userid"));
	} catch (org.openqa.selenium.NoSuchElementException e) {
	    java.util.List<WebElement> maintenanceMessage = null;
	    try {
		maintenanceMessage = browser.findElements(
		    By.partialLinkText(
			    MAINTENANCE
			    )
		    );
	    } catch (Exception e1) {
		e1.printStackTrace() ;
		System.exit(-6) ;
	    }
	    if ( (maintenanceMessage != null ) && 
		    (maintenanceMessage.size() > 0 )) {
		fb.log(EMPTY) ;
		for (WebElement m : maintenanceMessage) {
			fb.log(m) ;
		}
	    }
	    fb.log(EMPTY) ;
	    fb.log("TRY AGAIN LATER !") ;
	    fb.log(EMPTY) ;
	    System.exit(-5) ;
	}
	WebElement password = browser.findElement(By.name("password")) ;

	String u = (String) Info.PreferencesEnum.keyUserID.getStoredValue() ;
	String p = (String) Info.PreferencesEnum.keyPassword.getStoredValue() ;
	if (DEBUG_SHOW_MESSAGES) {
	    msg("User ID  is " + u + ".") ;
	    msg("Password is " + p + ".") ;
	}
	sendStringToWebElement(u, userid) ; 
	sendStringToWebElement(p, password) ; 
	WebElement button   = 
		browser.findElement(By.xpath("//form//button")) ;
	button.click() ;
	waitForFirstPageAfterLogin() ;
	return browser ;
    }
    
    private void getDataHelper(WebDriver wd) {
	if (dataAvailable(wd)) {
	    getLatestEndMeterReadingAndUpdateCache(wd) ;
	} else {
	    System.out.println() ;
	    System.out.println("Received " + NO_DATA) ;
	    System.out.println() ;
	    System.out.println() ;
	}
    }

    /**
     * @param wd  
     */
    void getData(WebDriver wd) {
	ValuesForDate values = null ;
	getDataHelper(wd) ;
	String dateString ;
	    /*
	     * vvvv    To get rid of a warning. vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	     */
	dateString = "" ;
	msg(dateString) ;
	    /*
	     * ^^^^    To get rid of a warning. ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	     */
	/*
	 * Need to compare variable date of type LocalDate
	 * with variable cachedDate of type LocalDate,
	 * using cache lock cacheLock to synchronize.
	 * 
	 * If boolean variable cachedValuesValid is true
	 * and the comparison is equal or after, then
	 * get the cached meter reading from the long variable
	 * cachedMeterReading.
	 * 
	 */
	synchronized (cacheLock) {
	    cachedValuesUsed  = false ;
	    if (cachedValuesValid && 
		    (date.isEqual(cachedDate) || date.isAfter(cachedDate))
		    ) {
		cachedValuesUsed = true ;

		synchronized (lock) {
		    startRead = (int) cachedMeterReading;
		    dataValid = true ;
		    dateString = date.format(DATE_PATTERN) ;
		    if (date.isAfter(cachedDate)) {
			//
			//
			//  This next line is a MAJOR design decision
			//  to change the date of this object to the cached
			//  date despite this object having been created 
			//  with a different date.
			//
			//
			date = cachedDate ;
			//
			//
			//
			dateChanged = true ;
		    }
		    values = new ValuesForDate.Builder()
		    .success(true)
		    .date(dateString) 
		    .startRead(Integer.toString(startRead))
		    .endRead(Integer.toString(startRead))
		    .consumption("0")
		    .build() ;
		} // End of synchronized on lock.
	    } else {
		synchronized (lock) {
		    dateString = date.format(DATE_PATTERN) ;
		}  // End of synchronized on lock.
	    }
	}  // End of synchronized on cacheLock.

	//
	// Check that there really is data.
	//

	//
	// First, check that the data was properly accessed.
	//

	//
	// Second, check that the server is up.
	//


	/*
	 * ***********************************************************
	 */
	String dateWantedString = 
		((date.getMonthValue()<10)?"0":"") +
		Integer.toString(date.getMonthValue()) +
		"/" +
		((date.getDayOfMonth()<10)?"0":"") +
		Integer.toString(date.getDayOfMonth()) + 
		"/" +
		Integer.toString(date.getYear()) ;
	
	if (values == null) {
	    values = getAllValuesForDate(wd, dateWantedString) ;
	}
	float startReadFloat = Float.parseFloat(values.getStartRead()) ;
	if (DEBUG_SHOW_MESSAGES) {
	    msg("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv") ;
	    msg("") ;
	    msg("Here is the data in ValuesForDate values:") ;
	    msg("") ;
	    msg("Date           is " + values.getDate()) ;
	    msg("Success status is " + values.isSuccess()) ;
	    msg("Consumption    is " + values.getConsumption()) ;
	    msg("Start reading  is " + values.getStartRead()) ;
	    msg("End   reading  is " + values.getEndRead()) ;
	    msg("") ;
	    msg("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^") ;
	    msg("") ;
	}
	/*
	 * ***********************************************************
	 */

	    synchronized (cacheLock) {
		if (!cachedValuesUsed) {
		    synchronized (lock) {
			startRead = (int) startReadFloat;
			dataValid = true;
		    }
		}
	    }
    }  // End of getData

    private void logout(WebDriver wd) {
	//
	// Logging out appears to be UNNECESSARY.
	//
	// However, closing the browser is nice.
	// NOTE:  This closes a browser tab but exits the browser
	//        if and only if this was the last open tab.
	wd.close() ;
    }

    private void invoke() {
	browser = login() ;
//	getData(browser) ; 
	//
	// Unnecessary because also has calling chain:  
	//
	// login -> waitForFirstPageAfterLogin -> getData
	//
	logout(browser) ;
    }

    /**
     * @param stringToSend
     * @param webElement
     */
    private static void sendStringToWebElement(String stringToSend,
	    WebElement webElement) {
	webElement.clear() ;
	webElement.sendKeys(stringToSend) ;
    }
    
    private static boolean dataAvailable(WebDriver w) {
	for (WebElement e : w.findElements(By.xpath("//span"))) {
	    if (e.getText().contains(NO_DATA) ) {
		return false ;
	    }
	}
	return true ;
    }
    
    private static String dataFound(WebDriver w) {
    //
    // "we" is WebElement
    //
	boolean weTextContainsSearchFor = false ; 
	// The initialization above makes the Eclipse compiler happy.
	boolean weTextContainsNoData = false ; 
	// The initialization above makes the Eclipse compiler happy.
	String weText = null ; 
	// The initialization above makes the Eclipse compiler happy.
	WebElement we = null ;
	for (WebElement e : w.findElements(By.xpath("//div"))) {
	    try {
		we = e ;
		if (we !=null) weText = we.getText() ;
		if (weText != null) {
		    weTextContainsSearchFor = weText.contains(SEARCH_FOR) ;
		    weTextContainsNoData    = weText.contains(NO_DATA) ;
		}
		if (weTextContainsSearchFor) return weText ;
		if (weTextContainsNoData) {
		    System.out.println();
		    System.out.println("*** NO DATA IS AVAILABLE. ***") ;
		    System.out.println();
		    return null ;
		}
	    } catch (org.openqa.selenium.StaleElementReferenceException e1) {
		System.out.println(" Caught SER Exception on: " + we + ".") ;
		e1.printStackTrace(System.out) ;
		System.out.println() ;
		return null ;
	    }
	}
	return null ;
    }

    /**
     * @param browser
     * @param date
     */
    private static ValuesForDate getAllValuesForDate
    (WebDriver browser, String date) 
    {
	/*
	 * The date String xx/yy/zzzz
	 * must be a two-digit month number (1 as 01, etc.) in 
	 * the range 01 through 12,
	 * followed by a slash, followed by
	 * a two-digit day number (1 as 01, etc.)in the range 01 through 31,
	 * followed by a slash, followed by
	 * a four-digit year number.
	 * 
	 * Partially verify the date String: 
	 */
	if (date == null) {
	    System.out.println("Null date in " + Util.getCallerMethodName()) ;
	} else {
	    if ( ! (
		    date.length() == 10 &&
		    Character.isDigit(date.charAt(0)) &&
		    Character.isDigit(date.charAt(1)) &&
		    date.charAt(2) == '/' &&
		    Character.isDigit(date.charAt(3)) &&
		    Character.isDigit(date.charAt(4)) &&
		    date.charAt(5) == '/' &&
		    Character.isDigit(date.charAt(6)) &&
		    Character.isDigit(date.charAt(7)) &&
		    Character.isDigit(date.charAt(8)) &&
		    Character.isDigit(date.charAt(9))
		    ) ) {
		System.out.println("Bad date " 
			+ date 
			+ " in " 
			+ Util.getCallerMethodName()) ;
	    }
	}
	try {
	    LocalDate.parse( date, DATE_PATTERN ) ;
	} 
	catch (DateTimeParseException e) {
		System.out.println("Exception for bad date " 
			+ date 
			+ " in " 
			+ Util.getCallerMethodName()) ;
		e.printStackTrace() ;
	}
	ValuesForDate resultForDate ;
	boolean successful = false ;
	String result = "" ;
	if (browser == null) {
	    System.out.println("Null browser in " + 
		    Util.getCallerMethodName()) ;
	    return new ValuesForDate.Builder()
		    .success(false)
		    .date(EMPTY) 
		    .startRead(EMPTY)
		    .endRead(EMPTY)
		    .consumption(EMPTY)
		    .build() ;
	}
	//
	// Set Start Date and End Date
	//
	List<WebElement> input = browser.findElements(By.xpath("//input")) ;
	for (WebElement e : input) sendStringToWebElement(date, e) ;
	    //
	    // "D" = Daily Meter Reads
	    //
	List<WebElement> select = browser.findElements(By.xpath("//select")) ;
	for (WebElement e : select) e.sendKeys("D") ;
	
	//
	//  Results here.
	//
	
	String results[] = {"", "", "", "", ""} ;
	if (dataAvailable(browser)) {
	    for (int i = 0; i < DATA_RETRY_LIMIT; i++) {
		result = dataFound(browser);
		if (result != null) break;
		sleepMillis(DATA_RETRY_MILLIS);
	    } 
	}
	if (result == null) {
	    System.out.println();
	    System.out.println("Could not find data after " + 
		    DATA_RETRY_LIMIT + " tries and after " + 
		    (DATA_RETRY_LIMIT*DATA_RETRY_MILLIS) + 
		    " milliseconds for date: " + date + ".") ;
	    System.out.println();
	    System.exit(-1) ;
	}
	//
	// The next line eliminates a compiler warning.
	//
	if (result == null) result = "" ;
	//
	//
	//
	result = result.substring(result.lastIndexOf(SEARCH_FOR)) ;
	results = result.split("\\s");
	if ( results[1].contentEquals(date) ) successful = true ;
	if (successful) {
	    System.out.println();
	    System.out.println("Date        is " + results[1]);
	    System.out.println("Start Read  is " + results[2]);
	    System.out.println("End   Read  is " + results[3]);
	    System.out.println("Consumption is " + results[4]);
	    System.out.println();
	    resultForDate = new ValuesForDate.Builder()
		    .success(true)
		    .date(results[1]) 
		    .startRead(results[2])
		    .endRead(results[3])
		    .consumption(results[4])
		    .build() ;
	} else {
	    resultForDate = new ValuesForDate.Builder().success(false).build() ;
	    return resultForDate ;
	}
	for (WebElement e : select) e.sendKeys("E") ;
	return resultForDate ; 
    }

    private void getLatestEndMeterReadingAndUpdateCache(WebDriver wd) {
	final int titleLiteral = 0 ;
	final int dateLiteral = 1 ;
	final int dateValue = 2 ;
	final int timeLiteral = 3 ;
	final int timeValue = 4 ;
	final int endMeterReadLiteral = 5 ;
	final int endMeterReadValue = 6 ;
	final int lastReadings = endMeterReadValue ;
	String[] readings = new String[lastReadings+1]  ;
	for (int i = 0 ; i<readings.length ; i++ ) readings[i] = "" ;
	Runtime rt = new Runtime() ; // Gets start time.
	WebElement select = 
		wd.findElement(By.xpath("//div[@class='last-meter-reading']")) ;
	readings = select.getText().split("\\R") ;
	if (DEBUG_SHOW_MESSAGES) {
	    System.out.print(
		    "... done finding.  Took " + rt.measurement()/1000.0) ;
	    System.out.println(" seconds.") ;
	}

	if ( ! readings[titleLiteral].contains("Latest End of Day Read")) {
	    throw new Error("Latest End of Day Read has wrong title of " + 
		    readings[titleLiteral] + ".") ;
	}
	if ( ! readings[dateLiteral].contains("Date")) {
	    throw new 
	    Error("Latest End of Day Read has wrong date subtitle of " + 
		    readings[dateLiteral] + ".") ;
	}
	if ( ! readings[timeLiteral].contains("Time")) {
	    throw new 
	    Error("Latest End of Day Read has wrong time subtitle of " + 
		    readings[timeLiteral] + ".") ;
	}
	if ( ! readings[timeValue].contains("00:00:00")) {
	    throw new 
	    Error("Latest End of Day Read has wrong time value of " + 
		    readings[timeValue] + ".") ;
	}
	if ( ! readings[endMeterReadLiteral].contains("Meter Read")) {
	    throw new 
	    Error("Latest End of Day Read has wrong meter read subtitle of " + 
		    readings[endMeterReadLiteral] + ".") ;
	}
	
	LocalDate startDate = getLatestStartDate(readings[dateValue]) ;
	long startReading = getLatestStartRead(readings[endMeterReadValue]) ;
	synchronized(cacheLock) {
	    cachedDate         = startDate ;
	    cachedMeterReading = startReading ;
	    cachedValuesValid = true ;
	}
	if (DEBUG_SHOW_MESSAGES) {
	    System.out.println("Latest Start Date of " + startDate.toString() + 
		    " has reading of " + startReading + ".") ;
	}
    }

    private LocalDate getLatestStartDate(String dateIn) {
	final char FSLASH = '/' ;
	if ((dateIn.charAt(2) == FSLASH) && (dateIn.charAt(5) == FSLASH)) {
	    String yearString = dateIn.substring(6, 10) ;
	    String monthString = dateIn.substring(0, 2) ;
	    String dayString = dateIn.substring(3, 5) ;
	    int year  = Integer.parseInt(yearString) ;
	    int month = Integer.parseInt(monthString) ;
	    int day   = Integer.parseInt(dayString) ; 
	    return LocalDate.of(year, month, day).plusDays(1) ;
	}
	throw new AssertionError(
		"Bad date string of " +
		dateIn +
		" in getLatestStartDate."
		) ;
    }

    private long getLatestStartRead(String in) {
	return (long)Float.parseFloat(in) ;
    }
    
    private static Context saveContextAndHideMessages() {
	Context c = new Context() ;
	c.ps = System.err ;
	System.setErr(new PrintStream(new OutputStream() {
	    @Override
	    public void write(int b) {
		/* Intentionally do nothing. */
	    } } )) ;
	return c ;
    }
    private static void restoreContextAndUnhideMessages(Context c) {
	System.setErr(c.ps) ;
    }

    /**
     * @return the date
     */
    @Override
    public LocalDate getDate() {
	return date;
    }

    /**
     * @return the startRead
     */
    @Override
    public int getStartRead() {
	int value ;
	boolean dv ;
	synchronized(lock) {
	    value = startRead ;
	    dv = dataValid ;
	}
	if (!dv) {
	    invoke() ;
	    value = startRead ;
	}
	return value;
    }

    /**
     * @return whether the data is valid
     */
    public boolean isDataValid() {
	boolean value ;
	synchronized(lock) {
	    value = dataValid ;
	}
	return value;
    }

    /**
     * @return the dateChanged
     */
    @Override
    public boolean isDateChanged() {
	boolean dc ;
	synchronized (lock) {
	    dc = dateChanged ;
	}
	return dc ;
    }
    
    @Override
    public int getGreenStart() {
	return 500 ; // Ian Shef ibs
    }
    
    @Override
    public int getGreenEnd() {
	return 1000 ; //  Ian Shef  ibs
    }
    
    static class AccountInfo {
	private static final Info info = new Info() ;
	
	public int getGreenStart() {
	    return info.getGreenStart() ;
	}
	
	public int getGreenEnd() {
	    return info.getGreenEnd() ;
	}

    }
    
    private void waitForFirstPageAfterLogin() {
	int tries = 1 ;

	//
	//
	//  Selenium javadoc documentation available through
	//
	//  https://www.selenium.dev/selenium/docs/api/java/
	//
	//  Web Scraping Documentation
	//
	//  https://towardsdatascience.com/top-25-selenium-functions-that-will-make-you-pro-in-web-scraping-5c937e027244
	//
	//

	do {
	    int correctTitleTimesInARow = 0 ;
	    int titleTriesRemaining ;
	    for (   titleTriesRemaining = 12 ; 
		    titleTriesRemaining > 0 ; 
		    titleTriesRemaining-- ) {
		if (browser.getTitle().startsWith(TITLE)) {
		    correctTitleTimesInARow++ ;
		    if (correctTitleTimesInARow == 3) break ;
		} else {
		    correctTitleTimesInARow = 0 ;
		    System.out.println("Title is: '" +
		    browser.getTitle() + "'") ;
		}
		sleepMillis(1000) ;
	    }
	    if (titleTriesRemaining == 0) {
		System.out.print("Failed to get title ") ;
		System.out.print(TITLE) ;
		System.out.println(" .") ;
		browser.quit() ;
	    } else {
		getData(browser) ;
		break ;
	    }
	} while (tries++ < RETRY_LIMIT) ;
    }

    /**
     * 
     */
    private static void sleepMillis(int sleepMilliseconds) {
	try {
	    Thread.sleep(sleepMilliseconds) ;
	} catch (InterruptedException e1) {
	    Thread.currentThread().interrupt() ;
	    e1.printStackTrace();
	}
    }

    static class Context {
	PrintStream ps ;
    }
    
    static class Runtime {
	private long startTime = System.currentTimeMillis() ;
	private long endTime ;
	
	public long measurement() {
	    makeMeasurement() ;
	    return getMeasurement() ;
	}
	
	public void makeMeasurement() {
	    endTime = System.currentTimeMillis() ;
	}
	
	public long getMeasurement() {
	    return endTime - startTime ;
	}
    }
    static class ValuesForDate {
	private boolean success = false ;
	private String date = " 01/01/00" ;
	private String startRead = "0" ;
	private String endRead = "0" ;
	private String consumption = "0" ;
	
	//
	// No accessible no-argument constructor.
	//
	private ValuesForDate() {}
	
	private ValuesForDate(Builder b) {
	    success     = b.success ;
	    date        = b.date ;
	    startRead   = b.startRead ;
	    endRead     = b.endRead ;
	    consumption = b.consumption ;
	}
	
	    public static class Builder {
		// Required parameters
		boolean success ;
		String date ;
		String startRead ;
		String endRead ;
		String consumption ;
		
		// Optional parameters initialized to default values - NONE
		
		public Builder success(boolean s) {
		    success = s ;
		    return this ;
		}
		
		public Builder date(String dateOfValuesForDate) {
		    date = dateOfValuesForDate ;  
		    return this ;
		}
		
		public Builder startRead(String startValue) {
		    startRead = startValue ;
		    return this ;
		}
		
		public Builder endRead(String endValue) {
		    endRead = endValue ;
		    return this ;
		}
		
		public Builder consumption(String consumptionValue) {
		    consumption = consumptionValue ;
		    return this ;
		}
		
		public ValuesForDate build() {
		    return new ValuesForDate(this) ;
		}
	    }

	    /**
	     * @return the success
	     */
	    public boolean isSuccess() {
	        return success;
	    }

	    /**
	     * @return the date
	     */
	    public String getDate() {
	        return date;
	    }

	    /**
	     * @return the startRead
	     */
	    public String getStartRead() {
	        return startRead;
	    }

	    /**
	     * @return the endRead
	     */
	    public String getEndRead() {
	        return endRead;
	    }

	    /**
	     * @return the consumption
	     */
	    public String getConsumption() {
	        return consumption;
	    }

    }
}
