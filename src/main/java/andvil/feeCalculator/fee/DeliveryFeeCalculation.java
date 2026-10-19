package andvil.feeCalculator.fee;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import andvil.feeCalculator.weather.WeatherObservation;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** An immutable saved outcome of a delivery fee request. */
@Entity
@Immutable
@Table(name = "delivery_fee_calculation")
public class DeliveryFeeCalculation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "requested_at") private OffsetDateTime requestedAt;
    @Column(name = "calculated_at", nullable = false) private OffsetDateTime calculatedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private City city;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "weather_observation_id") private WeatherObservation weatherObservation;
    private BigDecimal rbf;
    private BigDecimal atef;
    private BigDecimal wsef;
    private BigDecimal wpef;
    @Column(name = "total_fee") private BigDecimal totalFee;
    @Column(name = "error_message") private String errorMessage;

    protected DeliveryFeeCalculation() { }

    DeliveryFeeCalculation(OffsetDateTime requestedAt, OffsetDateTime calculatedAt, City city,
            VehicleType vehicleType, WeatherObservation observation, BigDecimal rbf, BigDecimal atef,
            BigDecimal wsef, BigDecimal wpef, BigDecimal totalFee, String errorMessage) {
        this.requestedAt = requestedAt; this.calculatedAt = calculatedAt; this.city = city;
        this.vehicleType = vehicleType; this.weatherObservation = observation; this.rbf = rbf;
        this.atef = atef; this.wsef = wsef; this.wpef = wpef; this.totalFee = totalFee;
        this.errorMessage = errorMessage;
    }
    /** Returns the generated record identifier. */
    public Long getId() { return id; }
    /** Returns the requested time, or null when the caller requested current conditions. */
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    /** Returns when this calculation was performed. */
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    /** Returns the requested city. */
    public City getCity() { return city; }
    /** Returns the requested vehicle type. */
    public VehicleType getVehicleType() { return vehicleType; }
    /** Returns the selected weather observation when one was found. */
    public WeatherObservation getWeatherObservation() { return weatherObservation; }
    /** Returns the regional base fee. */
    public BigDecimal getRbf() { return rbf; }
    /** Returns the air temperature extra fee. */
    public BigDecimal getAtef() { return atef; }
    /** Returns the wind speed extra fee. */
    public BigDecimal getWsef() { return wsef; }
    /** Returns the weather phenomenon extra fee. */
    public BigDecimal getWpef() { return wpef; }
    /** Returns the total fee, or null for a failed calculation. */
    public BigDecimal getTotalFee() { return totalFee; }
    /** Returns the failure reason, or null when calculation succeeded. */
    public String getErrorMessage() { return errorMessage; }
}
