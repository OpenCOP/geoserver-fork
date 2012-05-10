package org.geoserver.web.webeoc.poller;

import java.io.Serializable;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.webeoc.poller.Poller;

public class ManageWebEocPollerPage extends GeoServerSecuredPage {

  private final Form form;
  private boolean pollerEnabledModel;
  private String pollerIntervalModel;
  private static final long DEV_defaultInterval = 50000l;

  public ManageWebEocPollerPage() {

    // Set the model for whether or not the poller is enabled
    pollerEnabledModel = Poller.getInstance().isRunning();
    // Get the poller interval from global settings, gotta be null-safe!
    Serializable temp = GeoServerApplication.get().getGeoServer().getGlobal()
            .getMetadata().get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);
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

    // create the save and cancel buttons
    form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
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
        if (!pollerEnabledModel && poller.isRunning()) {
          System.out.println("Stop this!");
          poller.stop();
        } else if (pollerEnabledModel && !poller.isRunning()) {
          System.out.println("Start this!");
          poller.start(DEV_defaultInterval);
        }

        // if you don't do this, the page won't refresh right
        setResponsePage(new ManageWebEocPollerPage());
      }
    };
    form.add(saveLink);
    form.setDefaultButton(saveLink);
  }
}
