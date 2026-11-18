import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/** Unit 5 : Swing GUI using GridLayout + BorderLayout and ActionListener. */
public class MainFrame extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;

    /** True while combo boxes are being refilled, so their events are ignored. */
    private boolean refreshing = false;

    // Module 1
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"DOMESTIC", "COMMERCIAL"});
    private final JTextField nameF = new JTextField(), addrF = new JTextField(), phoneF = new JTextField();
    private final DefaultTableModel consumerModel = model("ID", "Name", "Type", "Address", "Phone");
    // Module 2
    private final JComboBox<Consumer> readConsumer = new JComboBox<>();
    private final JTextField prevF = new JTextField(), currF = new JTextField();
    // Module 3
    private final JComboBox<Database.Item> pendingBox = new JComboBox<>();
    private final JTextArea billArea = new JTextArea();
    // Module 4
    private final JComboBox<Bill> unpaidBox = new JComboBox<>();
    private final JTextArea payArea = new JTextArea();
    // Module 5
    private final DefaultTableModel defModel =
        model("Consumer ID", "Name", "Phone", "Bill #", "Amount (Rs.)", "Due Date", "Days Overdue");
    // Alerts
    private final JLabel alertLabel = new JLabel("Checking for overdue bills...");

    public MainFrame() {
        super("Electricity Bill Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        JLabel title = new JLabel("Electricity Bill Management System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Consumer Registration", registrationPanel());
        tabs.addTab("2. Meter Reading", readingPanel());
        tabs.addTab("3. Bill Calculation", billPanel());
        tabs.addTab("4. Payment", paymentPanel());
        tabs.addTab("5. Defaulter List", defaulterPanel());
        add(tabs, BorderLayout.CENTER);

        alertLabel.setForeground(Color.RED.darker());
        alertLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(alertLabel, BorderLayout.SOUTH);

        refreshAll();
        new DueDateAlertThread(alertLabel, 30_000).start();
    }

    // ---------- panel builders ----------
    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    private JButton button(String text, String cmd) {
        JButton b = new JButton(text);
        b.setActionCommand(cmd);
        b.addActionListener(this);
        return b;
    }
    private JPanel registrationPanel() {
        JPanel form = new JPanel(new GridLayout(5, 2, 6, 6));
        form.add(new JLabel("Consumer Type:")); form.add(typeBox);
        form.add(new JLabel("Name:"));          form.add(nameF);
        form.add(new JLabel("Address:"));       form.add(addrF);
        form.add(new JLabel("Phone:"));         form.add(phoneF);
        form.add(new JLabel());                 form.add(button("Register", "REGISTER"));
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(consumerModel)), BorderLayout.CENTER);
        return p;
    }
    private JPanel readingPanel() {
        readConsumer.setActionCommand("PICK_CONSUMER");
        readConsumer.addActionListener(this);
        prevF.setEditable(false);
        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Consumer:"));         form.add(readConsumer);
        form.add(new JLabel("Previous Reading:")); form.add(prevF);
        form.add(new JLabel("Current Reading:"));  form.add(currF);
        form.add(new JLabel());                    form.add(button("Save Reading", "SAVE_READING"));
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(form, BorderLayout.NORTH);
        return p;
    }
    private JPanel billPanel() {
        JPanel top = new JPanel(new GridLayout(1, 2, 6, 6));
        top.add(pendingBox);
        top.add(button("Calculate Bill", "CALC_BILL"));
        billArea.setEditable(false);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(billArea), BorderLayout.CENTER);
        return p;
    }
    private JPanel paymentPanel() {
        JPanel top = new JPanel(new GridLayout(1, 2, 6, 6));
        top.add(unpaidBox);
        top.add(button("Record Payment", "PAY"));
        payArea.setEditable(false);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(payArea), BorderLayout.CENTER);
        return p;
    }
    private JPanel defaulterPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JScrollPane(new JTable(defModel)), BorderLayout.CENTER);
        p.add(button("Refresh List", "REFRESH_DEF"), BorderLayout.SOUTH);
        return p;
    }

    // ---------- data refresh ----------
    private void refreshAll() {
        refreshing = true;
        try {
            List<Consumer> cons = Database.getConsumers();
            consumerModel.setRowCount(0);
            readConsumer.removeAllItems();
            for (Consumer c : cons) {
                consumerModel.addRow(new Object[]{c.getId(), c.getName(), c.getType(), c.getAddress(), c.getPhone()});
                readConsumer.addItem(c);
            }
            pendingBox.removeAllItems();
            for (Database.Item i : Database.getPendingReadings()) pendingBox.addItem(i);
            unpaidBox.removeAllItems();
            for (Bill b : Database.getUnpaidBills()) unpaidBox.addItem(b);
            defModel.setRowCount(0);
            for (Object[] r : Database.getDefaulters()) defModel.addRow(r);
        } catch (SQLException e) {
            error("Database error: " + e.getMessage());
        } finally {
            refreshing = false;
        }
        try { showPrevious(); } catch (SQLException e) { prevF.setText(""); }
    }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg); }

    // ---------- event handling ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (refreshing) return;
        try {
            switch (e.getActionCommand()) {
                case "REGISTER":      register();     break;
                case "PICK_CONSUMER": showPrevious(); break;
                case "SAVE_READING":  saveReading();  break;
                case "CALC_BILL":     calcBill();     break;
                case "PAY":           pay();          break;
                case "REFRESH_DEF":   refreshAll();   break;
            }
        } catch (InvalidReadingException ex) {
            error(ex.getMessage());
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    private void register() throws SQLException {
        String n = nameF.getText().trim(), a = addrF.getText().trim(), ph = phoneF.getText().trim();
        if (n.isEmpty() || ph.isEmpty()) { error("Name and phone are required."); return; }
        Consumer c = "COMMERCIAL".equals(typeBox.getSelectedItem())
            ? new CommercialConsumer(0, n, a, ph) : new DomesticConsumer(0, n, a, ph);
        int id = Database.addConsumer(c);
        nameF.setText(""); addrF.setText(""); phoneF.setText("");
        refreshAll();
        info("Consumer registered with ID " + id);
    }
    private void showPrevious() throws SQLException {
        Consumer c = (Consumer) readConsumer.getSelectedItem();
        prevF.setText(c == null ? "" : String.valueOf(Database.getLastReading(c.getId())));
    }
    private void saveReading() throws SQLException, InvalidReadingException {
        Consumer c = (Consumer) readConsumer.getSelectedItem();
        if (c == null) { error("Register a consumer first."); return; }
        int curr;
        try { curr = Integer.parseInt(currF.getText().trim()); }
        catch (NumberFormatException ex) { throw new InvalidReadingException("Reading must be a whole number."); }
        int prev;
        try { prev = Integer.parseInt(prevF.getText().trim()); }
        catch (NumberFormatException ex) { prev = Database.getLastReading(c.getId()); }
        Meter m = new Meter(c.getId(), prev, curr);
        m.validate();
        int id = Database.addReading(m);
        currF.setText("");
        refreshAll();
        for (int i = 0; i < readConsumer.getItemCount(); i++)
            if (readConsumer.getItemAt(i).getId() == c.getId()) { readConsumer.setSelectedIndex(i); break; }
        showPrevious();
        info("Reading #" + id + " saved. Units consumed: " + m.getUnits()
             + "\nEstimated bill: Rs." + String.format("%.2f", c.calculateBill(m.getUnits()))
             + "\nGo to 'Bill Calculation' to generate the bill.");
    }
    private void calcBill() throws SQLException {
        Database.Item item = (Database.Item) pendingBox.getSelectedItem();
        if (item == null) { error("No pending readings. Enter a meter reading first."); return; }
        Bill b = Database.generateBill(item.id);                       // CallableStatement
        Consumer c = Database.getConsumer(b.getConsumerId());
        billArea.setText("BILL GENERATED\n--------------------------\n"
            + "Bill No      : " + b.getBillId() + "\n"
            + "Consumer     : " + b.getConsumerName() + " (" + c.getType() + ")\n"
            + "Units        : " + b.getUnits() + "\n"
            + "Amount (DB)  : Rs." + String.format("%.2f", b.getAmount()) + "\n"
            + "Check (Java) : Rs." + String.format("%.2f", c.calculateBill(b.getUnits())) + "\n"
            + "Bill Date    : " + b.getBillDate() + "\n"
            + "Due Date     : " + b.getDueDate() + "\n"
            + "Status       : " + b.getStatus() + "\n");
        refreshAll();
    }
    private void pay() throws SQLException {
        Bill b = (Bill) unpaidBox.getSelectedItem();
        if (b == null) { error("No unpaid bills."); return; }
        Consumer c = Database.getConsumer(b.getConsumerId());
        c.pay(b.getBillId());                                          // Payable interface
        payArea.setText("PAYMENT RECORDED\n--------------------------\n"
            + "Bill No  : " + b.getBillId() + "\nConsumer : " + b.getConsumerName()
            + "\nAmount   : Rs." + String.format("%.2f", b.getAmount()) + "\nStatus   : PAID\n");
        refreshAll();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
