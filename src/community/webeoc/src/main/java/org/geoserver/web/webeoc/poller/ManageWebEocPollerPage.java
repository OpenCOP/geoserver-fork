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

	private Form<?> form;
	private Form<?> resetLayerForm;
	private DropDownChoice<?> layerDropDown;
	private boolean pollerEnabledModel;
	private String pollerIntervalModelMins;
	public String selectedResetLayer = "NOTHING";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ManageWebEocPollerPage() {

		// assume that the enabled setting and whether the poller is running are
		// synced, and believe the poller
		pollerEnabledModel = Poller.getInstance().isRunning();

		pollerIntervalModelMins = getPollerIntervalModelMins();

		// Create the form
		add(form = new Form("form"));

		// Create the text field for setting the poller interval
		TextField<String> pollingInterval = new TextField<String>("pollingInterval",
				new PropertyModel<String>(this, "pollerIntervalModelMins"));
		pollingInterval.setOutputMarkupId(true);
		form.add(pollingInterval);

		// Create the checkbox for enabling/disabling the poller
		final CheckBox pollerEnabled = new CheckBox("pollerEnabled", new PropertyModel<Boolean>(
				this, "pollerEnabledModel"));
		form.add(pollerEnabled);

		// create the save, reset and cancel buttons
		form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));

		SubmitLink saveLink = new SaveLink("save", form);
		form.add(saveLink);
		form.setDefaultButton(saveLink);

		form.add(new PollNowLink("pollnow", form));

		initResetLayerForm();
	}

	@SuppressWarnings("serial")
	private final class PollNowLink extends SaveLink {

		private PollNowLink(String id, Form<?> form) {
			super(id, form);
		}

		@Override
		public void onSubmit() {
			Poller.getInstance().pollNow();

			// refresh page
			setResponsePage(new ManageWebEocPollerPage());
		}
	}

	@SuppressWarnings("serial")
	class SaveLink extends SubmitLink {

		public SaveLink(String id, Form<?> form) {
			super(id, form);
		}

		@Override
		public void onSubmit() {
			GeoServerInfo global = GeoServerApplication.get().getGeoServer().getGlobal();
			MetadataMap metadata = global.getMetadata();

			long newIntervalMs = extractPollerIntervalMsFromModel(pollerIntervalModelMins,
					getCurrentSettingsIntervalMs());
			metadata.put(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, Long.toString(newIntervalMs));

			// Change the poller's status based on user input
			if (pollerEnabledModel) {
				System.out.println("Start this!");
				metadata.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, true);
				Poller.getInstance().start(newIntervalMs);
			} else {
				System.out.println("Stop this!");
				metadata.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, false);
				Poller.getInstance().stop();
			}

			// persist the global settings
			GeoServerApplication.get().getGeoServer().save(global);

			// if you don't do this, the page won't refresh right
			setResponsePage(new ManageWebEocPollerPage());
		}
	}

	private String getPollerIntervalModelMins() {

		Serializable intervalMsFromSettings = GeoServerApplication.get().getGeoServer().getGlobal()
				.getMetadata().get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);

		if (intervalMsFromSettings == null) {
			return defaultPollingIntervalMins();
		}

		// Convert the value back into minutes for the user
		try {
			long intervalMs = Long.parseLong(intervalMsFromSettings.toString());
			float intervalMins = intervalMs / (60 * 1000f);
			return Float.toString(intervalMins);
		} catch (Exception e) {
			e.printStackTrace();
			return defaultPollingIntervalMins();
		}
	}

	/**
	 * Grab a default polling interval from our hardcoded defaults.
	 */
	private String defaultPollingIntervalMins() {
		return Long.toString((long) WebEOCConstants.WEBEOC_POLLING_INTERVAL_DEFAULT / (60 * 1000));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initResetLayerForm() {
		// Create the form
		// System.out.println("IN INITRESETLAYERFORM CODE");
		add(resetLayerForm = new Form("resetPollerForm"));

		Catalog catalog = GeoServerApplication.get().getCatalog();
		List<LayerInfo> layerList = catalog.getLayers();
		// for (LayerInfo l : layerList) {
		// System.out.println("FOUND THESE LAYER NAMES " + l.getName());
		// }
		ArrayList<String> webEocLayers = new ArrayList<String>();

		/*
		 * prune out layers that aren't web eoc layers
		 */
		for (LayerInfo l : layerList) {
			// System.out.println("FOUND TYPE " +
			// l.getResource().getStore().getType());
			if (WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(l.getResource().getStore().getType())) {
				webEocLayers.add(l.getName());
			}
		}

		// System.out.println("THESE LOOK LIKE WEBEOC LAYERS");
		for (String s : webEocLayers) {
			System.out.println(s);
		}

		layerDropDown = new DropDownChoice("layersDropDown", new PropertyModel(this,
				"selectedResetLayer"), webEocLayers);
		resetLayerForm.add(layerDropDown);

		@SuppressWarnings("serial")
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
				Map<String, Serializable> m = layer.getResource().getStore()
						.getConnectionParameters();

				String tableName = layer.getResource().getNativeName() == null ? val : layer
						.getResource().getNativeName();
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

	private long getCurrentSettingsIntervalMs() {

		MetadataMap metadata = GeoServerApplication.get().getGeoServer().getGlobal().getMetadata();

		Serializable currentIntervalSettingMs = metadata
				.get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY);

		if (currentIntervalSettingMs == null) {
			// if it was null that means there wasn't anything set for the
			// polling interval, that is silly! we'll put one in right now!
			long defaultInterval = (long) WebEOCConstants.WEBEOC_POLLING_INTERVAL_DEFAULT;
			metadata.put(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY, defaultInterval);
			return defaultInterval;
		}

		return Long.parseLong(currentIntervalSettingMs.toString());
	}

	/**
	 * Extract poller interval from model. If invalid, return default value.
	 * 
	 * @param pollerIntervalModelMins
	 * @param defaultIntervalMs
	 * @return rounded interval
	 */
	private long extractPollerIntervalMsFromModel(String pollerIntervalModelMins,
			float defaultIntervalMs) {
		try {

			// Convert from minutes to milliseconds
			float intervalMs = Float.parseFloat(pollerIntervalModelMins) * 60 * 1000;

			/*
			 * Check to make sure pollerIntervalModel is greater min, if it
			 * isn't, we will silently set it min
			 */
			if (intervalMs < (long) WebEOCConstants.WEBEOC_POLLING_INTERVAL_MINIMUM) {
				intervalMs = (long) WebEOCConstants.WEBEOC_POLLING_INTERVAL_MINIMUM;
			}

			return Math.round(intervalMs);

		} catch (NumberFormatException e) {
			System.out.println("User entered in an invalid polling interval, "
					+ pollerIntervalModelMins + " " + e.getMessage());
			return Math.round(defaultIntervalMs);
		}

	}
}
