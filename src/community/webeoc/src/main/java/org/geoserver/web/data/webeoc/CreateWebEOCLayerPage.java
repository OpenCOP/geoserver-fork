package org.geoserver.web.data.webeoc;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.ArrayOfString;
import com.esi911.webeoc7.api._1.WebEOCCredentials;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geotools.data.webeoc.WebEOCDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author yancy
 */
public class CreateWebEOCLayerPage extends GeoServerSecuredPage {
  public static final String WEBEOC_INCIDENT = "webeocIncident";
  public static final String WEBEOC_BOARD = "webeocBoard";
  public static final String WEBEOC_VIEW = "webeocView";
  
  static final CoordinateReferenceSystem WGS84;

  static {
    try {
      WGS84 = CRS.decode("EPSG:4326");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private final Form form;
  private final DropDownChoice stores;
  private final DropDownChoice incidents;
  private final DropDownChoice boards;
  private final DropDownChoice views;
  private final TextField<String> layerTitle;
  private final WebEOCCredentials credentials;
  private final WebEOCLayerInfo webeocLayerInfo;
  
		API webEOC;
  
  public CreateWebEOCLayerPage() {
    credentials = new WebEOCCredentials();
    webeocLayerInfo = new WebEOCLayerInfoImpl();
    
    // create the form
    form = new Form("form");
    add(form);
    
    // create the datastore picker, only include WebEOC stores
    stores = getStoresDropDown();
    form.add(stores);
    
    incidents = getIncidentsDropDown();
    form.add(incidents);

    boards = getBoardsDropDown();
    form.add(boards);
    
    views = getViewsDropDown();
    form.add(views);

    // create the title field
    form.add(layerTitle = new TextField<String>("layerTitle", new Model<String>()));

    // create the save and cancel buttons
    form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
    SubmitLink saveLink = new SubmitLink("save", form) {
      @Override
      public void onSubmit() {
        submit();
      }
    };
    form.add(saveLink);
    form.setDefaultButton(saveLink);
  }


  private void submit() {
    Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore((StoreInfo)stores.getDefaultModelObject());
//
//        // Build the geoserver feature type object
//        FeatureTypeInfo fti = builder.buildFeatureType(getFeatureSource(ds, layerTitle.getDefaultModelObjectAsString()));
//        // Set the bounding boxes to makes things happy
//        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
//        fti.setLatLonBoundingBox(world);
//        fti.setNativeBoundingBox(world);
//
        // Build the geoserver layer object
//        LayerInfo layerInfo = builder.buildLayer(fti);
        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setName(layerTitle.getDefaultModelObjectAsString());
        MetadataMap map = layerInfo.getMetadata();
        map.put(WEBEOC_INCIDENT, webeocLayerInfo.getIncident());
        map.put(WEBEOC_BOARD, webeocLayerInfo.getBoard());
        map.put(WEBEOC_VIEW, webeocLayerInfo.getView());
        
    // Save the layer and resource
    catalog.save(layerInfo);
  }


  private DropDownChoice getStoresDropDown() {
    DropDownChoice storesChoice = new DropDownChoice("storesDropDown", new Model(), 
             new WebEOCStoreListModel(), new StoreListChoiceRenderer());
    storesChoice.setOutputMarkupId(true);
    storesChoice.setRequired(true);

    // Add an onChange action to the stores drop down that uses the legend
    // ajax updater to change the legend graphic on the page.
    storesChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
        Map<String, Serializable> connectionParameters = storeInfo.getConnectionParameters();
        credentials.setUsername(connectionParameters.get(WebEOCDataStoreFactory.WEBEOC_USER.key).toString());
        credentials.setPassword(connectionParameters.get(WebEOCDataStoreFactory.WEBEOC_PASSWORD.key).toString());
        credentials.setPosition(connectionParameters.get(WebEOCDataStoreFactory.WEBEOC_POSITION.key).toString());
        
        try {
          webEOC = new API(new URL(
              connectionParameters.get(WebEOCDataStoreFactory.WEBEOC_WSDL.key).toString()));
        } catch (MalformedURLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        if (target != null) {
          target.addComponent(incidents);
        }
      }
    });
    return storesChoice;
  }

  private DropDownChoice getIncidentsDropDown() {
    final IModel incidentChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (null == credentials || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getIncidents();
      }
    };
    DropDownChoice incidentsChoice = new DropDownChoice("incidents", 
            new PropertyModel(webeocLayerInfo, "incident"), incidentChoiceModel);
    incidentsChoice.setOutputMarkupId(true);
    incidentsChoice.setRequired(true);
    
    incidentsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        credentials.setIncident(getDefaultModelObjectAsString());
        
        if (target != null) {
          target.addComponent(boards);
        }
      }
    });
    return incidentsChoice;
  }

  private DropDownChoice getBoardsDropDown() {
    final IModel boardChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (credentials == null || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getBoards();
      }
    };
    DropDownChoice boardsChoice = new DropDownChoice("boards", 
            new PropertyModel(webeocLayerInfo, "board"), boardChoiceModel);
    boardsChoice.setOutputMarkupId(true);
    boardsChoice.setRequired(true);
    
    boardsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        if (target != null) {
          target.addComponent(views);
        }
      }
    });
    return boardsChoice;
  }

  private DropDownChoice getViewsDropDown() {   
    final IModel viewChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (credentials == null || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getViews();
      }
    };
    DropDownChoice viewsChoice = new DropDownChoice("views", 
            new PropertyModel(webeocLayerInfo, "view"), viewChoiceModel);
    viewsChoice.setOutputMarkupId(true);
    viewsChoice.setRequired(true);

    viewsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        System.out.println(getViewFields());
      }
    });
    return viewsChoice;
  }
  

  /*  WebEOC API calls  */
  private List<String> getIncidents() {
    return webEOC.getAPISoap().getIncidents(credentials).getString();
  }

  private List<String> getBoards() {
    return webEOC.getAPISoap().getBoardNames(credentials).getString();
  }

  private List<String> getViews() {
    return webEOC.getAPISoap().getDisplayViews(credentials, 
            webeocLayerInfo.getBoard()).getString();
  }

  private List<String> getViewFields() {
    return webEOC.getAPISoap().getViewFields(credentials, 
            webeocLayerInfo.getBoard(),
            webeocLayerInfo.getView()).getString();
  }
}
