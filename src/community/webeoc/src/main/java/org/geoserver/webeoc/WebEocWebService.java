package org.geoserver.webeoc;

import java.net.MalformedURLException;
import java.net.URL;
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
 * A wrapper around the WebEOC web services.
 */
public class WebEocWebService {

	private APISoap apiSoap;
	
	public WebEocWebService(String wsdlUrl) {
		URL url = null;
		System.out.println("Url is " + wsdlUrl);
		try {
			url = new URL(wsdlUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		apiSoap = new API(url).getAPISoap();
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
	public List<String> getBoards(BasicCredentials bcreds, String incident) {
		return apiSoap.getBoardNames(bcreds.getWebEOCCredentials(incident)).getString();
	}
	
	/** 
	 * Return a list of all incident names.
	 */
	public List<String> getIncidents(BasicCredentials bcreds) { 
		return apiSoap.getIncidents(bcreds.getWebEOCCredentialsNoIncident()).getString();
	}

	/** 
	 * Return a list of all views associated with a board.
	 */
	public List<String> getViews(BasicCredentials bcreds, String boardName) {
		return apiSoap.getDisplayViews(bcreds.getWebEOCCredentialsNoIncident(), boardName).getString();
	}
	
	/** 
	 * Return a list of all view field names associated with the board and view.
	 */
	public List<String> getViewFields(BasicCredentials bcreds, String board, String view) {
		return apiSoap.getViewFields(bcreds.getWebEOCCredentialsNoIncident(), board, view).getString();
	}

	/** 
	 * Return all xml data associated with a particular incident, board, and view.
	 */
	public String getData(BasicCredentials bcreds, String incident, String board, String view) {
		return apiSoap.getData(bcreds.getWebEOCCredentials(incident), board, view);
	}

	/**
	 * Return all xml data associated with a particular incident, board, and
	 * view, that's occurred after the given date.
	 */
	public String getUpdatedData(BasicCredentials bcreds, String board, String view, Date updatedSince)
			throws DatatypeConfigurationException {
		
		WebEOCCredentials creds = bcreds.getWebEOCCredentialsNoIncident();
		XMLGregorianCalendar date = convertDate(updatedSince);
		return apiSoap.getUpdatedData(creds, board, view, date);
	}
}
