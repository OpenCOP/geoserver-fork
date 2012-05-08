package org.geoserver.webeoc.poller;

import java.util.Timer;

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

}
