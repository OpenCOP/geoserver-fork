package org.geoserver.webeoc.poller;

import java.util.Timer;
import java.util.TimerTask;

import org.geoserver.catalog.Catalog;
import org.geoserver.webeoc.UpdateTask;

public class Poller {
	
	// singleton pattern
	private static final Poller INSTANCE = new Poller();
	private Poller() {}
	public static Poller getInstance() { return INSTANCE; }

	private Timer timer = null;
	private long intervalMs;
	private Catalog catalog = null;

	/**
	 * Starts the poller. If the poller is already running, changes the polling
	 * interval and forces the poller to poll immediately.
	 */
	public void start(long intervalMs) {
		stop();
		
		this.intervalMs = intervalMs;
		timer = new Timer(true); 
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				UpdateTask.updateWebEocTables(catalog);	
			}
		}, 0l, intervalMs);
	}

	public void stop() {
		if (timer == null) return;

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

  /**
   * @return the catalog
   */
  public Catalog getCatalog() {
    return catalog;
  }

  /**
   * @param catalog the catalog to set
   */
  public void setCatalog(Catalog catalog) {
    this.catalog = catalog;
  }

}
