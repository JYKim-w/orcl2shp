import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OracleDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OracleConn {

    private static String _host = "";
    private static String _port = "";
    private static String _sid = "";
    private static String _user = "";
    private static String _pwd = "";
    private static String _table = "";
    private static String _file = "";


    private static OracleConnection ora_conn = null;

    public static OracleConnection getConnection(String host, String port, String sid, String user, String pwd) {
        _host = host;
        _port = port;
        _sid = sid;
        _user = user;
        _pwd = pwd;
        _getConnection();
        return ora_conn;
    }

    public static OracleConnection reConnect() {
        return _getConnection();
    }

    private static OracleConnection _getConnection() {
        try {
            if (ora_conn == null || ora_conn.isClosed()) {
                String url = "jdbc:oracle:thin:@ " + _host + ":" + _port + ":" + _sid;
                Connection conn = null;
                if (_sid != null && _sid.length() != 0) {
                    try {
                        DriverManager.registerDriver(new OracleDriver());
                        ora_conn = (OracleConnection) DriverManager.getConnection(url, _user, _pwd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        conn = DriverManager.getConnection("jdbc:oracle:thin:@(DESCRIPTION = (ADDRESS = (PROTOCOL = TCP)(HOST = " + _host + ")(PORT = " + _port + ")) (LOAD_BALANCE = yes) " + "(CONNECT_DATA = (SERVER = DEDICATED)(SERVICE_NAME = " + _sid + ")) )", _user, _pwd);
                        ora_conn = (OracleConnection) conn;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ora_conn.setAutoCommit(false);
            }
        } catch (Exception e) {
            safeClose();
        }
        return ora_conn;
    }

    public static boolean connectionTest(String host, String port, String sid, String user, String pwd) {
        OracleConnection conn = getConnection(host, port, sid, user, pwd);
        boolean isVaid = false;
        try {
            isVaid = conn.isValid(200);
        } catch (SQLException e) {
            isVaid = false;
        }
        return isVaid;
    }

    public static void safeClose() {
        if (ora_conn != null) {
            try {
                ora_conn.close();
            } catch (Exception e) {

            }
            ora_conn = null;
        }
    }

    public static void commit() {
        if (ora_conn != null) {
            try {
                ora_conn.commit();
            } catch (Exception e) {

            }
            ora_conn = null;
        }
    }
}
