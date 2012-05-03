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
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
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
    stores = new DropDownChoice("storesDropDown", new Model(), 
             new WebEOCStoreListModel(), new StoreListChoiceRenderer());
    stores.setOutputMarkupId(true);
    stores.setRequired(true);
    form.add(stores);

    // Add an onChange action to the stores drop down that uses the legend
    // ajax updater to change the legend graphic on the page.
    stores.add(new AjaxFormComponentUpdatingBehavior("onchange") {
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
    

    final IModel incidentChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (credentials == null || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getIncidents();
      }
    };
    incidents = new DropDownChoice("incidents", new PropertyModel(webeocLayerInfo, "incident"), incidentChoiceModel);
    incidents.setOutputMarkupId(true);
    incidents.setRequired(true);
    form.add(incidents);
    
    incidents.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        credentials.setIncident(getDefaultModelObjectAsString());
        
        if (target != null) {
          target.addComponent(boards);
        }
      }
    });

    
    final IModel boardChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (credentials == null || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getBoards();
      }
    };
    boards = new DropDownChoice("boards", new PropertyModel(webeocLayerInfo, "board"), boardChoiceModel);
    boards.setOutputMarkupId(true);
    boards.setRequired(true);
    form.add(boards);
    
    boards.add(new AjaxFormComponentUpdatingBehavior("onchange") {
      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        if (target != null) {
          target.addComponent(views);
        }
      }
    });

    
    final IModel viewChoiceModel = new AbstractReadOnlyModel() {
      public Object getObject() {
        if (credentials == null || null == webEOC ) {
          return Collections.EMPTY_LIST;
        }
        return getViews();
      }
    };
    views = new DropDownChoice("views", new PropertyModel(webeocLayerInfo, "view"), viewChoiceModel);
    views.setOutputMarkupId(true);
    views.setRequired(true);
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
    
//        CatalogBuilder builder = new CatalogBuilder(getCatalog());
//        builder.setStore((StoreInfo)stores.getDefaultModelObject());
//
//        // Build the geoserver feature type object
//        FeatureTypeInfo fti = builder.buildFeatureType(getFeatureSource(ds, layerTitle.getDefaultModelObjectAsString()));
//        // Set the bounding boxes to makes things happy
//        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
//        fti.setLatLonBoundingBox(world);
//        fti.setNativeBoundingBox(world);
//
//        // Build the geoserver layer object
//        LayerInfo layerInfo = builder.buildLayer(fti);
  }


  private List<String> getIncidents() {
    return webEOC.getAPISoap().getIncidents(credentials).getString();
  }

  private List<String> getBoards() {
    return webEOC.getAPISoap().getBoardNames(credentials).getString();
  }

  private List<String> getViews() {
    return webEOC.getAPISoap().getDisplayViews(credentials, 
            boards.getDefaultModelObjectAsString()).getString();
  }
}
