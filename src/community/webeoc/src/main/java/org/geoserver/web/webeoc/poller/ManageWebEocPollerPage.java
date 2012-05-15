package org.geoserver.web.webeoc.poller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
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
    private static final long DEFAULT_POLLING_INTERVAL_MILLISECONDS = 5 * 60 * 1000L;

    public ManageWebEocPollerPage() {

        // Set the model for whether or not the poller is enabled
        pollerEnabledModel = Poller.getInstance().isRunning();
        // Get the poller interval from global settings, gotta be null-safe!
        Serializable temp = GeoServerApplication.get().getGeoServer().getGlobal().getMetadata().get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);
        if (null != temp) {
            /*
             * Convert the value back into minutes for the user
             */
            try {
                long tempLong = Long.parseLong(temp.toString());
                float tempFloat = tempLong / (60 * 1000f);
                pollerIntervalModel = Float.toString(tempFloat);
            } catch (Exception e) {
                e.printStackTrace();
                pollerIntervalModel = Long.toString(DEFAULT_POLLING_INTERVAL_MILLISECONDS / (60 * 1000));
            }
        }

        Serializable pollerEnabledValue = GeoServerApplication.get().getGeoServer().getGlobal().getMetadata().get(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY);
        if (pollerEnabledValue != null) {
            pollerEnabledModel = Boolean.parseBoolean(pollerEnabledValue.toString());
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
                GeoServerInfo global = GeoServerApplication.get().getGeoServer().getGlobal();
                MetadataMap metadata = global.getMetadata();

                // Gotta be null-safe!
                Serializable temp = metadata.get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);
                Long oldInterval = DEFAULT_POLLING_INTERVAL_MILLISECONDS; /*
                 * initialize oldInterval to 5 minutes worth of milliseconds
                 * incase it isn't set
                 */
                if (null != temp) {
                    oldInterval = Long.parseLong(temp.toString());
                } else {
                    /*
                     * if it was null that means there wasn't anything set for
                     * the polling interval, that is silly! we'll put one in
                     * right now!
                     */
                    metadata.put(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, DEFAULT_POLLING_INTERVAL_MILLISECONDS);
                }

                float pollerIntervalFloat = -1;
                try {
                    pollerIntervalFloat = Float.parseFloat(pollerIntervalModel);

                    /*
                     * Convert from minutes to milliseconds
                     */
                    pollerIntervalFloat = pollerIntervalFloat * 60 * 1000;

                    /*
                     * Check to make sure pollerIntervalModel is greater than 5
                     * minutes, if it isn't, we will silently set it to 5
                     * minutes
                     */
                    if (pollerIntervalFloat < DEFAULT_POLLING_INTERVAL_MILLISECONDS) {
                        pollerIntervalFloat = DEFAULT_POLLING_INTERVAL_MILLISECONDS;
                    }

                    metadata.put(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, Long.toString((long) Math.round(pollerIntervalFloat)));

                } catch (NumberFormatException e) {
                    System.out.println("User entered in an invalid polling interval, " + pollerIntervalModel + " " + e.getMessage());
                }


                // Change the poller's status based on user input
                if (!pollerEnabledModel) {
                    System.out.println("Stop this!");
                    metadata.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, false);
                    Poller.getInstance().stop();
                } else {
                    System.out.println("Start this!");
                    metadata.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, true);
                    /*
                     * If the pollerIntervalLong was set incorrectly by the user
                     * (and is therefore still set to -1) we will use the
                     * previously recorded value, otherwise we will use the new
                     * value)
                     */
                    Poller.getInstance().start(pollerIntervalFloat == -1 ? oldInterval : (long) Math.round(pollerIntervalFloat));
                }

                // persist the global settings
                GeoServerApplication.get().getGeoServer().save(global);


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
//        System.out.println("IN INITRESETLAYERFORM CODE");
        add(resetLayerForm = new Form("resetPollerForm"));

        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<LayerInfo> layerList = catalog.getLayers();
//        for (LayerInfo l : layerList) {
//            System.out.println("FOUND THESE LAYER NAMES " + l.getName());
//        }
        ArrayList<String> webEocLayers = new ArrayList<String>();

        /*
         * prune out layers that aren't web eoc layers
         */
        for (LayerInfo l : layerList) {
//            System.out.println("FOUND TYPE " + l.getResource().getStore().getType());
            if (WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(l.getResource().getStore().getType())) {
                webEocLayers.add(l.getName());
            }
        }

//        System.out.println("THESE LOOK LIKE WEBEOC LAYERS");
        for (String s : webEocLayers) {
            System.out.println(s);
        }

        layerDropDown = new DropDownChoice("layersDropDown", new PropertyModel(this, "selectedResetLayer"), webEocLayers);
        resetLayerForm.add(layerDropDown);
        SubmitLink resetLink = new SubmitLink("reset", resetLayerForm) {

            @Override
            public void onSubmit() {
                String val = selectedResetLayer;
                if (val == null) {
                    return;
                }
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
