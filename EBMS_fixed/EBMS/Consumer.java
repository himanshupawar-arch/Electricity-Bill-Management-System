import java.sql.SQLException;

/** Unit 1 & 2 : base class; subclasses override calculateBill(). */
public abstract class Consumer implements Payable {
    protected int id;
    protected String name, address, phone;

    public Consumer(int id, String name, String address, String phone) {
        this.id = id; this.name = name; this.address = address; this.phone = phone;
    }
    public int getId()          { return id; }
    public String getName()     { return name; }
    public String getAddress()  { return address; }
    public String getPhone()    { return phone; }

    public abstract String getType();
    public abstract double calculateBill(int units);

    /** Slab calculation using the Math class: 0-100, 101-300, above 300. */
    protected static double slab(int units, double r1, double r2, double r3) {
        double s1 = Math.min(units, 100);
        double s2 = Math.max(Math.min(units, 300) - 100, 0);
        double s3 = Math.max(units - 300, 0);
        return s1 * r1 + s2 * r2 + s3 * r3;
    }

    @Override
    public void pay(int billId) throws SQLException {
        if (Database.recordPayment(billId) == 0)
            throw new SQLException("Bill not found or already paid.");
    }

    @Override public String toString() { return id + " - " + name + " (" + getType() + ")"; }
}
