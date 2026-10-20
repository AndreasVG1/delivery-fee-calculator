package andvil.feeCalculator.fee.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import andvil.feeCalculator.fee.model.City;
import andvil.feeCalculator.fee.model.DeliveryFeeCalculation;
import andvil.feeCalculator.fee.model.VehicleType;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Append and read access to recorded delivery fee calculations. */
public interface DeliveryFeeCalculationRepository extends Repository<DeliveryFeeCalculation, Long> {
    /** Persists a new calculation outcome, including a failed outcome. */
    DeliveryFeeCalculation save(DeliveryFeeCalculation calculation);

    /**
     * Finds successful calculations for the city and vehicle whose selected observation time is
     * between {@code from} and {@code to}, inclusive. The result has no guaranteed order.
     */
    @Query("select c from DeliveryFeeCalculation c join fetch c.weatherObservation o where c.city = :city and c.vehicleType = :vehicleType and c.errorMessage is null and o.observedAt between :from and :to")
    List<DeliveryFeeCalculation> findSuccessfulInWindow(@Param("city") City city, @Param("vehicleType") VehicleType vehicleType,
                                                        @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Returns a recorded calculation with its observation loaded, or empty for an unknown ID. */
    @Query("select c from DeliveryFeeCalculation c left join fetch c.weatherObservation where c.id = :id")
    Optional<DeliveryFeeCalculation> findWithObservationById(@Param("id") Long id);
}
