package org.geoserver.web.data.webeoc;

/**
 *
 * @author yancy
 */
public class WebEOCConstants {
  // Datastore Contstants
  public static final String WEBEOC_DATASTORE_NAME = "WebEOC";
  public static final String WEBEOC_DATASTORE_DESCRIPTION = "WebEOC Datastore.  This uses a PostGIS database as caching mechanism.  Credentials for both the Web EOC instance, and write access to a PostGIS database must be provided";
  public static final String WEBEOC_WSDL_KEY = "WebEOC WSDL";
  public static final String WEBEOC_POSITION_KEY = "WebEOC Position";
  public static final String WEBEOC_USER_KEY = "WebEOC Username";
  public static final String WEBEOC_PASSWORD_KEY = "WebEOC Password";
  public static final String WEBEOC_POLLING_INTERVAL_KEY = "WebEOC Polling Interval";
  public static final int    WEBEOC_POLLING_INTERVAL_DEFAULT = 720000;
  
  // Layer Constants
  public static final String WEBEOC_INCIDENT_KEY = "webeoc.incident";
  public static final String WEBEOC_BOARD_KEY = "webeoc.board";
  public static final String WEBEOC_VIEW_KEY = "webeoc.view";
  public static final String WEBEOC_POLLINGACTIVE_KEY = "webeoc.pollingActive";
  public static final String WEBEOC_LONFIELD_KEY = "webeoc.lonField";
  public static final String WEBEOC_LATFIELD_KEY = "webeoc.latField";
  public static final String WEBEOC_LASTUPDATEDFIELD_KEY = "webeoc.lastUpdatedField";

  public static final String WEBEOC_DEFAULT_GEOM_NAME = "the_geom";

  public static final int TABLE_NAME_MAXLENGTH = 63;
  
  public static final String DEFAULT_CRS = "EPSG:4326";
}
