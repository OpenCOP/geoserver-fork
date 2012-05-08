package org.geoserver.web.data.resource;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.web.util.MapModel;
import org.geotools.data.webeoc.WebEOCDataStoreFactory;

/**
 *
 * @author yancy
 */
public class WebEOCTabPanel extends LayerEditTabPanel {

  public WebEOCTabPanel(String id, IModel model) {
    super(id, model);

    final LayerInfo layer = (LayerInfo) model.getObject();
    final ResourceInfo resource = layer.getResource();

    WebMarkupContainer notWebeocMessage = new WebMarkupContainer("notWebeocMessage");
    WebMarkupContainer webeocContainer = new WebMarkupContainer("webeocContainer");
    add(notWebeocMessage);
    add(webeocContainer);

    if (new WebEOCDataStoreFactory().getDisplayName()
            .equals(resource.getStore().getType())) {
       notWebeocMessage.setVisible(false);
       webeocContainer.setVisible(true);
    } else {
       notWebeocMessage.setVisible(true);
       webeocContainer.setVisible(false);
    }

    PropertyModel metadata = new PropertyModel(model, "resource.metadata");

    TextField<String> incident = new TextField<String>("incident", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_INCIDENT_KEY));
    incident.setEnabled(false);
    webeocContainer.add(incident);

    TextField<String> board = new TextField<String>("board", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_BOARD_KEY));
    board.setEnabled(false);
    webeocContainer.add(board);

    TextField<String> view = new TextField<String>("view", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_VIEW_KEY));
    view.setEnabled(false);
    webeocContainer.add(view);


    TextField<String> lon = new TextField<String>("lon", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_LONFIELD_KEY));
    lon.setEnabled(false);
    webeocContainer.add(lon);

    TextField<String> lat = new TextField<String>("lat", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_LATFIELD_KEY));
    lat.setEnabled(false);
    webeocContainer.add(lat);

    TextField<String> lastUpdated = new TextField<String>("lastUpdated", 
            new MapModel(metadata, WebEOCConstants.WEBEOC_LASTUPDATEDFIELD_KEY));
    lastUpdated.setEnabled(false);
    webeocContainer.add(lastUpdated);

    
  }
}
