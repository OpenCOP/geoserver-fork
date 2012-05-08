package org.geoserver.web.webeoc.poller;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.webeoc.poller.Poller;

public class ManageWebEocPollerPage extends GeoServerSecuredPage {

	private final Form form;

	private static final long DEV_defaultInterval = 5000l;

	public ManageWebEocPollerPage() {

		final Poller poller = Poller.getInstance();
		final boolean isRunning = poller.isRunning();

		if (isRunning) {
			add(new Label("msg", "Is running"));
		} else {
			add(new Label("msg", "Isn't running"));
		}

		add(form = new Form("form") {

			@Override
			protected void onSubmit() {
				if (isRunning) {
					System.out.println("Stop this!");
					poller.stop();
				} else {
					System.out.println("Start this!");
					poller.start(DEV_defaultInterval);
				}
			}
		});

	}
}
