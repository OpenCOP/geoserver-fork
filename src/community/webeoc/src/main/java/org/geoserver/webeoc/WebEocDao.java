package org.geoserver.webeoc;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.referencing.CRS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

public class WebEocDao {
	/*
	 * TODO: The following varibles are only hard coded to enable easier
	 * developement, in the future these will all be obtained from the Geoserver
	 * datatype, possibly passed into the contructor
	 */
	private static final String CONNECTION_STRING = "jdbc:postgresql://localhost:5432/webeoc";
	private static final String DB_USERNAME = "opencop";
	private static final String DB_PASSWORD = "57levelsofeoc";
	private static final String LATITUDE_COLUMN = "latitude";
	private static final String LONGITUDE_COLUMN = "longitude";
	/* END HARDCODED LIST */

	protected static Connection conn;

	private int geomColumnIndex = -1; /*
									 * Default values if we don't have a
									 * geometry information in this table, this
									 * should not happen
									 */
	private int latColumnIndex = -1;
	private int lonColumnIndex = -1;

	/*
	 * Keeps a map of 'data type' (returned from postgres db table metadata) ->
	 * java.sql.Types integer
	 */
	private static final HashMap<String, Integer> dataTypeMap = new HashMap<String, Integer>();
	private String tableName;
	private String webEOCXMLResponse;

	/* Keeps a map of 'column name' -> 'data type' */
	private HashMap<String, String> columnTypeMap = new HashMap<String, String>();
	private String[] columnOrder;

	static {
		/*
		 * The following is taken pretty much verbatim from
		 * http://postgis.refractions.net/documentation/manual-1.5/ch05.html#id2633989
		 */
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(CONNECTION_STRING, DB_USERNAME,
					DB_PASSWORD);

//			((org.postgresql.PGConnection) conn).addDataType("geometry",
//					Class.forName("com.vividsolutions.jts.geom.Geometry"));
//			((org.postgresql.PGConnection) conn).addDataType("box3d",
//					Class.forName("org.postgis.PGbox3d"));
		} catch (Exception e) {
			System.out.println("SOMETHING WENT WRONNNGG (updated)");
			e.printStackTrace();
		}

		/*
		 * TODO THIS WILL NEED TO BE UPDATED WITH ALL OF THE VALUES MAPPING
		 * DTD_ITENTIFIER WITH THE EQUIVELLENT JAVA SQL TYPES
		 */
		dataTypeMap.put("double precision", Types.DOUBLE);
		dataTypeMap.put("text", Types.VARCHAR);
		dataTypeMap.put("character varying", Types.VARCHAR);
		dataTypeMap.put("integer", Types.INTEGER);
		dataTypeMap.put("timestamp without time zone", Types.TIMESTAMP);
		dataTypeMap.put("boolean", Types.BOOLEAN);
	}

	public WebEocDao(String tableName, String webEOCXMLResponse)
			throws Exception {
		this.tableName = tableName;
		this.webEOCXMLResponse = webEOCXMLResponse;
		this.columnOrder = new String[getNumColumns()];
		initTableDataInfo();
		setGeomColumnIndicies();
	}

	private int getNumColumns() throws Exception {
		PreparedStatement s = conn
				.prepareStatement("select COUNT(*) from INFORMATION_SCHEMA.COLUMNS where table_name = ?");
		s.setString(1, tableName);
		ResultSet rs = s.executeQuery();
		if (rs.next()) {
			return rs.getInt("count");
		} else {
			throw new Exception(String.format("ERROR: Table %s NOT FOUND",
					tableName));
		}
	}

	private void setGeomColumnIndicies() {
		this.geomColumnIndex = -1;
		this.latColumnIndex = -1;
		/* Figure out where the geometry column is */
		for (String key : columnTypeMap.keySet()) {
			System.out.println("Checking key " + key + " its value is "
					+ columnTypeMap.get(key));
			if (columnTypeMap.get(key).equals("USER-DEFINED")) {
				String locationColumnName = key;
				for (int i = 0; i < columnOrder.length; i++) {
					if (columnOrder[i].equals(locationColumnName)) {
						geomColumnIndex = i;
						break;
					}
				}
			}
			if (geomColumnIndex != -1)
				break;
		}
		/* Figure out where the latitude and longitude information is */
		for (int i = 0; i < columnOrder.length; i++) {
			if (columnOrder[i].equals(LATITUDE_COLUMN))
				this.latColumnIndex = i;
			if (columnOrder[i].equals(LONGITUDE_COLUMN))
				this.lonColumnIndex = i;
		}
		return;
	}

	private void initTableDataInfo() throws Exception {
		PreparedStatement s = conn
				.prepareStatement("select column_name, data_type, dtd_identifier from INFORMATION_SCHEMA.COLUMNS where table_name = ?");
		s.setString(1, tableName);
		ResultSet rs = s.executeQuery();
		while (rs.next()) {
			String colName = rs.getString("column_name");
			String dataType = rs.getString("data_type");
			this.columnTypeMap.put(colName, dataType);
			try {
				int i = Integer.parseInt(rs.getString("dtd_identifier"));
				this.columnOrder[i - 1] = colName;
			} catch (NumberFormatException e) {
				System.out
						.println(String
								.format("WARNING: column name %s of type %s has a non integer index in the table, why is that?",
										colName, dataType));
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out
						.println(String
								.format("ERROR: There was an array out of bounds exception, the reported column index of value %s was bigger than the max number of columns",
										colName));
			}
		}
	}

	public void insertIntoEOCTable() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(new ByteArrayInputStream(webEOCXMLResponse
				.getBytes()));
		Element docEl = dom.getDocumentElement();
		NodeList recordNodes = docEl.getElementsByTagName("record");
		String insertStatement = buildStartOfInsertSQL();
		PreparedStatement s = conn.prepareStatement(insertStatement);
		String[] valArray = new String[columnOrder.length];
		/* For each record node */
		for (int i = 0; i < recordNodes.getLength(); i++) {
			Node curNode = recordNodes.item(i);
			/* For each of the desired value names */
			for (int j = 0; j < valArray.length; j++) {
				/* Get the value of the corresponding data item name */
				String val = ((Element) curNode).getAttribute(columnOrder[j]);
				valArray[j] = val.isEmpty() ? null : val;
			}

			/* TODO: TEST LINES, THROW OUT WHEN DONE WITH TEXT */
			valArray[latColumnIndex] = "150";
			valArray[lonColumnIndex] = "20";
			/* END TEST LINES */

			/* If we have latitude and longitude information */
			Geometry g = null;
			if (latColumnIndex != -1 && lonColumnIndex != -1
					&& valArray[latColumnIndex] != null
					&& valArray[lonColumnIndex] != null) {
				g = point(valArray[latColumnIndex], valArray[lonColumnIndex],
						4326);
			}

			/*
			 * Now that we have all of the information out of our current node,
			 * we need to put it in the database
			 */
			addValuesToPreparedStatement(s, valArray, g);
			s.setInt(1, (int) (Math.random() * 1000000));
			try {
				System.out.println("The prepared sql statement is:");
				System.out.println(s.toString());
				s.executeUpdate();
			} catch (SQLException e) {
				System.out
						.println("There was a problem inserting this data record, on error resume next: "
								+ e.getMessage());
			}
		}
	}

	private Geometry point(String lat, String lon, int srid) {
		try {
			return geometry(String.format("POINT(%s %s)", lat, lon), srid);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Geometry geometry(String wkt, int srid) throws Exception {
		Geometry geom = new WKTReader().read(wkt);
		geom.setUserData(CRS.decode("EPSG:" + srid));
		return geom;
	}

	private void addValuesToPreparedStatement(PreparedStatement ps,
			String[] valArray, Geometry g) {
		for (int i = 0; i < valArray.length; i++) {
			String dataType = columnTypeMap.get(columnOrder[i]);
			System.out.println("Datatype is " + dataType + " and value is "
					+ valArray[i]);
			try {
				if (dataType.equals("USER-DEFINED")) {
					ps.setObject(i + 1, g, Types.OTHER);
				} else if (valArray[i] == null) {
					ps.setNull(i + 1, dataTypeMap.get(dataType));
				} else if (dataType.equals("double precision")) {
					ps.setDouble(i + 1, Double.parseDouble(valArray[i]));
				} else if (dataType.equals("text") || dataType.equals("character varying")) {
					ps.setString(i + 1, valArray[i]);
				} else if (dataType.equals("timestamp without time zo ne")) {
					Date d = parseDate(valArray[i]);
					if (d == null)
						ps.setNull(i + 1, dataTypeMap.get(dataType));
					else
						ps.setDate(i + 1, new java.sql.Date(d.getTime()));
				} else if (dataType.equals("integer")) {
					ps.setInt(i + 1, Integer.parseInt(valArray[i]));
				} else if (dataType.equals("boolean")) {
					ps.setBoolean(i + 1, interpretTruth(valArray[i]));
				} else {
					/*
					 * We don't know what the hell it is, print a warning and
					 * move on!
					 */
					System.out
							.println("WARNING The program cannot currently parse '"
									+ dataType
									+ "'. Setting this to null for now.");
					ps.setNull(i + 1, dataTypeMap.get(dataType));
				}

			} catch (Exception e) {
				System.out
						.println("WARNING something went wrong, tried to parse value '"
								+ valArray[i]
								+ "' and couldn't. Interpreting it as null.");
				try {
					System.out.println(String.format("The datatype is '%s'.", dataType));
					System.out.println(String.format("The dataTypeMap.get(dataType) is '%s'.", dataTypeMap.get(dataType)));
					ps.setNull(i + 1, dataTypeMap.get(dataType));
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private Boolean interpretTruth(String boolString) {
		/*
		 * TODO we might need to add new interpretations of true if their data
		 * is messy
		 */
		if (boolString.toLowerCase().equals("yes")) {
			return true;
		} else {
			return Boolean.parseBoolean("boolString");
		}
	}

	private Date parseDate(String date) {
		/*
		 * TODO we may need other simple date formatters in the future if we
		 * discover they store dates in a variety of formats
		 */
		String dateFormatString = "MM/dd/yyyy HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);
		try {
			return sdf.parse(date);
		} catch (ParseException e) {
			System.out.println("WARNING DATE " + date
					+ " Could not be parsed as " + dateFormatString
					+ " Returning null");
			return null;
		}
	}

	private String buildStartOfInsertSQL() {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO \"" + this.tableName + "\" VALUES(");
		boolean first = true;
		for (int i = 0; i < this.columnOrder.length; i++) {
			if (first) {
				sb.append("?");
				first = false;
			} else {
				sb.append(", ?");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Code stolen from http://totheriver.com/learn/xml/xmltutorial.html#5.2
	 */
	private String getTextValue(Element ele, String tagName) {
		return ele.getAttribute(tagName);
	}
}
