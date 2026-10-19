package andvil.feeCalculator.weather;

import java.util.List;

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

    /** Returns whether the observation key already exists. */
    boolean existsByWmoCodeAndObservedAt(String wmoCode, java.time.OffsetDateTime observedAt);
}
