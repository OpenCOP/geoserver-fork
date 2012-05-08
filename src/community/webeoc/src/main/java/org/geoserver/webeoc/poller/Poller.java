package org.geoserver.webeoc.poller;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Poller {
	
	// singleton pattern
	private static final Poller INSTANCE = new Poller();
	private Poller() {}
	public static Poller getInstance() { return INSTANCE; }

	private Timer timer = null;
	private long intervalMs;

	public void start(long intervalMs) {
		stop();
		
		this.intervalMs = intervalMs;
		timer = new Timer(true); 
		timer.scheduleAtFixedRate(new PollerTask(), 0l, intervalMs);
	}

	public void stop() {
		if(timer == null) return;
		
		timer.cancel();
		timer = null;
	}
	
	public void pollNow() {
		stop();
		start(intervalMs);
	}
	
	public boolean isRunning() {
		return timer != null;
	}

	/* inner classes */

	public class PollerTask extends TimerTask {
		@Override
		public void run() {
			// TODO: the polling logic
			System.out.println("  " + new Date().toString());
		}
	}
}
