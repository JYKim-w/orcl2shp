import oracle.jdbc.driver.OracleConnection;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static java.awt.image.ImageObserver.ABORT;

public class Frame {
    private JPanel panel1;
    private JTextField inputHost;
    private JTextField inputPort;
    private JTextField inputSid;
    private JTextField inputUser;
    private JTextField inputPwd;
    private JButton connectionTestButton;
    private JRadioButton fileRadioButton;
    private JRadioButton directoryRadioButton;
    private JTextField inputFile;
    private JButton searchButton;
    private JButton importButton;
    private JButton cancelButton;
    private JPanel panel2;
    private JLabel hostLabel;
    private JLabel portLabel;
    private JLabel sidLabel;
    private JLabel userLabel;
    private JLabel pwdLabel;
    private JLabel fileLabel;
    private JTextField inputSRID;
    private JProgressBar progressBar1;
    private JLabel fileCount;
    private JLabel record;
    private JTextArea logArea;
    private ButtonGroup buttonGroup1;
    private JFileChooser fileChooser;
    private JFrame fileChooserFrame = new JFrame();

    private String host = "";
    private String port = "";
    private String sid = "";
    private String user = "";
    private String pwd = "";
    private String table = "";
    private String file = "";


    public Frame() {
        ConnectTestBtn();
        fileChooser();
        cancelBtn();
        importBtn();
        progressBar1.setStringPainted(true);

    }

    public void setFileCountLabel(String msg) {
        fileCount.setText(msg);
    }

    public void setRecordCountLabel(String msg) {
        record.setText(msg);
    }

    public void setProgress(int min, int max) {
        progressBar1.setValue(0);
        progressBar1.setMinimum(min);
        progressBar1.setMaximum(max);
    }

    public void setProgress(int n) {
        progressBar1.setValue(n);
    }

    public void appenLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public JPanel getPanel() {
        return panel1;
    }

    public void refreshProgress() {
        progressBar1.repaint();
    }

    public void importBtn() {
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean connOk = connectionTest(false);
                if (!connOk) {
                    JOptionPane.showMessageDialog(null, "DB 접속 정보를 확인해주세요.", "DB 연결 오류", JOptionPane.ERROR_MESSAGE);
                } else {
                    OracleConnection conn = OracleConn.getConnection(host, port, sid, user, pwd);
                    String sridStr = inputSRID.getText();
                    if (sridStr.equalsIgnoreCase("")) {
                        JOptionPane.showMessageDialog(null, "SRID를 입력해주세요", "오류", JOptionPane.ERROR_MESSAGE);
                    } else {
                        int srid = Integer.parseInt(sridStr);
                        try {
                            if (fileRadioButton.isSelected()) {
                                OracleShpImport.importFile(conn, inputFile.getText(), srid);
                            } else if (directoryRadioButton.isSelected()) {
                                OracleShpImport.importDir(conn, inputFile.getText(), srid);
                            }
                        } catch (Exception e2) {
                            JOptionPane.showMessageDialog(null, e2.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }

    private void ConnectTestBtn() {
        connectionTestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectionTest(true);
            }
        });
    }


    private boolean connectionTest(boolean useAlert) {
        getConnectionInfo();
        boolean isValid = OracleConn.connectionTest(host, port, sid, user, pwd);
        if (useAlert) {
            if (isValid) {
                JOptionPane.showMessageDialog(null, "DB 접속 성공", "DB 연결 테스트", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "DB 접속 실패", "DB 연결 테스트", JOptionPane.ERROR_MESSAGE);
            }
        }
        return isValid;
    }

    private void getConnectionInfo() {
        host = inputHost.getText();
        port = inputPort.getText();
        sid = inputSid.getText();
        user = inputUser.getText();
        pwd = inputPwd.getText();
    }

    private void fileChooser() {
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser = new JFileChooser();
                if (fileRadioButton.isSelected()) {
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            if (f.isFile()) {
                                return f.getPath().toLowerCase().endsWith(".shp");

                            } else {
                                return true;
                            }
                        }

                        @Override
                        public String getDescription() {
                            return ".shp(ShapeFile)";
                        }
                    });
                } else if (directoryRadioButton.isSelected()) {
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }

                int returnVal = fileChooser.showSaveDialog(fileChooserFrame);

                if (returnVal == fileChooser.APPROVE_OPTION) {
                    String path = fileChooser.getSelectedFile().toURI().toString();
                    path = path.substring(6, path.length());
                    inputFile.setText(path);
                }
            }
        });
    }

    private void cancelBtn() {
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OracleShpImport.cancel();
            }
        });
    }


}
