package org.geoserver.webeoc;

import java.io.Serializable;
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

	/**
	 * Get data for a particular feature from WebEoc. If updatedSince is null,
	 * get all data. Otherwise, get data only from that date on.
	 */
	private static String getDataAsXml(DataStoreInfo store, 
								FeatureTypeInfo featureType, 
								Date updatedSince) 
						throws DatatypeConfigurationException {
		
		Map<String, Serializable> conn = store.getConnectionParameters();
		
		String wsdlUrl = conn.get(WebEOCConstants.WEBEOC_WSDL_KEY).toString();
		
		WebEOCCredentialsSerializedWrapper creds = new WebEOCCredentialsSerializedWrapper();
		creds.set(conn);

		BasicCredentials bcreds = new BasicCredentials(
			creds.getUsername(), 
			creds.getPassword(),
			creds.getPosition());

		WebEOCLayerInfo webeoc = new WebEOCLayerInfoImpl();
		webeoc.set(featureType.getMetadata().getMap());
	
		String incident = webeoc.getIncident();
		String board = webeoc.getBoard();
		String view = webeoc.getView();

		WebEocWebService webEocWebService = new WebEocWebService(wsdlUrl);
		if(updatedSince == null) {
			return webEocWebService.getData(bcreds, incident, board, view);
		} else {
			return webEocWebService.getUpdatedData(bcreds, board, view, updatedSince);
		}
	}
	
	private static List<DataStoreInfo> webEocDataStores(Catalog catalog) {
		ArrayList<DataStoreInfo> webEocStores = new ArrayList<DataStoreInfo>();
		for(DataStoreInfo store : catalog.getDataStores()) {
			if(WebEOCConstants.WEBEOC_DATASTORE_NAME.equals(store.getType())) {
				webEocStores.add(store);
			}
		}
		return webEocStores;
	}
	
	private static boolean isPollingEnabled(FeatureTypeInfo featureType) {
		WebEOCLayerInfo webeoc = new WebEOCLayerInfoImpl();
		webeoc.set(featureType.getMetadata().getMap());
		return webeoc.isPollingEnabled();
	}

	/**
	 * For each WebEOC table, update based on whatever the last-updated date is.
	 * If there is no such date, or it's empty, update the entire table.
	 */
	public static void updateWebEocTables(Catalog catalog) {
		
		for (DataStoreInfo store : webEocDataStores(catalog)) {
			
			String username = store.getConnectionParameters().get("user").toString();
			String password = store.getConnectionParameters().get("passwd").toString();
			System.out.println("*********************************");
			System.out.println("Connection parameters: " + store.getConnectionParameters());
			System.out.println("**** Username: " + username);
			System.out.println("**** password: " + password);

			List<FeatureTypeInfo> featureTypes = catalog.getResourcesByStore(store, FeatureTypeInfo.class);
			for (FeatureTypeInfo featureType : featureTypes) {

				if (isPollingEnabled(featureType)) {
					try {
						
						String tableName = featureType.getNativeName();
						WebEocDao webEocDao = new WebEocDao(username, password, tableName);
						
						// get last-updated date (max date)
						Date lastUpdated = webEocDao.getMaxDate();
						System.out.println("*****  Last updated date is " + lastUpdated);
					
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

