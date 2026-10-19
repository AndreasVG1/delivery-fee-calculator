package andvil.feeCalculator.weather;

/** Indicates that a weather feed could not be parsed or imported. */
public class WeatherImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates an import exception with a descriptive message. */
    public WeatherImportException(String message) {
        super(message);
    }

    /** Creates an import exception with a descriptive message and original cause. */
    public WeatherImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
