package andvil.feeCalculator.web.configuration;

import andvil.feeCalculator.fee.application.ConfigurationException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps configuration request failures to the configuration API error shape. */
@RestControllerAdvice(assignableTypes = FeeConfigurationController.class)
public class ConfigurationErrorHandler {
    /** Returns the configured 400, 404, or 409 status with an {@code error} message body. */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<Map<String, String>> configuration(ConfigurationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("error", exception.getMessage()));
    }

    /** Returns 400 with an {@code error} message for malformed configuration request bodies. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformed(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Malformed or invalid configuration data"));
    }
}
