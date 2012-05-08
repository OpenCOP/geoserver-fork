package com.geocent.webeoc;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.APISoap;
import com.esi911.webeoc7.api._1.WebEOCCredentials;

/**
 * A stateless wrapper around the WebEOC web services.
 */
public class WebEocWebService {

	/**
	 * Represents the WSDL-generated WebEOC API. It doesn't take any parameters
	 * because they're all hardcoded into the API source. Not my design...
	 */
	private static APISoap apiSoap = new API().getAPISoap();
	
	public static void main(String[] args) {
//		exerciseAllCalls();
		exerciseTimer();
	}
	
	private static void exerciseTimer() {
		Poller poller = Poller.getInstance();
		
		System.out.println("Just a few seconds...");
		poller.start(1000l);
		pause(3000l);
		poller.stop();
		
		System.out.println("A respectful waiting period.");
		pause(3000l);
		
		System.out.println("A 5 second polling period, cut short.");
		poller.start(5000l);
		pause(3000l);
		System.out.println("Poll now!");
		poller.pollNow();
		
		pause(1000l);
		System.out.println("A respectful waiting period.");
		pause(2000l);
		
		System.out.println("Poller is running (should be true): " + poller.isRunning());
 		
		poller.stop();
		System.out.println("Poller is running (should be false): " + poller.isRunning());
		
		System.out.println("Done.");
	}
	
	private static void pause(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For dev purposes only, exercise all calls with reasonable defaults.
	 */
	private static void exerciseAllCalls() {
		
		String username = "WebEOC Administrator";
		String password = "123456";
		String position = "Emergency Manager";
		BasicCredentials bcreds = new BasicCredentials(username, password, position);
		
		String incident = "UC San Diego";
		String board = "Shelters";
		String view = "List";
		
		// end of constant definitions
		
		System.out.println("Querying Incidents...");
		displayList(getIncidents(bcreds));
		
		System.out.println("\nQuerying Boards...");
		displayList(getBoards(bcreds, incident));
		
		System.out.println("\nQuerying Views...");
		displayList(getViews(bcreds, board));
		
		System.out.println("\nQuerying View Fields...");
		displayList(getViewFields(bcreds, board, view));
		
		System.out.println("\nGet Data...");
		System.out.println("  " + getData(bcreds, incident, board, view));
		
		try {
			System.out.println("Get Updated Data...");
			System.out.println("  " + getUpdatedData(bcreds, board, view, new Date()));
		} catch (DatatypeConfigurationException e) {
			System.out.println("Error: date-time conversion.");
			e.printStackTrace();
		}
		
		System.out.println("Done.");
	}

	/**
	 * For dev purposes only, display a list of strings in a pretty way.
	 */
	private static void displayList(List<String> list) {
		System.out.print("  " + list.size() + ": ");
		for(String i : list ) {
			System.out.print(i + "; ");
		}
	}
	
	/**
     * Convert Date -> XMLGregorianCalendar.  Yes, it really is this difficult.
	 * @throws DatatypeConfigurationException 
     */
	private static XMLGregorianCalendar convertDate(Date date) throws DatatypeConfigurationException {
	    GregorianCalendar gCal = new GregorianCalendar();
	    gCal.setTime(date);

	    XMLGregorianCalendar xGCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);
	    xGCal.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
	    return xGCal;
    }

	/**
	 * Return a list of all board names associated with an incident.
	 */
	public static List<String> getBoards(BasicCredentials bcreds, String incident) {
		return apiSoap.getBoardNames(bcreds.getWebEOCCredentials(incident)).getString();
	}
	
	/** 
	 * Return a list of all incident names.
	 */
	public static List<String> getIncidents(BasicCredentials bcreds) { 
		return apiSoap.getIncidents(bcreds.getWebEOCCredentialsNoIncident()).getString();
	}

	/** 
	 * Return a list of all views associated with a board.
	 */
	public static List<String> getViews(BasicCredentials bcreds, String boardName) {
		return apiSoap.getDisplayViews(bcreds.getWebEOCCredentialsNoIncident(), boardName).getString();
	}
	
	/** 
	 * Return a list of all view field names associated with the board and view.
	 */
	public static List<String> getViewFields(BasicCredentials bcreds, String board, String view) {
		return apiSoap.getViewFields(bcreds.getWebEOCCredentialsNoIncident(), board, view).getString();
	}

	/** 
	 * Return all xml data associated with a particular incident, board, and view.
	 */
	public static String getData(BasicCredentials bcreds, String incident, String board, String view) {
		return apiSoap.getData(bcreds.getWebEOCCredentials(incident), board, view);
	}

	/**
	 * Return all xml data associated with a particular incident, board, and
	 * view, that's occurred after the given date.
	 */
	public static String getUpdatedData(BasicCredentials bcreds, String board, String view, Date updatedSince)
			throws DatatypeConfigurationException {
		
		WebEOCCredentials creds = bcreds.getWebEOCCredentialsNoIncident();
		XMLGregorianCalendar date = convertDate(updatedSince);
		return apiSoap.getUpdatedData(creds, board, view, date);
	}
}
