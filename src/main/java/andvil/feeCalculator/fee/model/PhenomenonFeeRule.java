package andvil.feeCalculator.fee.model;

import java.math.BigDecimal;

import jakarta.persistence.*;

/** Fee outcome for a vehicle type and weather-phenomenon category. */
@Entity
@Table(name = "phenomenon_fee_rule")
public class PhenomenonFeeRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WeatherPhenomenon phenomenon;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RuleOutcome outcome;
    @Column(name = "fee_amount", nullable = false) private BigDecimal feeAmount;
    protected PhenomenonFeeRule() {}
    /** Creates a phenomenon rule for the supplied vehicle and condition. */
    public PhenomenonFeeRule(VehicleType vehicleType, WeatherPhenomenon phenomenon,
                             RuleOutcome outcome, BigDecimal feeAmount) {
        replace(vehicleType, phenomenon, outcome, feeAmount);
    }
    /** Returns the persistent ID. */
    public Long getId() { return id; }
    /** Replaces all configurable fields. */
    public void replace(VehicleType vehicleType, WeatherPhenomenon phenomenon,
                        RuleOutcome outcome, BigDecimal feeAmount) {
        this.vehicleType = vehicleType;
        this.phenomenon = phenomenon;
        this.outcome = outcome;
        this.feeAmount = feeAmount;
    }
    /** Returns the vehicle type to which this rule applies. */
    public VehicleType getVehicleType() { return vehicleType; }
    /** Returns the matched weather-phenomenon category. */
    public WeatherPhenomenon getPhenomenon() { return phenomenon; }
    /** Returns whether the rule applies a surcharge or forbids delivery. */
    public RuleOutcome getOutcome() { return outcome; }
    /** Returns the extra fee in euros when the outcome is a surcharge. */
    public BigDecimal getFeeAmount() { return feeAmount; }
}
