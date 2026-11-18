import java.sql.*;
import java.util.*;

/** Unit 4 : all JDBC work. Bill calculation uses a CallableStatement. */
public class Database {
    // Optional overrides via environment variables: EBMS_DB_HOST, EBMS_DB_PORT, EBMS_DB_USER, EBMS_DB_PASS
    private static final String HOST = env("EBMS_DB_HOST", "localhost");
    private static final String USER = env("EBMS_DB_USER", "root");
    private static final String PASS = env("EBMS_DB_PASS", "root");   // empty password is also tried automatically
    private static final String DB   = "ebms";

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    /** Small id + label holder for combo boxes. */
    public static class Item {
        public final int id; public final String label;
        public Item(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    // ---------- zero-setup bootstrap: driver download + automatic MySQL/MariaDB detection ----------
    /** One supported JDBC driver (MySQL Connector/J or MariaDB Connector/J - XAMPP ships MariaDB). */
    private static class Flavor {
        final String scheme, cls, jarPrefix, jarName, jarUrl; Driver drv;
        Flavor(String scheme, String cls, String jarPrefix, String jarName, String jarUrl) {
            this.scheme = scheme; this.cls = cls; this.jarPrefix = jarPrefix; this.jarName = jarName; this.jarUrl = jarUrl;
        }
    }
    private static final Flavor[] FLAVORS = {
        new Flavor("mysql", "com.mysql.cj.jdbc.Driver", "mysql-connector", "mysql-connector-j-8.4.0.jar",
            "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"),
        new Flavor("mariadb", "org.mariadb.jdbc.Driver", "mariadb-java-client", "mariadb-java-client-3.4.1.jar",
            "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar")
    };

    private static Flavor chosen;            // driver + port + password that worked
    private static String chosenPort, chosenPass;
    private static boolean ready = false;    // schema checked/created

    /** Finds the driver on the classpath, in ./lib, or downloads it once. */
    private static Driver loadDriver(Flavor f) throws Exception {
        if (f.drv != null) return f.drv;
        try {
            return f.drv = (Driver) Class.forName(f.cls).getDeclaredConstructor().newInstance();
        } catch (Exception notOnClasspath) { /* fall through */ }

        java.io.File[] dirs = { new java.io.File("lib"),
            new java.io.File(System.getProperty("user.home"), ".ebms/lib") };
        for (java.io.File dir : dirs) {                          // existing jar?
            java.io.File[] jars = dir.listFiles((d, n) -> n.startsWith(f.jarPrefix) && n.endsWith(".jar"));
            if (jars != null && jars.length > 0) return f.drv = openJar(jars[0], f);
        }
        java.io.IOException failure = null;
        for (java.io.File dir : dirs) {                          // download once
            try {
                dir.mkdirs();
                java.io.File jar = new java.io.File(dir, f.jarName);
                java.net.URLConnection c = java.net.URI.create(f.jarUrl).toURL().openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(60000);
                try (java.io.InputStream in = c.getInputStream()) {
                    java.nio.file.Files.copy(in, jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                return f.drv = openJar(jar, f);
            } catch (java.io.IOException e) { failure = e; }
        }
        throw new SQLException("driver not found and download failed (" + failure + ")");
    }
    private static Driver openJar(java.io.File jar, Flavor f) throws Exception {
        java.net.URLClassLoader cl = new java.net.URLClassLoader(
            new java.net.URL[]{ jar.toURI().toURL() }, Database.class.getClassLoader());
        return (Driver) cl.loadClass(f.cls).getDeclaredConstructor().newInstance();
    }

    private static Connection open(Flavor f, String port, String pass, String db) throws Exception {
        java.util.Properties pr = new java.util.Properties();
        pr.setProperty("user", USER); pr.setProperty("password", pass);
        pr.setProperty("connectTimeout", "5000");
        if (f.scheme.equals("mysql")) {
            pr.setProperty("useSSL", "false"); pr.setProperty("allowPublicKeyRetrieval", "true");
        }
        Connection c = loadDriver(f).connect("jdbc:" + f.scheme + "://" + HOST + ":" + port + "/" + db, pr);
        if (c == null) throw new SQLException("driver rejected url");
        return c;
    }

    /** Connects to the given database ("" = server only), auto-detecting driver, port and password. */
    private static synchronized Connection connect(String db) throws SQLException {
        if (chosen != null) {
            try { return open(chosen, chosenPort, chosenPass, db); }
            catch (SQLException e) { throw e; }
            catch (Exception e) { throw new SQLException(e); }
        }
        String[] ports = System.getenv("EBMS_DB_PORT") != null
            ? new String[]{ System.getenv("EBMS_DB_PORT") } : new String[]{ "3306", "3307" };
        String[] passes = PASS.isEmpty() ? new String[]{ "" } : new String[]{ PASS, "" };
        StringBuilder errors = new StringBuilder();
        for (Flavor f : FLAVORS) {
            try { loadDriver(f); }
            catch (Exception e) {
                errors.append("\n - ").append(f.scheme).append(" driver: ").append(e.getMessage());
                continue;
            }
            for (String port : ports)
                for (String pw : passes) {
                    try {
                        Connection c = open(f, port, pw, db);
                        chosen = f; chosenPort = port; chosenPass = pw;
                        return c;
                    } catch (Exception e) {
                        String m = String.valueOf(e.getMessage());
                        if (m.length() > 160) m = m.substring(0, 160) + "...";
                        errors.append("\n - ").append(f.scheme).append(" driver, port ").append(port)
                              .append(pw.isEmpty() ? ", empty password: " : ", password: ").append(m);
                    }
                }
        }
        throw new SQLException("Cannot connect to the database. Make sure MySQL is started in XAMPP "
            + "(and that you are online the first time so the driver can download)." + errors);
    }

    private static synchronized void ensureSchema() throws SQLException {
        if (ready) return;
        try (Connection con = connect(""); Statement st = con.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS ebms");
            st.execute("USE ebms");
            st.execute("CREATE TABLE IF NOT EXISTS consumers (consumer_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(60) NOT NULL, address VARCHAR(120), phone VARCHAR(15), "
                + "type ENUM('DOMESTIC','COMMERCIAL') NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS readings (reading_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "consumer_id INT NOT NULL, reading_date DATE NOT NULL, previous_reading INT NOT NULL, "
                + "current_reading INT NOT NULL, FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id))");
            st.execute("CREATE TABLE IF NOT EXISTS bills (bill_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "consumer_id INT NOT NULL, reading_id INT NOT NULL UNIQUE, units INT NOT NULL, "
                + "amount DECIMAL(10,2) NOT NULL, bill_date DATE NOT NULL, due_date DATE NOT NULL, "
                + "status ENUM('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID', paid_date DATE NULL, "
                + "FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id), "
                + "FOREIGN KEY (reading_id) REFERENCES readings(reading_id))");
            st.execute("DROP PROCEDURE IF EXISTS calculate_bill");
            st.execute("CREATE PROCEDURE calculate_bill(IN p_reading_id INT, OUT p_bill_id INT, "
                + "OUT p_units INT, OUT p_amount DECIMAL(10,2)) BEGIN "
                + "DECLARE v_cons INT; DECLARE v_type VARCHAR(12); DECLARE v_prev INT; DECLARE v_curr INT; "
                + "DECLARE r1, r2, r3, fixed_chg DECIMAL(8,2); "
                + "SELECT r.consumer_id, c.type, r.previous_reading, r.current_reading "
                + "INTO v_cons, v_type, v_prev, v_curr "
                + "FROM readings r JOIN consumers c ON c.consumer_id = r.consumer_id "
                + "WHERE r.reading_id = p_reading_id; "
                + "SET p_units = v_curr - v_prev; "
                + "IF v_type = 'DOMESTIC' THEN SET fixed_chg = 50, r1 = 3, r2 = 5, r3 = 7; "
                + "ELSE SET fixed_chg = 150, r1 = 6, r2 = 8, r3 = 10; END IF; "
                + "SET p_amount = fixed_chg + LEAST(p_units, 100) * r1 "
                + "+ GREATEST(LEAST(p_units, 300) - 100, 0) * r2 + GREATEST(p_units - 300, 0) * r3; "
                + "INSERT INTO bills(consumer_id, reading_id, units, amount, bill_date, due_date, status) "
                + "VALUES (v_cons, p_reading_id, p_units, p_amount, CURDATE(), "
                + "DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'UNPAID'); "
                + "SET p_bill_id = LAST_INSERT_ID(); END");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consumers")) {
                rs.next();
                if (rs.getInt(1) == 0) {                          // sample data on first run only
                    st.execute("INSERT INTO consumers(name,address,phone,type) VALUES "
                        + "('Ravi Kumar','12 Model Town, Ludhiana','9876500001','DOMESTIC'),"
                        + "('Sharma Traders','Shop 4, Chaura Bazar','9876500002','COMMERCIAL')");
                    st.execute("INSERT INTO readings(consumer_id,reading_date,previous_reading,current_reading) "
                        + "VALUES (1, DATE_SUB(CURDATE(), INTERVAL 40 DAY), 0, 180)");
                    st.execute("INSERT INTO bills(consumer_id,reading_id,units,amount,bill_date,due_date,status) "
                        + "VALUES (1, 1, 180, 50+100*3+80*5, DATE_SUB(CURDATE(), INTERVAL 40 DAY), "
                        + "DATE_SUB(CURDATE(), INTERVAL 25 DAY), 'UNPAID')");
                }
            }
        }
        ready = true;
    }

    public static Connection getConnection() throws SQLException {
        ensureSchema();
        return connect(DB);
    }

    private static Consumer makeConsumer(ResultSet rs) throws SQLException {
        int id = rs.getInt("consumer_id");
        String n = rs.getString("name"), a = rs.getString("address"), p = rs.getString("phone");
        return "COMMERCIAL".equals(rs.getString("type"))
                ? new CommercialConsumer(id, n, a, p) : new DomesticConsumer(id, n, a, p);
    }

    // ---------- Module 1 : Consumer registration ----------
    public static int addConsumer(Consumer c) throws SQLException {
        String sql = "INSERT INTO consumers(name,address,phone,type) VALUES(?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName()); ps.setString(2, c.getAddress());
            ps.setString(3, c.getPhone()); ps.setString(4, c.getType());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }
    public static List<Consumer> getConsumers() throws SQLException {
        List<Consumer> list = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM consumers ORDER BY consumer_id")) {
            while (rs.next()) list.add(makeConsumer(rs));
        }
        return list;
    }
    public static Consumer getConsumer(int id) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM consumers WHERE consumer_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return makeConsumer(rs); }
        }
    }

    // ---------- Module 2 : Meter reading entry ----------
    public static int getLastReading(int consumerId) throws SQLException {
        String sql = "SELECT current_reading FROM readings WHERE consumer_id=? " +
                     "ORDER BY reading_id DESC LIMIT 1";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, consumerId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
    public static int addReading(Meter m) throws SQLException {
        String sql = "INSERT INTO readings(consumer_id,reading_date,previous_reading,current_reading) " +
                     "VALUES(?,CURDATE(),?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getConsumerId()); ps.setInt(2, m.getPrevious()); ps.setInt(3, m.getCurrent());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    // ---------- Module 3 : Bill calculation by slab (CallableStatement) ----------
    public static List<Item> getPendingReadings() throws SQLException {
        String sql = "SELECT r.reading_id, c.name, r.current_reading - r.previous_reading AS units " +
                     "FROM readings r JOIN consumers c ON c.consumer_id=r.consumer_id " +
                     "WHERE NOT EXISTS (SELECT 1 FROM bills b WHERE b.reading_id=r.reading_id) " +
                     "ORDER BY r.reading_id";
        List<Item> list = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new Item(rs.getInt(1),
                    "Reading #" + rs.getInt(1) + " - " + rs.getString(2) + " - " + rs.getInt(3) + " units"));
        }
        return list;
    }
    public static Bill generateBill(int readingId) throws SQLException {
        try (Connection con = getConnection();
             CallableStatement cs = con.prepareCall("{call calculate_bill(?,?,?,?)}")) {
            cs.setInt(1, readingId);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DECIMAL);
            cs.execute();
            return getBill(cs.getInt(2));
        }
    }
    public static Bill getBill(int billId) throws SQLException {
        String sql = "SELECT b.*, c.name FROM bills b JOIN consumers c ON c.consumer_id=b.consumer_id " +
                     "WHERE b.bill_id=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Bill(rs.getInt("bill_id"), rs.getInt("consumer_id"), rs.getString("name"),
                    rs.getInt("units"), rs.getDouble("amount"), rs.getDate("bill_date"),
                    rs.getDate("due_date"), rs.getString("status"));
            }
        }
    }

    // ---------- Module 4 : Payment recording ----------
    public static List<Bill> getUnpaidBills() throws SQLException {
        List<Bill> list = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT bill_id FROM bills WHERE status='UNPAID' ORDER BY bill_id")) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getInt(1));
            for (int id : ids) list.add(getBill(id));
        }
        return list;
    }
    public static int recordPayment(int billId) throws SQLException {
        String sql = "UPDATE bills SET status='PAID', paid_date=CURDATE() WHERE bill_id=? AND status='UNPAID'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, billId);
            return ps.executeUpdate();
        }
    }

    // ---------- Module 5 : Defaulter list ----------
    private static final String DEFAULTER_FROM =
        "FROM bills b JOIN consumers c ON c.consumer_id=b.consumer_id " +
        "WHERE b.status='UNPAID' AND b.due_date < CURDATE()";

    public static List<Object[]> getDefaulters() throws SQLException {
        String sql = "SELECT c.consumer_id, c.name, c.phone, b.bill_id, b.amount, b.due_date, " +
                     "DATEDIFF(CURDATE(), b.due_date) " + DEFAULTER_FROM + " ORDER BY b.due_date";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                rows.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                                       rs.getDouble(5), rs.getDate(6), rs.getInt(7) });
        }
        return rows;
    }
    public static int countOverdue() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) " + DEFAULTER_FROM)) {
            rs.next(); return rs.getInt(1);
        }
    }
}
