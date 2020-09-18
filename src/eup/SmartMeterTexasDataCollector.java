package eup;

import java.awt.Container;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.time.LocalDate;

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

    private static final boolean DEBUG_SHOW_BROWSER = true ;
    private static final int RETRY_LIMIT = 5 ;
    private static final int DATA_RETRY_LIMIT = 5 ;
    private static final int DATA_RETRY_MILLIS = 1000 ;
    private static final String SEARCH_FOR = "(Kwh)" ;
    private static final String TITLE = "Dashboard" ;
    private static final String NO_DATA = "No data available " +
	    "for the date range you requested." ;
    
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
    }

    /**
     * A private constructor for getting
     * Smart Meter of Texas information
     * from my electrical meter.  Use
     * the builder SmartMeterTexasDataCollector.Builder
     * to instantiate a SmartMeterTexasDataCollector.
     * 
     */
    @SuppressWarnings("synthetic-access")
    private SmartMeterTexasDataCollector(Builder builder) {
	this.date = builder.date ;
	this.progressStart = builder.progressStart ;
	this.progressDelta = builder.progressDelta ;
	this.progressLabel = builder.progressLabel ;
	progress = progressStart ;
//	System.setProperty(
//		"webdriver.gecko.driver", 
//		"/home/xxxxxx/Desktop/workspaces/workspace_(Java_Luna)" +
//		"/BrowserScraping2/drivers/geckodriver") ;
	System.setProperty(
		"webdriver.gecko.driver", 
		"/home/vaj4088/git/ElectricityUsagePredictor5/drivers/" +
		"geckodriver") ;
	/*
	 * /home/xxxxxx/git/ElectricityUsagePredictor5/drivers/geckodriver
	 */
	if (displayUseProxy)     useProxy(firefoxOptions) ;
	if (!DEBUG_SHOW_BROWSER) firefoxOptions.setHeadless(true);	
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
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
	
	@SuppressWarnings("synthetic-access")
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
	//	System.exit(0);
    }

    @Override
    public String toString() {
	return new String(getClass().getName());
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
//	    HostConfiguration hostConfiguration = 
//		    h.getHostConfiguration() ;
//	    hostConfiguration.setProxy("localhost", 8080)  ;
//	    h.setHostConfiguration(hostConfiguration) ;
	}
    }

//    private void getLatestEndMeterReadingAndUpdateCache(WebPage wp) {
//	WPLocation wpl =
//		wp.indexOf("Latest End of Day Read") ;
//	assertGoodLocation(wpl) ;
//	int line = wpl.getLine() ;
//	String endDate = wp.subString(
//		line+1, 
//		"<TD><SPAN name=\"ler_date\">", 
//		"</SPAN></TD>"
//		) ;
//	String endValue = wp.subString(
//		line+3, 
//		"<TD><SPAN name=\"ler_read\">", 
//		"</SPAN></TD>"
//		) ;
//	LocalDate startDate = getLatestStartDate(endDate) ;
//	long startReading = getLatestStartRead(endValue) ;
//	synchronized(cacheLock) {
//	    cachedDate         = startDate ;
//	    cachedMeterReading = startReading ;
//	    cachedValuesValid = true ;
//	}
//    }

//    private LocalDate getLatestStartDate(String dateIn) {
//	final char FSLASH = '/' ;
//	if ((dateIn.charAt(2) == FSLASH) &&
//		(dateIn.charAt(5) == FSLASH) &&
//		(dateIn.length()==10)) {
//	    String yearString = dateIn.substring(6, 10) ;
//	    String monthString = dateIn.substring(0, 2) ;
//	    String dayString = dateIn.substring(3, 5) ;
//	    int year  = Integer.parseInt(yearString) ;
//	    int month = Integer.parseInt(monthString) ;
//	    int day   = Integer.parseInt(dayString) ; 
//	    return LocalDate.of(year, month, day).plusDays(1) ;
//	}
//	throw new AssertionError(
//		"Bad date string of " +
//		dateIn +
//		" in getLatestStartDate."
//		) ;
//    }

//    private long getLatestStartRead(String in) {
//	return (long)Float.parseFloat(in) ;
//    }
    /*
     * 	DateTimeFormatter dtf = 
		DateTimeFormatter.ofPattern("MM'/'dd'/'yyyy") ;

     */

    private WebDriver login() {
	Context context = new Context() ;
	//
	// There are lots of warnings and INFO messages... ignore them.
	//
	System.out.println(
		"Ignoring messages and going to the login page.") ;
	context = saveContextAndHideMessages() ;
	try {
	    browser = new FirefoxDriver(firefoxOptions) ;
	    browser.get("https://smartmetertexas.com");
	    System.out.println("Went to the login page.") ;
	} finally {
	    //
	    // Done with ignoring warnings and INFO messages.  Restore...
	    //
	    System.out.println("Done ignoring messages.") ;
	    restoreContextAndUnhideMessages(context) ;
	}
	WebElement userid   = browser.findElement(By.name("userid")) ;
	WebElement password = browser.findElement(By.name("password")) ;

	String u = (String) Info.PreferencesEnum.keyUserID.getStoredValue() ;
	String p = (String) Info.PreferencesEnum.keyPassword.getStoredValue() ;
	msg("User ID  is " + u + ".") ;
	msg("Password is " + p + ".") ;
	sendStringToWebElement(u, userid) ; 
	sendStringToWebElement(p, password) ; 
	WebElement button   = 
		browser.findElement(By.xpath("//form//button")) ;
	button.click() ;
	waitForFirstPageAfterLogin() ;
	return browser ;
    }
    
    private void getData(WebDriver wd) {
	//	   private volatile LocalDate date ; // The date of this object.
//	DateTimeFormatter dtf = 
//		DateTimeFormatter.ofPattern("MM'/'dd'/'yyyy") ;
//	String formattedDate = dtf.format(date) ;
//	System.out.println() ;
//	System.out.println("Formatted date is " + formattedDate + " .") ;
//	System.out.println() ;
//	System.out.println() ;
	if (dataAvailable(wd)) {
	    getLatestEndMeterReadingAndUpdateCache(wd) ;
//	    scrapeData(wd, formattedDate) ;
	} else {
	    System.out.println() ;
	    System.out.println("Received " + NO_DATA) ;
	    System.out.println() ;
	    System.out.println() ;
	}
//	scrapeData(wd, "02/20/2020") ;
//	scrapeData(wd, "02/21/2020") ;
//	System.out.println() ;
    }

    private void logout() {
	//
	// Logging out appears to be UNNECESSARY.
	//
    }

    private void invoke() {
	browser = login() ;
//	getData(browser) ; 
	//
	// Unnecessary because also has calling chain:  
	//
	// login -> waitForFirstPageAfterLogin -> getData
	//
	logout() ;
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
    
//    private static String dataFound(WebDriver w) {
//	boolean weTextContainsSearchFor = false ; // The initialization
//	  // makes the Eclipse
//	  // compiler happy.
//	boolean weTextContainsNoData = false ; // The initialization
//	  // makes the Eclipse
//	  // compiler happy.
//	String weText = null ; // The initialization makes 
//			       // the Eclipse compiler happy.
//	WebElement we = null ;
//	for (WebElement e : w.findElements(By.xpath("//div"))) {
//	    try {
//		we = e ;
//		if (we !=null) weText = we.getText() ;
//		if (weText != null) {
//		    weTextContainsSearchFor = weText.contains(SEARCH_FOR) ;
//		    weTextContainsNoData    = weText.contains(NO_DATA) ;
//		}
//		if (weTextContainsSearchFor) return weText ;
//		if (weTextContainsNoData) {
//		    System.out.println();
//		    System.out.println("*** NO DATA IS AVAILABLE. ***") ;
//		    System.out.println();
//		    return null ;
//		}
//	    } catch (org.openqa.selenium.StaleElementReferenceException e1) {
//		System.out.println(" Caught SER Exception on: " + we + ".") ;
////		String s = e1.getMessage() ;
////		if (s != null) {
////		    System.out.println("Message is: " + s) ;
////		}
//		e1.printStackTrace(System.out) ;
//		System.out.println() ;
//		return null ;
//	    }
//	}
//	return null ;
//    }

    /**
     * @param browser
     * @param date
     */
//    private static boolean scrapeData(WebDriver browser, String date) {
//	boolean successful = false ;
//	String result = "" ;
//	List<WebElement> input = browser.findElements(By.xpath("//input")) ;
//	for (WebElement e : input) sendStringToWebElement(date, e) ;
//	List<WebElement> select = browser.findElements(By.xpath("//select")) ;
//	for (WebElement e : select) e.sendKeys("D") ;
//	
//	//
//	//  Results here.
//	//
//	
//	String results[] = {"", "", "", "", ""} ;
//	if (dataAvailable(browser)) {
//	    for (int i = 0; i < DATA_RETRY_LIMIT; i++) {
//		result = dataFound(browser);
//		if (result != null) break;
//		sleepMillis(DATA_RETRY_MILLIS);
//	    } 
//	}
//	if (result == null) {
//	    System.out.println();
//	    System.out.println("Could not find data after " + 
//		    DATA_RETRY_LIMIT + " tries and after " + 
//		    (DATA_RETRY_LIMIT*DATA_RETRY_MILLIS) + 
//		    " milliseconds for date: " + date + ".") ;
//	    System.out.println();
//	    System.exit(-1) ;
//	}
//	//
//	// The next line eliminates a compiler warning.
//	//
//	if (result == null) result = "" ;
//	//
//	//
//	//
//	result = result.substring(result.lastIndexOf(SEARCH_FOR)) ;
//	results = result.split("\\s");
//	if ( results[1].contentEquals(date) ) successful = true ;
//	if (successful) {
//	    System.out.println();
//	    System.out.println("Date        is " + results[1]);
//	    System.out.println("Start Read  is " + results[2]);
//	    System.out.println("End   Read  is " + results[3]);
//	    System.out.println("Consumption is " + results[4]);
//	    System.out.println();
//	}
//	for (WebElement e : select) e.sendKeys("E") ;
//	return successful ; 
//    }

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
/*
	WPLocation wpl =
		wp.indexOf("Latest End of Day Read") ;
	assertGoodLocation(wpl) ;
	int line = wpl.getLine() ;
	String endDate = wp.subString(
		line+1, 
		"<TD><SPAN name=\"ler_date\">", 
		"</SPAN></TD>"
		) ;
	String endValue = wp.subString(
		line+3, 
		"<TD><SPAN name=\"ler_read\">", 
		"</SPAN></TD>"
		) ;
	LocalDate startDate = getLatestStartDate(endDate) ;
	long startReading = getLatestStartRead(endValue) ;
	synchronized(cacheLock) {
	    cachedDate         = startDate ;
	    cachedMeterReading = startReading ;
	    cachedValuesValid = true ;
	}
	*/
	Runtime rt = new Runtime() ; // Gets start time.
//	System.out.println();
//	System.out.println("Finding...") ;
//	System.out.println();
//	List<WebElement> select = 
//		wd.findElements(By.xpath("//div[@class='last-meter-reading']")) ;
	WebElement select = 
		wd.findElement(By.xpath("//div[@class='last-meter-reading']")) ;
//	System.out.println("FOUND " + select.size() +
//		" ELEMENTS:") ;
//	System.out.println() ;
//	for (WebElement w : select) {
//	    System.out.println(">> with text:") ;
//	    System.out.println("#") ;
//	    String text = w.getText() ;
//	    System.out.println(text) ;
//	    System.out.println("#") ;
//	    readings = text.split("\\R") ;
	readings = select.getText().split("\\R") ;
//	    for (String s : text.split("\\R")) {
//		System.out.println("{"+ s + "}") ;
//	    }
	    /*
	     {Latest End of Day Read}  
	     {Date}  
	     {07/25/2020}  
	     {Time}  
	     {00:00:00}  
	     {Meter Read}  
	     {41725.018} 
	     */
//	}
	System.out.print("... done finding.  Took " + rt.measurement()/1000.0) ;
	System.out.println(" seconds.") ;

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
	System.out.println("Latest Start Date of " + startDate.toString() + 
		" has reading of " + startReading + ".") ;
	/*
	List<WebElement> select = wd.findElements(By.xpath("//div")) ;
	for (WebElement e : select) {
	    if (e.getText().equals("Latest End of Day Read")) {
		//WHAT TO DO ?
//		LocalDate latestStartDate = getLatestStartDate(wd) ;
//		long      latestStartReading = getLatestStartRead(wd) ;
		System.out.println();
		System.out.println("Found web element, with text " + 
			e.getText()) ;
		System.out.println() ;
		List<WebElement> subElements = 
			e.findElements(By.xpath("./descendant::div")) ;
		System.out.println() ;
		System.out.println("FOUND " + subElements.size() +
			" ELEMENTS:") ;
		System.out.println() ;
		for (WebElement w : subElements) {
		    System.out.println("  " + "with text " + w.getText()) ;
		}
		return ;
	    }
	}
	System.out.println() ;
	System.out.println("Could not get latest end of day read !") ;
	System.out.println() ;
	System.out.println() ;
	*/
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
     * @return the endRead
     */
//    public int getEndRead() {
//	int value ;
//	boolean dv ;
//	synchronized(lock) {
//	    value = endRead ;
//	    dv = dataValid ;
//	}
//	if (!dv) {
//	    invoke() ;  
//	    value = endRead ;
//	}
//	return value;
//    }

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
//	return accountInfo.getGreenStart() ;
    }
    
    @Override
    public int getGreenEnd() {
	return 1000 ; //  Ian Shef  ibs
//	return accountInfo.getGreenEnd() ;
    }
    
    static class AccountInfo {
	private String iD = "" ;
	private String password = "" ;
	private String greenStartString = "" ;
	private String greenEndString = "" ;
	private static final String charEncoding = "UTF-8" ;
	final URL url = this.getClass().getProtectionDomain().
		getCodeSource().getLocation() ;
	final String f1 = decode(url.getPath()) ;
	final String f2 = getClass().getSimpleName() + ".txt" ;
	final File f ;

	AccountInfo() {
	    //
	    // Using Util.makeArrayList(0) eliminates a compiler warning.
	    //
	    List<String> list  = Util.makeArrayList(0) ;
	    
	    if (f1.endsWith("/")) {
		f = new File(f1 + f2) ;  // Needed fo Linux.
	    } else {
		f = new File(f1 + "\\" + f2) ;  // Needed fo Windows.
	    }
	    if (!f.exists()) {
		throw new AssertionError("File " +
			f + " is missing.") ;
	    }
	    if (!f.canRead()) {
		throw new AssertionError("File " +
			f + " is not readable.") ;
	    }
	    try {
		list = Files.readAllLines(f.toPath()) ;
	    } catch (IOException e) {
		e.printStackTrace();
		throw new AssertionError("File " +
			f + " cannot be read for unexpected reason.") ;
	    }
	    int listSize = list.size() ;
	    if (listSize<4) {
		throw new AssertionError("File " +
			f + " needs at least four lines, has only " + 
			listSize + ".") ;
	    }
	    iD = list.get(0).toUpperCase() ;
	    password = list.get(1) ;
	    greenStartString = list.get(2) ;
	    greenEndString = list.get(3) ;
	}
	
	private static String decode(String s) {
		String result ;
		try {
			result = URLDecoder.decode(s, charEncoding);
		} catch (UnsupportedEncodingException e) {
			result = s ;
		}
		return result ;
	}
	
	String getID() {
	    return iD ;
	}
	String getPassword() {
	    return password ;
	}
	int getGreenStart() {
	    return Integer.parseUnsignedInt(greenStartString) ;
	}
	int getGreenEnd() {
	    return Integer.parseUnsignedInt(greenEndString) ;
	}
	@Override
	public String toString() {
	    return "File used is " + f  ;
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
		}
		sleepMillis(1000) ;
	    }
	    if (titleTriesRemaining == 0) {
		System.out.println("Failed to get title.") ;
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
}

