/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.web.data.webeoc;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author yancy
 */
public interface WebEOCLayerInfo extends Serializable {

  /**
   * @return the board
   */
  String getBoard();

  /**
   * @return the incident
   */
  String getIncident();

  /**
   * @return the view
   */
  String getView();

  /**
   * @return the pollingEnabled
   */
  boolean isPollingEnabled();

  /**
   * @param board the board to set
   */
  void setBoard(String board);

  /**
   * @param incident the incident to set
   */
  void setIncident(String incident);

  /**
   * @param pollingEnabled the pollingEnabled to set
   */
  void setPollingEnabled(boolean pollingEnabled);

  /**
   * @param view the view to set
   */
  void setView(String view);

  /**
   * @return the lastUpdatedField
   */
  String getLastUpdatedField();

  /**
   * @return the latField
   */
  String getLatField();

  /**
   * @return the lonField
   */
  String getLonField();

  /**
   * @param lastUpdatedField the lastUpdatedField to set
   */
  void setLastUpdatedField(String lastUpdatedField);

  /**
   * @param latField the latField to set
   */
  void setLatField(String latField);

  /**
   * @param lonField the lonField to set
   */
  void setLonField(String lonField);

  Map<String, String> getAsMap();
  
}
