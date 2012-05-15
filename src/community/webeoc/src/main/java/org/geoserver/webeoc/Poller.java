package org.geoserver.webeoc;

import java.util.Timer;
import java.util.TimerTask;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.webeoc.WebEOCConstants;

public class Poller {

    private Timer timer = null;
    private long intervalMs;

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

        int pollingInterval = getPollingInterval(geoserver);
        System.out.printf("The polling interval is %s\n", pollingInterval);
        /*
         * This start needs to be wrapped in some sort of if statement depending
         * on if the poller is enabled or not, check the saved settings for this
         * information
         */
        Object o = geoserver.getGlobal().getMetadata().get(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY);
        if (o == null) {
            /*
             * if it isn't set we will assume its true and put it in there
             */
            start(pollingInterval);
            geoserver.getGlobal().getMetadata().put(WebEOCConstants.WEBEOC_POLLING_ENABLED_KEY, true);
        } else {
            Boolean pollDat = Boolean.parseBoolean(o.toString());
            if (pollDat) {
                start(pollingInterval);
            }
        }
    }

    /**
     * Retrieve the polling interval from geoserver's xml files.
     */
    private int getPollingInterval(GeoServer geoserver) {
        try {
            String pollingIntervalStr = geoserver.getGlobal().getMetadata().get(WebEOCConstants.WEBEOC_POLLING_INTERVAL_KEY).toString();
            return Integer.valueOf(pollingIntervalStr);
        } catch (Exception e) {  // if anything at all goes wrong, use default
            System.out.println("ERROR: failed to get polling interval from file, using default.");
            e.printStackTrace();
            return WebEOCConstants.WEBEOC_POLLING_INTERVAL_DEFAULT;
        }
    }

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
        if (timer == null) {
            return;
        }

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