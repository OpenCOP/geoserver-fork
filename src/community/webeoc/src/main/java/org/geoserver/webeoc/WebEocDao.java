package org.geoserver.webeoc;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class WebEocDao {

    /*
     * TODO: The following varibles are only hard coded to enable easier
     * developement, in the future these will all be obtained from the Geoserver
     * datatype, possibly passed into the contructor
     */
    private static final String LATITUDE_COLUMN = "latitude";
    private static final String LONGITUDE_COLUMN = "longitude";
    private static final String UPDATE_DATE_COLUMN = "entrydate";
    /*
     * END HARDCODED LIST
     */
    
    protected Connection conn;
    private int geomColumnIndex = -1; /*
     * Default values if we don't have a geometry information in this table,
     * this should not happen
     */

    private int latColumnIndex = -1;
    private int lonColumnIndex = -1;
    
    /*
     * Keeps a map of 'data type' (returned from postgres db table metadata) ->
     * java.sql.Types integer
     */
    private static final HashMap<String, Integer> dataTypeMap = new HashMap<String, Integer>();
    private String tableName;

    /*
     * Keeps a map of 'column name' -> 'data type'
     */
    private HashMap<String, String> columnTypeMap = new HashMap<String, String>();
    private String[] columnOrder;

    static {
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

    public WebEocDao(Map<String, Serializable> connParams, String tableName)
            throws Exception {
        this.conn = buildConnection(connParams);
        this.tableName = tableName;
        this.columnOrder = new String[getNumColumns()];
        initTableDataInfo();
        setGeomColumnIndicies();
    }
    
    private static Connection buildConnection(Map<String, Serializable> connParams) 
    						throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");  // affirm that this class is available
					
		String username = connParams.get("user").toString();
		String password = connParams.get("passwd").toString();
		String database = connParams.get("database").toString();
		String host = connParams.get("host").toString();
		String port = connParams.get("port").toString();

		String connStr = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
		return DriverManager.getConnection(connStr, username, password);
		
//		((org.postgresql.PGConnection) conn).addDataType("geometry", Class.forName("com.vividsolutions.jts.geom.Geometry"));
//		((org.postgresql.PGConnection) conn).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));
    }

    private int getNumColumns() throws Exception {
        PreparedStatement s = conn.prepareStatement("select COUNT(*) from INFORMATION_SCHEMA.COLUMNS where table_name = ?");
        s.setString(1, tableName);
        ResultSet rs = s.executeQuery();
        if (rs.next()) {
            return rs.getInt("count");
        } else {
            throw tableNotFoundException(tableName);
        }
    }

    private void setGeomColumnIndicies() {
        this.geomColumnIndex = -1;
        this.latColumnIndex = -1;
        /*
         * Figure out where the geometry column is
         */
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
            if (geomColumnIndex != -1) {
                break;
            }
        }
        
        /*
         * Figure out where the latitude and longitude information is
         */
        this.latColumnIndex = getIndexOfColumnName(LATITUDE_COLUMN);
        this.lonColumnIndex = getIndexOfColumnName(LONGITUDE_COLUMN);
    }

    private void initTableDataInfo() throws Exception {
        PreparedStatement s = conn.prepareStatement("select column_name, data_type, dtd_identifier from INFORMATION_SCHEMA.COLUMNS where table_name = ?");
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
                System.out.println(String.format("WARNING: column name %s of type %s has a non integer index in the table, why is that?",
                        colName, dataType));
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println(String.format("ERROR: There was an array out of bounds exception, the reported column index of value %s was bigger than the max number of columns",
                        colName));
            }
        }
    }

    /**
     * Returns the index in columnOrder of the given name.
     *
     * ex: "fid" -> 1
     */
    private int getIndexOfColumnName(String name) {
    	return ArrayUtils.indexOf(columnOrder, name);
    }

    public void insertIntoEOCTable(String webEOCXMLResponse) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new ByteArrayInputStream(webEOCXMLResponse.getBytes()));
        Element docEl = dom.getDocumentElement();
        NodeList recordNodes = docEl.getElementsByTagName("record");
        String insertStatement = buildStartOfInsertSQL();
        PreparedStatement s = conn.prepareStatement(insertStatement);
        String[] valArray = new String[columnOrder.length];

        /*
         * For each record node
         */
        for (int i = 0; i < recordNodes.getLength(); i++) {
            Node curNode = recordNodes.item(i);
            /*
             * For each of the desired value names
             */
            for (int j = 0; j < valArray.length; j++) {
                /*
                 * Get the value of the corresponding data item name
                 */
                String val = ((Element) curNode).getAttribute(columnOrder[j]);
                valArray[j] = val.isEmpty() ? null : val;
            }

            /*
             * If we have latitude and longitude information
             */
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

            // use their unique ID (dataid) as our unique id (fid)
            int fidColId = getIndexOfColumnName("fid");
            int dataidColId = getIndexOfColumnName("dataid");
            // Prepared statement is 1-index.  I know, right?
            s.setInt(fidColId + 1, Integer.valueOf(valArray[dataidColId]));

            try {
                System.out.println("Ensuring that this id is not currently in the database (if it is, it won't be there long)");
                checkNDel(Integer.parseInt(valArray[dataidColId]));
                System.out.println("The prepared sql statement is:");
                System.out.println(s.toString());
                s.executeUpdate();
            } catch (SQLException e) {
                System.out.println("There was a problem inserting this data record, on error resume next: "
                        + e.getMessage());
            }
            catch(NumberFormatException e1){
                System.out.println("WARNING: dataID was not a number even though the FID unique key MUST be, cannot compare the two, skipping this record. " + e1.getMessage());
            }
        }
    }

    private Geometry point(String lat, String lon, int srid) {
        try {
            return geometry(String.format("POINT(%s %s)", lon, lat), srid);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Geometry geometry(String wkt, int srid) throws Exception {
        Geometry geom = new WKTReader().read(wkt);
        geom.setSRID(srid);  // this isn't ultimately used, but whatever
        return geom;
    }

    private void checkNDel(int id) throws SQLException {
        /*
         * Removes any row(s) from the database with this given id
         */
        Statement s = conn.createStatement();
        int rowsAffected = s.executeUpdate("DELETE FROM " + this.tableName + " WHERE fid=" + id);
        String message = rowsAffected > 0 ? "Deleted " + rowsAffected + " Row(s)." : "No rows found";
        System.out.println(message);
        return;
    }

    private void addValuesToPreparedStatement(PreparedStatement ps,
            String[] valArray, Geometry g) {
        for (int i = 0; i < valArray.length; i++) {
            String dataType = columnTypeMap.get(columnOrder[i]);
            System.out.println("Datatype is " + dataType + " And value is "
                    + valArray[i] + " And Column Name is " + columnOrder[i]);
            try {
                if (dataType.equals("USER-DEFINED")) {
                    if(g == null)
                        ps.setNull(i+1, Types.OTHER);
                    else
                        ps.setString(i + 1, g.toText());
                } else if (valArray[i] == null) {
                    ps.setNull(i + 1, dataTypeMap.get(dataType));
                } else if (dataType.equals("double precision")) {
                    ps.setDouble(i + 1, Double.parseDouble(valArray[i]));
                } else if (dataType.equals("text")
                        || dataType.equals("character varying")) {
                    ps.setString(i + 1, valArray[i]);
                } else if (dataType.equals("timestamp without time zone")) {
                    Date d = parseMonthFirst(valArray[i]);
                    if (d == null) {
                        ps.setNull(i + 1, dataTypeMap.get(dataType));
                    } else {
                        ps.setDate(i + 1, new java.sql.Date(d.getTime()));
                    }
                } else if (dataType.equals("integer")) {
                    ps.setInt(i + 1, Integer.parseInt(valArray[i]));
                } else if (dataType.equals("boolean")) {
                    ps.setBoolean(i + 1, interpretTruth(valArray[i]));
                } else {
                    /*
                     * We don't know what the hell it is, print a warning and
                     * move on!
                     */
                    System.out.println("WARNING The program cannot currently parse '"
                            + dataType
                            + "'. Setting this to null for now.");
                    ps.setNull(i + 1, dataTypeMap.get(dataType));
                }

            } catch (Exception e) {
                System.out.println("WARNING something went wrong, tried to parse value '"
                        + valArray[i]
                        + "' and couldn't. Interpreting it as null.");
                try {
                    System.out.println(String.format("The datatype is '%s'.",
                            dataType));
                    System.out.println(String.format(
                            "The dataTypeMap.get(dataType) is '%s'.",
                            dataTypeMap.get(dataType)));
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

    private Date parseMonthFirst(String date) {
    	return parseDate(date, "MM/dd/yyyy HH:mm:ss");
    }
    
    private Date parseYearFirst(String date) {
    	return parseDate(date, "yyyy-MM-dd HH:mm:ss");
    }
    
    private Date parseDate(String dateStr, String format) {
    	
    	if(dateStr == null) return null;
    	
        try {
            return new SimpleDateFormat(format).parse(dateStr);
        } catch (ParseException e) {
            System.out.println("WARNING DATE " + dateStr
                    + " Could not be parsed as " + format
                    + " Returning null");
        	e.printStackTrace();
            return null;
        }
    }

    private String buildStartOfInsertSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO \"" + this.tableName + "\" VALUES(");
        boolean first = true;
        for (int i = 0; i < this.columnOrder.length; i++) {
            String placeHolderVal = i == geomColumnIndex ? "ST_GeomFromText(?, 4326)" : "?";
            if (first) {
                sb.append(placeHolderVal);
                first = false;
            } else {
                sb.append(", " + placeHolderVal);
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    public Date getMaxDate() throws Exception {
    	
    	PreparedStatement s = conn.prepareStatement(
    			String.format("select max(%s) from %s", 
    					UPDATE_DATE_COLUMN, 
    					tableName));
    	
    	ResultSet rs = s.executeQuery();
    	if(rs.next()) {
    		return parseYearFirst(rs.getString(1));
    	} else {
    		throw tableNotFoundException(tableName);
    	}
    }
    
    private Exception tableNotFoundException(String tableName) {
    	return new Exception(String.format("ERROR: Table %s NOT FOUND", tableName));
    }
    
    public void delTableContents() throws SQLException {
        System.out.println("DELETEING CONTENTS FROM " + this.tableName);
        Statement s = conn.createStatement();
        s.executeUpdate("DELETE FROM " + this.tableName);
    }
}
