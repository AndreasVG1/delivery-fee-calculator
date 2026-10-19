package andvil.feeCalculator.fee;

import java.math.BigDecimal;
import jakarta.persistence.*;

/** Wind-speed range with a surcharge or forbidden outcome for a vehicle type. */
@Entity
@Table(name = "wind_fee_rule")
public class WindFeeRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Column(name = "lower_threshold") private BigDecimal lowerThreshold;
    @Column(name = "upper_threshold") private BigDecimal upperThreshold;
    @Column(name = "lower_inclusive", nullable = false) private boolean lowerInclusive;
    @Column(name = "upper_inclusive", nullable = false) private boolean upperInclusive;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RuleOutcome outcome;
    @Column(name = "fee_amount", nullable = false) private BigDecimal feeAmount;
    protected WindFeeRule() {}
    /** Creates a wind rule with the supplied bounds and outcome. */
    WindFeeRule(VehicleType vehicleType, BigDecimal lowerThreshold, BigDecimal upperThreshold,
            boolean lowerInclusive, boolean upperInclusive, RuleOutcome outcome, BigDecimal feeAmount) {
        replace(vehicleType, lowerThreshold, upperThreshold, lowerInclusive, upperInclusive, outcome, feeAmount);
    }
    /** Returns the persistent ID. */
    public Long getId() { return id; }
    /** Replaces all configurable fields. */
    void replace(VehicleType vehicleType, BigDecimal lowerThreshold, BigDecimal upperThreshold,
            boolean lowerInclusive, boolean upperInclusive, RuleOutcome outcome, BigDecimal feeAmount) {
        this.vehicleType = vehicleType;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        this.outcome = outcome;
        this.feeAmount = feeAmount;
    }
    /** Returns the vehicle type to which this rule applies. */
    public VehicleType getVehicleType() { return vehicleType; }
    /** Returns the lower wind-speed bound, or null when unbounded. */
    public BigDecimal getLowerThreshold() { return lowerThreshold; }
    /** Returns the upper wind-speed bound, or null when unbounded. */
    public BigDecimal getUpperThreshold() { return upperThreshold; }
    /** Returns whether the lower bound is included. */
    public boolean isLowerInclusive() { return lowerInclusive; }
    /** Returns whether the upper bound is included. */
    public boolean isUpperInclusive() { return upperInclusive; }
    /** Returns whether the rule applies a surcharge or forbids delivery. */
    public RuleOutcome getOutcome() { return outcome; }
    /** Returns the extra fee in euros when the outcome is a surcharge. */
    public BigDecimal getFeeAmount() { return feeAmount; }
}
