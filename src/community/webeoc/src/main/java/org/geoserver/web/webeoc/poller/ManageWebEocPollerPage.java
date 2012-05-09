package org.geoserver.web.webeoc.poller;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.webeoc.poller.Poller;

public class ManageWebEocPollerPage extends GeoServerSecuredPage {

	private final Form form;

	private static final long DEV_defaultInterval = 5000l;

	public ManageWebEocPollerPage() {

		if (Poller.getInstance().isRunning()) {
			add(new Label("msg", "Is running"));
		} else {
			add(new Label("msg", "Isn't running"));
		}

		add(form = new Form("form") {

			@Override
			protected void onSubmit() {
				Poller poller = Poller.getInstance();
        if (null == poller.getCatalog()) {
          poller.setCatalog(getCatalog());
        }

				if (poller.isRunning()) {
					System.out.println("Stop this!");
					poller.stop();
				} else {
					System.out.println("Start this!");
					poller.start(DEV_defaultInterval);
				} 
				
				// if you don't do this, the page won't refresh right
				setResponsePage(new ManageWebEocPollerPage());
			}
		});

	}
}
