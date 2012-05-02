package org.geoserver.web.data.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.webeoc.WebEOCDataStoreFactory;

/**
 * Model providing the list of PostGIS stores.
 * 
 */
public class WebEOCStoreListModel extends LoadableDetachableModel<List<DataStoreInfo>> {

  private static final long serialVersionUID = -7742496075623731474L;

  @Override
  protected List<DataStoreInfo> load() {
    Catalog catalog = GeoServerApplication.get().getCatalog();
    ResourcePool resourcePool = catalog.getResourcePool();
    List<DataStoreInfo> stores = catalog.getStores(DataStoreInfo.class);
    List<DataStoreInfo> storesToKeep = catalog.getStores(DataStoreInfo.class);

    WebEOCDataStoreFactory datastoreFactory = new WebEOCDataStoreFactory() {};
    String datastoreFactoryName = datastoreFactory.getDisplayName();

    for (DataStoreInfo store : stores) {
      DataAccessFactory factory = null;
      try {
        factory = resourcePool.getDataStoreFactory(store);
      } catch (IOException ex) {
        Logger.getLogger(WebEOCStoreListModel.class.getName()).log(Level.SEVERE, null, ex);
      }
      if (factory == null || !datastoreFactoryName.equals(factory.getDisplayName())) {
        storesToKeep.remove(store);
      }
    }

    stores = new ArrayList<DataStoreInfo>(storesToKeep);
    Collections.sort(stores, new Comparator<StoreInfo>() {

      public int compare(StoreInfo o1, StoreInfo o2) {
        if (o1.getWorkspace().equals(o2.getWorkspace())) {
          return o1.getName().compareTo(o2.getName());
        }
        return o1.getWorkspace().getName().compareTo(o2.getWorkspace().getName());
      }
    });
    return stores;
  }
}