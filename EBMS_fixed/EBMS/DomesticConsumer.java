public class DomesticConsumer extends Consumer {
    public DomesticConsumer(int id, String n, String a, String p) { super(id, n, a, p); }
    @Override public String getType() { return "DOMESTIC"; }
    @Override public double calculateBill(int units) { return 50 + slab(units, 3, 5, 7); }
}
