package org.geoserver.webeoc;

import com.esi911.webeoc7.api._1.WebEOCCredentials;

/**
 * The {@link WebEOCCredentials} object has a pretty messed up idea of personal
 * credentials. This object contains the more standard items, and lets you
 * easily build a {@link WebEOCCredentials} object from there.
 */
public class BasicCredentials {
	private String username;
	private String password;
	private String position;
	
	@Override
	public String toString() {
		return "BasicCredentials [username=" + username + ", password="
				+ password + ", position=" + position + "]";
	}

	public BasicCredentials(String username, String password, String position) {
		this.username = username;
		this.password = password;
		this.position = position;
	}
	
	public WebEOCCredentials getWebEOCCredentialsNoIncident() {
		WebEOCCredentials creds = new WebEOCCredentials();
		creds.setUsername(username);
		creds.setPassword(password);
		creds.setPosition(position);
		return creds;
	}
	
	public WebEOCCredentials getWebEOCCredentials(String incident) {
		if( incident == null ) throw new IllegalArgumentException("Incident cannot be null");
		
		WebEOCCredentials creds = new WebEOCCredentials();
		creds.setUsername(username);
		creds.setPassword(password);
		creds.setPosition(position);
		creds.setIncident(incident);
		return creds;
	}
}
