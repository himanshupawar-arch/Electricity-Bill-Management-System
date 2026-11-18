/** Unit 1 : meter reading entry. */
public class Meter {
    private final int consumerId, previous, current;

    public Meter(int consumerId, int previous, int current) {
        this.consumerId = consumerId; this.previous = previous; this.current = current;
    }
    public void validate() throws InvalidReadingException {
        if (current < 0)
            throw new InvalidReadingException("Reading cannot be negative.");
        if (current < previous)
            throw new InvalidReadingException(
                "Current reading (" + current + ") is less than previous reading (" + previous + ").");
    }
    public int getConsumerId() { return consumerId; }
    public int getPrevious()   { return previous; }
    public int getCurrent()    { return current; }
    public int getUnits()      { return current - previous; }
}
