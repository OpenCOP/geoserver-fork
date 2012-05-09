package org.geoserver.webeoc.poller;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.web.data.webeoc.WebEOCCredentialsSerializedWrapper;
import org.geoserver.web.data.webeoc.WebEOCLayerInfo;
import org.geoserver.web.data.webeoc.WebEOCLayerInfoImpl;

public class WebEOCPollerTask extends TimerTask {
  // This class isn't managed by GeoServer, so the catalog has to be passed 
  // down into it.
  private Catalog catalog;
  
  public WebEOCPollerTask(Catalog catalog) {
    super();
    this.catalog = catalog;
  }

  @Override
  public void run() {
    // TODO: the polling logic
    System.out.println("  " + new Date().toString());

    // Get datastores
    List<DataStoreInfo> stores = catalog.getDataStores();

    for (DataStoreInfo store : stores) {
      // Only want to look at WebEOC datastores
      if (!WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(store.getType())) {
        continue;
      }
      
      // Get the connectionParameters for this datastore, where the WebEOC
      // credentials and WSDL URL are stored.
      // Also, the PostGIS connection info is in this map.
      Map<String, Serializable> connectionParameters = store.getConnectionParameters();
      // Extract the credentials from the connectionParameters map
      WebEOCCredentialsSerializedWrapper creds = new WebEOCCredentialsSerializedWrapper();
      creds.set(connectionParameters);
      
      // Get the WebEOC WSDL endpoint from the datastore connectionParameters map
      String wsdlUrl = connectionParameters.get(WebEOCConstants.WEBEOC_WSDL_KEY).toString();

      // Get all the layers for this WebEOC datastore
      List<FeatureTypeInfo> layers = catalog.getResourcesByStore(store, FeatureTypeInfo.class);
      for (FeatureTypeInfo featureType : layers) {
        // Get the WebEOC layer information from this featureType's metadata
        // map.
        WebEOCLayerInfo webeoc = new WebEOCLayerInfoImpl();
        webeoc.set(featureType.getMetadata().getMap());

        // Only poll the layer if the user wants to
        if (webeoc.isPollingEnabled()) {
          System.out.println("Polling URL(" + wsdlUrl + ") for layer: " + featureType.getName());
          System.out.println(creds.toString());
          System.out.println(webeoc.toString());
        }

      }

    }
  }
}
