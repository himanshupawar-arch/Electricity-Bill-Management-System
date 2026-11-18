import javax.swing.*;
import java.sql.SQLException;

/** Unit 3 : background thread that periodically checks for overdue bills. */
public class DueDateAlertThread extends Thread {
    private final JLabel alertLabel;
    private final long intervalMs;

    public DueDateAlertThread(JLabel alertLabel, long intervalMs) {
        this.alertLabel = alertLabel; this.intervalMs = intervalMs;
        setDaemon(true);
    }

    @Override public void run() {
        while (!isInterrupted()) {
            String msg;
            try {
                int n = Database.countOverdue();
                msg = n > 0 ? "ALERT: " + n + " bill(s) are past their due date!"
                            : "No overdue bills.";
            } catch (SQLException e) {
                msg = "Alert check failed: " + e.getMessage();
            }
            final String text = msg;
            SwingUtilities.invokeLater(() -> alertLabel.setText(text));
            try { Thread.sleep(intervalMs); } catch (InterruptedException e) { return; }
        }
    }
}
