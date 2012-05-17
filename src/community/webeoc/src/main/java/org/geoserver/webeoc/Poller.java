package org.geoserver.webeoc;

import java.io.Serializable;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.web.data.webeoc.WebEOCConstants;

public class Poller {

	private Timer timer = null;
	private float intervalMs;
	private Date lastPolled = null;

	/*
	 * This class has some round-about hacks to it. There were two problems to
	 * solve.
	 * 
	 * 1. The poller needs an instance of catalog. This can only reasonably come
	 * from a Spring injection, which necessitates a public constructor and a
	 * catalog instance variable.
	 * 
	 * 2. Pages need a way to access this class. Spring dependency injection
	 * isn't going to work because of limitations in GeoserverBasePage. Hence,
	 * we need to save a static instance of this class.
	 */
	private final Catalog catalog;
	private static Poller instance = null;

	public static Poller getInstance() {
		return instance;
	}

	/**
	 * It is available for Spring's use only. Use `getInstance()` instead.
	 * 
	 * @throws Exception
	 */
	public Poller(GeoServer geoserver) throws Exception {
		if (instance != null) {
			throw new Exception("Poller can only be instantiated once.");
		}

		instance = this;
		this.catalog = geoserver.getCatalog();
		this.intervalMs = getPollingIntervalMsSetting(geoserver);

		System.out.printf("The polling interval is %s\n", intervalMs);

		if (isPollingEnabledSetting(geoserver)) {
			start(intervalMs);
		}
	}

	private boolean isPollingEnabledSetting(GeoServer geoserver) {
		Serializable enabled = geoserver.getGlobal().getMetadata()
				.get(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY);
		if (enabled == null) {
			// if it wasn't set in settings, make it true
			geoserver.getGlobal().getMetadata()
					.put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, true);
			return true;
		}
		return Boolean.parseBoolean(enabled.toString());
	}

	/**
	 * Retrieve the polling interval from geoserver's xml files.
	 */
	private float getPollingIntervalMsSetting(GeoServer geoserver) {
		try {
			String pollingIntervalStr = geoserver.getGlobal().getMetadata()
					.get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY).toString();
			return Float.valueOf(pollingIntervalStr);
		} catch (Exception e) { // if anything at all goes wrong, use default
			System.out.println("ERROR: failed to get polling interval from file, using default.");
			e.printStackTrace();
			return WebEOCConstants.WEBEOC_POLLING_INTERVAL_MS_DEFAULT;
		}
	}

	/**
	 * Starts the poller. If the poller is already running, changes the polling
	 * interval and forces the poller to poll immediately.
	 */
	public void start(float intervalMs) {
		stop();

		this.intervalMs = intervalMs;
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				doUpdate();
			}
		}, 0l, (long) intervalMs);
	}

	public void stop() {
		if (timer == null) {
			return;
		}

		timer.cancel();
		timer = null;
	}

	public void pollNow() {
		// restart if running
		if (isRunning()) {
			stop();
			start(intervalMs);
		}
		// run once if not running
		else {
			doUpdate();
		}
	}

	public boolean isRunning() {
		return timer != null;
	}

	/**
	 * Returns when the poller last ran. If the poller has not yet run, returns
	 * null.
	 */
	public Date lastRanAt() {
		return lastPolled;
	}
	
	/**
	 * Returns whether poller has run since startup.
	 */
	public boolean hasRun() {
		return lastPolled != null;
	}

	/**
	 * Do the actual work of updating the tables.
	 */
	private void doUpdate() {
		lastPolled = new Date();
		UpdateTask.updateWebEocTables(catalog);
	}
}