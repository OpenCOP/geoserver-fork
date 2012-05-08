package org.geoserver.webeoc.poller;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.geoserver.web.GeoServerSecuredPage;

public class ManageWebEocPollerPage extends GeoServerSecuredPage {

	private final Form form;
	
	private static final long DEV_defaultInterval = 5000l;

	public ManageWebEocPollerPage() {

		add(form = new Form("form"));
		
		final Poller poller = Poller.getInstance();
		final boolean isRunning = poller.isRunning();

		@SuppressWarnings("serial")
		SubmitLink saveLink = new SubmitLink(isRunning ? "Stop" : "Start", form) {
			@Override
			public void onSubmit() {
				if(isRunning) {
					System.out.println("Stop this!");
					poller.stop();
				} else {
					System.out.println("Start this!");
					poller.start(DEV_defaultInterval);
				}
			}
		};
		form.add(saveLink);
		form.setDefaultButton(saveLink);

	}
}
