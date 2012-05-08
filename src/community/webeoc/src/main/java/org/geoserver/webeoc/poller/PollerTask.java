package org.geoserver.webeoc.poller;

import java.util.Date;
import java.util.TimerTask;

public class PollerTask extends TimerTask {
	@Override
	public void run() {
		// TODO: the polling logic
		System.out.println("  " + new Date().toString());
	}
}
