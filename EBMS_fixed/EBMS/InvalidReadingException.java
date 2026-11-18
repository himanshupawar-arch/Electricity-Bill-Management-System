/** Unit 3 : user-defined checked exception for wrong meter readings. */
public class InvalidReadingException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidReadingException(String message) { super(message); }
}
