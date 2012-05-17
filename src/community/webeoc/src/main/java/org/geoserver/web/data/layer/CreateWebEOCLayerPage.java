package org.geoserver.web.data.layer;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.APISoap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
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
import javax.xml.ws.WebServiceException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layer.AttributeEditPage.BindingChoiceRenderer;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.web.data.webeoc.WebEOCCredentialsSerializedWrapper;
import org.geoserver.web.data.webeoc.WebEOCLayerInfo;
import org.geoserver.web.data.webeoc.WebEOCLayerInfoImpl;
import org.geoserver.web.data.webeoc.WebEOCStoreListModel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.web.publish.LegendGraphicAjaxUpdaterPublic;
import org.geoserver.wms.web.publish.StyleChoiceRenderer;
import org.geoserver.wms.web.publish.StylesModel;
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
  private final TextField<String> layerNameField;
  private final TextField<String> layerTitleField;
  private final Model<StyleInfo> defaultStyleModel;
  private final WebEOCCredentialsSerializedWrapper credentials;
  private final WebEOCLayerInfo webeocLayerInfo;
  WebEOCAttributesProvider attributesProvider;
  GeoServerTablePanel<AttributeDescription> attributeTable;
  private String wsdlUrl;

  private String layerName;
  private String layerTitle;

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

    // create the name field
    form.add(layerNameField = new TextField<String>("layerName", new PropertyModel<String>(this, "layerName")));
    layerNameField.add(StringValidator.maximumLength(WebEOCConstants.TABLE_NAME_MAXLENGTH));
    layerNameField.add(new PatternValidator("[A-Za-z_]\\w*"));
    layerNameField.setOutputMarkupId(true);
    layerNameField.setRequired(true);
    
    // create the title field
    form.add(layerTitleField = new TextField<String>("layerTitle", new PropertyModel<String>(this, "layerTitle")));
    layerTitleField.setOutputMarkupId(true);
    layerTitleField.setRequired(true);

    // Add the style picker elements
    defaultStyleModel = new Model<StyleInfo>();
    defaultStyleModel.setObject(getCatalog().getStyleByName(WebEOCConstants.DEFAULT_STYLE_NAME));
    addStylePicker(form);

    // Add the remove selected attributes link
    form.add(new GeoServerAjaxFormLink("removeSelected", form) {

      @Override
      public void onClick(AjaxRequestTarget target, Form form) {
        attributesProvider.removeAll(attributeTable.getSelection());
        attributeTable.clearSelection();
        target.addComponent(form);
      }
    });
    
    // Create the radio button groups
    final RadioGroup lonRadioGroup = new RadioGroup("lonRadioGroup", new PropertyModel(webeocLayerInfo, "lonField"));
    final RadioGroup latRadioGroup = new RadioGroup("latRadioGroup", new PropertyModel(webeocLayerInfo, "latField"));
    final RadioGroup lastUpdatedRadioGroup = new RadioGroup("lastUpdatedRadioGroup", new PropertyModel(webeocLayerInfo, "lastUpdatedField"));

    // Create the schema table
    attributesProvider = new WebEOCAttributesProvider();
    attributeTable = new GeoServerTablePanel<AttributeDescription>("attributes",
            attributesProvider, true) {

      @Override
      protected Component getComponentForProperty(String id, IModel itemModel,
              Property<AttributeDescription> property) {
        AttributeDescription att = (AttributeDescription) itemModel.getObject();
        if (property == WebEOCAttributesProvider.NAME) {
          return new Label(id, att.getName());
        } else if (property == WebEOCAttributesProvider.BINDING) {
          Fragment f = new Fragment(id, "bindingFragment", CreateWebEOCLayerPage.this);
          DropDownChoice choice = new DropDownChoice("binding", new PropertyModel(itemModel, "binding"),
                  WebEOCAttributesProvider.BINDINGS, new BindingChoiceRenderer());
          choice.setOutputMarkupId(true);
          f.add(choice);
          return f;
        } else if (property == WebEOCAttributesProvider.LON) {
          Fragment f = new Fragment(id, "lonFieldFragment", CreateWebEOCLayerPage.this);
          f.add(new Radio("lonField", new PropertyModel(itemModel, "name"), lonRadioGroup));
          return f;
        } else if (property == WebEOCAttributesProvider.LAT) {
          Fragment f = new Fragment(id, "latFieldFragment", CreateWebEOCLayerPage.this);
          f.add(new Radio("latField", new PropertyModel(itemModel, "name"), latRadioGroup));
          return f;
        } else if (property == WebEOCAttributesProvider.LAST_UPDATED) {
          Fragment f = new Fragment(id, "lastUpdatedFieldFragment", CreateWebEOCLayerPage.this);
          f.add(new Radio("lastUpdatedField", new PropertyModel(itemModel, "name"), lastUpdatedRadioGroup));
          return f;
        }

        return null;
      }
    };
    attributeTable.setOutputMarkupId(true);
    attributeTable.setSortable(false);
    attributeTable.setFilterable(false);
    attributeTable.getBottomPager().setVisible(false);
    
    // Add the table and radiogroups to the form
    lastUpdatedRadioGroup.add(attributeTable);
    latRadioGroup.add(lastUpdatedRadioGroup);
    lonRadioGroup.add(latRadioGroup);
    form.add(lonRadioGroup);

    // create the save and cancel buttons
    form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
    SubmitLink saveLink = new SubmitLink("save", form) {

      @Override
      public void onSubmit() {
        submit();
      }
    };
    
    webeocLayerInfo.setLatField(WebEOCConstants.WEBEOC_DEFAULT_LATITUDENAME);
    webeocLayerInfo.setLonField(WebEOCConstants.WEBEOC_DEFAULT_LONGITUDENAME);
    webeocLayerInfo.setLastUpdatedField(WebEOCConstants.WEBEOC_DEFAULT_LASTUPDATEDNAME);
    
    form.add(saveLink);
    form.setDefaultButton(saveLink);
  }

  private void submit() {
    Catalog catalog = getCatalog();

    // create table
    DataStore ds = null;
    DataStoreInfo dsInfo = null;
    try {
      // basic checks
      dsInfo = catalog.getDataStore(((StoreInfo) stores.getDefaultModelObject()).getId());
      ds = (DataStore) dsInfo.getDataStore(null);
      // Check if the layername already exists in the datastore
      if (Arrays.asList(ds.getTypeNames()).contains(layerName)) {
        error(new ParamResourceModel("duplicateTypeName", this, dsInfo.getName(),
                layerName).getString());
        return;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Convert the fields to a SimpleFeatureType
    SimpleFeatureType featureType = null;
    String lonField = webeocLayerInfo.getLonField();
    String latField = webeocLayerInfo.getLatField();
    // check to see if lon/lat are defined
    if (null != lonField && !lonField.trim().isEmpty() && 
        null != latField && !latField.trim().isEmpty()) {
      // create the geom attribute
      AttributeDescription geom = new AttributeDescription();
      geom.setBinding(Point.class);
      geom.setName(WebEOCConstants.WEBEOC_DEFAULT_GEOM_NAME);
      List<AttributeDescription> attrs = attributesProvider.getAttributes();
      attrs.add(geom);
      featureType = buildFeatureType(attrs, layerName);
    } else {
      featureType = buildFeatureType(attributesProvider.getAttributes(), layerName);
    }
      
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
      fti = builder.buildFeatureType(getFeatureSource(ds, layerName));
      // Set the bounding boxes to makes things happy
      ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
      fti.setSRS(WebEOCConstants.DEFAULT_CRS);
      fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
      fti.setNativeBoundingBox(world);
      fti.setLatLonBoundingBox(world);
      // Build the geoserver layer object
      LayerInfo layerInfo = builder.buildLayer(fti);
      layerInfo.setName(layerName);
      layerInfo.getResource().setTitle(layerTitle);
      // Get the chosen style
      StyleInfo styleInfo = defaultStyleModel.getObject();
      // Set the default style for the layer
      layerInfo.setDefaultStyle(styleInfo);
      // Set pollingEnabled to true by default
      webeocLayerInfo.setPollingEnabled(true);
      // Put the WebEOC configs in the metadata for the feature type
      MetadataMap map = fti.getMetadata();
      map.putAll(webeocLayerInfo.getAsMap());

      // Save the layer and resource
      catalog.add(fti);
      catalog.add(layerInfo);
      
      // Go to the layer edit page
      setResponsePage(new ResourceConfigurationPage(layerInfo, false));
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

  SimpleFeatureType buildFeatureType(List<AttributeDescription> attributes, String name) {
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    for (AttributeDescription attribute : attributes) {
      if (Geometry.class.isAssignableFrom(attribute.getBinding())) {
        builder.add(attribute.getName(), attribute.getBinding(), attribute.getCrs());
      } else {
        builder.add(attribute.getName(), attribute.getBinding());
      }
    }
    builder.setName(name);
    return builder.buildFeatureType();
  }

  private DropDownChoice getStoresDropDown() {
    DropDownChoice storesChoice = new DropDownChoice("storesDropDown", new Model(),
            new WebEOCStoreListModel(), new StoreListChoiceRenderer());
    storesChoice.setOutputMarkupId(true);
    storesChoice.setRequired(true);

    storesChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
        Map<String, Serializable> connectionParameters = storeInfo.getConnectionParameters();
        // Get the WebEOC credentials from the datastore object
        credentials.setUsername(connectionParameters.get(WebEOCConstants.WEBEOC_USER_KEY).toString());
        credentials.setPassword(connectionParameters.get(WebEOCConstants.WEBEOC_PASSWORD_KEY).toString());
        credentials.setPosition(connectionParameters.get(WebEOCConstants.WEBEOC_POSITION_KEY).toString());

        // Set the incindent/board/view to null in case the user is going backwards
        webeocLayerInfo.setIncident(null);
        webeocLayerInfo.setBoard(null);
        webeocLayerInfo.setView(null);
        attributesProvider.removeAll(attributesProvider.getAttributes());

        // Get the WebEOC WSDL endpoint from the datastore object
        wsdlUrl = connectionParameters.get(WebEOCConstants.WEBEOC_WSDL_KEY).toString();

        if (target != null) {
          target.addComponent(incidents);
          target.addComponent(boards);
          target.addComponent(views);
          target.addComponent(attributeTable);
          target.addComponent(feedbackPanel);
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
        try {
          return getIncidents();
        } catch (Exception e) {
          error("Error getting incidents: " + e.getLocalizedMessage());
          return Collections.EMPTY_LIST;
        }
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
        
        // Set the board/view to null in case the user is going backwards
        webeocLayerInfo.setBoard(null);
        webeocLayerInfo.setView(null);
        attributesProvider.removeAll(attributesProvider.getAttributes());

        if (target != null) {
          target.addComponent(boards);
          target.addComponent(views);
          target.addComponent(attributeTable);
          target.addComponent(feedbackPanel);
        }
      }
    });
    return incidentsChoice;
  }

  private DropDownChoice getBoardsDropDown() {
    final IModel boardChoiceModel = new AbstractReadOnlyModel() {

      public Object getObject() {
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty() 
                || null == webeocLayerInfo.getIncident()) {
          return Collections.EMPTY_LIST;
        }
        try {
          return getBoards();
        } catch (Exception e) {
          error("Error getting boards: " + e.getLocalizedMessage());
          return Collections.EMPTY_LIST;
        }
      }
    };
    DropDownChoice boardsChoice = new DropDownChoice("boards",
            new PropertyModel(webeocLayerInfo, "board"), boardChoiceModel);
    boardsChoice.setOutputMarkupId(true);
    boardsChoice.setRequired(true);

    boardsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        // Set the view to null in case the user is going backwards
        webeocLayerInfo.setView(null);
        attributesProvider.removeAll(attributesProvider.getAttributes());
        
        if (target != null) {
          target.addComponent(views);
          target.addComponent(attributeTable);
          target.addComponent(feedbackPanel);
        }
      }
    });
    return boardsChoice;
  }

  private DropDownChoice getViewsDropDown() {
    final IModel viewChoiceModel = new AbstractReadOnlyModel() {

      public Object getObject() {
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty() 
                || null == webeocLayerInfo.getIncident()
                || null == webeocLayerInfo.getBoard()) {
          return Collections.EMPTY_LIST;
        }
        try {
          return getViews();
        } catch (Exception e) {
          error("Error getting views: " + e.getLocalizedMessage());
          return Collections.EMPTY_LIST;
        }
      }
    };
    DropDownChoice viewsChoice = new DropDownChoice("views",
            new PropertyModel(webeocLayerInfo, "view"), viewChoiceModel);
    viewsChoice.setOutputMarkupId(true);
    viewsChoice.setRequired(true);

    viewsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        if (null == credentials || null == wsdlUrl || wsdlUrl.isEmpty() 
                || null == webeocLayerInfo.getIncident()
                || null == webeocLayerInfo.getBoard()
                || null == webeocLayerInfo.getView()) {
          return; 
        }
        // Create the column datatype editor
        List<String> fields = Collections.EMPTY_LIST;
        try {
          fields = getViewFields();
        } catch (Exception e) {
          error("Error getting fields: " + e.getLocalizedMessage());
          // continue on with an empty list to clear out old fields if the
          // user is changing views and an error occurred.
        }
        attributesProvider.removeAll(attributesProvider.getAttributes());
        attributeTable.clearSelection();
        for (String field : fields) {
          AttributeDescription attr = new AttributeDescription();
          attr.setName(field);
          attributesProvider.addNewAttribute(attr);
        }
        attributeTable.setItemsPerPage(attributesProvider.size());

        // Set the layerName and layerTitle to appropriate defaults based on
        // the incident-board-view combination
        layerName = getDefaultLayerName();
        layerTitle = getDefaultLayerTitle();
        
        if (target != null) {
          target.addComponent(attributeTable);
          target.addComponent(layerNameField);
          target.addComponent(layerTitleField);
          target.addComponent(feedbackPanel);
        }
      }
    });
    return viewsChoice;
  }

  private void addStylePicker(Form form) {
    // default styleXml chooser. A default styleXml is required
    final DropDownChoice defaultStyle = new DropDownChoice("defaultStyle",
            defaultStyleModel,
            new StylesModel(), new StyleChoiceRenderer());
    defaultStyle.setOutputMarkupId(true);
    defaultStyle.setRequired(true);
    form.add(defaultStyle);

    // Add the Style's legend graphic to the page
    final Image defStyleImg = new Image("defaultStyleLegendGraphic");
    defStyleImg.setOutputMarkupId(true);
    form.add(defStyleImg);

    // Add a legend graphic ajax updater object
    String wmsURL = getRequest().getRelativePathPrefixToContextRoot();
    wmsURL += wmsURL.endsWith("/") ? "wms?" : "/wms?";
    final LegendGraphicAjaxUpdaterPublic defaultStyleUpdater;
    defaultStyleUpdater = new LegendGraphicAjaxUpdaterPublic(wmsURL, defStyleImg, defaultStyleModel);

    // Add an onChange action to the styleXml drop down that uses the legend
    // ajax updater to change the legend graphic on the page.
    defaultStyle.add(new OnChangeAjaxBehavior() {

      @Override
      protected void onUpdate(AjaxRequestTarget target) {
        defaultStyleUpdater.updateStyleImage(target);
      }
    });
  }

  private String getDefaultLayerName() {
    // Concate the webeoc names together
    StringBuilder name = new StringBuilder();
    name.append(webeocLayerInfo.getIncident()).append("_");
    name.append(webeocLayerInfo.getBoard()).append("_");
    name.append(webeocLayerInfo.getView());
    
    String nameString = name.toString()
            .replaceAll("[^\\w\\s+]", "") // remove all weird characters
            .replaceAll("\\s+", "_") // change whitespace to underscore
            .toLowerCase();
    
    // Apparently there is a max length to table names
    // Truncate if its too long
    if (nameString.length() > WebEOCConstants.TABLE_NAME_MAXLENGTH) {
      return nameString.substring(0, WebEOCConstants.TABLE_NAME_MAXLENGTH);
    }
    
    return nameString;
  }

  private String getDefaultLayerTitle() {
    StringBuilder name = new StringBuilder();
    // Concate the webeoc names together
    name.append(webeocLayerInfo.getIncident()).append(" ");
    name.append(webeocLayerInfo.getBoard()).append(" ");
    name.append(webeocLayerInfo.getView());
    return name.toString();
  }

  /*  WebEOC API calls  */
  private APISoap getWebEOC() throws MalformedURLException {
    APISoap webEOC = null;
    try {
      webEOC = (new API(new URL(wsdlUrl))).getAPISoap();
    } catch (MalformedURLException e) {
      error("Bad WebEOC URL specified in datastore.");
      throw e;
    } catch (WebServiceException e) {
      error("Error connecting to WebEOC: " + e.getLocalizedMessage());
      throw e;
    }
    return webEOC;
  }

  private List<String> getIncidents() throws MalformedURLException {
    return getWebEOC().getIncidents(credentials.getWebEOCCredentials()).getString();
  }

  private List<String> getBoards() throws MalformedURLException {
    return getWebEOC().getBoardNames(credentials.getWebEOCCredentials()).getString();
  }

  private List<String> getViews() throws MalformedURLException {
    return getWebEOC().getDisplayViews(credentials.getWebEOCCredentials(),
            webeocLayerInfo.getBoard()).getString();
  }

  private List<String> getViewFields() throws MalformedURLException {
    return getWebEOC().getViewFields(credentials.getWebEOCCredentials(),
            webeocLayerInfo.getBoard(),
            webeocLayerInfo.getView()).getString();
  }
}
