package andvil.feeCalculator.web;

import andvil.feeCalculator.fee.City;
import andvil.feeCalculator.fee.DeliveryFeeCalculation;
import andvil.feeCalculator.fee.DeliveryFeeService;
import andvil.feeCalculator.fee.VehicleType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes delivery fee calculations over HTTP. */
@RestController
@RequestMapping("/api/delivery-fee")
public class DeliveryFeeController {
    private static final String NO_WEATHER = "No weather data available for the requested time";
    private final DeliveryFeeService deliveryFeeService;

    /** Creates the endpoint with the delivery fee calculation service. */
    public DeliveryFeeController(DeliveryFeeService deliveryFeeService) {
        this.deliveryFeeService = deliveryFeeService;
    }

    /** Returns a saved or newly calculated fee for the city, vehicle, and optional ISO-8601 offset datetime. */
    @GetMapping()
    public ResponseEntity<?> calculate(@RequestParam(required = false) String city,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String datetime) {
        City parsedCity = parseEnum(city, City.class);
        VehicleType parsedVehicle = parseEnum(vehicleType, VehicleType.class);
        OffsetDateTime requestedAt = parseDateTime(datetime);
        if (parsedCity == null || parsedVehicle == null || (datetime != null && requestedAt == null)) {
            return ResponseEntity.badRequest().body(new InvalidRequestResponse("Invalid or missing request parameter"));
        }

        DeliveryFeeCalculation calculation = deliveryFeeService.calculate(parsedCity, parsedVehicle, requestedAt);
        if (calculation.getErrorMessage() != null) {
            HttpStatus status = NO_WEATHER.equals(calculation.getErrorMessage()) ? HttpStatus.NOT_FOUND
                    : HttpStatus.UNPROCESSABLE_CONTENT;
            return ResponseEntity.status(status).body(new ErrorResponse(calculation.getId(), calculation.getErrorMessage()));
        }
        return ResponseEntity.ok(new FeeResponse(calculation.getId(), calculation.getCity(), calculation.getVehicleType(),
                calculation.getRequestedAt(), calculation.getTotalFee()));
    }

    /** Returns the original values of a saved calculation. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCalculation(@PathVariable long id) {
        try {
            DeliveryFeeCalculation calculation = deliveryFeeService.getCalculation(id);
            return ResponseEntity.ok(new CalculationResponse(calculation.getId(), calculation.getRequestedAt(),
                    calculation.getCalculatedAt(), calculation.getCity(), calculation.getVehicleType(),
                    calculation.getWeatherObservation() == null ? null : calculation.getWeatherObservation().getId(),
                    calculation.getRbf(), calculation.getAtef(), calculation.getWsef(), calculation.getWpef(),
                    calculation.getTotalFee(), calculation.getErrorMessage()));
        } catch (NoSuchElementException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private record FeeResponse(Long id, City city, VehicleType vehicleType, OffsetDateTime requestedAt, BigDecimal totalFee) { }
    private record CalculationResponse(Long id, OffsetDateTime requestedAt, OffsetDateTime calculatedAt, City city,
            VehicleType vehicleType, Long weatherObservationId, BigDecimal rbf, BigDecimal atef, BigDecimal wsef,
            BigDecimal wpef, BigDecimal totalFee, String errorMessage) { }
    private record ErrorResponse(Long id, String error) { }
    private record InvalidRequestResponse(String error) { }
}
