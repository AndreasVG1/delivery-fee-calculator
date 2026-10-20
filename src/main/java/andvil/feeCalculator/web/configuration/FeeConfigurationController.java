package andvil.feeCalculator.web.configuration;

import andvil.feeCalculator.fee.model.City;
import andvil.feeCalculator.fee.application.ConfigurationException;
import andvil.feeCalculator.fee.application.FeeConfigurationService;
import andvil.feeCalculator.fee.model.PhenomenonFeeRule;
import andvil.feeCalculator.fee.model.RegionalBaseFee;
import andvil.feeCalculator.fee.model.RuleOutcome;
import andvil.feeCalculator.fee.model.TemperatureFeeRule;
import andvil.feeCalculator.fee.model.VehicleType;
import andvil.feeCalculator.fee.model.WeatherPhenomenon;
import andvil.feeCalculator.fee.model.WindFeeRule;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD REST interface for fee configuration. Creates return 201 with a Location header,
 * updates return 200, and deletes return 204. Invalid data returns 400, unknown IDs
 * return 404, and duplicate or overlapping rules return 409.
 */
@RestController
@RequestMapping("/api/configurations")
public class FeeConfigurationController {
    private final FeeConfigurationService service;

    /** Creates the controller with the fee configuration service. */
    public FeeConfigurationController(FeeConfigurationService service) {
        this.service = service;
    }

    /**
     * Request body for a regional base fee.
     *
     * @param city delivery city
     * @param vehicleType delivery vehicle
     * @param feeAmount non-negative base fee in euros
     */
    public record BaseRequest(City city, VehicleType vehicleType, BigDecimal feeAmount) {}

    /**
     * Temperature rule body. Null thresholds are unbounded, but both cannot be null.
     *
     * @param vehicleType delivery vehicle
     * @param lowerThreshold lower temperature bound in degrees Celsius, or null
     * @param upperThreshold upper temperature bound in degrees Celsius, or null
     * @param lowerInclusive whether the lower bound is included
     * @param upperInclusive whether the upper bound is included
     * @param feeAmount non-negative surcharge in euros
     */
    public record TemperatureRequest(VehicleType vehicleType, BigDecimal lowerThreshold,
            BigDecimal upperThreshold, Boolean lowerInclusive, Boolean upperInclusive,
            BigDecimal feeAmount) {}

    /**
     * Wind rule body. Null thresholds are unbounded; forbidden rules require a zero fee.
     *
     * @param vehicleType delivery vehicle
     * @param lowerThreshold lower wind-speed bound in metres per second, or null
     * @param upperThreshold upper wind-speed bound in metres per second, or null
     * @param lowerInclusive whether the lower bound is included
     * @param upperInclusive whether the upper bound is included
     * @param outcome surcharge or forbidden delivery
     * @param feeAmount non-negative surcharge in euros, zero when forbidden
     */
    public record WindRequest(VehicleType vehicleType, BigDecimal lowerThreshold,
            BigDecimal upperThreshold, Boolean lowerInclusive, Boolean upperInclusive,
            RuleOutcome outcome, BigDecimal feeAmount) {}

    /**
     * Phenomenon rule body. Forbidden rules require a zero fee.
     *
     * @param vehicleType delivery vehicle
     * @param phenomenon category matched against provider descriptions
     * @param outcome surcharge or forbidden delivery
     * @param feeAmount non-negative surcharge in euros, zero when forbidden
     */
    public record PhenomenonRequest(VehicleType vehicleType, WeatherPhenomenon phenomenon,
            RuleOutcome outcome, BigDecimal feeAmount) {}

    /** Lists regional base fees. */
    @GetMapping("/base-fees")
    public List<RegionalBaseFee> base() {
        return service.baseFees();
    }

    /** Creates a regional base fee. */
    @PostMapping("/base-fees")
    public ResponseEntity<RegionalBaseFee> createBase(@RequestBody BaseRequest request) {
        RegionalBaseFee fee = service.createBase(request.city(), request.vehicleType(), request.feeAmount());
        return created("base-fees", fee.getId(), fee);
    }

    /** Replaces a regional base fee. */
    @PutMapping("/base-fees/{id}")
    public RegionalBaseFee updateBase(@PathVariable long id, @RequestBody BaseRequest request) {
        return service.updateBase(id, request.city(), request.vehicleType(), request.feeAmount());
    }

    /** Lists temperature rules. */
    @GetMapping("/temperature-rules")
    public List<TemperatureFeeRule> temperatures() {
        return service.temperatureRules();
    }

    /** Creates a temperature rule. */
    @PostMapping("/temperature-rules")
    public ResponseEntity<TemperatureFeeRule> createTemperature(@RequestBody TemperatureRequest request) {
        TemperatureFeeRule rule = service.createTemperature(request.vehicleType(), request.lowerThreshold(),
                request.upperThreshold(), bool(request.lowerInclusive()), bool(request.upperInclusive()),
                request.feeAmount());
        return created("temperature-rules", rule.getId(), rule);
    }

    /** Replaces a temperature rule. */
    @PutMapping("/temperature-rules/{id}")
    public TemperatureFeeRule updateTemperature(@PathVariable long id,
            @RequestBody TemperatureRequest request) {
        return service.updateTemperature(id, request.vehicleType(), request.lowerThreshold(),
                request.upperThreshold(), bool(request.lowerInclusive()), bool(request.upperInclusive()),
                request.feeAmount());
    }

    /** Lists wind rules. */
    @GetMapping("/wind-rules")
    public List<WindFeeRule> winds() {
        return service.windRules();
    }

    /** Creates a wind rule. */
    @PostMapping("/wind-rules")
    public ResponseEntity<WindFeeRule> createWind(@RequestBody WindRequest request) {
        WindFeeRule rule = service.createWind(request.vehicleType(), request.lowerThreshold(),
                request.upperThreshold(), bool(request.lowerInclusive()), bool(request.upperInclusive()),
                request.outcome(), request.feeAmount());
        return created("wind-rules", rule.getId(), rule);
    }

    /** Replaces a wind rule. */
    @PutMapping("/wind-rules/{id}")
    public WindFeeRule updateWind(@PathVariable long id, @RequestBody WindRequest request) {
        return service.updateWind(id, request.vehicleType(), request.lowerThreshold(),
                request.upperThreshold(), bool(request.lowerInclusive()), bool(request.upperInclusive()),
                request.outcome(), request.feeAmount());
    }

    /** Lists phenomenon rules. */
    @GetMapping("/phenomenon-rules")
    public List<PhenomenonFeeRule> phenomena() {
        return service.phenomenonRules();
    }

    /** Creates a phenomenon rule. */
    @PostMapping("/phenomenon-rules")
    public ResponseEntity<PhenomenonFeeRule> createPhenomenon(@RequestBody PhenomenonRequest request) {
        PhenomenonFeeRule rule = service.createPhenomenon(request.vehicleType(), request.phenomenon(),
                request.outcome(), request.feeAmount());
        return created("phenomenon-rules", rule.getId(), rule);
    }

    /** Replaces a phenomenon rule. */
    @PutMapping("/phenomenon-rules/{id}")
    public PhenomenonFeeRule updatePhenomenon(@PathVariable long id,
            @RequestBody PhenomenonRequest request) {
        return service.updatePhenomenon(id, request.vehicleType(), request.phenomenon(),
                request.outcome(), request.feeAmount());
    }

    /** Returns a base fee by ID. */
    @GetMapping("/base-fees/{id}")
    public RegionalBaseFee getBase(@PathVariable long id) {
        return service.getBase(id);
    }

    /** Returns a temperature rule by ID. */
    @GetMapping("/temperature-rules/{id}")
    public TemperatureFeeRule getTemperature(@PathVariable long id) {
        return service.getTemperature(id);
    }

    /** Returns a wind rule by ID. */
    @GetMapping("/wind-rules/{id}")
    public WindFeeRule getWind(@PathVariable long id) {
        return service.getWind(id);
    }

    /** Returns a phenomenon rule by ID. */
    @GetMapping("/phenomenon-rules/{id}")
    public PhenomenonFeeRule getPhenomenon(@PathVariable long id) {
        return service.getPhenomenon(id);
    }

    /** Deletes a base fee. */
    @DeleteMapping("/base-fees/{id}")
    public ResponseEntity<Void> deleteBase(@PathVariable long id) {
        service.deleteBase(id);
        return ResponseEntity.noContent().build();
    }

    /** Deletes a temperature rule. */
    @DeleteMapping("/temperature-rules/{id}")
    public ResponseEntity<Void> deleteTemperature(@PathVariable long id) {
        service.deleteTemperature(id);
        return ResponseEntity.noContent().build();
    }

    /** Deletes a wind rule. */
    @DeleteMapping("/wind-rules/{id}")
    public ResponseEntity<Void> deleteWind(@PathVariable long id) {
        service.deleteWind(id);
        return ResponseEntity.noContent().build();
    }

    /** Deletes a phenomenon rule. */
    @DeleteMapping("/phenomenon-rules/{id}")
    public ResponseEntity<Void> deletePhenomenon(@PathVariable long id) {
        service.deletePhenomenon(id);
        return ResponseEntity.noContent().build();
    }

    private boolean bool(Boolean value) {
        if (value == null) {
            throw new ConfigurationException(400, "Inclusivity flags are required");
        }
        return value;
    }

    private <T> ResponseEntity<T> created(String kind, Long id, T body) {
        URI location = URI.create("/api/configurations/" + kind + "/" + id);
        return ResponseEntity.created(location).body(body);
    }
}
