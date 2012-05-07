package org.geoserver.web.data.layer;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.APISoap;
import com.vividsolutions.jts.geom.Geometry;
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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
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
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layer.AttributeEditPage.BindingChoiceRenderer;
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
  private final TextField<String> layerName;
  private final TextField<String> layerTitle;
  private final Model<StyleInfo> defaultStyleModel;
  private final WebEOCCredentialsSerializedWrapper credentials;
  private final WebEOCLayerInfo webeocLayerInfo;
  AttributesProvider attributesProvider;
  GeoServerTablePanel<AttributeDescription> attributeTable;
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
    form.add(layerName = new TextField<String>("layerName", new Model<String>()));
    form.add(layerTitle = new TextField<String>("layerTitle", new Model<String>()));

    // Add the style picker elements
    defaultStyleModel = new Model<StyleInfo>();
    addStylePicker(form);

    form.add(new GeoServerAjaxFormLink("removeSelected", form) {

      @Override
      public void onClick(AjaxRequestTarget target, Form form) {
        attributesProvider.removeAll(attributeTable.getSelection());
        attributeTable.clearSelection();
        target.addComponent(form);
      }
    });

    attributesProvider = new AttributesProvider();
    attributeTable = new GeoServerTablePanel<AttributeDescription>("attributes",
            attributesProvider, true) {

      @Override
      protected Component getComponentForProperty(String id, IModel itemModel,
              Property<AttributeDescription> property) {
        AttributeDescription att = (AttributeDescription) itemModel.getObject();
        if (property == AttributesProvider.NAME) {
          return new Label(id, att.getName());
        } else if (property == AttributesProvider.BINDING) {
          Fragment f = new Fragment(id, "bindingFragment", CreateWebEOCLayerPage.this);
          DropDownChoice choice = new DropDownChoice("binding", new PropertyModel(itemModel, "binding"),
                  AttributeDescription.BINDINGS, new BindingChoiceRenderer());
          choice.setOutputMarkupId(true);
          f.add(choice);
          return f;
//          return new Label(id, AttributeDescription.getLocalizedName(att.getBinding()));
        } else if (property == AttributesProvider.CRS) {
          if (att.getBinding() != null
                  && Geometry.class.isAssignableFrom(att.getBinding())) {
            try {
              Integer epsgCode = CRS.lookupEpsgCode(att.getCrs(), false);
              return new Label(id, "EPSG:" + epsgCode);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          } else {
            return new Label(id, "");
          }
        } else if (property == AttributesProvider.SIZE) {
          if (att.getBinding() != null && String.class.equals(att.getBinding())) {
            return new Label(id, String.valueOf(att.getSize()));
          } else {
            return new Label(id, "");
          }
        } else if (property == AttributesProvider.UPDOWN) {
          return new Label(id, "");
//          return upDownFragment(id, att);
        }

        return null;
      }
    };
    attributeTable.setOutputMarkupId(true);
    attributeTable.setSortable(false);
    attributeTable.setFilterable(false);
    attributeTable.getBottomPager().setVisible(false);
    form.add(attributeTable);

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
    String layername = layerName.getDefaultModelObjectAsString();
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
    SimpleFeatureType featureType = buildFeatureType(attributesProvider.getAttributes(), layername);
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
      layerInfo.getResource().setTitle(layerTitle.getDefaultModelObjectAsString());
      // Get the chosen style
      StyleInfo styleInfo = defaultStyleModel.getObject();
      // Set the default style for the layer
      layerInfo.setDefaultStyle(styleInfo);
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

  SimpleFeatureType buildFeatureType(List<AttributeDescription> attributes, String name) {
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    for (AttributeDescription attribute : attributes) {
      // TODO: Make the bindings selectable
      builder.add(attribute.getName(), attribute.getBinding());
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
        List<String> fields = getViewFields();
        attributesProvider.removeAll(attributesProvider.getAttributes());
        attributeTable.clearSelection();
        for (String field : fields) {
          AttributeDescription attr = new AttributeDescription();
          attr.setName(field);
          attr.setBinding(String.class);
          attributesProvider.addNewAttribute(attr);
        }
        attributeTable.setItemsPerPage(attributesProvider.size());
        if (target != null) {
          target.addComponent(attributeTable);
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
