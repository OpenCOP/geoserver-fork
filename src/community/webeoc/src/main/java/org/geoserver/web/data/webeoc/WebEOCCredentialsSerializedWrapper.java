package org.geoserver.web.data.webeoc;

import com.esi911.webeoc7.api._1.WebEOCCredentials;
import java.io.Serializable;

/**
 *
 * @author yancy
 */
public class WebEOCCredentialsSerializedWrapper implements Serializable {

  private String username;
  private String password;
  private String position;
  private String jurisdiction;
  private String incident;

  public WebEOCCredentials getWebEOCCredentials() {
    WebEOCCredentials creds = new WebEOCCredentials();
    creds.setUsername(username);
    creds.setPassword(password);
    creds.setPosition(position);
    creds.setJurisdiction(jurisdiction);
    creds.setIncident(incident);
    return creds;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return the position
   */
  public String getPosition() {
    return position;
  }

  /**
   * @param position the position to set
   */
  public void setPosition(String position) {
    this.position = position;
  }

  /**
   * @return the jurisdiction
   */
  public String getJurisdiction() {
    return jurisdiction;
  }

  /**
   * @param jurisdiction the jurisdiction to set
   */
  public void setJurisdiction(String jurisdiction) {
    this.jurisdiction = jurisdiction;
  }

  /**
   * @return the incident
   */
  public String getIncident() {
    return incident;
  }

  /**
   * @param incident the incident to set
   */
  public void setIncident(String incident) {
    this.incident = incident;
  }
}
