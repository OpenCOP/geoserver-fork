package org.geoserver.web.data.webeoc;

/**
 *
 * @author yancy
 */
public class WebEOCLayerInfoImpl implements WebEOCLayerInfo {
  private String incident;
  private String board;
  private String view;
  private boolean pollingActive;

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
   * @return the pollingActive
   */
  @Override
  public boolean isPollingActive() {
    return pollingActive;
  }

  /**
   * @param pollingActive the pollingActive to set
   */
  @Override
  public void setPollingActive(boolean pollingActive) {
    this.pollingActive = pollingActive;
  }
  
}
