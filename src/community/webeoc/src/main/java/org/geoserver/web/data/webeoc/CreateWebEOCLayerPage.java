package org.geoserver.web.data.webeoc;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.APISoap;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author yancy
 */
public class CreateWebEOCLayerPage extends GeoServerSecuredPage {

  static final CoordinateReferenceSystem WGS84;

  static {
    try {
      WGS84 = CRS.decode(WebEOCConstants.DEFAULT_CRS);
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
  private final WebEOCCredentialsSerializedWrapper credentials;
  private final WebEOCLayerInfo webeocLayerInfo;

  private String wsdlUrl;

  public CreateWebEOCLayerPage() {
    credentials = new WebEOCCredentialsSerializedWrapper();
    webeocLayerInfo = new WebEOCLayerInfoImpl();

    // create the form
    add(form = new Form("form"));

    // create all the dropdowns
    form.add(stores = getStoresDropDown());
    form.add(incidents = getIncidentsDropDown());
    form.add(boards = getBoardsDropDown());
    form.add(views = getViewsDropDown());

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
    String layername = layerTitle.getDefaultModelObjectAsString();
    Catalog catalog = getCatalog();

    // create table
    DataStore ds = null;
    DataStoreInfo dsInfo = null;
    try {
      // basic checks
      dsInfo = catalog.getDataStore(((StoreInfo) stores.getDefaultModelObject()).getId());
      ds = (DataStore) dsInfo.getDataStore(null);
      // Check if the layername already exists in the datastore
      if (Arrays.asList(ds.getTypeNames()).contains(layername)) {
        error(new ParamResourceModel("duplicateTypeName", this, dsInfo.getName(),
                layername).getString());
        return;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Convert the fields to a SimpleFeatureType
    SimpleFeatureType featureType = buildFeatureType(getViewFields(), layername);
    try {
      // Persist the SimpleFeatureType to the datastore
      ds.createSchema(featureType);
    } catch (IOException ex) {
      Logger.getLogger(CreateWebEOCLayerPage.class.getName()).log(Level.SEVERE, null, ex);
    }

    // Get the catalog builder and set it to use the selected datastore
    CatalogBuilder builder = new CatalogBuilder(catalog);
    builder.setStore(dsInfo);

    // Build the geoserver feature type object
    FeatureTypeInfo fti;
    try {
      fti = builder.buildFeatureType(getFeatureSource(ds, layername));
      // Set the bounding boxes to makes things happy
      ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
      fti.setSRS(WebEOCConstants.DEFAULT_CRS);
      fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
      fti.setNativeBoundingBox(world);
      fti.setLatLonBoundingBox(world);
      // Build the geoserver layer object
      LayerInfo layerInfo = builder.buildLayer(fti);
      layerInfo.setName(layername);
      // Put the WebEOC configs in the metadata for the feature type
      MetadataMap map = fti.getMetadata();
      map.put(WebEOCConstants.WEBEOC_INCIDENT_KEY, webeocLayerInfo.getIncident());
      map.put(WebEOCConstants.WEBEOC_BOARD_KEY, webeocLayerInfo.getBoard());
      map.put(WebEOCConstants.WEBEOC_VIEW_KEY, webeocLayerInfo.getView());

      // Save the layer and resource
      catalog.add(fti);
      catalog.add(layerInfo);
    } catch (IOException ex) {
      Logger.getLogger(CreateWebEOCLayerPage.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(DataStore ds, String name)
          throws IOException {
    try {
      return ds.getFeatureSource(name);
    } catch (IOException e) {
      // maybe it's Oracle?
      try {
        return ds.getFeatureSource(name.toUpperCase());
      } catch (Exception ora) {
        // nope, the reason was another one
        throw e;
      }
    }
  }

  SimpleFeatureType buildFeatureType(List<String> fields, String name) {
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    for (String field : fields) {
      // TODO: Make the bindings selectable
      builder.add(field, String.class);
    }
    builder.setName(name);
    return builder.buildFeatureType();
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
        credentials.setUsername(connectionParameters.get(WebEOCConstants.WEBEOC_USER_KEY).toString());
        credentials.setPassword(connectionParameters.get(WebEOCConstants.WEBEOC_PASSWORD_KEY).toString());
        credentials.setPosition(connectionParameters.get(WebEOCConstants.WEBEOC_POSITION_KEY).toString());

        wsdlUrl = connectionParameters.get(WebEOCConstants.WEBEOC_WSDL_KEY).toString();

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
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty()) {
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
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty()) {
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
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty()) {
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
//        System.out.println(getViewFields());
        // Create the column datatype editor
      }
    });
    return viewsChoice;
  }


  /*  WebEOC API calls  */
  private APISoap getWebEOC() {
    APISoap webEOC = null;
    try {
      webEOC = (new API(new URL(wsdlUrl))).getAPISoap();
    } catch (MalformedURLException e) {
      error("Bad WebEOC URL specified in datastore.");
    }
    return webEOC;
  }
  
  private List<String> getIncidents() {
    return getWebEOC().getIncidents(credentials.getWebEOCCredentials()).getString();
  }

  private List<String> getBoards() {
    return getWebEOC().getBoardNames(credentials.getWebEOCCredentials()).getString();
  }

  private List<String> getViews() {
    return getWebEOC().getDisplayViews(credentials.getWebEOCCredentials(),
            webeocLayerInfo.getBoard()).getString();
  }

  private List<String> getViewFields() {
    return getWebEOC().getViewFields(credentials.getWebEOCCredentials(),
            webeocLayerInfo.getBoard(),
            webeocLayerInfo.getView()).getString();
  }
}
