package andvil.feeCalculator.fee.model;

import java.math.BigDecimal;

import jakarta.persistence.*;

/**
 * Temperature range and surcharge for a vehicle. Bounds are in degrees Celsius; a null bound
 * leaves that side unbounded. The configuration service validates amounts and overlap before saving.
 */
@Entity
@Table(name = "temperature_fee_rule")
public class TemperatureFeeRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Column(name = "lower_threshold") private BigDecimal lowerThreshold;
    @Column(name = "upper_threshold") private BigDecimal upperThreshold;
    @Column(name = "lower_inclusive", nullable = false) private boolean lowerInclusive;
    @Column(name = "upper_inclusive", nullable = false) private boolean upperInclusive;
    @Column(name = "fee_amount", nullable = false) private BigDecimal feeAmount;
    protected TemperatureFeeRule() {}
    /** Creates a temperature rule with the supplied bounds and amount. */
    public TemperatureFeeRule(VehicleType vehicleType, BigDecimal lowerThreshold, BigDecimal upperThreshold,
                              boolean lowerInclusive, boolean upperInclusive, BigDecimal feeAmount) {
        replace(vehicleType, lowerThreshold, upperThreshold, lowerInclusive, upperInclusive, feeAmount);
    }
    /** Returns the persistent ID. */
    public Long getId() { return id; }
    /** Replaces all configurable fields without validating the range or amount. */
    public void replace(VehicleType vehicleType, BigDecimal lowerThreshold, BigDecimal upperThreshold,
                        boolean lowerInclusive, boolean upperInclusive, BigDecimal feeAmount) {
        this.vehicleType = vehicleType;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        this.feeAmount = feeAmount;
    }
    /** Returns the vehicle type to which this rule applies. */
    public VehicleType getVehicleType() { return vehicleType; }
    /** Returns the lower temperature bound, or null when unbounded. */
    public BigDecimal getLowerThreshold() { return lowerThreshold; }
    /** Returns the upper temperature bound, or null when unbounded. */
    public BigDecimal getUpperThreshold() { return upperThreshold; }
    /** Returns whether the lower bound is included. */
    public boolean isLowerInclusive() { return lowerInclusive; }
    /** Returns whether the upper bound is included. */
    public boolean isUpperInclusive() { return upperInclusive; }
    /** Returns the extra fee in euros when this range matches. */
    public BigDecimal getFeeAmount() { return feeAmount; }
}
