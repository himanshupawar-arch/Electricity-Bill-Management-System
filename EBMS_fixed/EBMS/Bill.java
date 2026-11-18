import java.sql.Date;

/** Unit 1 : Bill entity. */
public class Bill {
    private final int billId, consumerId, units;
    private final String consumerName, status;
    private final double amount;
    private final Date billDate, dueDate;

    public Bill(int billId, int consumerId, String consumerName, int units,
                double amount, Date billDate, Date dueDate, String status) {
        this.billId = billId; this.consumerId = consumerId; this.consumerName = consumerName;
        this.units = units; this.amount = amount; this.billDate = billDate;
        this.dueDate = dueDate; this.status = status;
    }
    public int getBillId()          { return billId; }
    public int getConsumerId()      { return consumerId; }
    public String getConsumerName() { return consumerName; }
    public int getUnits()           { return units; }
    public double getAmount()       { return amount; }
    public Date getBillDate()       { return billDate; }
    public Date getDueDate()        { return dueDate; }
    public String getStatus()       { return status; }

    @Override public String toString() {
        return "Bill #" + billId + " - " + consumerName + " - Rs." + String.format("%.2f", amount)
             + " (due " + dueDate + ")";
    }
}
