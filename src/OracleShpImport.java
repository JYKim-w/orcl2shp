import oracle.jdbc.driver.OracleConnection;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OracleShpImport {

    static int done = 0;
    public static ImportShapeWorker currentWorker = null;


    public static boolean importFile(OracleConnection conn, String filePath, int srid) throws Exception {
        filePath = removeExtenstion(filePath);
        String tableName = getTableName(filePath);
        Main.setFileCountLabel("진행도 : 0/1 개 파일 저장 완료.");
        currentWorker = new ImportShapeWorker(conn, filePath, tableName, srid);
        currentWorker.execute();
        Main.setFileCountLabel("진행도 : 1/1 개 파일 저장 완료.");
        OracleConn.commit();
        OracleConn.safeClose();
        return true;
    }


    public static boolean importDir(OracleConnection conn, String filePath, int srid) throws Exception {
        File dir = new File(filePath);
        List<String> list = new ArrayList<>();
        getSHPFileList(dir, list);
        done = 0;
        workSync(conn, srid, list, done);
        OracleConn.commit();
        OracleConn.safeClose();
        return true;
    }

    public static void workSync(OracleConnection conn, int srid, List<String> fileList, int index) {
        try {

            String file = fileList.get(index);
            String tableName = getTableName(file);
            currentWorker = new ImportShapeWorker(conn, file, tableName, srid);
            currentWorker.execute();
            currentWorker.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equalsIgnoreCase("state") && evt.getNewValue().toString().equalsIgnoreCase("DONE")) {
                    if (currentWorker.isCancelled()) {
                        Main.appendLog("작업취소.");
                    } else {
                        if (done >= fileList.size()) {
                            JOptionPane.showMessageDialog(null, "저장 완료.", "작업완료", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            Main.setFileCountLabel("진행도 : " + (done++) + "/" + fileList.size() + " 개 파일 저장 완료.");
                            workSync(conn, srid, fileList, done);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String getTableName(String path) {
        String name = "";
        if (path.indexOf(File.separator) != -1) {
            name = path.substring(path.lastIndexOf(File.separator) + 1, path.length());
        } else if (path.indexOf("/") != -1) {
            name = path.substring(path.lastIndexOf("/") + 1, path.length());
        }
        return name;
    }


    public static List<String> getSHPFileList(File root, List<String> shpList) {
        if (!root.exists()) return null;
        if (root.isDirectory()) {
            File[] list = root.listFiles();
            for (File file : list) {
                if (file.isFile()) {
                    String filePath = file.getPath();
                    if (filePath.toLowerCase().endsWith(".shp")) {
                        shpList.add(removeExtenstion(filePath));
                    }
                } else if (file.isDirectory()) {
                    getSHPFileList(file, shpList);
                }
            }
        }
        return shpList;
    }

    private static String removeExtenstion(String path) {
        return path.substring(0, path.lastIndexOf("."));
    }

    public static void cancel() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(false);
        }
    }
}
