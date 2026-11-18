public class CommercialConsumer extends Consumer {
    public CommercialConsumer(int id, String n, String a, String p) { super(id, n, a, p); }
    @Override public String getType() { return "COMMERCIAL"; }
    @Override public double calculateBill(int units) { return 150 + slab(units, 6, 8, 10); }
}
