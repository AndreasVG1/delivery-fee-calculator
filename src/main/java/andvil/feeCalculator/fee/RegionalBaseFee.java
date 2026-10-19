package andvil.feeCalculator.fee;

import java.math.BigDecimal;

import jakarta.persistence.*;

/** Regional base amount for a city and vehicle type. */
@Entity
@Table(name = "regional_base_fee")
public class RegionalBaseFee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private City city;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Column(name = "fee_amount", nullable = false) private BigDecimal feeAmount;
    protected RegionalBaseFee() {}
    /** Returns the city associated with this base fee. */
    public City getCity() { return city; }
    /** Returns the vehicle type associated with this base fee. */
    public VehicleType getVehicleType() { return vehicleType; }
    /** Returns the base amount in euros. */
    public BigDecimal getFeeAmount() { return feeAmount; }
}
