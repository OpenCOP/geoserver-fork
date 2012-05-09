package org.geoserver.web.data.webeoc;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author yancy
 */
public class WebEOCLayerInfoImpl implements WebEOCLayerInfo {
  private String incident;
  private String board;
  private String view;
  private boolean pollingEnabled;

  private String lonField;
  private String latField;
  private String lastUpdatedField;
  
  /**
   * Convenience setter for getting the WebEOC info out of the featureType's
   * metadata map.
   * 
   * @param map 
   */
  public void set(Map<String, Serializable> map) {
    if (null != map.get(WebEOCConstants.WEBEOC_INCIDENT_KEY))
      incident = String.valueOf(map.get(WebEOCConstants.WEBEOC_INCIDENT_KEY));
    if (null != map.get(WebEOCConstants.WEBEOC_BOARD_KEY))
      board = String.valueOf(map.get(WebEOCConstants.WEBEOC_BOARD_KEY));
    if (null != map.get(WebEOCConstants.WEBEOC_VIEW_KEY))
      view = String.valueOf(map.get(WebEOCConstants.WEBEOC_VIEW_KEY));
    if (null != map.get(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY))
      pollingEnabled = Boolean.valueOf(String.valueOf(map.get(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY)));
    if (null != map.get(WebEOCConstants.WEBEOC_LONFIELD_KEY))
      lonField = String.valueOf(map.get(WebEOCConstants.WEBEOC_LONFIELD_KEY));
    if (null != map.get(WebEOCConstants.WEBEOC_LATFIELD_KEY))
      latField = String.valueOf(map.get(WebEOCConstants.WEBEOC_LATFIELD_KEY));
    if (null != map.get(WebEOCConstants.WEBEOC_LASTUPDATEDFIELD_KEY))
      lastUpdatedField = String.valueOf(map.get(WebEOCConstants.WEBEOC_LASTUPDATEDFIELD_KEY));
  }

  public Map<String, String> getAsMap() {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put(WebEOCConstants.WEBEOC_INCIDENT_KEY, incident);
    map.put(WebEOCConstants.WEBEOC_BOARD_KEY, board);
    map.put(WebEOCConstants.WEBEOC_VIEW_KEY, view);
    map.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, String.valueOf(pollingEnabled));
    map.put(WebEOCConstants.WEBEOC_LONFIELD_KEY, lonField);
    map.put(WebEOCConstants.WEBEOC_LATFIELD_KEY, latField);
    map.put(WebEOCConstants.WEBEOC_LASTUPDATEDFIELD_KEY, lastUpdatedField);
    return map;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[Incident=").append(incident)
           .append(",Board=").append(board)
           .append(",View=").append(view)
           .append(",PollingEnabled=").append(pollingEnabled)
           .append(",LonField=").append(lonField)
           .append(",LatField=").append(latField)
           .append(",LastUpdated=").append(lastUpdatedField)
           .append("]");
    return builder.toString();
  }

  /**
   * @return the incident
   */
  @Override
  public String getIncident() {
    return incident;
  }

  /**
   * @param incident the incident to set
   */
  @Override
  public void setIncident(String incident) {
    this.incident = incident;
  }

  /**
   * @return the board
   */
  @Override
  public String getBoard() {
    return board;
  }

  /**
   * @param board the board to set
   */
  @Override
  public void setBoard(String board) {
    this.board = board;
  }

  /**
   * @return the view
   */
  @Override
  public String getView() {
    return view;
  }

  /**
   * @param view the view to set
   */
  @Override
  public void setView(String view) {
    this.view = view;
  }

  /**
   * @return the pollingEnabled
   */
  @Override
  public boolean isPollingEnabled() {
    return pollingEnabled;
  }

  /**
   * @param pollingEnabled the pollingEnabled to set
   */
  @Override
  public void setPollingEnabled(boolean pollingEnabled) {
    this.pollingEnabled = pollingEnabled;
  }

  /**
   * @return the lonField
   */
  public String getLonField() {
    return lonField;
  }

  /**
   * @param lonField the lonField to set
   */
  public void setLonField(String lonField) {
    this.lonField = lonField;
  }

  /**
   * @return the latField
   */
  public String getLatField() {
    return latField;
  }

  /**
   * @param latField the latField to set
   */
  public void setLatField(String latField) {
    this.latField = latField;
  }

  /**
   * @return the lastUpdatedField
   */
  public String getLastUpdatedField() {
    return lastUpdatedField;
  }

  /**
   * @param lastUpdatedField the lastUpdatedField to set
   */
  public void setLastUpdatedField(String lastUpdatedField) {
    this.lastUpdatedField = lastUpdatedField;
  }
}
