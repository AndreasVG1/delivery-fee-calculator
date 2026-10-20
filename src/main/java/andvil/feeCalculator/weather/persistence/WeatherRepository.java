package andvil.feeCalculator.weather.persistence;

import java.util.List;

import andvil.feeCalculator.weather.model.WeatherObservation;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for weather observations. */
public interface WeatherRepository extends JpaRepository<WeatherObservation, Long> {

    /**
     * Returns all observations with the given WMO code in ascending observation-time order.
     *
     * @param wmoCode station WMO code to search for
     * @return the station's observations, oldest first
     */
    List<WeatherObservation> findAllByWmoCodeOrderByObservedAtAsc(String wmoCode);

    /** Returns whether a station WMO code and observation time have already been imported. */
    boolean existsByWmoCodeAndObservedAt(String wmoCode, java.time.OffsetDateTime observedAt);

    /**
     * Returns observations for the station whose observation times are between {@code from}
     * and {@code to}, including both endpoints. The result may be empty and has no guaranteed order.
     */
    List<WeatherObservation> findAllByStationNameAndObservedAtBetween(
            String stationName, java.time.OffsetDateTime from, java.time.OffsetDateTime to);
}
