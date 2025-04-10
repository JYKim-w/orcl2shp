import javax.swing.*;

public class Main {


    private static JFrame jFrame = null;
    private static Frame frame = null;

    public static void main(String[] var0) throws Exception {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        jFrame = new JFrame("Frame");
        frame = new Frame();
        jFrame.setTitle("Oracle Shape Import");
        jFrame.setContentPane(frame.getPanel());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);
        jFrame.setLocationRelativeTo(null);
    }


    public static void setProgress(int min, int max) {
        frame.setProgress(min, max);
    }

    public static void setProgressValue(int n) {
        frame.setProgress(n);
    }

    public static void setFileCountLabel(String msg) {
        frame.setFileCountLabel(msg);
    }

    public static void setRecordCountLabel(String msg) {
        frame.setRecordCountLabel(msg);
    }

    public static void appendLog(String msg) {
        frame.appenLog(msg);
    }

}

