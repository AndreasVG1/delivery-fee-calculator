package andvil.feeCalculator.weather.importing;

/** Indicates that the weather portal could not provide an observation document. */
public class WeatherClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates an exception with a descriptive message. */
    public WeatherClientException(String message) {
        super(message);
    }

    /** Creates an exception with a descriptive message and original cause. */
    public WeatherClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
