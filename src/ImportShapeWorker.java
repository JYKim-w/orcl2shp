//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import oracle.jdbc.OracleConnection;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.AdapterShapefileJGeom;
import oracle.spatial.util.DBFReaderJGeom;
import oracle.spatial.util.ShapefileReaderJGeom;
import oracle.sql.STRUCT;

import javax.swing.*;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class ImportShapeWorker extends SwingWorker {
    private static final double EPSILON = 1.0E-130D;
    protected static String[] reservedWords = new String[]{"ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUDIT", "BETWEEN", "BY", "CHAR", "CHECK", "CLUSTER", "COLUMN", "COMMENT", "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL", "DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE", "EXISTS", "FILE", "FLOAT", "FOR", "FROM", "GRANT", "GROUP", "HAVING", "IDENTIFIED", "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL", "INSERT", "INTEGER", "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", "LOCK", "LONG", "MAXEXTENTS", "MINUS", "MLSLABEL", "MODE", "MODIFY", "NOAUDIT", "NOCOMPRESS", "NOT", "NOWAIT", "NULL", "NUMBER", "OF", "OFFLINE", "ON", "ONLINE", "OPTION", "OR", "ORDER", "PCTFREE", "PRIOR", "PRIVILEGES", "PUBLIC", "RAW", "RENAME", "RESOURCE", "REVOKE", "ROW", "ROWID", "ROWNUN", "ROWS", "SELECT", "SESSION", "SET", "SHARE", "SIZE", "SMALLINT", "START", "SUCCESSFUL", "SYNONYM", "SYSDATE", "TABLE", "THEN", "TO", "TRIGGER", "UID", "UNION", "UNIQUE", "UPDATE", "USER", "VALIDATE", "VALUES", "VARCHAR", "VARCHAR2", "VIEW", "WHENEVER", "WHERE", "WITH"};

    private String filePath;
    private String tableName;
    private int srid;

    private DBFReaderJGeom dbfReader;
    private ShapefileReaderJGeom shpReader;

    private String min_x = "-180";
    private String min_y = "-90";
    private String max_x = "180";
    private String max_y = "90";
    private String m_tolerance = "0.05";
    private String mg_tolerance = "0.000000005";
    private int m_start_id = 1;
    private int m_commit_interval = -1;
    private int m_println_interval = 10;
    private String dimArray = null;
    private String dimArrayMig = null;
    private boolean defaultX = true;
    private boolean defaultY = true;
    private boolean appendMode = false;
    private String idName = "GID";
    private String geom = "GEOMETRY";


    private OracleConnection conn;

    public ImportShapeWorker(OracleConnection conn, String filePath, String tableName, int srid) throws Exception {
        this.conn = conn;
        this.filePath = filePath;
        this.tableName = tableName;
        this.srid = srid;
        init();
    }

    public void init() throws Exception {
        try {

            dbfReader = new DBFReaderJGeom(filePath);
            if (dbfReader == null) {
                throw new Exception("입력파일 : " + filePath + " 의 DBF 파일이 없습니다.");
            }
            shpReader = new ShapefileReaderJGeom(filePath);
            if (shpReader == null) {
                throw new Exception("입력파일 : " + filePath + " 의 SHP 파일이 없습니다.");
            }
            int shpType = shpReader.getShpFileType();
            double shpMin = shpReader.getMinMeasure();
            double shpMax = shpReader.getMaxMeasure();
            if (shpMax <= -1.0E39D) {
                shpMax = 0.0D / 0.0;
            } else if (shpMax == shpMin && (shpType == 11 || shpType == 13 || shpType == 15 || shpType == 18) && !validateMvalue(shpReader, srid)) {
                shpMax = 0.0D / 0.0;
            }

            double var18 = shpReader.getMinZ();
            double var20 = shpReader.getMaxZ();
            if (defaultX && !isGeodetic(conn, srid)) {
                min_x = String.valueOf(shpReader.getMinX());
                max_x = String.valueOf(shpReader.getMaxX());
            }

            if (defaultY && !isGeodetic(conn, srid)) {
                min_y = String.valueOf(shpReader.getMinY());
                max_y = String.valueOf(shpReader.getMaxY());
            }

            int var22 = ShapefileReaderJGeom.getShpDims(shpType, shpMax);
            dimArray = getDimArray(var22, m_tolerance, min_x, max_x, min_y, max_y, var18, var20, shpMin, shpMax);
            dimArrayMig = getDimArray(var22, mg_tolerance, min_x, max_x, min_y, max_y, var18, var20, shpMin, shpMax);
        } catch (Exception e) {

        }

    }

    @Override
    protected Object doInBackground() {
        try {
            setProgress(1);
            checkConnection();
            prepareTableForData(conn, dbfReader, tableName, idName, geom, srid, dimArray, null);
            setPK(conn, tableName);
            insertFeatures(conn, dbfReader, shpReader, tableName, idName, m_start_id, m_commit_interval, m_println_interval, srid, dimArrayMig);
            createIndex(conn, tableName);
            dbfReader.closeDBF();
            shpReader.closeShapefile();
            setProgress(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void checkConnection() {
        try {

            if (!conn.isValid(100)) {
                conn.close();
                conn = OracleConn.reConnect();
            }
        } catch (Exception e) {
        }
    }

    public void setPK(OracleConnection conn, String table) {
        String pkName = table + "_PK";
        String sql = "ALTER TABLE " + table + " ADD CONSTRAINT " + pkName + " PRIMARY KEY (" + idName + ")";
        Main.appendLog(sql);
        excuteUpdate(conn, sql);
    }

    public void createIndex(OracleConnection conn, String table) {
        String indexName = table + "_SPA_IDX";
        String sql = "CREATE INDEX " + indexName + " ON " + table + "(" + geom + ") INDEXTYPE IS MDSYS.SPATIAL_INDEX";
        Main.appendLog(sql);
        excuteUpdate(conn, sql);
    }

    private void excuteUpdate(OracleConnection conn, String sql) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public boolean isReservedWord(String var0) {
        if (var0 == null) {
            return false;
        } else {
            for (int var1 = 0; var1 < reservedWords.length; ++var1) {
                if (var0.equalsIgnoreCase(reservedWords[var1])) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isReservedWord(OracleConnection var0, String var1, String var2) {
        boolean var3 = false;
        PreparedStatement var4 = null;
        ResultSet var5 = null;
        if (var2 == null) {
            return isReservedWord(var1);
        } else {
            try {
                var4 = var0.prepareStatement("SELECT keyword FROM " + var2 + " WHERE UPPER(keyword)=?");
                var4.setString(1, var1.toUpperCase());

                for (var5 = var4.executeQuery(); var5.next(); var3 = true) {
                    ;
                }

                var4.close();
                var5.close();
            } catch (SQLException var9) {
                try {
                    if (var4 != null) {
                        var4.close();
                    }

                    if (var5 != null) {
                        var5.close();
                    }
                } catch (SQLException var8) {
                    ;
                }
            }

            return var3;
        }
    }

    public static boolean validateMvalue(ShapefileReaderJGeom var0, int var1) throws Exception {
        boolean var2 = false;
        AdapterShapefileJGeom var3 = new AdapterShapefileJGeom();
        JGeometry var4 = null;
        byte[] var5 = var0.getGeometryBytes(0);

        try {
            var4 = var3.importGeometry(var5, var1);
        } catch (RuntimeException var7) {
            var7.printStackTrace();
        }

        if (var4 != null) {
            var2 = ShapefileReaderJGeom.shpMval;
        }

        return var2;
    }

    public String getRelSchema(DBFReaderJGeom var0) {
        int var1 = var0.numFields();
        byte[] var2 = new byte[var1];

        for (int var3 = 0; var3 < var1; ++var3) {
            var2[var3] = var0.getFieldType(var3);
        }

        String var5 = "";

        for (int var4 = 0; var4 < var1; ++var4) {
            switch (var2[var4]) {
                case 66:
                    Main.appendLog("Field type B not yet supported");
                    break;
                case 67:
                case 76:
                    if (var4 != 0) {
                        var5 = var5 + ", " + var0.getFieldName(var4).toLowerCase() + " VARCHAR2(" + var0.getFieldLength(var4) + ")";
                    } else {
                        var5 = var0.getFieldName(var4).toLowerCase() + " VARCHAR2(" + var0.getFieldLength(var4) + ")";
                    }
                    break;
                case 68:
                    if (var4 != 0) {
                        var5 = var5 + ", " + var0.getFieldName(var4).toLowerCase() + " NUMBER(38)";
                    } else {
                        var5 = var0.getFieldName(var4).toLowerCase() + " NUMBER(38)";
                    }
                    break;
                case 69:
                case 72:
                case 74:
                case 75:
                default:
                    throw new RuntimeException("Undefined DBF field type (1) " + var2[var4]);
                case 70:
                case 78:
                    if (var4 != 0) {
                        var5 = var5 + ", " + var0.getFieldName(var4).toLowerCase() + " NUMBER";
                    } else {
                        var5 = var0.getFieldName(var4).toLowerCase() + " NUMBER";
                    }
                    break;
                case 71:
                    Main.appendLog("Field type G not yet supported");
                    break;
                case 73:
                    if (var4 != 0) {
                        var5 = var5 + ", " + var0.getFieldName(var4).toLowerCase() + " NUMBER(38)";
                    } else {
                        var5 = var0.getFieldName(var4).toLowerCase() + " NUMBER(38)";
                    }
                    break;
                case 77:
                    Main.appendLog("Field type M not yet supported");
            }
        }

        var5 = var5 + ", geometry MDSYS.SDO_GEOMETRY";
        return var5;
    }

    public String getRelSchema(DBFReaderJGeom var0, String var1) {
        String var2 = getRelSchema(var0);
        return var1 != null ? var1 + " NUMBER(38), " + var2 : var2;
    }

    public String[] getOraFieldNames(DBFReaderJGeom var0, byte[] var1, int var2) throws IOException {
        String[] var3 = new String[var2];
        int var4 = 0;

        for (int var5 = 0; var5 < var2; ++var5) {
            switch (var1[var5]) {
                case 66:
                    Main.appendLog("Field type B not yet supported");
                    break;
                case 67:
                case 68:
                case 70:
                case 73:
                case 76:
                case 78:
                    var3[var4] = var0.getFieldName(var5).toLowerCase();
                    break;
                case 69:
                case 72:
                case 74:
                case 75:
                default:
                    throw new RuntimeException("Undefined DBF field type (1) " + var1[var5]);
                case 71:
                    Main.appendLog("Field type G not yet supported");
                    break;
                case 77:
                    Main.appendLog("Field type M not yet supported");
            }

            ++var4;
        }

        return var3;
    }

    public String[] getOraFieldNames(DBFReaderJGeom var0, byte[] var1, int[] var2) throws IOException {
        if (var1 != null && var2 != null) {
            String[] var3 = new String[var1.length];
            int var4 = 0;

            for (int var5 = 0; var5 < var1.length; ++var5) {
                switch (var1[var5]) {
                    case 66:
                        Main.appendLog("Field type B not yet supported");
                        break;
                    case 67:
                    case 68:
                    case 70:
                    case 73:
                    case 76:
                    case 78:
                        var3[var4] = var0.getFieldName(var2[var5]).toLowerCase();
                        break;
                    case 69:
                    case 72:
                    case 74:
                    case 75:
                    default:
                        throw new RuntimeException("Undefined DBF field type (1) " + var1[var5]);
                    case 71:
                        Main.appendLog("Field type G not yet supported");
                        break;
                    case 77:
                        Main.appendLog("Field type M not yet supported");
                }

                ++var4;
            }

            return var3;
        } else {
            return null;
        }
    }

    public Hashtable fromRecordToFeature(DBFReaderJGeom var0, ShapefileReaderJGeom var1, byte[] var2, int var3, int var4, int var5) throws IOException {
        AdapterShapefileJGeom var6 = new AdapterShapefileJGeom();
        Hashtable var7 = new Hashtable();
        byte[] var8 = var0.getRecord(var4);

        for (int var9 = 0; var9 < var3; ++var9) {
            switch (var2[var9]) {
                case 66:
                    Main.appendLog("Field type B not yet supported");
                    break;
                case 67:
                case 76:
                    var7.put(var0.getFieldName(var9).toLowerCase(), var0.getFieldData(var9, var8));
                    break;
                case 68:
                    String var10 = var0.getFieldData(var9, var8);
                    var10 = var10.trim();

                    Integer var11;
                    try {
                        var11 = new Integer(var10);
                    } catch (NumberFormatException var16) {
                        var11 = new Integer(0);
                    }

                    var7.put(var0.getFieldName(var9).toLowerCase(), var11);
                    break;
                case 69:
                case 72:
                case 74:
                case 75:
                default:
                    throw new RuntimeException("Undefined DBF field type (1) " + var2[var9]);
                case 70:
                case 73:
                case 78:
                    Double var12;
                    try {
                        var12 = new Double(var0.getFieldData(var9, var8));
                        if (Math.abs(var12) <= 1.0E-130D) {
                            var12 = 0.0D;
                        }
                    } catch (NumberFormatException var15) {
                        var12 = new Double(0.0D / 0.0);
                    }

                    var7.put(var0.getFieldName(var9).toLowerCase(), var12);
                    break;
                case 71:
                    Main.appendLog("Field type G not yet supported");
                    break;
                case 77:
                    Main.appendLog("Field type M not yet supported");
            }
        }

        byte[] var18 = var1.getGeometryBytes(var4);
        JGeometry var17 = null;

        try {
            var17 = var6.importGeometry(var18, var5);
        } catch (RuntimeException var14) {
            var14.printStackTrace();
        }

        if (var17 != null) {
            var7.put("geometry", var17);
        }

        return var7;
    }

    public Hashtable fromRecordToFeature(DBFReaderJGeom var0, ShapefileReaderJGeom var1, String[] var2, int[] var3, byte[] var4, int var5, int var6) throws IOException {
        AdapterShapefileJGeom var7 = new AdapterShapefileJGeom();
        Hashtable var8 = new Hashtable();
        if (var2 != null && var3 != null && var4 != null) {
            int var9 = var2.length;
            byte[] var10 = var0.getRecord(var5);

            for (int var11 = 0; var11 < var9; ++var11) {
                switch (var4[var11]) {
                    case 66:
                        Main.appendLog("Field type B not yet supported");
                        break;
                    case 67:
                    case 76:
                        var8.put(var2[var11].toLowerCase(), var0.getFieldData(var3[var11], var10));
                        break;
                    case 68:
                        String var12 = var0.getFieldData(var3[var11], var10);
                        var12 = var12.trim();

                        Integer var13;
                        try {
                            var13 = new Integer(var12);
                        } catch (NumberFormatException var18) {
                            var13 = new Integer(0);
                        }

                        var8.put(var2[var11].toLowerCase(), var13);
                        break;
                    case 69:
                    case 72:
                    case 74:
                    case 75:
                    default:
                        throw new RuntimeException("Undefined DBF field type (1) " + var4[var11]);
                    case 70:
                    case 73:
                    case 78:
                        Double var14;
                        try {
                            var14 = new Double(var0.getFieldData(var3[var11], var10));
                            if (Math.abs(var14) <= 1.0E-130D) {
                                var14 = 0.0D;
                            }
                        } catch (NumberFormatException var17) {
                            var14 = new Double(0.0D / 0.0);
                        }

                        var8.put(var2[var11].toLowerCase(), var14);
                        break;
                    case 71:
                        Main.appendLog("Field type G not yet supported");
                        break;
                    case 77:
                        Main.appendLog("Field type M not yet supported");
                }
            }
        }

        byte[] var19 = var1.getGeometryBytes(var5);
        JGeometry var20 = null;

        try {
            var20 = var7.importGeometry(var19, var6);
        } catch (RuntimeException var16) {
            var16.printStackTrace();
        }

        if (var20 != null) {
            var8.put("geometry", var20);
        }

        return var8;
    }

    public static boolean isGeodetic(OracleConnection var0, int var1) throws SQLException {
        PreparedStatement var2 = var0.prepareStatement("SELECT COUNT(*) cnt FROM MDSYS.GEODETIC_SRIDS WHERE srid = ?");
        var2.setInt(1, var1);
        ResultSet var3 = var2.executeQuery();
        boolean var4 = false;
        if (var3.next() && var3.getInt("cnt") > 0) {
            var4 = true;
        }

        var2.close();
        return var4;
    }

    public static String getDimArray(int var0, String var1, String var2, String var3, String var4, String var5, double var6, double var8, double var10, double var12) {
        String var14 = null;
        if (var0 != 2 && var0 != 0) {
            if (var0 == 3 && Double.isNaN(var12)) {
                var14 = "MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X', " + var2 + ", " + var3 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + var4 + ", " + var5 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Z', " + var6 + ", " + var8 + ", " + var1 + "))";
            } else if (var0 == 3) {
                var14 = "MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X', " + var2 + ", " + var3 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + var4 + ", " + var5 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('M', " + var10 + ", " + var12 + ", " + var1 + "))";
            } else if (var0 == 4) {
                var14 = "MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X', " + var2 + ", " + var3 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + var4 + ", " + var5 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Z', " + var6 + ", " + var8 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('M', " + var10 + ", " + var12 + ", " + var1 + "))";
            }
        } else {
            var14 = "MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X', " + var2 + ", " + var3 + ", " + var1 + "), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + var4 + ", " + var5 + ", " + var1 + "))";
        }

        return var14;
    }

    public void prepareTableForData(OracleConnection var0, DBFReaderJGeom var1, String var2, String var3, String var4, int var5, String var6) throws IOException, SQLException {
        prepareTableForData(var0, var1, var2, var3, var4, var5, var6, (String) null);
    }

    public void prepareTableForData(OracleConnection var0, DBFReaderJGeom var1, String var2, String var3, String var4, int var5, String var6, String var7) throws IOException, SQLException {
        Main.appendLog("Dropping old table...");
        Statement var8 = null;

        String var9;
        int var11;
        try {
            var8 = var0.createStatement();
            var9 = "DROP TABLE " + var2;
            var8.executeUpdate(var9);
            var8.close();
        } catch (SQLException var18) {
            var11 = var18.getErrorCode();
            if (var11 == 942) {
                Main.appendLog("   Table not previously created.");
            } else {
                Main.appendLog(var18.getMessage());
            }
        }

        try {
            var8 = var0.createStatement();
            var9 = "DELETE FROM user_sdo_geom_metadata WHERE table_name = '" + var2.toUpperCase() + "'" + " AND column_name = '" + var4.toUpperCase() + "'";
            var8.executeUpdate(var9);
            var8.close();
        } catch (SQLException var16) {
            ;
        }

        String var10;
        try {
            var10 = getRelSchema(var1, var3);
            String var19 = var10;
            if (var4.toLowerCase() != "geometry") {
                if (isReservedWord(var0, var4, var7)) {
                    var19 = replaceAllWords(var10, "geometry", var4 + "_");
                } else {
                    var19 = replaceAllWords(var10, "geometry", var4);
                }
            }

            int var20 = var1.numFields();

            for (int var21 = 0; var21 < var20; ++var21) {
                String var14 = var1.getFieldName(var21).toLowerCase();
                if (isReservedWord(var0, var14, var7)) {
                    var19 = replaceAllWords(var19, var14, var14 + "_");
                }
            }

            Main.appendLog("Creating new table...");
            var8 = var0.createStatement();
            var9 = "CREATE TABLE " + var2 + " (" + var19 + ")";
            var8.executeUpdate(var9);
            var8.close();
        } catch (SQLException var17) {
            var11 = var17.getErrorCode();
            if (var11 == 904) {
                Main.appendLog("CREATE TABLE statement failed. Create table manually or try using the -k option.");
                String var12 = getRelSchema(var1, var3);
                String var13 = var12;
                if (var4.toLowerCase() != "geometry") {
                    var13 = replaceAllWords(var12, "geometry", var4);
                }

                Main.appendLog("CREATE TABLE " + var2 + "(" + var13 + ");");
            } else {
                Main.appendLog(var17.getMessage());
            }
        }

        var10 = "NULL";
        if (var5 > 0) {
            var10 = String.valueOf(var5);
        }

        try {
            var8 = var0.createStatement();
            var9 = "INSERT INTO user_sdo_geom_metadata VALUES ('" + var2.toUpperCase() + "', '" + var4.toUpperCase() + "', " + var6 + ", " + var10 + ")";
            var8.executeUpdate(var9);
            var8.close();
        } catch (SQLException var15) {
            Main.appendLog(var15.getMessage());
        }

    }

    static String replaceAllWords(String var0, String var1, String var2) {
        String var3 = "";
        String var4 = "+-*/(),. ";
        StringTokenizer var5 = new StringTokenizer(var0, var4, true);

        while (var5.hasMoreTokens()) {
            String var6 = var5.nextToken();
            if (var6.equals(var1)) {
                var3 = var3 + var2;
            } else {
                var3 = var3 + var6;
            }
        }

        return var3;
    }


    public void insertFeatures(OracleConnection var0, DBFReaderJGeom var1, ShapefileReaderJGeom var2, String var3, String var4, int var5, int var6, int var7, int var8, String var9) throws SQLException, IOException {
        int var10 = 0;
        int var11 = var1.numFields();
        int var12 = var1.numRecords();
        byte[] var13 = new byte[var11];

        for (int var14 = 0; var14 < var11; ++var14) {
            var13[var14] = var1.getFieldType(var14);
        }

        Hashtable var32 = null;
        var32 = fromRecordToFeature(var1, var2, var13, var11, 0, var8);
        int var15 = var32.size();
        String var16 = null;
        String var17 = null;
        if (var4 == null) {
            var16 = "(";
        } else {
            var16 = "(?,";
        }

        for (int var18 = 0; var18 < var15; ++var18) {
            if (var18 == 0) {
                var16 = var16 + " ?";
            } else {
                var16 = var16 + ", ?";
            }
        }

        var16 = var16 + ")";
        var17 = var16.substring(0, var16.length() - 2) + "MDSYS.SDO_MIGRATE.TO_CURRENT(?, " + var9 + "))";
        String[] var33 = getOraFieldNames(var1, var13, var11);
        String var19 = "INSERT INTO " + var3 + " VALUES" + var16;
        PreparedStatement var20 = var0.prepareStatement(var19);
        PreparedStatement var21 = var0.prepareStatement("COMMIT");
        String var22 = "INSERT INTO " + var3 + " VALUES" + var17;
        PreparedStatement var23 = var0.prepareStatement(var22);
        Object var24 = null;
        STRUCT var25 = null;
        int var26 = var2.getShpFileType();

        Main.setProgress(0, var12);

        for (int var27 = 0; var27 < var12; ++var27) {
            if ((var27 + 1) % var7 == 0) {
//                Main.appendLog("Converting record #" + (var27 + 1) + " of " + var12);
            }
            Main.setRecordCountLabel(tableName + " 작업중. : " + (var27 + 1) + " / " + var12 + " 행 입력 완료.");
            Main.setProgressValue(var27 + 1);
//            int currentProgress = Math.round(((var27 + 1) / var12) * 100);
//            if (getProgress() < currentProgress) {
//                setProgress(currentProgress);
//            }

            var32 = fromRecordToFeature(var1, var2, var13, var11, var27, var8);
            int var28;
            if (var4 == null) {
                try {
                    JGeometry var34;
                    if (var26 != 5 && var26 != 15 && var26 != 25) {
                        for (var28 = 0; var28 < var33.length; ++var28) {
                            if (var32.get(var33[var28]) instanceof String) {
                                var20.setString(var28 + 1, (String) var32.get(var33[var28]));
                            } else if (var32.get(var33[var28]) instanceof Integer) {
                                var20.setInt(var28 + 1, (Integer) var32.get(var33[var28]));
                            } else {
                                if (!(var32.get(var33[var28]) instanceof Double)) {
                                    throw new RuntimeException("Unsupported Column Type");
                                }

                                if (var32.get(var33[var28]).equals(new Double(0.0D / 0.0))) {
                                    var20.setNull(var28 + 1, 8);
                                } else {
                                    var20.setDouble(var28 + 1, (Double) var32.get(var33[var28]));
                                }
                            }
                        }

                        var34 = (JGeometry) var32.get("geometry");
                        if (var34 != null) {
                            var25 = JGeometry.store(var34, var0);
                            var20.setObject(var33.length + 1, var25);
                        } else {
                            var20.setNull(var33.length + 1, 2002, "MDSYS.SDO_GEOMETRY");
                        }

                        var20.executeUpdate();
                    } else {
                        for (var28 = 0; var28 < var33.length; ++var28) {
                            if (var32.get(var33[var28]) instanceof String) {
                                var23.setString(var28 + 1, (String) var32.get(var33[var28]));
                            } else if (var32.get(var33[var28]) instanceof Integer) {
                                var23.setInt(var28 + 1, (Integer) var32.get(var33[var28]));
                            } else {
                                if (!(var32.get(var33[var28]) instanceof Double)) {
                                    throw new RuntimeException("Unsupported Column Type");
                                }

                                if (var32.get(var33[var28]).equals(new Double(0.0D / 0.0))) {
                                    var23.setNull(var28 + 1, 8);
                                } else {
                                    var23.setDouble(var28 + 1, (Double) var32.get(var33[var28]));
                                }
                            }
                        }

                        var34 = (JGeometry) var32.get("geometry");
                        if (var34 != null) {
                            var25 = JGeometry.store(var34, var0);
                            var23.setObject(var33.length + 1, var25);
                        } else {
                            var23.setNull(var33.length + 1, 2002, "MDSYS.SDO_GEOMETRY");
                        }

                        var23.executeUpdate();
                    }
                } catch (SQLException var30) {
                    ++var10;
                    Main.appendLog(var30 + "\nRecord #" + (var27 + 1) + " not converted.");
                }
            } else {
                var28 = var27 + var5;

                try {
                    int var29;
                    JGeometry var35;
                    if (var26 != 5 && var26 != 15 && var26 != 25) {
                        var20.setInt(1, var28);

                        for (var29 = 0; var29 < var33.length; ++var29) {
                            if (var32.get(var33[var29]) instanceof String) {
                                var20.setString(var29 + 2, (String) var32.get(var33[var29]));
                            } else if (var32.get(var33[var29]) instanceof Integer) {
                                var20.setInt(var29 + 2, (Integer) var32.get(var33[var29]));
                            } else {
                                if (!(var32.get(var33[var29]) instanceof Double)) {
                                    throw new RuntimeException("Unsupported Column Type");
                                }

                                if (var32.get(var33[var29]).equals(new Double(0.0D / 0.0))) {
                                    var20.setNull(var29 + 2, 8);
                                } else {
                                    var20.setDouble(var29 + 2, (Double) var32.get(var33[var29]));
                                }
                            }
                        }

                        var35 = (JGeometry) var32.get("geometry");
                        if (var35 != null) {
                            var25 = JGeometry.store(var35, var0);
                            var20.setObject(var33.length + 2, var25);
                        } else {
                            var20.setNull(var33.length + 2, 2002, "MDSYS.SDO_GEOMETRY");
                        }

                        var20.executeUpdate();
                    } else {
                        var23.setInt(1, var28);

                        for (var29 = 0; var29 < var33.length; ++var29) {
                            if (var32.get(var33[var29]) instanceof String) {
                                var23.setString(var29 + 2, (String) var32.get(var33[var29]));
                            } else if (var32.get(var33[var29]) instanceof Integer) {
                                var23.setInt(var29 + 2, (Integer) var32.get(var33[var29]));
                            } else {
                                if (!(var32.get(var33[var29]) instanceof Double)) {
                                    throw new RuntimeException("Unsupported Column Type");
                                }

                                if (var32.get(var33[var29]).equals(new Double(0.0D / 0.0))) {
                                    var23.setNull(var29 + 2, 8);
                                } else {
                                    var23.setDouble(var29 + 2, (Double) var32.get(var33[var29]));
                                }
                            }
                        }

                        var35 = (JGeometry) var32.get("geometry");
                        if (var35 != null) {
                            var25 = JGeometry.store(var35, var0);
                            var23.setObject(var33.length + 2, var25);
                        } else {
                            var23.setNull(var33.length + 2, 2002, "MDSYS.SDO_GEOMETRY");
                        }

                        var23.executeUpdate();
                    }
                } catch (SQLException var31) {
                    ++var10;
                    Main.appendLog(var31 + "\nRecord #" + (var27 + 1) + " not converted.");
                }
            }

            if (var6 == -1) {
                if ((var27 + 1) % 1000 == 0) {
                    var0.commit();
                }
            } else if ((var27 + 1) % var6 == 0) {
                var0.commit();
            }
        }
        var0.commit();
        var20.close();
        var23.close();
        var21.close();
        if (var10 > 0) {
            Main.appendLog(var10 + " record(s) not converted.");
        }

        Main.appendLog(var12 - var10 + " record(s) converted.");
        Main.appendLog("Done.");
        Main.setProgressValue(var12);
    }

    public void insertFeatures(OracleConnection var0, DBFReaderJGeom var1, ShapefileReaderJGeom var2, String var3, int var4, int var5, String var6) throws SQLException, IOException {
        Object var7 = null;
        byte var8 = 1;
        byte var9 = 10;
        insertFeatures(var0, var1, var2, var3, (String) var7, var8, var4, var9, var5, var6);
    }
}
