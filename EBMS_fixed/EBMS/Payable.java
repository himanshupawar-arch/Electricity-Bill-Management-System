import java.sql.SQLException;

/** Unit 2 : interface implemented by Consumer. */
public interface Payable {
    void pay(int billId) throws SQLException;
}
