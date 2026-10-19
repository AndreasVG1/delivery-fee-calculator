package andvil.feeCalculator.weather;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A weather observation recorded at a station at a specific time. */
@Entity
@Table(name = "weather_observation")
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "wmo_code", nullable = false)
    private String wmoCode;

    @Column(name = "air_temperature", nullable = false, precision = 8, scale = 3)
    private BigDecimal airTemperature;

    @Column(name = "wind_speed", nullable = false, precision = 8, scale = 3)
    private BigDecimal windSpeed;

    @Column(nullable = false)
    private String phenomenon;

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;

    protected WeatherObservation() {
    }

    /** Creates a weather observation with the values returned by the weather source. */
    public WeatherObservation(String stationName, String wmoCode, BigDecimal airTemperature,
            BigDecimal windSpeed, String phenomenon, OffsetDateTime observedAt) {
        this.stationName = stationName;
        this.wmoCode = wmoCode;
        this.airTemperature = airTemperature;
        this.windSpeed = windSpeed;
        this.phenomenon = phenomenon;
        this.observedAt = observedAt;
    }

    /** Returns the generated observation identifier. */
    public Long getId() {
        return id;
    }

    /** Returns the station name. */
    public String getStationName() {
        return stationName;
    }

    /** Returns the station WMO code. */
    public String getWmoCode() {
        return wmoCode;
    }

    /** Returns the air temperature in degrees Celsius. */
    public BigDecimal getAirTemperature() {
        return airTemperature;
    }

    /** Returns the wind speed in metres per second. */
    public BigDecimal getWindSpeed() {
        return windSpeed;
    }

    /** Returns the exact phenomenon value supplied by the weather source. */
    public String getPhenomenon() {
        return phenomenon;
    }

    /** Returns the time at which the observation was recorded. */
    public OffsetDateTime getObservedAt() {
        return observedAt;
    }
}
