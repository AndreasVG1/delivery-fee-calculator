package andvil.feeCalculator.fee.application;

/** An invalid or conflicting fee configuration request. */
public class ConfigurationException extends RuntimeException {
    private final int status;

    /** Creates an API error with its HTTP status and display message. */
    public ConfigurationException(int status, String message) {
        super(message);
        this.status = status;
    }

    /** Returns the HTTP status associated with this error. */
    public int getStatus() {
        return status;
    }
}
