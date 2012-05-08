package org.geotools.data.webeoc;

import java.util.Collections;
import java.util.Map;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.Parameter;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

public class WebEOCDataStoreFactory extends PostgisNGDataStoreFactory {

    public static final Param WEBEOC_WSDL = new Param(WebEOCConstants.WEBEOC_WSDL_KEY, 
            String.class, "The WSDL address for the WebEOC service.", true);
    public static final Param WEBEOC_POSITION = new Param(WebEOCConstants.WEBEOC_POSITION_KEY, 
            String.class, "WebEOC Position", true);
    public static final Param WEBEOC_USER = new Param(WebEOCConstants.WEBEOC_USER_KEY, 
            String.class, "WebEOC User", true);
    public static final Param WEBEOC_PASSWORD = new Param(WebEOCConstants.WEBEOC_PASSWORD_KEY, 
            String.class, "WebEOC Password", true, null, 
            Collections.singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));
    public static final Param WEBEOC_POLLING_INTERVAL = new Param(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, 
            Integer.class, "WebEOC Polling Interval", true, WebEOCConstants.WEBEOC_POLLING_INTERVAL_DEFAULT);

    public WebEOCDataStoreFactory() {
        super();
    }

    @Override
    protected void setupParameters(Map parameters) {
        parameters.put(WEBEOC_WSDL.key, WEBEOC_WSDL);
        parameters.put(WEBEOC_POSITION.key, WEBEOC_POSITION);
        parameters.put(WEBEOC_USER.key, WEBEOC_USER);
        parameters.put(WEBEOC_PASSWORD.key, WEBEOC_PASSWORD);
        parameters.put(WEBEOC_POLLING_INTERVAL.key, WEBEOC_POLLING_INTERVAL);
        super.setupParameters(parameters);
    }

    @Override
    public String getDisplayName() {
        return WebEOCConstants.WEBEOC_DATASTORE_NAME;
    }

    @Override
    public String getDescription() {
        return WebEOCConstants.WEBEOC_DATASTORE_DESCRIPTION;
    }
}