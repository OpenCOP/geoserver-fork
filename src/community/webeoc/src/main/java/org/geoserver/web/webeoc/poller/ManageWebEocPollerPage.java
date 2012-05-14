package org.geoserver.web.webeoc.poller;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.wicket.markup.html.form.*;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.LayerInfo;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.web.data.webeoc.WebEOCLayerInfo;
import org.geoserver.web.data.webeoc.WebEOCLayerInfoImpl;
import org.geoserver.webeoc.Poller;
import org.geoserver.webeoc.WebEocDao;

public class ManageWebEocPollerPage extends GeoServerSecuredPage {

    private Form form;
    private Form resetLayerForm;
    private DropDownChoice layerDropDown;
    private boolean pollerEnabledModel;
    private String pollerIntervalModel;
    public String selectedResetLayer = "NOTHING";

    public ManageWebEocPollerPage() {

        // Set the model for whether or not the poller is enabled
        pollerEnabledModel = Poller.getInstance().isRunning();
        // Get the poller interval from global settings, gotta be null-safe!
        Serializable temp = GeoServerApplication.get().getGeoServer().getGlobal().getMetadata().get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);
        if (null != temp) {
            pollerIntervalModel = temp.toString();
        }

        // Create the form
        add(form = new Form("form"));

        // Create the text field for setting the poller interval
        TextField<String> pollingInterval = new TextField<String>("pollingInterval",
                new PropertyModel<String>(this, "pollerIntervalModel"));
        pollingInterval.setOutputMarkupId(true);
        form.add(pollingInterval);

        // Create the checkbox for enabling/disabling the poller
        final CheckBox pollerEnabled = new CheckBox("pollerEnabled",
                new PropertyModel<Boolean>(this, "pollerEnabledModel"));
        form.add(pollerEnabled);

        // create the save, reset and cancel buttons
        form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
        form.add(new BookmarkablePageLink("reset", GeoServerHomePage.class));



        SubmitLink saveLink = new SubmitLink("save", form) {

            @Override
            public void onSubmit() {
                Poller poller = Poller.getInstance();
                // Make sure the poller has a reference to the GeoServer Catalog
                if (null == poller.getCatalog()) {
                    poller.setCatalog(getCatalog());
                }

                GeoServerInfo global = GeoServerApplication.get().getGeoServer().getGlobal();
                MetadataMap metadata = global.getMetadata();

                // Gotta be null-safe!
                Serializable temp = metadata.get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);
                String oldInterval = "";
                if (null != temp) {
                    oldInterval = temp.toString();
                }

                // Only update the poller interval if its different
                // TODO: need further validation (number, greater than 0, etc.)
                if (null != pollerIntervalModel && !pollerIntervalModel.equals(oldInterval)) {
                    metadata.put(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, pollerIntervalModel);

                    // persist the global settings
                    GeoServerApplication.get().getGeoServer().save(global);
                }

                // Change the poller's status based on user input
                if (!pollerEnabledModel) {
                    System.out.println("Stop this!");
                    poller.stop();
                } else {
                    System.out.println("Start this!");
                    poller.start(Long.valueOf(pollerIntervalModel));
                }

                // if you don't do this, the page won't refresh right
                setResponsePage(new ManageWebEocPollerPage());
            }
        };
        form.add(saveLink);
        form.setDefaultButton(saveLink);

        initResetLayerForm();
    }

    private void initResetLayerForm() {
        /*
         * Create the reset layer form
         */
        // Create the form
        System.out.println("IN INITRESETLAYERFORM CODE");
        add(resetLayerForm = new Form("resetPollerForm"));

        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<LayerInfo> layerList = catalog.getLayers();
        for (LayerInfo l : layerList) {
            System.out.println("FOUND THESE LAYER NAMES " + l.getName());
        }
        ArrayList<String> webEocLayers = new ArrayList<String>();

        /*
         * prune out layers that aren't web eoc layers
         */
        for (LayerInfo l : layerList) {
            System.out.println("FOUND TYPE " + l.getResource().getStore().getType());
            if (WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(l.getResource().getStore().getType())) {
                webEocLayers.add(l.getName());
            }
        }

        System.out.println("THESE LOOK LIKE WEBEOC LAYERS");
        for (String s : webEocLayers) {
            System.out.println(s);
        }

        layerDropDown = new DropDownChoice("layersDropDown", new PropertyModel(this, "selectedResetLayer"), webEocLayers);
        resetLayerForm.add(layerDropDown);
        SubmitLink resetLink = new SubmitLink("reset", resetLayerForm) {

            @Override
            public void onSubmit() {
                String val = selectedResetLayer;
                if(val == null)
                    return;
                Catalog catalog = GeoServerApplication.get().getCatalog();
                LayerInfo layer = catalog.getLayerByName(val);
                if (layer == null) {
                    return;
                }
                Map<String, Serializable> m = layer.getResource().getStore().getConnectionParameters();

                String tableName = layer.getResource().getNativeName() == null ? val : layer.getResource().getNativeName();
                try {
                    WebEOCLayerInfo webInfo = new WebEOCLayerInfoImpl();
                    webInfo.set(layer.getResource().getMetadata());
                    WebEocDao webDAO = new WebEocDao(m, webInfo, tableName);
                    webDAO.delTableContents();
                    Poller.getInstance().pollNow();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        resetLayerForm.add(resetLink);
        resetLayerForm.setDefaultButton(resetLink);
    }
}
