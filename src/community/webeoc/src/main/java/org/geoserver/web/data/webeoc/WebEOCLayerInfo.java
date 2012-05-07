/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.web.data.webeoc;

import java.io.Serializable;

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
   * @return the pollingActive
   */
  boolean isPollingActive();

  /**
   * @param board the board to set
   */
  void setBoard(String board);

  /**
   * @param incident the incident to set
   */
  void setIncident(String incident);

  /**
   * @param pollingActive the pollingActive to set
   */
  void setPollingActive(boolean pollingActive);

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
  
}
