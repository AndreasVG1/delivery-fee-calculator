package andvil.feeCalculator.fee.persistence;

import java.util.Optional;

import andvil.feeCalculator.fee.model.City;
import andvil.feeCalculator.fee.model.RegionalBaseFee;
import andvil.feeCalculator.fee.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores regional base fees. */
public interface RegionalBaseFeeRepository extends JpaRepository<RegionalBaseFee, Long> {
    /** Returns the unique base fee for a city and vehicle, or empty when none is configured. */
    Optional<RegionalBaseFee> findByCityAndVehicleType(City city, VehicleType vehicleType);
}
