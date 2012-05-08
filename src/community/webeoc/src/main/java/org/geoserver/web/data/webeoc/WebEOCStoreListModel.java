package org.geoserver.web.data.webeoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Model providing the list of WebEOC datastores.
 * 
 */
public class WebEOCStoreListModel extends LoadableDetachableModel<List<DataStoreInfo>> {

  private static final long serialVersionUID = -7742496075623731474L;

  @Override
  protected List<DataStoreInfo> load() {
    Catalog catalog = GeoServerApplication.get().getCatalog();
    List<DataStoreInfo> stores = catalog.getDataStores();
    List<DataStoreInfo> storesToKeep = new ArrayList<DataStoreInfo>();

    for (DataStoreInfo store : stores) {
      if (WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(store.getType())) {
        storesToKeep.add(store);
      }
    }

    Collections.sort(storesToKeep, new Comparator<StoreInfo>() {

      public int compare(StoreInfo o1, StoreInfo o2) {
        if (o1.getWorkspace().equals(o2.getWorkspace())) {
          return o1.getName().compareTo(o2.getName());
        }
        return o1.getWorkspace().getName().compareTo(o2.getWorkspace().getName());
      }
    });
    return storesToKeep;
  }
}