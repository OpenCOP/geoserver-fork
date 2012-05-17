package org.geoserver.webeoc;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.data.webeoc.WebEOCConstants;
import org.geoserver.web.data.webeoc.WebEOCCredentialsSerializedWrapper;
import org.geoserver.web.data.webeoc.WebEOCLayerInfo;
import org.geoserver.web.data.webeoc.WebEOCLayerInfoImpl;

/**
 * Ties together the pieces of getting data from webeoc and pushing it into our
 * database.
 */
public class UpdateTask {

	private static final Logger logger = Logger.getLogger(UpdateTask.class.getName());

	/**
	 * Get data for a particular feature from WebEoc. If updatedSince is null,
	 * get all data. Otherwise, get data only from that date on.
	 */
	private static String getDataAsXml(DataStoreInfo store, FeatureTypeInfo featureType,
			Date updatedSince) throws DatatypeConfigurationException {

		Map<String, Serializable> conn = store.getConnectionParameters();

		String wsdlUrl = conn.get(WebEOCConstants.WEBEOC_WSDL_KEY).toString();

		WebEOCCredentialsSerializedWrapper creds = new WebEOCCredentialsSerializedWrapper();
		creds.set(conn);

		BasicCredentials bcreds = new BasicCredentials(creds.getUsername(), creds.getPassword(),
				creds.getPosition());

		WebEOCLayerInfo webeoc = getWebEocLayerInfo(featureType);

		String incident = webeoc.getIncident();
		String board = webeoc.getBoard();
		String view = webeoc.getView();

		WebEocWebService webEocWebService = new WebEocWebService(wsdlUrl);
		if (updatedSince == null) {
			return webEocWebService.getData(bcreds, incident, board, view);
		} else {
			return webEocWebService.getUpdatedData(bcreds, board, view, updatedSince);
		}
	}

	private static List<DataStoreInfo> webEocDataStores(Catalog catalog) {
		ArrayList<DataStoreInfo> webEocStores = new ArrayList<DataStoreInfo>();
		for (DataStoreInfo store : catalog.getDataStores()) {
			if (WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(store.getType())) {
				webEocStores.add(store);
			}
		}
		return webEocStores;
	}

	private static WebEOCLayerInfo getWebEocLayerInfo(FeatureTypeInfo featureType) {
		WebEOCLayerInfo webeoc = new WebEOCLayerInfoImpl();
		webeoc.set(featureType.getMetadata().getMap());
		return webeoc;
	}

	/**
	 * For each WebEOC table, update based on whatever the last-updated date is.
	 * If there is no such date, or it's empty, update the entire table.
	 */
	public static void updateWebEocTables(Catalog catalog) {

		for (DataStoreInfo store : webEocDataStores(catalog)) {

			List<FeatureTypeInfo> featureTypes = catalog.getResourcesByStore(store,
					FeatureTypeInfo.class);
			for (FeatureTypeInfo featureType : featureTypes) {

				WebEOCLayerInfo webEocLayerInfo = getWebEocLayerInfo(featureType);

				if (webEocLayerInfo.isPollingEnabled()) {
					try {

						String tableName = featureType.getNativeName();
						WebEocDao webEocDao = new WebEocDao(store.getConnectionParameters(),
								webEocLayerInfo, tableName);

						// get last-updated date (max date)
						Date lastUpdated = webEocDao.getMaxDate();
						logger.log(Level.INFO, String.format("Last updated date is %s", lastUpdated));

						// get data from webeoc
						String webEOCXMLResponse = getDataAsXml(store, featureType, lastUpdated);

						// write data to database
						webEocDao.insertIntoEOCTable(webEOCXMLResponse);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
